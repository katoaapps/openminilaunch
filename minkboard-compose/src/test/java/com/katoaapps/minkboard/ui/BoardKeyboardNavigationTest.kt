package com.katoaapps.minkboard.ui

import android.view.KeyEvent
import com.katoaapps.minkboard.model.Board
import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.StatusId
import org.junit.Assert.assertEquals
import org.junit.Test

class BoardKeyboardNavigationTest {
    @Test
    fun mapsPhysicalWasdClusterIndependentlyOfPrintedLetters() {
        assertEquals(BoardKeyboardCommand.MoveUp, command(keyCode = KeyEvent.KEYCODE_Z, scanCode = 17))
        assertEquals(BoardKeyboardCommand.MoveLeft, command(keyCode = KeyEvent.KEYCODE_Q, scanCode = 30))
        assertEquals(BoardKeyboardCommand.MoveDown, command(keyCode = KeyEvent.KEYCODE_S, scanCode = 31))
        assertEquals(BoardKeyboardCommand.MoveRight, command(keyCode = KeyEvent.KEYCODE_D, scanCode = 32))
    }

    @Test
    fun mapsNewAndOpenOnlyOnInitialShiftedKeyDown() {
        assertEquals(BoardKeyboardCommand.NewCard, command(keyCode = KeyEvent.KEYCODE_N))
        assertEquals(BoardKeyboardCommand.OpenCard, command(keyCode = KeyEvent.KEYCODE_ENTER))
        assertEquals(null, command(keyCode = KeyEvent.KEYCODE_N, repeatCount = 1))
        assertEquals(
            null,
            boardKeyboardCommand(
                action = KeyEvent.ACTION_DOWN,
                shiftPressed = false,
                keyCode = KeyEvent.KEYCODE_N,
                scanCode = 0,
                unicodeChar = 'n'.code,
                repeatCount = 0,
            ),
        )
    }

    @Test
    fun verticalMovementStaysInColumnAndHorizontalMovementPreservesRow() {
        val board = board()
        val first = BoardKeyboardSelection(StatusId("one"), CardId("one-1"))
        val second = moveBoardKeyboardSelection(
            board,
            first,
            BoardKeyboardCommand.MoveDown,
            StatusId("one"),
        )
        assertEquals(BoardKeyboardSelection(StatusId("one"), CardId("one-2")), second)

        val movedRight = moveBoardKeyboardSelection(
            board,
            second,
            BoardKeyboardCommand.MoveRight,
            StatusId("one"),
        )
        assertEquals(BoardKeyboardSelection(StatusId("three"), CardId("three-2")), movedRight)
    }

    @Test
    fun horizontalMovementSkipsEmptyColumnsAndClampsRow() {
        val baseBoard = board()
        val board = baseBoard.copy(
            cards = baseBoard.cards.filterNot { it.id == CardId("three-2") },
        )
        val movedRight = moveBoardKeyboardSelection(
            board,
            BoardKeyboardSelection(StatusId("one"), CardId("one-2")),
            BoardKeyboardCommand.MoveRight,
            StatusId("one"),
        )
        assertEquals(BoardKeyboardSelection(StatusId("three"), CardId("three-1")), movedRight)
    }

    private fun command(
        keyCode: Int,
        scanCode: Int = 0,
        repeatCount: Int = 0,
    ): BoardKeyboardCommand? = boardKeyboardCommand(
        action = KeyEvent.ACTION_DOWN,
        shiftPressed = true,
        keyCode = keyCode,
        scanCode = scanCode,
        unicodeChar = 0,
        repeatCount = repeatCount,
    )

    private fun board(): BoardSnapshot {
        val boardId = BoardId("board")
        val statuses = listOf(
            status(boardId, "one", 0),
            status(boardId, "two", 1),
            status(boardId, "three", 2),
        )
        return BoardSnapshot(
            board = Board(boardId, "Board", false, 0, 1L),
            statuses = statuses,
            cards = listOf(
                card("one-1", "one", 0),
                card("one-2", "one", 1),
                card("three-1", "three", 0),
                card("three-2", "three", 1),
            ),
        )
    }

    private fun status(boardId: BoardId, id: String, position: Int) = BoardStatus(
        id = StatusId(id),
        boardId = boardId,
        name = id,
        colorArgb = 0,
        isDoneStatus = false,
        position = position,
    )

    private fun card(id: String, statusId: String, position: Int) = BoardCard(
        id = CardId(id),
        statusId = StatusId(statusId),
        title = id,
        details = "",
        dueDate = null,
        isMajor = false,
        position = position,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )
}
