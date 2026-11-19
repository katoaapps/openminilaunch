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
import com.katoaapps.minklauncher.todos.home.TodoVisibleStatuses
import com.katoaapps.minklauncher.todos.home.canRevealTodoHomeBoard
import com.katoaapps.openminilaunch.todoFeatureRuntime
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.components.SettingsDropdownOption
import com.katoaapps.openminilaunch.ui.components.SettingsDropdownRow
import com.katoaapps.openminilaunch.ui.components.SettingsSwitchRow
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

/** Launcher-owned controls for the board/status projection shown by MinkSpace and Magic Box. */
@Composable
internal fun TodoHomeSettingsPage(goBack: () -> Unit) {
    val context = LocalContext.current
    val runtime = remember(context) { context.todoFeatureRuntime }
    val scope = rememberCoroutineScope()
    val controller = remember(context, runtime, scope) {
        TodoHomeSettingsController(context, runtime, scope)
    }
    val listState = rememberLazyListState()
    val boardSummaries by runtime.minkBoard.boards
        .observeBoardSummaries()
        .collectAsState(emptyList())
    val boardState by runtime.launcherController.boardState.collectAsState()
    val boardAccessRevision by runtime.boardAccessRevision.collectAsState()
    val visibility by runtime.homePreferences.visibleStatuses.collectAsState()
    LaunchedEffect(controller) { controller.initialize() }

    val ready = boardState as? LauncherBoardState.Ready
    val boardBindingBroken = boardState is LauncherBoardState.BrokenBinding
    LaunchedEffect(ready, visibility) {
        ready?.let(runtime.homePreferences::reconcile)
    }
    val selectedBoardId = ready?.designation?.boardId
    val selectedBoard = ready?.board?.takeIf { it.board.id == selectedBoardId }
    val canRevealSelectedBoard = remember(selectedBoard, boardAccessRevision) {
        selectedBoard?.let { snapshot ->
            canRevealTodoHomeBoard(
                isLocked = snapshot.board.isLocked,
                accessGranted = runtime.boardAccessSessions.isGranted(snapshot.board.id),
            )
        } ?: true
    }
    val selectedIntakeId = ready?.designation?.intakeStatusId
    val orderedBoards = boardSummaries.sortedBy { it.board.position }
    val orderedStatuses = if (canRevealSelectedBoard) {
        selectedBoard?.statuses?.sortedBy { it.position }.orEmpty()
    } else {
        emptyList()
    }
    val selectedStatusIds = when (val current = visibility) {
        TodoVisibleStatuses.All -> orderedStatuses.map { it.id }
        is TodoVisibleStatuses.Selected -> current.statusIds
    }

    LaunchedEffect(controller.pendingBoardLoadId, ready?.board?.board?.id, boardBindingBroken) {
        controller.reconcilePendingLoad(ready, boardBindingBroken)
    }

    SettingsLazyPage(
        title = stringResource(R.string.minkboard_home_settings_title),
        goBack = goBack,
        state = listState,
        blocked = controller.designationPending || controller.pendingBoardLoadId != null,
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
                options = orderedBoards.map { summary ->
                    SettingsDropdownOption<BoardId?>(summary.board.id, summary.board.name)
                },
                onValueSelected = { boardId ->
                    if (boardId == null) return@SettingsDropdownRow
                    orderedBoards.firstOrNull { it.board.id == boardId }?.let(controller::chooseBoard)
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
                            controller.applyDesignation(boardId, statusId, resetVisibility = false)
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
                    checked = visibility == TodoVisibleStatuses.All,
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
            if (visibility is TodoVisibleStatuses.Selected) {
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
        controller.actionError?.let { message ->
            item(key = "minkboard-home-error") {
                Text(message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
