package com.katoaapps.openminilaunch.ui.minkpad

import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

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
    var softwareEditorOpen by remember(enabled, hardwareKeyboard) { mutableStateOf(false) }
    val showEditor = !hardwareKeyboard && softwareEditorOpen
    val foreground = MaterialTheme.colorScheme.onSurface.toArgb()
    val hint = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    Column {
        if (!hardwareKeyboard) {
            TextButton(onClick = { softwareEditorOpen = !softwareEditorOpen }, enabled = enabled) {
                Text(if (showEditor) "Hide keyboard" else "Type")
            }
        }
        AndroidView(
            // GONE/INVISIBLE cannot retain focus. Keep a nonzero attached editor with no artwork.
            modifier = if (showEditor) Modifier.fillMaxWidth().heightIn(min = 72.dp)
                else Modifier.size(1.dp),
            factory = { MinkPadInputEditor(it) },
            update = {
                it.commit = onText
                it.backspace = onBackspace
                it.enter = onEnter
                it.forwardDelete = onForwardDelete
                it.alpha = if (showEditor) 1f else 0f
                it.isCursorVisible = showEditor
                it.importantForAccessibility = if (showEditor) View.IMPORTANT_FOR_ACCESSIBILITY_YES
                    else View.IMPORTANT_FOR_ACCESSIBILITY_NO
                it.setTextColor(foreground)
                it.setHintTextColor(hint)
                it.configureInput(enabled && showEditor, false, showEditor)
            },
            onRelease = { it.configureInput(false, false, false) },
        )
    }
}
