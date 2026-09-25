package com.katoaapps.openminilaunch.ui.magic

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MagicInputField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    prefix: Char?,
    focusRequester: FocusRequester,
    onPlaced: () -> Unit,
    onSubmit: () -> Unit,
    onHardwareKeyUp: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = 5,
) {
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var preferredVerticalX by remember { mutableStateOf<Float?>(null) }
    val interactionSource = remember { MutableInteractionSource() }
    val colors = TextFieldDefaults.colors(
        focusedContainerColor = MinkTransparent,
        unfocusedContainerColor = MinkTransparent,
        focusedIndicatorColor = MinkTransparent,
        unfocusedIndicatorColor = MinkTransparent,
    )
    val updateValue: (TextFieldValue) -> Unit = { updated ->
        preferredVerticalX = null
        onValueChange(updated)
    }
    val keyboardOptions = KeyboardOptions(
        showKeyboardOnFocus = false,
        imeAction = when {
            prefix == MAGIC_NOTE_PREFIX -> ImeAction.Default
            prefix in listOf('@', '#', '-', '+', '?') -> ImeAction.Send
            else -> ImeAction.Search
        },
    )
    val keyboardActions = KeyboardActions(
        onSearch = { onSubmit() },
        onSend = { onSubmit() },
    )

    BasicTextField(
        value = value,
        onValueChange = updateValue,
        modifier = modifier.focusRequester(focusRequester)
            .onGloballyPositioned { onPlaced() }
            .onPreviewKeyEvent { event ->
                val nativeEvent = event.nativeKeyEvent
                if (event.type == KeyEventType.KeyUp) {
                    onHardwareKeyUp(nativeEvent.keyCode)
                }
                if (prefix == '-' && event.key == Key.Enter) {
                    if (event.type == KeyEventType.KeyUp) onSubmit()
                    true
                } else {
                    handleMagicHardwareEditing(
                        event = event,
                        value = value,
                        textLayout = textLayout,
                        preferredVerticalX = preferredVerticalX,
                        onPreferredVerticalXChange = { preferredVerticalX = it },
                        onValueChange = onValueChange,
                    )
                }
            },
        textStyle = LocalTextStyle.current.merge(
            TextStyle(color = MaterialTheme.colorScheme.onSurface),
        ),
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        onTextLayout = { textLayout = it },
        decorationBox = { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = value.text,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = false,
                visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                interactionSource = interactionSource,
                placeholder = {
                    Text(stringResource(R.string.magic_box_hotkey_hint), color = Muted)
                },
                colors = colors,
            )
        },
    )
}
