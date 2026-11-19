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
import com.katoaapps.openminilaunch.features.minkboard.LauncherBoardInitializationState
import com.katoaapps.openminilaunch.features.minkboard.LauncherHomeConfigurationResult
import com.katoaapps.openminilaunch.features.minkboard.OpenMinkMinkBoardRuntime
import com.katoaapps.openminilaunch.features.minkboard.canRevealLauncherHomeBoard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Coordinates Home-board mutations without mixing persistence flow into the settings layout. */
internal class MinkBoardHomeSettingsController(
    context: Context,
    private val runtime: OpenMinkMinkBoardRuntime,
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
        if (runtime.launcherController.initialize() != LauncherBoardInitializationState.Ready) {
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
                    LauncherHomeConfigurationResult.Updated -> {
                        if (resetVisibility) runtime.homePreferences.showAllStatuses()
                    }
                    LauncherHomeConfigurationResult.InvalidTarget -> fail(
                        R.string.minkboard_home_invalid_target,
                    )
                    LauncherHomeConfigurationResult.Stale -> fail(
                        R.string.minkboard_home_changed_retry,
                    )
                    LauncherHomeConfigurationResult.Failed -> fail(
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
                val candidate = runtime.repository.launcherBoardDesignationCandidate(summary.board.id)
                when {
                    candidate == null -> failAndStopLoad(R.string.minkboard_home_invalid_target)
                    !canRevealLauncherHomeBoard(
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
                        LauncherHomeConfigurationResult.Updated -> {
                            runtime.homePreferences.showAllStatuses()
                        }
                        LauncherHomeConfigurationResult.InvalidTarget -> failAndStopLoad(
                            R.string.minkboard_home_invalid_target,
                        )
                        LauncherHomeConfigurationResult.Stale -> failAndStopLoad(
                            R.string.minkboard_home_changed_retry,
                        )
                        LauncherHomeConfigurationResult.Failed -> failAndStopLoad(
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
