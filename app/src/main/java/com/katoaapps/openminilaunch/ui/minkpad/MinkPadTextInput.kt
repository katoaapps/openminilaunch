package com.katoaapps.openminilaunch.ui.minkpad

import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.katoaapps.openminilaunch.R

/** The editor is an explicit software-keyboard bridge. Hardware keys use Activity dispatch only. */
@Composable
internal fun MinkPadLiveTextInput(
    enabled: Boolean,
    onText: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onForwardDelete: () -> Unit = {},
) {
    val configuration = LocalConfiguration.current
    val hardwareKeyboard = configuration.keyboard == Configuration.KEYBOARD_QWERTY &&
        configuration.hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_YES
    // The bottom keyboard control is already an explicit request to type. Touch-only phones
    // should not require a second tap before their IME appears.
    var softwareEditorOpen by remember(enabled, hardwareKeyboard) { mutableStateOf(!hardwareKeyboard) }
    val showEditor = !hardwareKeyboard && softwareEditorOpen

    Column {
        if (!hardwareKeyboard) {
            TextButton(onClick = { softwareEditorOpen = !softwareEditorOpen }, enabled = enabled) {
                Text(stringResource(if (showEditor) R.string.minkpad_hide_keyboard else R.string.minkpad_type))
            }
        }
        AndroidView(
            // GONE, INVISIBLE, and zero size can lose the input connection. A transparent,
            // attached 1 dp editor keeps the IME active without drawing a text field.
            modifier = Modifier.size(1.dp),
            factory = { MinkPadInputEditor(it) },
            update = {
                it.commit = onText
                it.backspace = onBackspace
                it.enter = onEnter
                it.forwardDelete = onForwardDelete
                it.alpha = 0f
                it.isCursorVisible = false
                it.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                it.configureInput(enabled && showEditor, false, showEditor)
            },
            onRelease = { it.configureInput(false, false, false) },
        )
    }
}
