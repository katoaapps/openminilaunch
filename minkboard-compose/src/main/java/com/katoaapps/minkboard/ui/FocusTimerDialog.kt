package com.katoaapps.minkboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.katoaapps.minkboard.core.FocusTimerRules

@Composable
internal fun FocusTimerDialog(
    draft: FocusTimerEditorDraft,
    replacesRunningTimer: Boolean,
    onDraftChange: (FocusTimerEditorDraft) -> Unit,
    onDismiss: () -> Unit,
    onStart: (durationMillis: Long, replaceExisting: Boolean) -> Unit,
) {
    var invalid by remember { mutableStateOf(false) }
    val numbers = KeyboardOptions(keyboardType = KeyboardType.Number)
    MinkBoardAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.focus_timer)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.focus_duration))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = draft.hours,
                        onValueChange = {
                            onDraftChange(draft.copy(hours = it.filter(Char::isDigit).take(2)))
                            invalid = false
                        },
                        label = { Text(stringResource(R.string.hours)) },
                        keyboardOptions = numbers,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = draft.minutes,
                        onValueChange = {
                            onDraftChange(draft.copy(minutes = it.filter(Char::isDigit).take(2)))
                            invalid = false
                        },
                        label = { Text(stringResource(R.string.minutes)) },
                        keyboardOptions = numbers,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = draft.seconds,
                        onValueChange = {
                            onDraftChange(draft.copy(seconds = it.filter(Char::isDigit).take(2)))
                            invalid = false
                        },
                        label = { Text(stringResource(R.string.seconds)) },
                        keyboardOptions = numbers,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (invalid) Text(stringResource(R.string.invalid_focus_duration))
                if (replacesRunningTimer) Text(stringResource(R.string.replace_focus_timer))
            }
        },
        confirmButton = {
            Button(onClick = {
                val duration = runCatching {
                    FocusTimerRules.durationMillis(
                        draft.hours.toIntOrNull() ?: 0,
                        draft.minutes.toIntOrNull() ?: 0,
                        draft.seconds.toIntOrNull() ?: 0,
                    )
                }.getOrNull()
                if (duration == null) invalid = true else onStart(duration, replacesRunningTimer)
            }) {
                Text(stringResource(if (replacesRunningTimer) R.string.replace_and_start else R.string.start_focus))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
