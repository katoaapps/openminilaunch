package com.katoaapps.minkboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DueDatePickerField(
    dueDateText: String,
    onDueDateChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedDate = remember(dueDateText) {
        runCatching { LocalDate.parse(dueDateText) }.getOrNull()
    }
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.due_date))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { showPicker = true },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                Text(
                    text = selectedDate?.toString() ?: stringResource(R.string.choose_due_date),
                    modifier = Modifier.weight(1f),
                )
            }
            if (selectedDate != null) {
                TextButton(onClick = { onDueDateChange("") }) {
                    Text(stringResource(R.string.clear))
                }
            }
        }
    }

    if (showPicker) {
        val pickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()
                ?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedDateMillis != null,
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            onDueDateChange(date.toString())
                        }
                        showPicker = false
                    },
                ) { Text(stringResource(R.string.done)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
