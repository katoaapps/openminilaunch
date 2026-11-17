package com.katoaapps.minkboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.katoaapps.minkboard.core.BoardTemplateId
import com.katoaapps.minkboard.core.MinkBoardTemplates
import com.katoaapps.minkboard.model.StatusDraft
import com.katoaapps.minkboard.core.MinkBoardRules

@Composable
internal fun BoardNameDialog(
    draft: BoardEditorDraft,
    canLockBoard: Boolean,
    supportsTemplates: Boolean,
    onDraftChange: (BoardEditorDraft) -> Unit,
    onDismiss: () -> Unit,
    onCreate: (String, BoardTemplateId?, Boolean) -> Unit,
) {
    var showError by remember { mutableStateOf(false) }
    val selectedTemplateValue = draft.templateIdValue ?: MinkBoardTemplates.Simple.id.value
    val useCompactTemplateCards = LocalDensity.current.fontScale >= 1.5f
    MinkBoardAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_board)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.protect_board))
                        Text(
                            stringResource(R.string.protect_board_description),
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = draft.isLocked,
                        onCheckedChange = { onDraftChange(draft.copy(isLocked = it)) },
                        enabled = canLockBoard,
                    )
                }
                if (!canLockBoard) {
                    Text(
                        stringResource(R.string.protect_board_unavailable),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = {
                        onDraftChange(
                            draft.copy(name = it.take(MinkBoardRules.MAX_BOARD_NAME_LENGTH)),
                        )
                        showError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.board_name)) },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(stringResource(R.string.required_field)) }
                    } else null,
                    singleLine = true,
                )
                if (supportsTemplates) {
                    Text(
                        text = stringResource(R.string.board_template),
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    MinkBoardTemplates.all.forEach { template ->
                        val selected = selectedTemplateValue == template.id.value
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        onDraftChange(
                                            draft.copy(templateIdValue = template.id.value),
                                        )
                                    },
                                ),
                            shape = androidx.compose.material3.MaterialTheme.shapes.medium,
                            color = if (selected) {
                                androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null,
                                    modifier = Modifier.clearAndSetSemantics { },
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = template.name,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = template.description,
                                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = if (useCompactTemplateCards) 1 else 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (draft.name.isBlank()) {
                    showError = true
                } else {
                    val templateId = selectedTemplateValue
                        .takeIf { supportsTemplates }
                        ?.let(::BoardTemplateId)
                    onCreate(draft.name, templateId, draft.isLocked)
                }
            }) { Text(stringResource(R.string.create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
@Composable
internal fun StatusDialog(
    draft: StatusEditorDraft,
    replacesDoneStatus: Boolean,
    onDraftChange: (StatusEditorDraft) -> Unit,
    onDismiss: () -> Unit,
    onCreate: (StatusDraft) -> Unit,
) {
    var showError by remember { mutableStateOf(false) }
    MinkBoardAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_status)) },
        text = {
            Box(modifier = Modifier.heightIn(max = 360.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    OutlinedTextField(
                        value = draft.name,
                        onValueChange = {
                            onDraftChange(
                                draft.copy(name = it.take(MinkBoardRules.MAX_STATUS_NAME_LENGTH)),
                            )
                            showError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.status_name)) },
                        isError = showError,
                        singleLine = true,
                    )
                    StatusColorPicker(
                        selectedColor = draft.colorArgb,
                        customColorDraft = draft.customColorDraft,
                        onColorSelected = {
                            onDraftChange(draft.copy(colorArgb = it, customColorDraft = null))
                        },
                        onCustomColorDraftChange = {
                            onDraftChange(draft.copy(customColorDraft = it))
                        },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(stringResource(R.string.done_status))
                        Switch(
                            checked = draft.isDone,
                            onCheckedChange = { onDraftChange(draft.copy(isDone = it)) },
                        )
                    }
                    if (draft.isDone && replacesDoneStatus) {
                        Text(stringResource(R.string.done_status_replacement))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (draft.name.isBlank()) {
                    showError = true
                } else {
                    onCreate(StatusDraft(draft.name, draft.colorArgb, draft.isDone))
                }
            }) { Text(stringResource(R.string.create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
