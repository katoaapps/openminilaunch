package com.katoaapps.openminilaunch.ui.magic

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.magic.MAGIC_NOTE_PREFIX
import com.katoaapps.openminilaunch.ui.theme.MinkTransparent
import com.katoaapps.openminilaunch.ui.theme.Muted

internal data class MagicActionVisuals(
    val color: Color,
    val icon: ImageVector,
)

/** The single text-entry surface shared by expanded and note modes. */
@Composable
internal fun MagicInputField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    prefix: Char?,
    focusRequester: FocusRequester,
    onPlaced: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = 5,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(stringResource(R.string.magic_box_hotkey_hint), color = Muted) },
        modifier = modifier.focusRequester(focusRequester)
            .onGloballyPositioned { onPlaced() }
            .onPreviewKeyEvent { event ->
                if (prefix == '-' && event.key == Key.Enter) {
                    if (event.type == KeyEventType.KeyUp) onSubmit()
                    true
                } else {
                    false
                }
            },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MinkTransparent,
            unfocusedContainerColor = MinkTransparent,
            focusedIndicatorColor = MinkTransparent,
            unfocusedIndicatorColor = MinkTransparent,
        ),
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(
            showKeyboardOnFocus = false,
            imeAction = when {
                prefix == MAGIC_NOTE_PREFIX -> ImeAction.Default
                prefix in listOf('@', '#', '-', '+', '?') -> ImeAction.Send
                else -> ImeAction.Search
            },
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSubmit() },
            onSend = { onSubmit() },
        ),
    )
}
