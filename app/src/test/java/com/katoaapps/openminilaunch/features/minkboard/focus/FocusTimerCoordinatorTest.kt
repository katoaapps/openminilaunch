package com.katoaapps.openminilaunch.features.minkboard.focus

import com.katoaapps.minkboard.core.FocusTimerStore
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.FocusSession
import com.katoaapps.minkboard.model.FocusSessionState
import com.katoaapps.minkboard.model.FocusTimerId
import com.katoaapps.minkboard.model.FocusTimerSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusTimerCoordinatorTest {
    private var now = 1_000L
    private val store = FakeStore()
    private val scheduler = FakeScheduler()
    private val notifier = FakeNotifier()
    private val coordinator = FocusTimerCoordinator(
        store = store,
        scheduler = scheduler,
        notifier = notifier,
        clockMillis = { now },
        idFactory = { FocusTimerId("new-timer") },
    )

    @Test
    fun activeTimerRequiresExplicitReplacement() = runBlocking {
        store.value = snapshot(session("old-timer", endsAt = 60_000L))

        val result = coordinator.start(BoardId("board"), CardId("card"), 5_000L)

        assertTrue(result is StartFocusTimerResult.ActiveTimerExists)
        assertEquals("old-timer", store.value?.session?.id?.value)
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun replacementCancelsOldAndStartsNew() = runBlocking {
        val old = session("old-timer", endsAt = 60_000L)
        store.value = snapshot(old)

        val result = coordinator.start(
            BoardId("board"),
            CardId("card"),
            5_000L,
            replaceCurrent = true,
        )

        assertTrue(result is StartFocusTimerResult.Started)
        assertEquals(listOf(old), scheduler.canceled)
        assertEquals("new-timer", store.value?.session?.id?.value)
        assertEquals(6_000L, store.value?.session?.endsAtEpochMillis)
        assertEquals(1, notifier.runningCount)
    }

    @Test
    fun invalidReplacementCannotDestroyTheExistingTimer() {
        val old = session("old-timer", endsAt = 60_000L)
        store.value = snapshot(old)
        store.rejectReplacement = true

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                coordinator.start(
                    BoardId("missing-board"),
                    CardId("missing-card"),
                    5_000L,
                    replaceCurrent = true,
                )
            }
        }

        assertEquals("old-timer", store.value?.session?.id?.value)
        assertTrue(scheduler.canceled.isEmpty())
    }

    @Test
    fun staleAlarmCannotCompleteCurrentTimer() = runBlocking {
        store.value = snapshot(session("current", endsAt = 900L))

        assertFalse(coordinator.complete(FocusTimerId("stale")))
        assertEquals(FocusSessionState.Running, store.value?.session?.state)
        assertEquals(0, notifier.finishedAlerts)
    }

    @Test
    fun dueTimerFinishesOnce() = runBlocking {
        store.value = snapshot(session("current", endsAt = 900L))

        assertTrue(coordinator.complete(FocusTimerId("current")))
        assertEquals(FocusSessionState.Finished, store.value?.session?.state)
        assertEquals(1, notifier.finishedAlerts)
        assertFalse(coordinator.complete(FocusTimerId("current")))
        assertEquals(1, notifier.finishedAlerts)
        assertEquals(1, notifier.finishedPresentations)
    }

    @Test
    fun reconcileClearsOrphanedRuntimeState() = runBlocking {
        coordinator.reconcile()

        assertNull(store.value)
        assertEquals(1, notifier.cancelCount)
    }

    @Test
    fun normalReconcilePreservesAndReschedulesRunningTimer() = runBlocking {
        val current = session("current", endsAt = 60_000L)
        store.value = snapshot(current)

        coordinator.reconcile()

        assertEquals(listOf(current), scheduler.scheduled)
        assertTrue(scheduler.canceled.isEmpty())
        assertEquals(1, notifier.runningCount)
        assertEquals(0, notifier.cancelCount)
        assertEquals(current, store.value?.session)
    }

    private fun session(id: String, endsAt: Long) = FocusSession(
        id = FocusTimerId(id),
        boardId = BoardId("board"),
        cardId = CardId("card"),
        startedAtEpochMillis = 0L,
        endsAtEpochMillis = endsAt,
        state = FocusSessionState.Running,
    )

    private fun snapshot(session: FocusSession) = FocusTimerSnapshot(
        session = session,
        cardTitle = "Card",
        cardDetails = "",
        boardIsLocked = false,
    )

    private class FakeStore : FocusTimerStore {
        var value: FocusTimerSnapshot? = null
        var rejectReplacement = false
        private val flow = MutableStateFlow<FocusSession?>(null)

        override fun observeFocusSession(): Flow<FocusSession?> = flow
        override suspend fun currentFocusTimer(): FocusTimerSnapshot? = value
        override suspend fun replaceFocusTimer(session: FocusSession): FocusTimerSnapshot =
            FocusTimerSnapshot(
                session = session,
                cardTitle = "Card",
                cardDetails = "",
                boardIsLocked = false,
            ).also {
                require(!rejectReplacement) { "Unknown timer target" }
                value = it
                flow.value = session
            }

        override suspend fun finishFocusTimer(timerId: FocusTimerId): FocusTimerSnapshot? {
            val current = value?.takeIf { it.session.id == timerId } ?: return null
            return current.copy(
                session = current.session.copy(state = FocusSessionState.Finished),
            ).also {
                value = it
                flow.value = it.session
            }
        }

        override suspend fun clearFocusTimer(timerId: FocusTimerId): Boolean {
            if (value?.session?.id != timerId) return false
            value = null
            flow.value = null
            return true
        }
    }

    private class FakeScheduler : FocusTimerScheduler {
        val scheduled = mutableListOf<FocusSession>()
        val canceled = mutableListOf<FocusSession>()

        override fun schedule(session: FocusSession): FocusTimerSchedulePrecision {
            scheduled += session
            return FocusTimerSchedulePrecision.Exact
        }

        override fun cancel(session: FocusSession) {
            canceled += session
        }
    }

    private class FakeNotifier : FocusTimerNotifier {
        var runningCount = 0
        var finishedAlerts = 0
        var finishedPresentations = 0
        var cancelCount = 0

        override fun showRunning(snapshot: FocusTimerSnapshot) {
            runningCount++
        }

        override fun showFinished(snapshot: FocusTimerSnapshot, alert: Boolean) {
            finishedPresentations++
            if (alert) finishedAlerts++
        }

        override fun cancelAll() {
            cancelCount++
        }
    }
}
