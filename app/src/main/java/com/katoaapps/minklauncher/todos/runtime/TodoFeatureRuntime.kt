package com.katoaapps.minklauncher.todos.runtime

import com.katoaapps.minklauncher.todos.backend.minkboard.MinkBoardTodoBackend
import com.katoaapps.minklauncher.todos.home.TodoCompactHomePosition
import com.katoaapps.minklauncher.todos.home.TodoInitializationState
import com.katoaapps.minklauncher.todos.home.TodoNavigationState

import android.content.Context
import android.content.Intent
import com.katoaapps.minkboard.core.MinkBoardFocus
import com.katoaapps.minkboard.core.MinkBoardHostAccess
import com.katoaapps.minkboard.focus.android.AndroidMinkBoardFocusRuntime
import com.katoaapps.minkboard.focus.android.MinkBoardFocusIntents
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.FocusSessionState
import com.katoaapps.minkboard.model.StatusId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Application lifecycle boundary for the launcher Todos feature.
 *
 * MinkBoard-backed storage, focus, navigation, and launcher commands live in focused collaborators.
 * Host policy, localized notification resources, and backup archives stay outside this package.
 */
internal class TodoFeatureRuntime(
    context: Context,
    defaultBoardName: String,
    val hostAccess: MinkBoardHostAccess,
    val resources: TodoFeatureResources,
    private val focusRuntimeFactory: (Context, MinkBoardFocus) -> AndroidMinkBoardFocusRuntime,
) {
    private val applicationContext = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val graph = MinkBoardTodoBackend(
        context = applicationContext,
        applicationScope = applicationScope,
        defaultBoardName = defaultBoardName,
    )
    private val navigationState = TodoNavigationState()
    val focusRuntime by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        focusRuntimeFactory(applicationContext, graph.minkBoard.focus)
    }

    val boardAccessSessions by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        graph.minkBoard.access.createSession()
    }
    private val mutableBoardAccessRevision = MutableStateFlow(0L)
    val boardAccessRevision: StateFlow<Long> = mutableBoardAccessRevision

    val navigationRequest get() = navigationState.request
    val minkBoard get() = graph.minkBoard
    val launcherIntegration get() = graph.minkBoard.launcherIntegration
    val launcherFacade get() = graph.launcherFacade
    val legacyTodoMigration get() = graph.legacyTodoMigration
    val homePreferences get() = graph.homePreferences
    val uiPreferences get() = graph.uiPreferences
    val launcherController get() = graph.launcherController
    val focusTimerCoordinator get() = focusRuntime.coordinator

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
        val request = MinkBoardFocusIntents.navigationRequest(intent) ?: return
        applicationScope.launch {
            val current = minkBoard.focus.currentFocusTimer()?.session ?: return@launch
            if (
                current.id != request.timerId ||
                current.boardId != request.boardId ||
                current.cardId != request.cardId
            ) {
                return@launch
            }
            if (current.state == FocusSessionState.Finished) {
                focusTimerCoordinator.dismiss(request.timerId)
            }
            requestNavigation(request.boardId, cardId = request.cardId)
        }
    }

    fun requestHomeBoardNavigation() {
        applicationScope.launch {
            if (launcherController.initialize() != TodoInitializationState.Ready) {
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

    fun readCompactHomePosition(): TodoCompactHomePosition? =
        navigationState.compactHomePosition()

    fun rememberCompactHomePosition(position: TodoCompactHomePosition) {
        navigationState.rememberCompactHomePosition(position)
    }
}
