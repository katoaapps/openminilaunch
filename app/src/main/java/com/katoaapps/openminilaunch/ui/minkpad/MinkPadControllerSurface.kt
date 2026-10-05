package com.katoaapps.openminilaunch.ui.minkpad

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.features.minkpad.MinkPadPreferences
import com.katoaapps.openminilaunch.features.minkpad.MinkPadRepository
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadGestureAction
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadHostPlatform

internal enum class MinkPadControllerMode(val label: String) { TRADITIONAL("Traditional"), REMOTE("TV remote") }

@Composable
internal fun MinkPadModeSwitch(mode: MinkPadControllerMode, onMode: (MinkPadControllerMode) -> Unit) {
    TabRow(selectedTabIndex = mode.ordinal) {
        MinkPadControllerMode.entries.forEach { choice ->
            Tab(selected = choice == mode, onClick = { onMode(choice) }, text = { Text(choice.label) })
        }
    }
}

@Composable
internal fun MinkPadControllerSurface(
    repository: MinkPadRepository, preferences: MinkPadPreferences, mode: MinkPadControllerMode,
    platform: MinkPadHostPlatform, mouseEnabled: Boolean, keyboardEnabled: Boolean, mediaEnabled: Boolean,
    modifiers: Int, onModifiers: (Int) -> Unit, onHelp: () -> Unit, onAction: (MinkPadGestureAction) -> Unit,
) {
    if (mode == MinkPadControllerMode.REMOTE) {
        MinkPadRemoteControls(repository, keyboardEnabled, mediaEnabled)
        return
    }
    val pad: @Composable () -> Unit = {
        MinkPadTrackpad(repository, preferences, mouseEnabled, platform, onHelp, onAction)
    }
    val keys: @Composable () -> Unit = {
        MinkPadKeyboardGrid(keyboardEnabled, modifiers, onModifiers, repository::perform)
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 700.dp) Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Box(Modifier.weight(1.5f)) { pad() }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Keyboard", style = MaterialTheme.typography.titleLarge)
                keys()
            }
        } else Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            pad()
            keys()
        }
    }
}
