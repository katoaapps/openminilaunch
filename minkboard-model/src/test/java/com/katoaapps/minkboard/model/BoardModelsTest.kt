package com.katoaapps.minkboard.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BoardModelsTest {
    @Test
    fun completionCountUsesExplicitDoneStatus() {
        val boardId = BoardId("board")
        val todo = StatusId("todo")
        val done = StatusId("done")
        val board = BoardSnapshot(
            board = Board(boardId, "Home", false, 0, 0),
            statuses = listOf(
                BoardStatus(todo, boardId, "To do", 0, false, 0),
                BoardStatus(done, boardId, "Done", 0, true, 1),
            ),
            cards = listOf(
                card("one", todo),
                card("two", done),
                card("three", done),
            ),
        )

        assertEquals(2, board.completedCardCount)
    }

    private fun card(id: String, statusId: StatusId) = BoardCard(
        id = CardId(id),
        statusId = statusId,
        title = id,
        details = "",
        dueDate = null,
        isMajor = false,
        position = 0,
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = 0,
    )
}
