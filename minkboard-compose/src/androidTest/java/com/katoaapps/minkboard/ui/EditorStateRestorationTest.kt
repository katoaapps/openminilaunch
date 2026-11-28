package com.katoaapps.minkboard.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.BoardSubtask
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.minkboard.model.SubtaskId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class EditorStateRestorationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun controlledNewCardEditorReconstructsExactRestoredDraftWithoutCommitting() {
        var draft by mutableStateOf(CardEditorDraft())
        var createCount = 0
        var dismissCount = 0
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            MaterialTheme {
                NewCardDialog(
                    draft = draft,
                    onDraftChange = { draft = it },
                    onDismiss = { dismissCount += 1 },
                    onCreate = { createCount += 1 },
                )
            }
        }

        taggedTextField(CARD_TITLE_FIELD_TEST_TAG)
            .performTextReplacement("Ship MinkBoard")
        taggedTextField(CARD_DETAILS_FIELD_TEST_TAG)
            .performTextReplacement("Preserve this exact draft 🦦")
        composeRule.runOnIdle {
            draft = draft.copy(
                dueDateText = "2027-03-04",
                isMajor = true,
            )
        }
        val restoredDraft = draft

        // The production host reconstructs this same controlled editor state after decrypting its
        // SavedStateHandle payload. Recreating the Compose registry must not overwrite that value.
        restoration.emulateSavedInstanceStateRestore()

        assertEquals(restoredDraft, draft)
        taggedTextField(CARD_TITLE_FIELD_TEST_TAG)
            .assertEditableText("Ship MinkBoard")
        taggedTextField(CARD_DETAILS_FIELD_TEST_TAG)
            .assertEditableText("Preserve this exact draft 🦦")
        assertEquals(true, draft.isMajor)
        assertEquals(0, createCount)
        assertEquals(0, dismissCount)

        composeRule.onNodeWithText("Cancel").performClick()
        assertEquals(0, createCount)
        assertEquals(1, dismissCount)
    }

    @Test
    fun longEditCardBodyKeepsSaveAndCancelVisible() {
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
            title = "Ship MinkBoard",
            details = "Long note body.\n".repeat(100),
            dueDate = null,
            isMajor = false,
            position = 0,
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )
        var draft by mutableStateOf(
            CardEditorDraft(
                title = card.title,
                details = card.details,
                destinationStatusIdValue = status.id.value,
            ),
        )

        composeRule.setContent {
            MaterialTheme {
                EditCardDialog(
                    card = card,
                    draft = draft,
                    statuses = listOf(status),
                    currentStatus = status,
                    onDraftChange = { draft = it },
                    onDismiss = {},
                    onSave = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.save))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.cancel))
            .assertIsDisplayed()
    }

    @Test
    fun editSubtaskDispatchesUpdateAndDisplaysPersistedText() {
        val boardId = BoardId("board")
        val status = BoardStatus(
            id = StatusId("status"),
            boardId = boardId,
            name = "In progress",
            colorArgb = DEFAULT_STATUS_COLOR_ARGB,
            isDoneStatus = false,
            position = 0,
        )
        val cardId = CardId("card")
        val subtaskId = SubtaskId("subtask")
        var card by mutableStateOf(
            BoardCard(
                id = cardId,
                statusId = status.id,
                title = "Ship MinkBoard",
                details = "",
                dueDate = null,
                isMajor = false,
                position = 0,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L,
                subtasks = listOf(
                    BoardSubtask(
                        id = subtaskId,
                        cardId = cardId,
                        text = "Original step",
                        isCompleted = true,
                        position = 0,
                    ),
                ),
            ),
        )
        var draft by mutableStateOf(SubtaskManagerDraft())
        var updatedText: String? = null
        val editSubtaskLabel = composeRule.activity.getString(R.string.edit_subtask)
        val saveLabel = composeRule.activity.getString(R.string.save)

        composeRule.setContent {
            MaterialTheme {
                SubtaskManagerDialog(
                    card = card,
                    draft = draft,
                    onDraftChange = { draft = it },
                    onDismiss = {},
                    onAddSubtask = { _, _ -> },
                    onUpdateSubtask = { subtask, text, onSuccess ->
                        updatedText = text
                        card = card.copy(
                            subtasks = card.subtasks.map { current ->
                                if (current.id == subtask.id) current.copy(text = text) else current
                            },
                        )
                        onSuccess()
                    },
                    onToggleSubtask = { _, _ -> },
                    onDeleteSubtask = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription(editSubtaskLabel)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        editableTextField("Original step").performTextReplacement("Updated step")
        composeRule.onNodeWithContentDescription(saveLabel).performClick()

        composeRule.runOnIdle { assertEquals("Updated step", updatedText) }
        composeRule.onNodeWithText("Updated step").assertExists()
        assertEquals(true, card.subtasks.single().isCompleted)
        assertEquals(0, card.subtasks.single().position)
    }

    @Test
    fun doneSavesActiveEditAndPendingSubtaskBeforeDismissing() {
        val cardId = CardId("card")
        val existing = BoardSubtask(
            id = SubtaskId("existing"),
            cardId = cardId,
            text = "Original step",
            isCompleted = false,
            position = 0,
        )
        var card by mutableStateOf(
            BoardCard(
                id = cardId,
                statusId = StatusId("status"),
                title = "Ship MinkBoard",
                details = "",
                dueDate = null,
                isMajor = false,
                position = 0,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L,
                subtasks = listOf(existing),
            ),
        )
        var draft by mutableStateOf(
            SubtaskManagerDraft(
                pendingSubtaskText = "New step",
                editingSubtaskIdValue = existing.id.value,
                editingSubtaskText = "Updated step",
            ),
        )
        val operations = mutableListOf<String>()

        composeRule.setContent {
            MaterialTheme {
                SubtaskManagerDialog(
                    card = card,
                    draft = draft,
                    onDraftChange = { draft = it },
                    onDismiss = { operations += "dismiss" },
                    onAddSubtask = { text, onSuccess ->
                        operations += "add:$text"
                        onSuccess()
                    },
                    onUpdateSubtask = { subtask, text, onSuccess ->
                        operations += "update:${subtask.id.value}:$text"
                        card = card.copy(
                            subtasks = card.subtasks.map { current ->
                                if (current.id == subtask.id) current.copy(text = text) else current
                            },
                        )
                        onSuccess()
                    },
                    onToggleSubtask = { _, _ -> },
                    onDeleteSubtask = {},
                )
            }
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.done)).performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf("update:existing:Updated step", "add:New step", "dismiss"),
                operations,
            )
        }
    }

    @Test
    fun archivePassphrasesAreAbsentAfterSavedStateRestoration() {
        var exportCount = 0
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            MaterialTheme {
                PortableArchiveWorkflowDialog(
                    request = PortableArchiveWorkflowRequest.Export,
                    errorMessage = null,
                    isWorking = false,
                    onDismiss = {},
                    onExport = { exportCount += 1 },
                    onImport = { _, _ -> error("Import is unavailable in export mode") },
                )
            }
        }

        textFields()[0].performTextReplacement("secret phrase")
        textFields()[1].performTextReplacement("secret phrase")

        restoration.emulateSavedInstanceStateRestore()

        textFields()[0].assertEditableText("")
        textFields()[1].assertEditableText("")
        assertEquals(0, exportCount)
    }

    @Test
    fun archiveImportSecretIsAbsentAfterSavedStateRestoration() {
        var importCount = 0
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            MaterialTheme {
                PortableArchiveWorkflowDialog(
                    request = PortableArchiveWorkflowRequest.Import(
                        displayName = "launcher.minkbackup",
                        requiresPassphrase = true,
                        requiresBoardName = false,
                    ),
                    errorMessage = null,
                    isWorking = false,
                    onDismiss = {},
                    onExport = { error("Export is unavailable in import mode") },
                    onImport = { _, _ -> importCount += 1 },
                )
            }
        }

        textFields().single().performTextReplacement("secret phrase")
        restoration.emulateSavedInstanceStateRestore()
        textFields().single().assertEditableText("")
        assertEquals(0, importCount)
    }

    @Test
    fun batchImportIdentifiesTheCurrentArchiveAndItsProgress() {
        composeRule.setContent {
            MaterialTheme {
                PortableArchiveWorkflowDialog(
                    request = PortableArchiveWorkflowRequest.Import(
                        displayName = "quarterly-goals.minkboard",
                        requiresPassphrase = true,
                        requiresBoardName = false,
                        batchPosition = 2,
                        batchSize = 5,
                    ),
                    errorMessage = null,
                    isWorking = false,
                    onDismiss = {},
                    onExport = { error("Export is unavailable in import mode") },
                    onImport = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("Archive 2 of 5").assertExists()
        composeRule.onNodeWithText("quarterly-goals.minkboard").assertExists()
        composeRule.onNodeWithText(
            "Use at least 8 characters. You’ll unlock each selected archive separately.",
        ).assertExists()
    }

    @Test
    fun workingArchiveImportReplacesThePasswordFormWithProgress() {
        composeRule.setContent {
            MaterialTheme {
                PortableArchiveWorkflowDialog(
                    request = PortableArchiveWorkflowRequest.Import(
                        displayName = "massive-product-roadmap-demo.minkboard",
                        requiresPassphrase = true,
                        requiresBoardName = false,
                    ),
                    errorMessage = null,
                    isWorking = true,
                    onDismiss = {},
                    onExport = { error("Export is unavailable in import mode") },
                    onImport = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("massive-product-roadmap-demo.minkboard").assertExists()
        composeRule.onNodeWithText("Decrypting and importing this board…").assertExists()
        assertEquals(0, textFields().size)
    }

    private fun textFields(): List<SemanticsNodeInteraction> {
        val nodes = composeRule.onAllNodes(hasSetTextAction())
        return nodes.fetchSemanticsNodes().indices.map(nodes::get)
    }

    private fun taggedTextField(tag: String): SemanticsNodeInteraction =
        composeRule.onNode(hasTestTag(tag).and(hasSetTextAction()))

    private fun editableTextField(text: String): SemanticsNodeInteraction {
        val nodes = composeRule.onAllNodes(hasSetTextAction())
        return nodes.fetchSemanticsNodes().indices
            .map(nodes::get)
            .single { node ->
                node.fetchSemanticsNode().config[SemanticsProperties.EditableText].text == text
            }
    }

    private fun SemanticsNodeInteraction.assertEditableText(expected: String) {
        val actual = fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        assertEquals(expected, actual)
    }
}
