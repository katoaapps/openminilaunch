package com.katoaapps.minkboard.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.katoaapps.minkboard.model.Board
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.StatusId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoardMapScreenInteractionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun tappingMiniatureStatusReturnsItsIdentityToTheDetailedBoard() {
        val boardId = BoardId("board")
        val first = status(boardId, "inbox", "Inbox", 0)
        val second = status(boardId, "review", "Review", 1)
        val snapshot = BoardSnapshot(
            board = Board(
                id = boardId,
                name = "Product",
                isLocked = false,
                position = 0,
                createdAtEpochMillis = 0L,
            ),
            statuses = listOf(first, second),
            cards = emptyList(),
        )
        var selectedStatusId: StatusId? = null

        composeRule.setContent {
            MaterialTheme {
                BoardMapScreen(
                    board = snapshot,
                    focusedStatusId = first.id,
                    listState = rememberLazyListState(),
                    onDismiss = {},
                    onSelectStatus = { selectedStatusId = it.id },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Open Review").performClick()
        composeRule.runOnIdle { assertEquals(second.id, selectedStatusId) }
    }

    private fun status(
        boardId: BoardId,
        id: String,
        name: String,
        position: Int,
    ) = BoardStatus(
        id = StatusId(id),
        boardId = boardId,
        name = name,
        colorArgb = 0xFF245A44.toInt(),
        isDoneStatus = false,
        position = position,
    )
}
