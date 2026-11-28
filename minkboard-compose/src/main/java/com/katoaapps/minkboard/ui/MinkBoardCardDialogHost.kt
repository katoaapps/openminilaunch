package com.katoaapps.minkboard.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.BoardSubtask
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.CardPlacement
import com.katoaapps.minkboard.model.CardUpdate
import com.katoaapps.minkboard.model.FocusSession
import com.katoaapps.minkboard.model.FocusSessionState
import com.katoaapps.minkboard.model.FocusTimerId
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.minkboard.model.SubtaskId

/** Card-only mutation contract shared by the full board and launcher projections. */
data class MinkBoardCardActions(
    val closeCard: () -> Unit,
    val cloneCard: (CardId, String, (CardPlacement) -> Unit) -> Unit,
    val updateCardAndMove: (
        CardId,
        CardUpdate,
        StatusId,
        (CardPlacement?) -> Unit,
    ) -> Unit,
    val moveCard: (CardId, StatusId, Int, (CardPlacement) -> Unit) -> Unit,
    val deleteCard: (CardId, () -> Unit) -> Unit,
    val addSubtask: (CardId, String, () -> Unit) -> Unit,
    val updateSubtask: (SubtaskId, String, () -> Unit) -> Unit,
    val setSubtaskCompleted: (SubtaskId, Boolean) -> Unit,
    val deleteSubtask: (SubtaskId) -> Unit,
    val startFocus: (BoardId, CardId, Long, Boolean) -> Unit,
    val stopFocus: (FocusTimerId) -> Unit,
)

/**
 * The canonical card dialog workflow.
 *
 * Hosts provide the selected board and persistence actions. Full MinkBoard may react to a moved or
 * cloned card by scrolling the board, while compact launcher surfaces can deliberately ignore that
 * placement and remain on Home.
 */
@Composable
fun MinkBoardCardDialogHost(
    board: BoardSnapshot,
    openCardId: CardId?,
    activeFocusSession: FocusSession?,
    editorState: MinkBoardEditorState,
    onEditorStateChange: (MinkBoardEditorState) -> Unit,
    actions: MinkBoardCardActions,
    onCardPlacementChanged: (CardPlacement) -> Unit = {},
) {
    val openCard = board.cards.firstOrNull { it.id == openCardId }
    var pendingDeleteCardIdValue by remember(board.board.id.value) {
        mutableStateOf<String?>(null)
    }

    if (editorState == MinkBoardEditorState.None && openCard != null) {
        val cardStatus = board.statuses.firstOrNull { it.id == openCard.statusId }
        val doneStatus = board.statuses.firstOrNull { it.isDoneStatus }
        val cardDuplicateSuffix = stringResource(R.string.duplicate_title_suffix)
        if (cardStatus != null) {
            val statusCards = board.cardsIn(cardStatus.id)
            val cardIndex = statusCards.indexOfFirst { it.id == openCard.id }
            CardViewDialog(
                card = openCard,
                status = cardStatus,
                isFocusRunningForCard = activeFocusSession?.let {
                    it.state == FocusSessionState.Running && it.cardId == openCard.id
                } == true,
                onDismiss = actions.closeCard,
                onEdit = { onEditorStateChange(openCard.toEditorState(board.board.id.value)) },
                onManageSubtasks = {
                    onEditorStateChange(
                        MinkBoardEditorState.ManageCardSubtasks(
                            boardIdValue = board.board.id.value,
                            cardIdValue = openCard.id.value,
                        ),
                    )
                },
                onFocus = {
                    val active = activeFocusSession
                    if (
                        active?.state == FocusSessionState.Running &&
                        active.cardId == openCard.id
                    ) {
                        actions.stopFocus(active.id)
                    } else {
                        onEditorStateChange(
                            MinkBoardEditorState.FocusCard(
                                boardIdValue = board.board.id.value,
                                cardIdValue = openCard.id.value,
                            ),
                        )
                    }
                },
                canMarkDone = doneStatus != null && cardStatus.id != doneStatus.id,
                onMarkDone = {
                    doneStatus?.let { destination ->
                        actions.moveCard(openCard.id, destination.id, 0) { placement ->
                            actions.closeCard()
                            onEditorStateChange(MinkBoardEditorState.None)
                            onCardPlacementChanged(placement)
                        }
                    }
                },
                canSendToTop = cardIndex > 0,
                canSendToBottom = cardIndex in 0 until statusCards.lastIndex,
                onSendToTop = {
                    actions.moveCard(openCard.id, cardStatus.id, 0) { placement ->
                        actions.closeCard()
                        onEditorStateChange(MinkBoardEditorState.None)
                        onCardPlacementChanged(placement)
                    }
                },
                onSendToBottom = {
                    actions.moveCard(
                        openCard.id,
                        cardStatus.id,
                        statusCards.lastIndex,
                    ) { placement ->
                        actions.closeCard()
                        onEditorStateChange(MinkBoardEditorState.None)
                        onCardPlacementChanged(placement)
                    }
                },
                onDuplicate = {
                    actions.cloneCard(openCard.id, cardDuplicateSuffix) { placement ->
                        actions.closeCard()
                        onCardPlacementChanged(placement)
                    }
                },
                onDelete = { pendingDeleteCardIdValue = openCard.id.value },
            )
        }
    }

    when (val editor = editorState) {
        is MinkBoardEditorState.EditCard -> {
            val card = board.cards.firstOrNull { it.id.value == editor.cardIdValue }
            if (card != null && editor.boardIdValue == board.board.id.value) {
                val statuses = board.statuses.sortedBy { it.position }
                val currentStatus = statuses.first { it.id == card.statusId }
                EditCardDialog(
                    card = card,
                    draft = editor.draft,
                    statuses = statuses,
                    currentStatus = currentStatus,
                    onDraftChange = { onEditorStateChange(editor.copy(draft = it)) },
                    onDismiss = { onEditorStateChange(MinkBoardEditorState.None) },
                    onSave = { update, destination ->
                        actions.updateCardAndMove(card.id, update, destination.id) { placement ->
                            onEditorStateChange(MinkBoardEditorState.None)
                            if (placement != null) {
                                actions.closeCard()
                                onCardPlacementChanged(placement)
                            }
                        }
                    },
                )
            }
        }

        is MinkBoardEditorState.ManageCardSubtasks -> {
            val card = board.cards.firstOrNull { it.id.value == editor.cardIdValue }
            if (card != null && editor.boardIdValue == board.board.id.value) {
                SubtaskManagerDialog(
                    card = card,
                    draft = editor.draft,
                    onDraftChange = { onEditorStateChange(editor.copy(draft = it)) },
                    onDismiss = { onEditorStateChange(MinkBoardEditorState.None) },
                    onAddSubtask = { text, onSuccess ->
                        actions.addSubtask(card.id, text, onSuccess)
                    },
                    onUpdateSubtask = { subtask, text, onSuccess ->
                        actions.updateSubtask(subtask.id, text, onSuccess)
                    },
                    onToggleSubtask = { subtask: BoardSubtask, checked ->
                        actions.setSubtaskCompleted(subtask.id, checked)
                    },
                    onDeleteSubtask = { subtask -> actions.deleteSubtask(subtask.id) },
                )
            }
        }

        is MinkBoardEditorState.FocusCard -> {
            val card = board.cards.firstOrNull { it.id.value == editor.cardIdValue }
            if (card != null && editor.boardIdValue == board.board.id.value) {
                val returnToCard = {
                    val returnDraft = editor.returnCardDraft
                    if (returnDraft == null) {
                        onEditorStateChange(MinkBoardEditorState.None)
                    } else {
                        onEditorStateChange(
                            MinkBoardEditorState.EditCard(
                                boardIdValue = board.board.id.value,
                                cardIdValue = card.id.value,
                                draft = returnDraft,
                            ),
                        )
                    }
                }
                FocusTimerDialog(
                    draft = editor.draft,
                    replacesRunningTimer = activeFocusSession?.state ==
                        FocusSessionState.Running,
                    onDraftChange = { onEditorStateChange(editor.copy(draft = it)) },
                    onDismiss = returnToCard,
                    onStart = { durationMillis, replace ->
                        actions.startFocus(
                            board.board.id,
                            card.id,
                            durationMillis,
                            replace,
                        )
                        onEditorStateChange(MinkBoardEditorState.None)
                    },
                )
            }
        }

        MinkBoardEditorState.None,
        is MinkBoardEditorState.NewBoard,
        is MinkBoardEditorState.EditBoard,
        is MinkBoardEditorState.NewStatus,
        is MinkBoardEditorState.EditStatus,
        is MinkBoardEditorState.NewCard,
        -> Unit
    }

    val pendingDeleteCard = pendingDeleteCardIdValue?.let { cardIdValue ->
        board.cards.firstOrNull { it.id.value == cardIdValue }
    }
    if (pendingDeleteCard != null) {
        DeleteCardConfirmationDialog(
            cardTitle = pendingDeleteCard.title,
            onDismiss = { pendingDeleteCardIdValue = null },
            onConfirm = {
                actions.deleteCard(pendingDeleteCard.id) {
                    pendingDeleteCardIdValue = null
                    actions.closeCard()
                    onEditorStateChange(MinkBoardEditorState.None)
                }
            },
        )
    }
}

private fun BoardCard.toEditorState(boardIdValue: String): MinkBoardEditorState.EditCard =
    MinkBoardEditorState.EditCard(
        boardIdValue = boardIdValue,
        cardIdValue = id.value,
        draft = CardEditorDraft(
            title = title,
            details = details,
            dueDateText = dueDate?.toString().orEmpty(),
            isMajor = isMajor,
            destinationStatusIdValue = statusId.value,
        ),
    )
