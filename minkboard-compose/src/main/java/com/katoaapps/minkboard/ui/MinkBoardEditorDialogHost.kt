package com.katoaapps.minkboard.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.katoaapps.minkboard.core.MinkBoardHostAccess
import com.katoaapps.minkboard.model.BoardDraft
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.BoardSummary
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.CardPlacement
import com.katoaapps.minkboard.model.FocusSession

/**
 * Owns the transient editor and destructive-confirmation surfaces for a board screen.
 *
 * Confirmation state deliberately lives only in composition memory. Process recreation may
 * restore an editor draft, but must never restore a pending destructive decision or callback.
 */
@Composable
internal fun MinkBoardEditorDialogHost(
    editableBoards: List<BoardSummary>,
    selectedBoard: BoardSnapshot?,
    selectedBoardAccessible: Boolean,
    openCardId: CardId?,
    activeFocusSession: FocusSession?,
    editorState: MinkBoardEditorState,
    onEditorStateChange: (MinkBoardEditorState) -> Unit,
    actions: MinkBoardActions,
    hostAccess: MinkBoardHostAccess,
    canUseBoardLock: Boolean,
    isBoardAccessible: (BoardId) -> Boolean,
    showManageBoards: Boolean,
    onManageBoardsDismiss: () -> Unit,
    showCardStatusPicker: Boolean,
    onCardStatusPickerDismiss: () -> Unit,
    onRequestEditBoard: (BoardSummary) -> Unit,
    onCardPlacementChanged: (CardPlacement) -> Unit,
    actionErrorMessage: String?,
) {
    var pendingDeleteBoardIdValue by remember { mutableStateOf<String?>(null) }
    var pendingDeleteBoardIdValues by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingDeleteStatusIdValue by remember { mutableStateOf<String?>(null) }

    val pendingDeleteBoard = pendingDeleteBoardIdValue
        ?.let { id -> editableBoards.firstOrNull { it.board.id.value == id } }
    val pendingDeleteBoards = editableBoards.filter {
        it.board.id.value in pendingDeleteBoardIdValues
    }
    val pendingDeleteStatus = pendingDeleteStatusIdValue
        ?.let { id -> selectedBoard?.statuses?.firstOrNull { it.id.value == id } }

    if (showManageBoards) {
        ManageBoardsDialog(
            boards = editableBoards.map { it.board },
            actionErrorMessage = actionErrorMessage,
            onDismiss = onManageBoardsDismiss,
            onSelect = { boardId ->
                actions.selectBoard(boardId)
                onManageBoardsDismiss()
            },
            onEdit = { boardId ->
                onManageBoardsDismiss()
                editableBoards.firstOrNull { it.board.id == boardId }?.let(onRequestEditBoard)
            },
            onReorder = actions.reorderBoards,
            onDeleteMany = { boardIds ->
                onManageBoardsDismiss()
                pendingDeleteBoardIdValues = boardIds.map(BoardId::value).toSet()
            },
        )
    }

    if (showCardStatusPicker && selectedBoard != null && selectedBoardAccessible) {
        ChooseCardStatusDialog(
            statuses = selectedBoard.statuses,
            onDismiss = onCardStatusPickerDismiss,
            onSelect = { status ->
                onCardStatusPickerDismiss()
                onEditorStateChange(
                    MinkBoardEditorState.NewCard(
                        boardIdValue = selectedBoard.board.id.value,
                        statusIdValue = status.id.value,
                    ),
                )
            },
        )
    }

    if (selectedBoard != null && selectedBoardAccessible) {
        MinkBoardCardDialogHost(
            board = selectedBoard,
            openCardId = openCardId,
            activeFocusSession = activeFocusSession,
            editorState = editorState,
            onEditorStateChange = onEditorStateChange,
            actions = actions.toCardActions(),
            onCardPlacementChanged = onCardPlacementChanged,
        )
    }

    when (val editor = editorState) {
        MinkBoardEditorState.None -> Unit

        is MinkBoardEditorState.NewBoard -> BoardNameDialog(
            draft = editor.draft,
            canLockBoard = canUseBoardLock,
            supportsTemplates = hostAccess.capabilities.supportsTemplates,
            onDraftChange = { onEditorStateChange(editor.copy(draft = it)) },
            onDismiss = { onEditorStateChange(MinkBoardEditorState.None) },
            onCreate = { name, templateId, isLocked ->
                val boardDraft = BoardDraft(name, isLocked)
                val onCreated: (BoardId) -> Unit = {
                    onEditorStateChange(MinkBoardEditorState.None)
                }
                if (templateId == null) {
                    actions.createBoard(boardDraft, onCreated)
                } else {
                    actions.createBoardFromTemplate(boardDraft, templateId, onCreated)
                }
            },
        )

        is MinkBoardEditorState.EditBoard -> {
            val boardSummary = editableBoards.firstOrNull {
                it.board.id.value == editor.boardIdValue
            }
            if (boardSummary != null && isBoardAccessible(boardSummary.board.id)) {
                BoardSettingsDialog(
                    board = boardSummary.board,
                    draft = editor.draft,
                    canDelete = editableBoards.size > 1,
                    canLockBoard = canUseBoardLock,
                    onDraftChange = { onEditorStateChange(editor.copy(draft = it)) },
                    onDismiss = { onEditorStateChange(MinkBoardEditorState.None) },
                    onSave = { update ->
                        actions.updateBoard(boardSummary.board.id, update) {
                            onEditorStateChange(MinkBoardEditorState.None)
                        }
                    },
                    onRequestDelete = {
                        pendingDeleteBoardIdValue = boardSummary.board.id.value
                        onEditorStateChange(MinkBoardEditorState.None)
                    },
                )
            }
        }

        is MinkBoardEditorState.NewStatus -> {
            val boardSnapshot = selectedBoard?.takeIf {
                it.board.id.value == editor.boardIdValue
            }
            if (boardSnapshot != null && isBoardAccessible(boardSnapshot.board.id)) {
                StatusDialog(
                    draft = editor.draft,
                    replacesDoneStatus = boardSnapshot.statuses.any { it.isDoneStatus },
                    onDraftChange = { onEditorStateChange(editor.copy(draft = it)) },
                    onDismiss = { onEditorStateChange(MinkBoardEditorState.None) },
                    onCreate = { statusDraft ->
                        actions.addStatus(boardSnapshot.board.id, statusDraft) {
                            onEditorStateChange(MinkBoardEditorState.None)
                        }
                    },
                )
            }
        }

        is MinkBoardEditorState.EditStatus -> {
            val boardSnapshot = selectedBoard?.takeIf {
                it.board.id.value == editor.boardIdValue
            }
            val status = boardSnapshot?.statuses?.firstOrNull {
                it.id.value == editor.statusIdValue
            }
            if (boardSnapshot != null && status != null && isBoardAccessible(boardSnapshot.board.id)) {
                val statuses = boardSnapshot.statuses.sortedBy { it.position }
                StatusSettingsDialog(
                    status = status,
                    draft = editor.draft,
                    statusCount = statuses.size,
                    replacesDoneStatus = !status.isDoneStatus &&
                        statuses.any { it.id != status.id && it.isDoneStatus },
                    onDraftChange = { onEditorStateChange(editor.copy(draft = it)) },
                    onDismiss = { onEditorStateChange(MinkBoardEditorState.None) },
                    onSave = { update ->
                        actions.updateStatus(status.id, update) {
                            onEditorStateChange(MinkBoardEditorState.None)
                        }
                    },
                    onRequestDelete = {
                        pendingDeleteStatusIdValue = status.id.value
                        onEditorStateChange(MinkBoardEditorState.None)
                    },
                )
            }
        }

        is MinkBoardEditorState.NewCard -> {
            val boardSnapshot = selectedBoard?.takeIf {
                it.board.id.value == editor.boardIdValue
            }
            val status = boardSnapshot?.statuses?.firstOrNull {
                it.id.value == editor.statusIdValue
            }
            if (boardSnapshot != null && status != null && isBoardAccessible(boardSnapshot.board.id)) {
                NewCardDialog(
                    draft = editor.draft,
                    onDraftChange = { onEditorStateChange(editor.copy(draft = it)) },
                    onDismiss = { onEditorStateChange(MinkBoardEditorState.None) },
                    onCreate = { cardDraft ->
                        actions.addCard(status.id, cardDraft) { placement ->
                            onEditorStateChange(MinkBoardEditorState.None)
                            onCardPlacementChanged(placement)
                        }
                    },
                )
            }
        }

        is MinkBoardEditorState.EditCard,
        is MinkBoardEditorState.ManageCardSubtasks,
        is MinkBoardEditorState.FocusCard,
        -> Unit
    }

    pendingDeleteBoard
        ?.takeIf { boardSnapshot -> isBoardAccessible(boardSnapshot.board.id) }
        ?.let { boardSnapshot ->
            DeleteBoardConfirmationDialog(
                boardName = boardSnapshot.board.name,
                cardCount = boardSnapshot.cardCount,
                onDismiss = { pendingDeleteBoardIdValue = null },
                onConfirm = {
                    actions.deleteBoard(boardSnapshot.board.id) {
                        pendingDeleteBoardIdValue = null
                    }
                },
            )
        }
    if (pendingDeleteBoards.isNotEmpty()) {
        DeleteBoardsConfirmationDialog(
            boardCount = pendingDeleteBoards.size,
            cardCount = pendingDeleteBoards.sumOf { it.cardCount },
            onDismiss = { pendingDeleteBoardIdValues = emptySet() },
            onConfirm = {
                actions.deleteBoards(pendingDeleteBoards.map { it.board.id }.toSet()) {
                    pendingDeleteBoardIdValues = emptySet()
                }
            },
        )
    }
    if (pendingDeleteStatus != null && selectedBoard != null && selectedBoardAccessible) {
        DeleteStatusDialog(
            status = pendingDeleteStatus,
            cardCount = selectedBoard.cardsIn(pendingDeleteStatus.id).size,
            destinations = selectedBoard.statuses.filter { it.id != pendingDeleteStatus.id },
            onDismiss = { pendingDeleteStatusIdValue = null },
            onConfirm = { policy ->
                actions.deleteStatus(pendingDeleteStatus.id, policy) {
                    pendingDeleteStatusIdValue = null
                }
            },
        )
    }
}

private fun MinkBoardActions.toCardActions(): MinkBoardCardActions = MinkBoardCardActions(
    closeCard = closeCard,
    cloneCard = cloneCard,
    updateCardAndMove = updateCardAndMove,
    moveCard = moveCardAndReport,
    deleteCard = deleteCard,
    addSubtask = addSubtask,
    updateSubtask = updateSubtask,
    setSubtaskCompleted = setSubtaskCompleted,
    deleteSubtask = deleteSubtask,
    startFocus = startFocus,
    stopFocus = stopFocus,
)
