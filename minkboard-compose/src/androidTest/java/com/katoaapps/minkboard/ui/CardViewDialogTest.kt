package com.katoaapps.minkboard.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.StatusId
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardViewDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun overflowListsCardActionsInProductOrder() {
        val boardId = BoardId("board")
        val status = BoardStatus(
            id = StatusId("status"),
            boardId = boardId,
            name = "In progress",
            colorArgb = DEFAULT_STATUS_COLOR_ARGB,
            isDoneStatus = false,
            position = 0,
        )
        val card = BoardCard(
            id = CardId("card"),
            statusId = status.id,
            title = "Ship the release",
            details = "",
            dueDate = null,
            isMajor = false,
            position = 0,
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
            subtasks = emptyList(),
        )

        composeRule.setContent {
            MaterialTheme {
                CardViewDialog(
                    card = card,
                    status = status,
                    isFocusRunningForCard = false,
                    onDismiss = {},
                    onEdit = {},
                    onManageSubtasks = {},
                    onFocus = {},
                    canMarkDone = true,
                    onMarkDone = {},
                    canSendToTop = true,
                    canSendToBottom = true,
                    onSendToTop = {},
                    onSendToBottom = {},
                    onDuplicate = {},
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Card options").performClick()

        val actionTops = listOf(
            "Mark done",
            "Subtasks",
            "Start focus timer",
            "Send to top",
            "Send to bottom",
            "Duplicate card",
            "Delete",
        ).map { label ->
            composeRule.onNodeWithText(label).fetchSemanticsNode().boundsInRoot.top
        }
        assertTrue(actionTops.zipWithNext().all { (first, second) -> first < second })
    }
}
