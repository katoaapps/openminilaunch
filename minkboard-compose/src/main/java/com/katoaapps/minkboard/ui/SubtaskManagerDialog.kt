package com.katoaapps.minkboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.katoaapps.minkboard.core.MinkBoardRules
import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardSubtask

@Composable
internal fun SubtaskManagerDialog(
    card: BoardCard,
    draft: SubtaskManagerDraft,
    onDraftChange: (SubtaskManagerDraft) -> Unit,
    onDismiss: () -> Unit,
    onAddSubtask: (String, () -> Unit) -> Unit,
    onUpdateSubtask: (BoardSubtask, String, () -> Unit) -> Unit,
    onToggleSubtask: (BoardSubtask, Boolean) -> Unit,
    onDeleteSubtask: (BoardSubtask) -> Unit,
) {
    val latestDraft by rememberUpdatedState(draft)
    val orderedSubtasks = card.subtasks.sortedBy { it.position }
    val completedCount = orderedSubtasks.count(BoardSubtask::isCompleted)
    val editingSubtask = orderedSubtasks.firstOrNull {
        it.id.value == draft.editingSubtaskIdValue
    }

    fun saveDraftAndDismiss() {
        val pendingText = latestDraft.pendingSubtaskText.trim()
        val editedText = latestDraft.editingSubtaskText.trim()

        fun savePendingOrDismiss() {
            if (pendingText.isNotEmpty()) {
                onAddSubtask(pendingText, onDismiss)
            } else {
                onDismiss()
            }
        }

        if (editingSubtask != null && editedText.isNotEmpty()) {
            onUpdateSubtask(editingSubtask, editedText, ::savePendingOrDismiss)
        } else {
            savePendingOrDismiss()
        }
    }

    MinkBoardAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manage_subtasks)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(card.title, style = MaterialTheme.typography.titleMedium)
                if (orderedSubtasks.isNotEmpty()) {
                    Text(
                        pluralStringResource(
                            R.plurals.subtask_progress,
                            orderedSubtasks.size,
                            completedCount,
                            orderedSubtasks.size,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    LinearProgressIndicator(
                        progress = { completedCount.toFloat() / orderedSubtasks.size },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        stringResource(R.string.no_subtasks),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                orderedSubtasks.forEach { subtask ->
                    if (draft.editingSubtaskIdValue == subtask.id.value) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = draft.editingSubtaskText,
                                onValueChange = {
                                    onDraftChange(
                                        draft.copy(
                                            editingSubtaskText = it.take(
                                                MinkBoardRules.MAX_SUBTASK_LENGTH,
                                            ),
                                        ),
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.edit_subtask)) },
                                singleLine = true,
                            )
                            IconButton(
                                enabled = draft.editingSubtaskText.isNotBlank(),
                                onClick = {
                                    onUpdateSubtask(subtask, draft.editingSubtaskText) {
                                        onDraftChange(
                                            latestDraft.copy(
                                                editingSubtaskIdValue = null,
                                                editingSubtaskText = "",
                                            ),
                                        )
                                    }
                                },
                            ) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = stringResource(R.string.save),
                                )
                            }
                            IconButton(
                                onClick = {
                                    onDraftChange(
                                        draft.copy(
                                            editingSubtaskIdValue = null,
                                            editingSubtaskText = "",
                                        ),
                                    )
                                },
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.cancel),
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = subtask.isCompleted,
                                onCheckedChange = { checked -> onToggleSubtask(subtask, checked) },
                            )
                            Text(subtask.text, modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    onDraftChange(
                                        draft.copy(
                                            editingSubtaskIdValue = subtask.id.value,
                                            editingSubtaskText = subtask.text,
                                        ),
                                    )
                                },
                            ) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = stringResource(R.string.edit_subtask),
                                )
                            }
                            IconButton(onClick = { onDeleteSubtask(subtask) }) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = draft.pendingSubtaskText,
                        onValueChange = {
                            onDraftChange(
                                draft.copy(
                                    pendingSubtaskText = it.take(MinkBoardRules.MAX_SUBTASK_LENGTH),
                                ),
                            )
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.add_subtask)) },
                        singleLine = true,
                    )
                    IconButton(
                        enabled = draft.pendingSubtaskText.isNotBlank(),
                        onClick = {
                            onAddSubtask(draft.pendingSubtaskText) {
                                onDraftChange(latestDraft.copy(pendingSubtaskText = ""))
                            }
                        },
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.add_subtask_action),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = ::saveDraftAndDismiss,
                enabled = editingSubtask == null || draft.editingSubtaskText.isNotBlank(),
            ) {
                Text(stringResource(R.string.done))
            }
        },
    )
}
