package com.katoaapps.minkboard.focus.android

import com.katoaapps.minkboard.core.FocusTimerRules
import com.katoaapps.minkboard.core.FocusTimerStore
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.FocusSession
import com.katoaapps.minkboard.model.FocusSessionState
import com.katoaapps.minkboard.model.FocusTimerId
import com.katoaapps.minkboard.model.FocusTimerSnapshot
import java.util.UUID

enum class MinkBoardFocusSchedulePrecision {
    Exact,
    Inexact,
}

internal interface MinkBoardFocusScheduler {
    fun schedule(session: FocusSession): MinkBoardFocusSchedulePrecision
    fun cancel(session: FocusSession)
}

internal interface MinkBoardFocusNotifier {
    fun showRunning(snapshot: FocusTimerSnapshot)
    fun showFinished(snapshot: FocusTimerSnapshot, alert: Boolean)
    fun cancelAll()
}

sealed interface StartMinkBoardFocusResult {
    data class Started(
        val snapshot: FocusTimerSnapshot,
        val precision: MinkBoardFocusSchedulePrecision,
    ) : StartMinkBoardFocusResult

    data class ActiveTimerExists(
        val snapshot: FocusTimerSnapshot,
    ) : StartMinkBoardFocusResult
}

/** Coordinates durable MinkBoard focus truth with the reusable Android execution adapters. */
class MinkBoardFocusTimerCoordinator internal constructor(
    private val store: FocusTimerStore,
    private val scheduler: MinkBoardFocusScheduler,
    private val notifier: MinkBoardFocusNotifier,
    private val clockMillis: () -> Long = System::currentTimeMillis,
    private val idFactory: () -> FocusTimerId = { FocusTimerId(UUID.randomUUID().toString()) },
) {
    suspend fun start(
        boardId: BoardId,
        cardId: CardId,
        durationMillis: Long,
        replaceCurrent: Boolean = false,
    ): StartMinkBoardFocusResult {
        val validatedDuration = FocusTimerRules.durationMillis(durationMillis)
        val current = store.currentFocusTimer()
        if (current?.session?.state == FocusSessionState.Running && !replaceCurrent) {
            return StartMinkBoardFocusResult.ActiveTimerExists(current)
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
        return StartMinkBoardFocusResult.Started(snapshot, precision)
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
