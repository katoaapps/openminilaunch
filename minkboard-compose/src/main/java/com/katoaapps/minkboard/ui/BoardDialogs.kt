package com.katoaapps.minkboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.katoaapps.minkboard.core.MinkBoardRules
import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.CardDraft
import com.katoaapps.minkboard.model.CardUpdate
import java.time.LocalDate

internal const val CARD_TITLE_FIELD_TEST_TAG = "card-title-field"
internal const val CARD_DETAILS_FIELD_TEST_TAG = "card-details-field"

@Composable
internal fun NewCardDialog(
    draft: CardEditorDraft,
    onDraftChange: (CardEditorDraft) -> Unit,
    onDismiss: () -> Unit,
    onCreate: (CardDraft) -> Unit,
) {
    CardFormDialog(
        title = stringResource(R.string.new_card),
        draft = draft,
        onDraftChange = onDraftChange,
        onDismiss = onDismiss,
        onSave = { title, details, date, major ->
            onCreate(CardDraft(title, details, date, major))
        },
    )
}

@Composable
private fun CardFormDialog(
    title: String,
    draft: CardEditorDraft,
    onDraftChange: (CardEditorDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String, LocalDate?, Boolean) -> Unit,
) {
    var titleError by remember { mutableStateOf(false) }

    MinkBoardAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = {
                        onDraftChange(
                            draft.copy(title = it.take(MinkBoardRules.MAX_CARD_TITLE_LENGTH)),
                        )
                        titleError = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(CARD_TITLE_FIELD_TEST_TAG),
                    label = { Text(stringResource(R.string.card_title)) },
                    isError = titleError,
                )
                OutlinedTextField(
                    value = draft.details,
                    onValueChange = {
                        onDraftChange(
                            draft.copy(details = it.take(MinkBoardRules.MAX_CARD_DETAILS_LENGTH)),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(CARD_DETAILS_FIELD_TEST_TAG),
                    label = { Text(stringResource(R.string.card_details)) },
                    minLines = 3,
                )
                DueDatePickerField(
                    dueDateText = draft.dueDateText,
                    onDueDateChange = { onDraftChange(draft.copy(dueDateText = it)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = draft.isMajor,
                        onCheckedChange = { onDraftChange(draft.copy(isMajor = it)) },
                    )
                    Text(stringResource(R.string.major_card))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsedDate = draft.dueDateText.takeIf(String::isNotEmpty)?.let(LocalDate::parse)
                if (draft.title.isBlank()) {
                    titleError = true
                } else {
                    onSave(draft.title, draft.details, parsedDate, draft.isMajor)
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditCardDialog(
    card: BoardCard,
    draft: CardEditorDraft,
    statuses: List<BoardStatus>,
    currentStatus: BoardStatus,
    onDraftChange: (CardEditorDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: (CardUpdate, BoardStatus) -> Unit,
) {
    var titleError by remember(card.id.value) { mutableStateOf(false) }
    var statusMenuOpen by remember(card.id.value) { mutableStateOf(false) }
    val selectedStatus = statuses.firstOrNull {
        it.id.value == draft.destinationStatusIdValue
    } ?: currentStatus
    val saveCard = {
        val parsedDate = draft.dueDateText
            .takeIf(String::isNotEmpty)
            ?.let(LocalDate::parse)
        if (draft.title.isBlank()) {
            titleError = true
        } else {
            onSave(
                CardUpdate(
                    draft.title,
                    draft.details,
                    parsedDate,
                    draft.isMajor,
                ),
                selectedStatus,
            )
        }
    }

    MinkBoardAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.edit_card),
                    modifier = Modifier.weight(1f),
                )
                IconToggleButton(
                    checked = draft.isMajor,
                    onCheckedChange = { onDraftChange(draft.copy(isMajor = it)) },
                ) {
                    Icon(
                        imageVector = if (draft.isMajor) {
                            Icons.Filled.Star
                        } else {
                            Icons.Outlined.StarOutline
                        },
                        contentDescription = stringResource(R.string.important),
                        tint = if (draft.isMajor) {
                            IMPORTANT_COLOR
                        } else {
                            androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                Button(onClick = saveCard) {
                    Text(stringResource(R.string.save))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = {
                        onDraftChange(
                            draft.copy(title = it.take(MinkBoardRules.MAX_CARD_TITLE_LENGTH)),
                        )
                        titleError = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(CARD_TITLE_FIELD_TEST_TAG),
                    label = { Text(stringResource(R.string.card_title)) },
                    isError = titleError,
                    singleLine = true,
                )
                ExposedDropdownMenuBox(
                    expanded = statusMenuOpen,
                    onExpandedChange = { statusMenuOpen = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = selectedStatus.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.status)) },
                        leadingIcon = {
                            Box(
                                Modifier
                                    .size(14.dp)
                                    .background(
                                        Color(selectedStatus.colorArgb),
                                        CircleShape,
                                    ),
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusMenuOpen)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = statusMenuOpen,
                        onDismissRequest = { statusMenuOpen = false },
                    ) {
                        statuses.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.name) },
                                leadingIcon = if (status.id == selectedStatus.id) {
                                    { Icon(Icons.Outlined.Check, contentDescription = null) }
                                } else null,
                                onClick = {
                                    statusMenuOpen = false
                                    onDraftChange(
                                        draft.copy(destinationStatusIdValue = status.id.value),
                                    )
                                },
                            )
                        }
                    }
                }
                DueDatePickerField(
                    dueDateText = draft.dueDateText,
                    onDueDateChange = { onDraftChange(draft.copy(dueDateText = it)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.details,
                    onValueChange = {
                        onDraftChange(
                            draft.copy(details = it.take(MinkBoardRules.MAX_CARD_DETAILS_LENGTH)),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(CARD_DETAILS_FIELD_TEST_TAG),
                    label = { Text(stringResource(R.string.card_details)) },
                    minLines = 5,
                    maxLines = 10,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
internal fun DeleteCardConfirmationDialog(
    cardTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    MinkBoardAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_card_title)) },
        text = { Text(stringResource(R.string.delete_card_body, cardTitle)) },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
