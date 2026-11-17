package com.katoaapps.minkboard.model

import java.time.LocalDate

data class Board(
    val id: BoardId,
    val name: String,
    val isLocked: Boolean,
    val position: Int,
    val createdAtEpochMillis: Long,
)

data class BoardStatus(
    val id: StatusId,
    val boardId: BoardId,
    val name: String,
    val colorArgb: Int,
    val isDoneStatus: Boolean,
    val position: Int,
)

data class BoardCard(
    val id: CardId,
    val statusId: StatusId,
    val title: String,
    val details: String,
    val dueDate: LocalDate?,
    val isMajor: Boolean,
    val position: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val subtasks: List<BoardSubtask> = emptyList(),
)

data class BoardSubtask(
    val id: SubtaskId,
    val cardId: CardId,
    val text: String,
    val isCompleted: Boolean,
    val position: Int,
)

data class BoardSnapshot(
    val board: Board,
    val statuses: List<BoardStatus>,
    val cards: List<BoardCard>,
) {
    fun cardsIn(statusId: StatusId): List<BoardCard> = cards
        .asSequence()
        .filter { it.statusId == statusId }
        .sortedBy { it.position }
        .toList()

    val completedCardCount: Int
        get() {
            val doneIds = statuses.filter(BoardStatus::isDoneStatus).map(BoardStatus::id).toSet()
            return cards.count { it.statusId in doneIds }
        }
}

data class MinkBoardWorkspace(
    val boards: List<BoardSnapshot>,
) {
    fun board(id: BoardId?): BoardSnapshot? = id?.let { selected ->
        boards.firstOrNull { it.board.id == selected }
    }

    val firstBoard: BoardSnapshot?
        get() = boards.minByOrNull { it.board.position }

    companion object {
        val Empty = MinkBoardWorkspace(emptyList())
    }
}

data class BoardDraft(
    val name: String,
    val isLocked: Boolean = false,
)

data class BoardUpdate(
    val name: String,
    val isLocked: Boolean,
)

data class StatusDraft(
    val name: String,
    val colorArgb: Int,
    val isDoneStatus: Boolean = false,
)

data class StatusUpdate(
    val name: String,
    val colorArgb: Int,
    val isDoneStatus: Boolean,
)

sealed interface StatusDeletionPolicy {
    data object DeleteCards : StatusDeletionPolicy

    data class MoveCardsTo(
        val destinationStatusId: StatusId,
    ) : StatusDeletionPolicy
}

data class CardDraft(
    val title: String,
    val details: String = "",
    val dueDate: LocalDate? = null,
    val isMajor: Boolean = false,
)

data class CardUpdate(
    val title: String,
    val details: String,
    val dueDate: LocalDate?,
    val isMajor: Boolean,
)
