package com.katoaapps.openminilaunch.ui.minkspace.todo

import com.katoaapps.minkboard.model.Board
import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.openminilaunch.features.minkboard.LauncherVisibleStatuses
import org.junit.Assert.assertEquals
import org.junit.Test

class CompactBoardProjectionTest {
    @Test
    fun pagesVisibleStatusesInBoardOrder() {
        val pages = compactBoardPages(
            board = board(cardCount = 5),
            intakeStatusId = TODO,
            visibility = LauncherVisibleStatuses.All,
            itemsPerPage = 2,
        )

        assertEquals(listOf(TODO, TODO, TODO, DONE), pages.map { it.status.id })
        assertEquals(listOf(0, 1, 2, 0), pages.map { it.statusPage })
        assertEquals(listOf(3, 3, 3, 1), pages.map { it.statusPageCount })
    }

    @Test
    fun selectedVisibilityOmitsUnselectedStatus() {
        val pages = compactBoardPages(
            board = board(cardCount = 1),
            intakeStatusId = TODO,
            visibility = LauncherVisibleStatuses.Selected(listOf(DONE)),
            itemsPerPage = 3,
        )

        assertEquals(listOf(DONE), pages.map { it.status.id })
        assertEquals(emptyList<BoardCard>(), pages.single().cards)
    }

    private fun board(cardCount: Int): BoardSnapshot = BoardSnapshot(
        board = Board(BOARD, "Home", false, 0, 0),
        statuses = listOf(
            BoardStatus(TODO, BOARD, "To do", 0, false, 0),
            BoardStatus(DONE, BOARD, "Done", 0, true, 1),
        ),
        cards = List(cardCount) { index ->
            BoardCard(
                id = CardId("card-$index"),
                statusId = TODO,
                title = "Card $index",
                details = "",
                dueDate = null,
                isMajor = false,
                position = index,
                createdAtEpochMillis = 0,
                updatedAtEpochMillis = 0,
            )
        },
    )

    private companion object {
        val BOARD = BoardId("board")
        val TODO = StatusId("todo")
        val DONE = StatusId("done")
    }
}
