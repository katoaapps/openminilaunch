package com.katoaapps.openminilaunch.ui.minkpad

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.features.minkpad.MinkPadRepository
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadAction
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadConsumerAction

/** Native HID keyboard navigation and consumer-page media controls. */
@Composable
internal fun MinkPadRemoteControls(repository: MinkPadRepository, keyboardEnabled: Boolean, mediaEnabled: Boolean) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("TV remote", style = MaterialTheme.typography.titleLarge)
        Text("D-pad uses keyboard arrows and Enter. Back uses Esc. TV and app support varies.", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            FilledTonalButton({ repository.perform(MinkPadAction.UP) }, enabled = keyboardEnabled,
                modifier = Modifier.width(88.dp).heightIn(min = 56.dp)) { Text("↑") }
        }
        MinkPadControlRow(listOf(MinkPadAction.LEFT, MinkPadAction.ENTER, MinkPadAction.RIGHT), keyboardEnabled, repository::perform)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            FilledTonalButton({ repository.perform(MinkPadAction.DOWN) }, enabled = keyboardEnabled,
                modifier = Modifier.width(88.dp).heightIn(min = 56.dp)) { Text("↓") }
        }
        TextButton({ repository.perform(MinkPadAction.ESCAPE) }, enabled = keyboardEnabled) { Text("Back") }
        MinkPadConsumerAction.entries.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { action ->
                    FilledTonalButton({ repository.consumer(action.usage) }, enabled = mediaEnabled,
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 14.dp)) { Text(action.label) }
                }
            }
        }
        if (!mediaEnabled) Text("Media controls become available after the native HID connection is ready.", style = MaterialTheme.typography.bodySmall)
        Text("Keyboard entry remains available below. A television must accept Bluetooth HID accessories; this is not the Google TV network remote.", style = MaterialTheme.typography.bodySmall)
    }
}
