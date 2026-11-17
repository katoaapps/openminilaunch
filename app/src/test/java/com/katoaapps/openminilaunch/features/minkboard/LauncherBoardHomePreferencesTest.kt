package com.katoaapps.openminilaunch.features.minkboard

import com.katoaapps.minkboard.model.Board
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.StatusId
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherBoardHomePreferencesTest {
    @Test
    fun allPreservesBoardStatusOrder() {
        assertEquals(
            listOf(StatusId("todo"), StatusId("doing"), StatusId("done")),
            visibleStatusIds(BOARD, StatusId("todo"), LauncherVisibleStatuses.All),
        )
    }

    @Test
    fun selectedUsesBoardOrderRatherThanPreferenceOrder() {
        assertEquals(
            listOf(StatusId("doing"), StatusId("done")),
            visibleStatusIds(
                BOARD,
                StatusId("todo"),
                LauncherVisibleStatuses.Selected(
                    listOf(StatusId("done"), StatusId("missing"), StatusId("doing")),
                ),
            ),
        )
    }

    @Test
    fun staleEmptySelectionFallsBackToIntake() {
        assertEquals(
            listOf(StatusId("todo")),
            visibleStatusIds(
                BOARD,
                StatusId("todo"),
                LauncherVisibleStatuses.Selected(listOf(StatusId("missing"))),
            ),
        )
    }

    private companion object {
        val BOARD_ID = BoardId("board")
        val BOARD = BoardSnapshot(
            board = Board(
                id = BOARD_ID,
                name = "To-Dos",
                isLocked = false,
                position = 0,
                createdAtEpochMillis = 0,
            ),
            statuses = listOf(
                BoardStatus(StatusId("todo"), BOARD_ID, "To do", 1, false, 0),
                BoardStatus(StatusId("doing"), BOARD_ID, "Doing", 2, false, 1),
                BoardStatus(StatusId("done"), BOARD_ID, "Done", 3, true, 2),
            ),
            cards = emptyList(),
        )
    }
}
