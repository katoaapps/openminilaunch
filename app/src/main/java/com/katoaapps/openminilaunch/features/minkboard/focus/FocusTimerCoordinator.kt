package com.katoaapps.openminilaunch.features.minkboard.focus

import com.katoaapps.minkboard.core.FocusTimerRules
import com.katoaapps.minkboard.core.FocusTimerStore
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.FocusSession
import com.katoaapps.minkboard.model.FocusSessionState
import com.katoaapps.minkboard.model.FocusTimerId
import com.katoaapps.minkboard.model.FocusTimerSnapshot
import java.util.UUID

internal enum class FocusTimerSchedulePrecision { Exact, Inexact }

internal interface FocusTimerScheduler {
    fun schedule(session: FocusSession): FocusTimerSchedulePrecision
    fun cancel(session: FocusSession)
}

internal interface FocusTimerNotifier {
    fun showRunning(snapshot: FocusTimerSnapshot)
    fun showFinished(snapshot: FocusTimerSnapshot, alert: Boolean)
    fun cancelAll()
}

internal sealed interface StartFocusTimerResult {
    data class Started(
        val snapshot: FocusTimerSnapshot,
        val precision: FocusTimerSchedulePrecision,
    ) : StartFocusTimerResult

    data class ActiveTimerExists(val snapshot: FocusTimerSnapshot) : StartFocusTimerResult
}

/**
 * Launcher host adapter matching MinkBoard Pro's durable timer behavior. Room is the source of
 * truth; alarm, service, and notification state can always be reconstructed from it.
 */
internal class FocusTimerCoordinator(
    private val store: FocusTimerStore,
    private val scheduler: FocusTimerScheduler,
    private val notifier: FocusTimerNotifier,
    private val clockMillis: () -> Long = System::currentTimeMillis,
    private val idFactory: () -> FocusTimerId = { FocusTimerId(UUID.randomUUID().toString()) },
) {
    suspend fun start(
        boardId: BoardId,
        cardId: CardId,
        durationMillis: Long,
        replaceCurrent: Boolean = false,
    ): StartFocusTimerResult {
        val validatedDuration = FocusTimerRules.durationMillis(durationMillis)
        val current = store.currentFocusTimer()
        if (current?.session?.state == FocusSessionState.Running && !replaceCurrent) {
            return StartFocusTimerResult.ActiveTimerExists(current)
        }

        val startedAt = clockMillis()
        val session = FocusSession(
            id = idFactory(),
            boardId = boardId,
            cardId = cardId,
            startedAtEpochMillis = startedAt,
            endsAtEpochMillis = Math.addExact(startedAt, validatedDuration),
            state = FocusSessionState.Running,
        )
        val snapshot = store.replaceFocusTimer(session)
        current?.let { scheduler.cancel(it.session) }
        notifier.cancelAll()
        val precision = scheduler.schedule(snapshot.session)
        notifier.showRunning(snapshot)
        return StartFocusTimerResult.Started(snapshot, precision)
    }

    suspend fun stop(timerId: FocusTimerId): Boolean {
        val current = store.currentFocusTimer()
            ?.takeIf { it.session.id == timerId }
            ?: return false
        val cleared = store.clearFocusTimer(timerId)
        if (cleared) {
            scheduler.cancel(current.session)
            notifier.cancelAll()
        }
        return cleared
    }

    suspend fun dismiss(timerId: FocusTimerId): Boolean = stop(timerId)

    suspend fun complete(timerId: FocusTimerId): Boolean {
        val current = store.currentFocusTimer()
            ?.takeIf { it.session.id == timerId }
            ?: return false
        if (current.session.state == FocusSessionState.Finished) return false
        if (clockMillis() < current.session.endsAtEpochMillis) {
            scheduler.schedule(current.session)
            notifier.showRunning(current)
            return false
        }

        val finished = store.finishFocusTimer(timerId) ?: return false
        scheduler.cancel(current.session)
        notifier.showFinished(finished, alert = true)
        return true
    }

    suspend fun reconcile() {
        val current = store.currentFocusTimer()
        if (current == null) {
            notifier.cancelAll()
            return
        }
        when (current.session.state) {
            FocusSessionState.Running -> {
                if (clockMillis() >= current.session.endsAtEpochMillis) {
                    complete(current.session.id)
                } else {
                    scheduler.schedule(current.session)
                    notifier.showRunning(current)
                }
            }

            FocusSessionState.Finished -> {
                scheduler.cancel(current.session)
                notifier.showFinished(current, alert = false)
            }
        }
    }
}
