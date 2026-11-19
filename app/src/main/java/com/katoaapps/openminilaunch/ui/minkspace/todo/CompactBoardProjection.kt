package com.katoaapps.openminilaunch.ui.minkspace.todo

import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.openminilaunch.features.minkboard.LauncherVisibleStatuses
import com.katoaapps.openminilaunch.features.minkboard.visibleStatusIds

internal data class CompactBoardPage(
    val status: BoardStatus,
    val cards: List<BoardCard>,
    val statusPage: Int,
    val statusPageCount: Int,
)

/** Builds the Home mini-app projection without retaining unrelated board UI state. */
internal fun compactBoardPages(
    board: BoardSnapshot,
    intakeStatusId: StatusId,
    visibility: LauncherVisibleStatuses,
    itemsPerPage: Int,
): List<CompactBoardPage> {
    val pageSize = itemsPerPage.coerceIn(1, 5)
    val visibleIds = visibleStatusIds(board, intakeStatusId, visibility).toSet()
    return board.statuses
        .sortedBy { it.position }
        .filter { it.id in visibleIds }
        .flatMap { status ->
            val chunks = board.cardsIn(status.id).chunked(pageSize).ifEmpty { listOf(emptyList()) }
            chunks.mapIndexed { index, cards ->
                CompactBoardPage(
                    status = status,
                    cards = cards,
                    statusPage = index,
                    statusPageCount = chunks.size,
                )
            }
        }
}
