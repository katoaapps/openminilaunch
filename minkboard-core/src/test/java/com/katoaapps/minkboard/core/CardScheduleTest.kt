package com.katoaapps.minkboard.core

import com.katoaapps.minkboard.model.Board
import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.StatusId
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CardScheduleTest {
    private val today = LocalDate.of(2026, 11, 11)

    @Test
    fun dueStateSeparatesPastPresentFutureAndSuppressesCompletedCards() {
        assertEquals(CardDueState.None, card(null).dueState(today, isCompleted = false))
        assertEquals(CardDueState.Overdue, card(today.minusDays(1)).dueState(today, false))
        assertEquals(CardDueState.Today, card(today).dueState(today, false))
        assertEquals(CardDueState.Future, card(today.plusDays(1)).dueState(today, false))
        assertEquals(CardDueState.None, card(today.minusDays(1)).dueState(today, true))
    }

    @Test
    fun calendarReturnsOnlyUnfinishedCardsForTheSelectedDayInBoardOrder() {
        val toDo = status("todo", position = 0)
        val doing = status("doing", position = 1)
        val done = status("done", position = 2, isDone = true)
        val second = card(today, id = "second", status = doing.id, position = 0)
        val first = card(today, id = "first", status = toDo.id, position = 1)
        val completed = card(today, id = "complete", status = done.id, position = 0)
        val otherDay = card(today.plusDays(1), id = "tomorrow", status = toDo.id, position = 0)
        val snapshot = BoardSnapshot(
            board = Board(BoardId("board"), "Board", false, 0, 0L),
            statuses = listOf(toDo, doing, done),
            cards = listOf(second, completed, otherDay, first),
        )

        assertEquals(listOf(first.id, second.id), snapshot.cardsDueOn(today).map { it.id })
        assertEquals(2, snapshot.dueCardCount(today))
    }

    private fun status(id: String, position: Int, isDone: Boolean = false) = BoardStatus(
        id = StatusId(id),
        boardId = BoardId("board"),
        name = id,
        colorArgb = 0,
        isDoneStatus = isDone,
        position = position,
    )

    private fun card(
        date: LocalDate?,
        id: String = "card",
        status: StatusId = StatusId("todo"),
        position: Int = 0,
    ) = BoardCard(
        id = CardId(id),
        statusId = status,
        title = id,
        details = "",
        dueDate = date,
        isMajor = false,
        position = position,
        createdAtEpochMillis = 0L,
        updatedAtEpochMillis = 0L,
    )
}
