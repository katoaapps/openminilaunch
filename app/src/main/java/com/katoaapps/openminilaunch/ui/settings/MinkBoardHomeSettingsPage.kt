@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.minkboard.core.LauncherBoardState
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkboard.LauncherBoardInitializationState
import com.katoaapps.openminilaunch.features.minkboard.LauncherHomeConfigurationResult
import com.katoaapps.openminilaunch.features.minkboard.LauncherVisibleStatuses
import com.katoaapps.openminilaunch.features.minkboard.canRevealLauncherHomeBoard
import com.katoaapps.openminilaunch.openMinkMinkBoardRuntime
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.components.SettingsDropdownOption
import com.katoaapps.openminilaunch.ui.components.SettingsDropdownRow
import com.katoaapps.openminilaunch.ui.components.SettingsSwitchRow
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import kotlinx.coroutines.launch

/** Launcher-owned controls for the board/status projection shown by MinkSpace and Magic Box. */
@Composable
internal fun MinkBoardHomeSettingsPage(goBack: () -> Unit) {
    val context = LocalContext.current
    val runtime = remember(context) { context.openMinkMinkBoardRuntime }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val workspace by runtime.repository.observeWorkspace().collectAsState(
        initial = com.katoaapps.minkboard.model.MinkBoardWorkspace.Empty,
    )
    val boardState by runtime.launcherController.boardState.collectAsState()
    val boardAccessRevision by runtime.boardAccessRevision.collectAsState()
    val visibility by runtime.homePreferences.visibleStatuses.collectAsState()
    var actionError by remember { mutableStateOf<String?>(null) }
    var designationPending by remember { mutableStateOf(false) }

    LaunchedEffect(runtime) {
        if (runtime.launcherController.initialize() != LauncherBoardInitializationState.Ready) {
            actionError = context.getString(R.string.minkboard_home_needs_repair)
        }
    }

    val ready = boardState as? LauncherBoardState.Ready
    LaunchedEffect(ready, visibility) {
        ready?.let(runtime.homePreferences::reconcile)
    }
    val selectedBoardId = ready?.designation?.boardId
    val selectedBoard = workspace.board(selectedBoardId)
    val canRevealSelectedBoard = remember(selectedBoard, boardAccessRevision) {
        selectedBoard?.let { snapshot ->
            canRevealLauncherHomeBoard(
                isLocked = snapshot.board.isLocked,
                accessGranted = runtime.boardAccessSessions.isGranted(snapshot.board.id),
            )
        } ?: true
    }
    val selectedIntakeId = ready?.designation?.intakeStatusId
    val orderedBoards = workspace.boards.sortedBy { it.board.position }
    val orderedStatuses = if (canRevealSelectedBoard) {
        selectedBoard?.statuses?.sortedBy { it.position }.orEmpty()
    } else {
        emptyList()
    }
    val selectedStatusIds = when (val current = visibility) {
        LauncherVisibleStatuses.All -> orderedStatuses.map { it.id }
        is LauncherVisibleStatuses.Selected -> current.statusIds
    }

    fun applyDesignation(boardId: BoardId, intakeStatusId: StatusId, resetVisibility: Boolean) {
        if (designationPending) return
        designationPending = true
        scope.launch {
            try {
                actionError = null
                when (runtime.launcherController.designateHomeBoard(boardId, intakeStatusId)) {
                    LauncherHomeConfigurationResult.Updated -> {
                        if (resetVisibility) runtime.homePreferences.showAllStatuses()
                    }
                    LauncherHomeConfigurationResult.InvalidTarget -> {
                        actionError = context.getString(R.string.minkboard_home_invalid_target)
                    }
                    LauncherHomeConfigurationResult.Stale -> {
                        actionError = context.getString(R.string.minkboard_home_changed_retry)
                    }
                    LauncherHomeConfigurationResult.Failed -> {
                        actionError = context.getString(R.string.minkboard_home_save_failed)
                    }
                }
            } finally {
                designationPending = false
            }
        }
    }

    SettingsLazyPage(
        title = stringResource(R.string.minkboard_home_settings_title),
        goBack = goBack,
        state = listState,
        blocked = designationPending,
    ) {
        item(key = "minkboard-home-description") {
            Text(
                text = stringResource(R.string.minkboard_home_settings_description),
                color = Muted,
            )
        }
        item(key = "minkboard-home-board") {
            SettingsDropdownRow(
                title = stringResource(R.string.minkboard_home_board),
                selectedValue = selectedBoardId,
                options = orderedBoards.map { snapshot ->
                    SettingsDropdownOption<BoardId?>(snapshot.board.id, snapshot.board.name)
                },
                onValueSelected = { boardId ->
                    if (boardId == null) return@SettingsDropdownRow
                    val board = workspace.board(boardId)
                    if (
                        board != null &&
                        !canRevealLauncherHomeBoard(
                            isLocked = board.board.isLocked,
                            accessGranted = runtime.boardAccessSessions.isGranted(boardId),
                        )
                    ) {
                        actionError = context.getString(R.string.minkboard_home_locked)
                        return@SettingsDropdownRow
                    }
                    val intake = board?.statuses
                        ?.sortedBy { it.position }
                        ?.firstOrNull { !it.isDoneStatus }
                    if (intake == null) {
                        actionError = context.getString(R.string.minkboard_home_board_needs_intake)
                    } else {
                        applyDesignation(boardId, intake.id, resetVisibility = true)
                    }
                },
            )
        }
        if (!canRevealSelectedBoard) {
            item(key = "minkboard-home-locked") {
                Text(
                    text = stringResource(R.string.minkboard_home_locked),
                    color = Muted,
                )
            }
        } else {
            item(key = "minkboard-home-intake") {
                SettingsDropdownRow(
                    title = stringResource(R.string.minkboard_home_intake),
                    selectedValue = selectedIntakeId,
                    options = orderedStatuses
                        .filterNot { it.isDoneStatus }
                        .map { status ->
                            SettingsDropdownOption<StatusId?>(status.id, status.name)
                        },
                    onValueSelected = { statusId ->
                        if (statusId != null) selectedBoardId?.let { boardId ->
                            applyDesignation(boardId, statusId, resetVisibility = false)
                        }
                    },
                )
            }
            item(key = "minkboard-home-visibility-label") {
                SectionLabel(stringResource(R.string.minkboard_home_visible_statuses))
            }
            item(key = "minkboard-home-show-all") {
                SettingsSwitchRow(
                    title = stringResource(R.string.minkboard_home_show_all_statuses),
                    subtitle = stringResource(R.string.minkboard_home_show_all_statuses_description),
                    checked = visibility == LauncherVisibleStatuses.All,
                    onCheckedChange = { showAll ->
                        if (showAll) {
                            runtime.homePreferences.showAllStatuses()
                        } else {
                            runtime.homePreferences.showSelectedStatuses(
                                selectedStatusIds.ifEmpty { listOfNotNull(selectedIntakeId) },
                            )
                        }
                    },
                )
            }
            if (visibility is LauncherVisibleStatuses.Selected) {
                orderedStatuses.forEach { status ->
                    item(key = "minkboard-home-status-${status.id.value}") {
                        val checked = status.id in selectedStatusIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    RoundedCornerShape(Dimens.dp16),
                                )
                                .clickable {
                                    val next = if (checked) {
                                        selectedStatusIds - status.id
                                    } else {
                                        selectedStatusIds + status.id
                                    }.ifEmpty { listOfNotNull(selectedIntakeId) }
                                    runtime.homePreferences.showSelectedStatuses(next)
                                }
                                .padding(Dimens.dp12),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(Dimens.dp12)
                                    .background(Color(status.colorArgb), CircleShape),
                            )
                            Column(
                                Modifier
                                    .weight(1f)
                                    .padding(horizontal = Dimens.dp12),
                            ) {
                                Text(status.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = stringResource(
                                        R.string.minkboard_status_card_count,
                                        selectedBoard?.cardsIn(status.id)?.size ?: 0,
                                    ),
                                    color = Muted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { shouldShow ->
                                    val next = if (shouldShow) {
                                        selectedStatusIds + status.id
                                    } else {
                                        selectedStatusIds - status.id
                                    }.ifEmpty { listOfNotNull(selectedIntakeId) }
                                    runtime.homePreferences.showSelectedStatuses(next)
                                },
                            )
                        }
                    }
                }
            }
            item(key = "minkboard-home-preview") {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                    SectionLabel(stringResource(R.string.minkboard_home_preview))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
                        verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
                    ) {
                        orderedStatuses
                            .filter { it.id in selectedStatusIds }
                            .forEach { status ->
                                Row(
                                    modifier = Modifier
                                        .background(
                                            Color(status.colorArgb).copy(alpha = 0.18f),
                                            RoundedCornerShape(Dimens.dp12),
                                        )
                                        .padding(Dimens.dp8),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.dp6),
                                ) {
                                    Box(
                                        Modifier
                                            .size(Dimens.dp8)
                                            .background(Color(status.colorArgb), CircleShape),
                                    )
                                    Text(
                                        text = status.name,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                    }
                }
            }
        }
        item(key = "minkboard-archives") {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp14)) {
                MinkBoardArchiveSettingsSection(
                    runtime = runtime,
                    boards = orderedBoards,
                )
            }
        }
        actionError?.let { message ->
            item(key = "minkboard-home-error") {
                Text(message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
