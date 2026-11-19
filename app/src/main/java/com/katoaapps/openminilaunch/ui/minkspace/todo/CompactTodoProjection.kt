package com.katoaapps.openminilaunch.ui.minkspace.todo

import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.minklauncher.todos.home.TodoVisibleStatuses
import com.katoaapps.minklauncher.todos.home.resolveTodoVisibleStatusIds

internal data class CompactTodoPage(
    val status: BoardStatus,
    val cards: List<BoardCard>,
    val statusPage: Int,
    val statusPageCount: Int,
)

/** Builds the Home mini-app projection without retaining unrelated board UI state. */
internal fun compactTodoPages(
    board: BoardSnapshot,
    intakeStatusId: StatusId,
    visibility: TodoVisibleStatuses,
    itemsPerPage: Int,
): List<CompactTodoPage> {
    val pageSize = itemsPerPage.coerceIn(1, 5)
    val visibleIds = resolveTodoVisibleStatusIds(board, intakeStatusId, visibility).toSet()
    return board.statuses
        .sortedBy { it.position }
        .filter { it.id in visibleIds }
        .flatMap { status ->
            val chunks = board.cardsIn(status.id).chunked(pageSize).ifEmpty { listOf(emptyList()) }
            chunks.mapIndexed { index, cards ->
                CompactTodoPage(
                    status = status,
                    cards = cards,
                    statusPage = index,
                    statusPageCount = chunks.size,
                )
            }
        }
}
