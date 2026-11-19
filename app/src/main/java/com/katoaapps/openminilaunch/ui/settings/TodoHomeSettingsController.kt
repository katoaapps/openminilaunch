package com.katoaapps.openminilaunch.ui.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.katoaapps.minkboard.core.LauncherBoardState
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSummary
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.openminilaunch.R
import com.katoaapps.minklauncher.todos.home.TodoInitializationState
import com.katoaapps.minklauncher.todos.home.TodoHomeConfigurationResult
import com.katoaapps.minklauncher.todos.runtime.TodoFeatureRuntime
import com.katoaapps.minklauncher.todos.home.canRevealTodoHomeBoard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Coordinates Home-board mutations without mixing persistence flow into the settings layout. */
internal class TodoHomeSettingsController(
    context: Context,
    private val runtime: TodoFeatureRuntime,
    private val scope: CoroutineScope,
) {
    private val applicationContext = context.applicationContext

    var actionError by mutableStateOf<String?>(null)
        private set
    var designationPending by mutableStateOf(false)
        private set
    var pendingBoardLoadId by mutableStateOf<BoardId?>(null)
        private set

    suspend fun initialize() {
        if (runtime.launcherController.initialize() != TodoInitializationState.Ready) {
            actionError = applicationContext.getString(R.string.minkboard_home_needs_repair)
        }
    }

    fun reconcilePendingLoad(ready: LauncherBoardState.Ready?, bindingBroken: Boolean) {
        val pendingId = pendingBoardLoadId ?: return
        when {
            ready?.board?.board?.id == pendingId -> pendingBoardLoadId = null
            bindingBroken -> {
                pendingBoardLoadId = null
                actionError = applicationContext.getString(R.string.minkboard_home_save_failed)
            }
        }
    }

    fun applyDesignation(
        boardId: BoardId,
        intakeStatusId: StatusId,
        resetVisibility: Boolean,
    ) {
        if (designationPending) return
        designationPending = true
        scope.launch {
            try {
                actionError = null
                when (runtime.launcherController.designateHomeBoard(boardId, intakeStatusId)) {
                    TodoHomeConfigurationResult.Updated -> {
                        if (resetVisibility) runtime.homePreferences.showAllStatuses()
                    }
                    TodoHomeConfigurationResult.InvalidTarget -> fail(
                        R.string.minkboard_home_invalid_target,
                    )
                    TodoHomeConfigurationResult.Stale -> fail(
                        R.string.minkboard_home_changed_retry,
                    )
                    TodoHomeConfigurationResult.Failed -> fail(
                        R.string.minkboard_home_save_failed,
                    )
                }
            } finally {
                designationPending = false
            }
        }
    }

    fun chooseBoard(summary: BoardSummary) {
        if (designationPending) return
        designationPending = true
        pendingBoardLoadId = summary.board.id
        scope.launch {
            try {
                actionError = null
                val candidate = runtime.launcherIntegration
                    .launcherBoardDesignationCandidate(summary.board.id)
                when {
                    candidate == null -> failAndStopLoad(R.string.minkboard_home_invalid_target)
                    !canRevealTodoHomeBoard(
                        isLocked = candidate.isLocked,
                        accessGranted = runtime.boardAccessSessions.isGranted(candidate.boardId),
                    ) -> failAndStopLoad(R.string.minkboard_home_locked)
                    candidate.intakeStatusId == null -> failAndStopLoad(
                        R.string.minkboard_home_board_needs_intake,
                    )
                    else -> when (
                        runtime.launcherController.designateHomeBoard(
                            candidate.boardId,
                            checkNotNull(candidate.intakeStatusId),
                        )
                    ) {
                        TodoHomeConfigurationResult.Updated -> {
                            runtime.homePreferences.showAllStatuses()
                        }
                        TodoHomeConfigurationResult.InvalidTarget -> failAndStopLoad(
                            R.string.minkboard_home_invalid_target,
                        )
                        TodoHomeConfigurationResult.Stale -> failAndStopLoad(
                            R.string.minkboard_home_changed_retry,
                        )
                        TodoHomeConfigurationResult.Failed -> failAndStopLoad(
                            R.string.minkboard_home_save_failed,
                        )
                    }
                }
            } catch (_: Exception) {
                failAndStopLoad(R.string.minkboard_home_save_failed)
            } finally {
                designationPending = false
            }
        }
    }

    private fun fail(messageRes: Int) {
        actionError = applicationContext.getString(messageRes)
    }

    private fun failAndStopLoad(messageRes: Int) {
        pendingBoardLoadId = null
        fail(messageRes)
    }
}
