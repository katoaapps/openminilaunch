package com.katoaapps.minkboard.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.katoaapps.minkboard.core.MinkBoardRules
import com.katoaapps.minkboard.model.Board
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardUpdate
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.StatusDeletionPolicy
import com.katoaapps.minkboard.model.StatusUpdate
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
internal fun ManageBoardsDialog(
    boards: List<Board>,
    actionErrorMessage: String?,
    onDismiss: () -> Unit,
    onSelect: (BoardId) -> Unit,
    onEdit: (BoardId) -> Unit,
    onReorder: (List<BoardId>) -> Unit,
    onDeleteMany: (Set<BoardId>) -> Unit,
) {
    val canonicalBoards = boards.sortedBy { it.position }
    val listState = rememberLazyListState()
    val visibleBoards = remember {
        mutableStateListOf<Board>().apply { addAll(canonicalBoards) }
    }
    var draggedBoardIdValue by remember { mutableStateOf<String?>(null) }
    var pendingBoardOrder by remember { mutableStateOf<List<String>?>(null) }
    var selectingForDeletion by remember { mutableStateOf(false) }
    var selectedBoardIdValues by remember { mutableStateOf<Set<String>>(emptySet()) }
    val hapticFeedback = LocalHapticFeedback.current
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        if (visibleBoards.moveItem(from.index, to.index)) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    val canonicalBoardOrder = canonicalBoards.map { it.id.value }
    LaunchedEffect(canonicalBoards, draggedBoardIdValue, pendingBoardOrder) {
        val persistedOrderArrived = pendingBoardOrder == canonicalBoardOrder
        if (persistedOrderArrived) pendingBoardOrder = null
        if (
            draggedBoardIdValue == null &&
            (pendingBoardOrder == null || persistedOrderArrived) &&
            visibleBoards != canonicalBoards
        ) {
            visibleBoards.clear()
            visibleBoards.addAll(canonicalBoards)
        }
    }
    LaunchedEffect(actionErrorMessage) {
        if (actionErrorMessage != null) {
            pendingBoardOrder = null
            visibleBoards.clear()
            visibleBoards.addAll(canonicalBoards)
        }
    }
    MinkBoardAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        if (selectingForDeletion) {
                            R.string.select_boards_to_delete
                        } else {
                            R.string.manage_boards
                        },
                    ),
                    modifier = Modifier.weight(1f),
                )
                if (!selectingForDeletion && visibleBoards.size > 1) {
                    IconButton(onClick = { selectingForDeletion = true }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.delete_multiple_boards),
                        )
                    }
                }
            }
        },
        text = {
            LazyColumn(
                state = listState,
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(visibleBoards, key = { _, board -> board.id.value }) { index, board ->
                    ReorderableItem(reorderState, key = board.id.value) { isDragging ->
                        val interactionSource = remember { MutableInteractionSource() }
                        val scale by animateFloatAsState(
                            targetValue = if (isDragging) 1.025f else 1f,
                            label = "board drag scale",
                        )
                        val elevation by animateFloatAsState(
                            targetValue = if (isDragging) 12f else 0f,
                            label = "board drag elevation",
                        )
                        val moveEarlierLabel = stringResource(R.string.move_board_earlier, board.name)
                        val moveLaterLabel = stringResource(R.string.move_board_later, board.name)
                        val accessibilityActions = buildList {
                            if (index > 0) {
                                add(CustomAccessibilityAction(moveEarlierLabel) {
                                    if (!visibleBoards.moveItem(index, index - 1)) {
                                        false
                                    } else {
                                        pendingBoardOrder = visibleBoards.map { it.id.value }
                                        onReorder(visibleBoards.map(Board::id))
                                        hapticFeedback.performHapticFeedback(
                                            HapticFeedbackType.TextHandleMove,
                                        )
                                        true
                                    }
                                })
                            }
                            if (index < visibleBoards.lastIndex) {
                                add(CustomAccessibilityAction(moveLaterLabel) {
                                    if (!visibleBoards.moveItem(index, index + 1)) {
                                        false
                                    } else {
                                        pendingBoardOrder = visibleBoards.map { it.id.value }
                                        onReorder(visibleBoards.map(Board::id))
                                        hapticFeedback.performHapticFeedback(
                                            HapticFeedbackType.TextHandleMove,
                                        )
                                        true
                                    }
                                })
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { customActions = accessibilityActions }
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    shadowElevation = elevation
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val isSelected = board.id.value in selectedBoardIdValues
                            val canSelect = isSelected ||
                                selectedBoardIdValues.size < visibleBoards.size - 1
                            if (selectingForDeletion) {
                                Checkbox(
                                    checked = isSelected,
                                    enabled = canSelect,
                                    onCheckedChange = { checked ->
                                        selectedBoardIdValues = if (checked) {
                                            selectedBoardIdValues + board.id.value
                                        } else {
                                            selectedBoardIdValues - board.id.value
                                        }
                                    },
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = stringResource(
                                        R.string.reorder_board,
                                        board.name,
                                    ),
                                    modifier = Modifier
                                        .size(44.dp)
                                        .draggableHandle(
                                            interactionSource = interactionSource,
                                            onDragStarted = {
                                                draggedBoardIdValue = board.id.value
                                                hapticFeedback.performHapticFeedback(
                                                    HapticFeedbackType.LongPress,
                                                )
                                            },
                                            onDragStopped = {
                                                pendingBoardOrder = visibleBoards.map { it.id.value }
                                                onReorder(visibleBoards.map(Board::id))
                                                draggedBoardIdValue = null
                                            },
                                        )
                                        .padding(10.dp),
                                )
                            }
                            TextButton(
                                onClick = {
                                    if (selectingForDeletion) {
                                        if (canSelect) {
                                            selectedBoardIdValues = if (isSelected) {
                                                selectedBoardIdValues - board.id.value
                                            } else {
                                                selectedBoardIdValues + board.id.value
                                            }
                                        }
                                    } else {
                                        onSelect(board.id)
                                    }
                                },
                                enabled = !selectingForDeletion || canSelect,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(board.name, modifier = Modifier.fillMaxWidth())
                            }
                            if (!selectingForDeletion) {
                                IconButton(onClick = { onEdit(board.id) }) {
                                    Icon(
                                        Icons.Outlined.Edit,
                                        contentDescription = stringResource(
                                            R.string.edit_board_named,
                                            board.name,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    if (index < visibleBoards.lastIndex) HorizontalDivider()
                }
            }
        },
        confirmButton = {
            if (selectingForDeletion) {
                Button(
                    onClick = {
                        onDeleteMany(selectedBoardIdValues.map(::BoardId).toSet())
                    },
                    enabled = selectedBoardIdValues.isNotEmpty(),
                ) {
                    Text(
                        pluralStringResource(
                            R.plurals.delete_selected_boards,
                            selectedBoardIdValues.size,
                            selectedBoardIdValues.size,
                        ),
                    )
                }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
            }
        },
        dismissButton = if (selectingForDeletion) {
            {
                TextButton(onClick = {
                    selectingForDeletion = false
                    selectedBoardIdValues = emptySet()
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        } else null,
    )
}

@Composable
internal fun BoardSettingsDialog(
    board: Board,
    draft: BoardEditorDraft,
    canDelete: Boolean,
    canLockBoard: Boolean,
    onDraftChange: (BoardEditorDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: (BoardUpdate) -> Unit,
    onRequestDelete: () -> Unit,
) {
    var showError by remember(board.id.value) { mutableStateOf(false) }
    MinkBoardAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_board)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = {
                        onDraftChange(
                            draft.copy(name = it.take(MinkBoardRules.MAX_BOARD_NAME_LENGTH)),
                        )
                        showError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.board_name)) },
                    singleLine = true,
                    isError = showError,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = draft.isLocked,
                            enabled = canLockBoard,
                            role = Role.Checkbox,
                            onValueChange = {
                                onDraftChange(draft.copy(isLocked = it))
                            },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Switch(
                        checked = draft.isLocked,
                        onCheckedChange = null,
                        enabled = canLockBoard,
                    )
                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        Text(stringResource(R.string.protect_board))
                        Text(stringResource(R.string.protect_board_description))
                    }
                }
                if (!canLockBoard) Text(stringResource(R.string.protect_board_unavailable))
                TextButton(
                    onClick = onRequestDelete,
                    enabled = canDelete,
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Text(
                        stringResource(R.string.delete_board),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                if (!canDelete) Text(stringResource(R.string.keep_one_board))
            }
        },
        confirmButton = {
            Button(onClick = {
                if (draft.name.isBlank()) {
                    showError = true
                } else {
                    onSave(BoardUpdate(draft.name, draft.isLocked))
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
internal fun DeleteBoardConfirmationDialog(
    boardName: String,
    cardCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val cardCountLabel = pluralStringResource(R.plurals.card_count, cardCount, cardCount)
    MinkBoardAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_board_title)) },
        text = { Text(stringResource(R.string.delete_board_body, boardName, cardCountLabel)) },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
internal fun DeleteBoardsConfirmationDialog(
    boardCount: Int,
    cardCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val boardCountLabel = pluralStringResource(R.plurals.board_count, boardCount, boardCount)
    val cardCountLabel = pluralStringResource(R.plurals.card_count, cardCount, cardCount)
    MinkBoardAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_boards_title)) },
        text = {
            Text(stringResource(R.string.delete_boards_body, boardCountLabel, cardCountLabel))
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
internal fun StatusSettingsDialog(
    status: BoardStatus,
    draft: StatusEditorDraft,
    statusCount: Int,
    replacesDoneStatus: Boolean,
    onDraftChange: (StatusEditorDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: (StatusUpdate) -> Unit,
    onRequestDelete: () -> Unit,
) {
    var showError by remember(status.id.value) { mutableStateOf(false) }
    MinkBoardAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_status)) },
        text = {
            Box(modifier = Modifier.heightIn(max = 360.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = draft.name,
                        onValueChange = {
                            onDraftChange(
                                draft.copy(name = it.take(MinkBoardRules.MAX_STATUS_NAME_LENGTH)),
                            )
                            showError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.status_name)) },
                        singleLine = true,
                        isError = showError,
                    )
                    StatusColorPicker(
                        selectedColor = draft.colorArgb,
                        customColorDraft = draft.customColorDraft,
                        onColorSelected = {
                            onDraftChange(draft.copy(colorArgb = it, customColorDraft = null))
                        },
                        onCustomColorDraftChange = {
                            onDraftChange(draft.copy(customColorDraft = it))
                        },
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = draft.isDone,
                                role = Role.Checkbox,
                                onValueChange = {
                                    onDraftChange(draft.copy(isDone = it))
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = draft.isDone, onCheckedChange = null)
                        Text(stringResource(R.string.done_status))
                    }
                    if (draft.isDone && replacesDoneStatus) {
                        Text(stringResource(R.string.done_status_replacement))
                    }
                    TextButton(
                        onClick = onRequestDelete,
                        enabled = statusCount > 1,
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                        Text(
                            stringResource(R.string.delete_status),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                    if (statusCount <= 1) Text(stringResource(R.string.keep_one_status))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (draft.name.isBlank()) {
                    showError = true
                } else {
                    onSave(StatusUpdate(draft.name, draft.colorArgb, draft.isDone))
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
internal fun DeleteStatusDialog(
    status: BoardStatus,
    cardCount: Int,
    destinations: List<BoardStatus>,
    onDismiss: () -> Unit,
    onConfirm: (StatusDeletionPolicy) -> Unit,
) {
    val sortedDestinations = destinations.sortedBy { it.position }
    var destinationId by remember(status.id.value) {
        mutableStateOf(sortedDestinations.firstOrNull()?.id?.value)
    }
    var deleteCards by remember(status.id.value) {
        mutableStateOf(sortedDestinations.isEmpty())
    }
    val selectedDestination = sortedDestinations.firstOrNull { it.id.value == destinationId }
        ?: sortedDestinations.firstOrNull()
    MinkBoardAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_status_title, status.name)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (cardCount == 0) {
                    Text(stringResource(R.string.delete_empty_status_body))
                } else {
                    Text(stringResource(R.string.delete_status_cards_body, cardCount))
                    sortedDestinations.forEach { destination ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = !deleteCards && selectedDestination?.id == destination.id,
                                    role = Role.RadioButton,
                                    onClick = {
                                        deleteCards = false
                                        destinationId = destination.id.value
                                    },
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = !deleteCards && selectedDestination?.id == destination.id,
                                onClick = {
                                    deleteCards = false
                                    destinationId = destination.id.value
                                },
                            )
                            Text(stringResource(R.string.move_cards_to_status, destination.name))
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = deleteCards,
                                role = Role.RadioButton,
                                onClick = { deleteCards = true },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = deleteCards,
                            onClick = { deleteCards = true },
                        )
                        Text(stringResource(R.string.delete_cards_with_status))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val policy = if (cardCount == 0 || deleteCards || selectedDestination == null) {
                    StatusDeletionPolicy.DeleteCards
                } else {
                    StatusDeletionPolicy.MoveCardsTo(selectedDestination.id)
                }
                onConfirm(policy)
            }) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
