package com.katoaapps.minkboard.ui

import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.BoardSummary

internal fun MinkBoardEditorState.isSelectedBoardEditor(): Boolean = when (this) {
    MinkBoardEditorState.None,
    is MinkBoardEditorState.NewBoard,
    is MinkBoardEditorState.EditBoard,
    -> false

    is MinkBoardEditorState.NewStatus,
    is MinkBoardEditorState.EditStatus,
    is MinkBoardEditorState.NewCard,
    is MinkBoardEditorState.EditCard,
    is MinkBoardEditorState.ManageCardSubtasks,
    is MinkBoardEditorState.FocusCard,
    -> true
}

internal fun MinkBoardEditorState.targetKey(): String = when (this) {
    MinkBoardEditorState.None -> "none"
    is MinkBoardEditorState.NewBoard -> "new-board"
    is MinkBoardEditorState.EditBoard -> "edit-board:$boardIdValue"
    is MinkBoardEditorState.NewStatus -> "new-status:$boardIdValue"
    is MinkBoardEditorState.EditStatus -> "edit-status:$boardIdValue:$statusIdValue"
    is MinkBoardEditorState.NewCard -> "new-card:$boardIdValue:$statusIdValue"
    is MinkBoardEditorState.EditCard -> "edit-card:$boardIdValue:$cardIdValue"
    is MinkBoardEditorState.ManageCardSubtasks -> "manage-subtasks:$boardIdValue:$cardIdValue"
    is MinkBoardEditorState.FocusCard -> "focus-card:$boardIdValue:$cardIdValue"
}

internal fun MinkBoardEditorState.isValidFor(
    boards: List<BoardSummary>,
    selectedBoard: BoardSnapshot?,
    canCreateBoard: Boolean,
): Boolean {
    fun boardExists(idValue: String) = boards.any { it.board.id.value == idValue }
    fun selected(idValue: String) = selectedBoard?.takeIf { it.board.id.value == idValue }

    return when (this) {
        MinkBoardEditorState.None -> true
        is MinkBoardEditorState.NewBoard -> canCreateBoard
        is MinkBoardEditorState.EditBoard -> boardExists(boardIdValue)
        is MinkBoardEditorState.NewStatus -> selected(boardIdValue) != null
        is MinkBoardEditorState.EditStatus -> selected(boardIdValue)
            ?.statuses
            ?.any { it.id.value == statusIdValue } == true
        is MinkBoardEditorState.NewCard -> selected(boardIdValue)
            ?.statuses
            ?.any { it.id.value == statusIdValue } == true
        is MinkBoardEditorState.EditCard -> selected(boardIdValue)
            ?.cards
            ?.any { it.id.value == cardIdValue } == true
        is MinkBoardEditorState.ManageCardSubtasks -> selected(boardIdValue)
            ?.cards
            ?.any { it.id.value == cardIdValue } == true
        is MinkBoardEditorState.FocusCard -> selected(boardIdValue)
            ?.cards
            ?.any { it.id.value == cardIdValue } == true
    }
}
