package com.katoaapps.openminilaunch.ui.minkspace.todo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkspace.todo.TodoItem
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Rust

@Composable
internal fun EditTodoDialog(
    item: TodoItem,
    largeDisplay: Boolean,
    textStyle: TextStyle,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var editText by remember(item.id) { mutableStateOf(item.text) }
    AlertDialog(
        modifier = Modifier.minkDialogWidth(),
        onDismissRequest = onDismiss,
        properties = MinkDialogDefaults.properties,
        title = { Text(stringResource(R.string.edit_todo)) },
        text = {
            OutlinedTextField(
                value = editText,
                onValueChange = { editText = it },
                modifier = Modifier.fillMaxWidth().heightIn(
                    min = if (largeDisplay) Dimens.dp240 else Dimens.dp160,
                    max = if (largeDisplay) Dimens.dp420 else Dimens.dp280,
                ),
                textStyle = textStyle,
                minLines = 5,
                maxLines = 10,
                singleLine = false,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (editText.isNotBlank()) onSave(editText)
                    onDismiss()
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
internal fun DeleteTodoDialog(
    item: TodoItem,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.minkDialogWidth(),
        onDismissRequest = onDismiss,
        properties = MinkDialogDefaults.properties,
        icon = { Icon(Icons.Default.DeleteOutline, null, tint = Rust) },
        title = { Text(stringResource(R.string.delete_todo_title)) },
        text = { Text(stringResource(R.string.delete_todo_description, item.text)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDelete()
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.delete), color = Rust)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
internal fun TodoExportDialog(
    onSendToNotes: () -> Unit,
    onSavePdf: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.minkDialogWidth(),
        onDismissRequest = onDismiss,
        properties = MinkDialogDefaults.properties,
        icon = { Icon(Icons.Default.IosShare, null) },
        title = { Text(stringResource(R.string.export_todo_list)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                Text(stringResource(R.string.export_todo_list_description))
                FilledTonalButton(onClick = onSendToNotes, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Filled.NoteAdd, null)
                    Spacer(Modifier.width(Dimens.dp8))
                    Text(stringResource(R.string.send_to_notes_app))
                }
                FilledTonalButton(onClick = onSavePdf, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PictureAsPdf, null)
                    Spacer(Modifier.width(Dimens.dp8))
                    Text(stringResource(R.string.save_as_pdf))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
