package com.katoaapps.openminilaunch.features.minkboard

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.katoaapps.openminilaunch.features.minkboard.focus.FocusTimerIntents
import com.katoaapps.minkboard.core.BoardAccessSessionController
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.FocusSessionState
import com.katoaapps.minkboard.model.FocusTimerId
import com.katoaapps.minkboard.model.StatusId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Application lifecycle boundary for OpenMink's MinkBoard integration.
 *
 * Construction, backup, focus, navigation, and launcher commands live in focused collaborators;
 * this class only exposes their application-scoped lifetime and coordinates lifecycle events.
 */
internal class OpenMinkMinkBoardRuntime(context: Context) {
    private val applicationContext = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val graph = OpenMinkMinkBoardGraph(applicationContext, applicationScope)
    private val navigationState = OpenMinkBoardNavigationState()
    private val backupGraph by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        OpenMinkMinkBoardBackupGraph(graph.launcherFacade, graph.homePreferences)
    }
    private val focusGraph by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        OpenMinkFocusTimerGraph(applicationContext, graph.repository)
    }

    val boardAccessSessions by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        BoardAccessSessionController(elapsedRealtimeMillis = SystemClock::elapsedRealtime)
    }
    private val mutableBoardAccessRevision = MutableStateFlow(0L)
    val boardAccessRevision: StateFlow<Long> = mutableBoardAccessRevision

    val navigationRequest get() = navigationState.request
    val repository get() = graph.repository
    val launcherFacade get() = graph.launcherFacade
    val legacyTodoMigration get() = graph.legacyTodoMigration
    val homePreferences get() = graph.homePreferences
    val uiPreferences get() = graph.uiPreferences
    val launcherController get() = graph.launcherController
    val backupWorkflow get() = backupGraph.workflow
    val backupCoordinator get() = backupGraph.coordinator
    val focusTimerNotifier get() = focusGraph.notifier
    val focusTimerCoordinator get() = focusGraph.coordinator

    fun onBackgrounded() {
        boardAccessSessions.onBackgrounded()
    }

    fun onForegrounded() {
        if (boardAccessSessions.onForegrounded()) markBoardAccessChanged()
    }

    fun markBoardAccessChanged() {
        mutableBoardAccessRevision.value++
    }

    fun handleNavigationIntent(intent: Intent?) {
        if (intent?.action != FocusTimerIntents.ACTION_OPEN) return
        val timerId = intent.getStringExtra(FocusTimerIntents.EXTRA_TIMER_ID)
            ?.let { value -> runCatching { FocusTimerId(value) }.getOrNull() } ?: return
        val boardId = intent.getStringExtra(FocusTimerIntents.EXTRA_BOARD_ID)
            ?.let { value -> runCatching { BoardId(value) }.getOrNull() } ?: return
        val cardId = intent.getStringExtra(FocusTimerIntents.EXTRA_CARD_ID)
            ?.let { value -> runCatching { CardId(value) }.getOrNull() } ?: return
        applicationScope.launch {
            val current = repository.currentFocusTimer()?.session ?: return@launch
            if (current.id != timerId || current.boardId != boardId || current.cardId != cardId) {
                return@launch
            }
            if (current.state == FocusSessionState.Finished) {
                focusTimerCoordinator.dismiss(timerId)
            }
            requestNavigation(boardId, cardId = cardId)
        }
    }

    fun requestHomeBoardNavigation() {
        applicationScope.launch {
            if (launcherController.initialize() != LauncherBoardInitializationState.Ready) {
                return@launch
            }
            val designation = (launcherController.boardState.value as?
                com.katoaapps.minkboard.core.LauncherBoardState.Ready)
                ?.designation ?: return@launch
            requestNavigation(designation.boardId)
        }
    }

    fun requestNavigation(
        boardId: BoardId,
        cardId: CardId? = null,
        statusId: StatusId? = null,
    ) = navigationState.request(boardId, cardId, statusId)

    fun consumeNavigationRequest(token: Long) = navigationState.consume(token)

    fun readCompactHomePosition(): OpenMinkCompactHomePosition? =
        navigationState.compactHomePosition()

    fun rememberCompactHomePosition(position: OpenMinkCompactHomePosition) {
        navigationState.rememberCompactHomePosition(position)
    }
}
