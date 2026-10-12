package com.katoaapps.openminilaunch.ui.minkpad

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.features.minkpad.MinkPadHost
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadHostPlatform
import com.katoaapps.openminilaunch.features.minkpad.MinkPadPreferences
import java.util.Locale

@Composable
internal fun MinkPadSettingsDialog(
    value: MinkPadPreferences, update: (MinkPadPreferences) -> Unit,
    host: MinkPadHost?, platform: MinkPadHostPlatform, setPlatform: (MinkPadHostPlatform) -> Unit,
    close: () -> Unit,
) {
    val context = LocalContext.current
    var hostMenu by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = close, title = { Text("Controller settings") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(host?.let { "Host system · ${it.name}" } ?: "Connect a host to save its system preset.",
                style = MaterialTheme.typography.titleSmall)
            Box {
                TextButton(onClick = { hostMenu = true }, enabled = host != null) { Text(platform.label) }
                DropdownMenu(expanded = hostMenu, onDismissRequest = { hostMenu = false }) {
                    MinkPadHostPlatform.entries.forEach { option ->
                        DropdownMenuItem(text = { Text(option.label) },
                            onClick = { setPlatform(option); hostMenu = false })
                    }
                }
            }
            Text("MinkPad uses a host hint when Bluetooth provides one. You can override it here; gesture shortcuts vary by host and app.",
                style = MaterialTheme.typography.bodySmall)
            SettingSwitch("Keep screen awake while connected", value.keepAwake) { update(value.copy(keepAwake = it)) }
            SettingSwitch("Invert two-finger scrolling", value.naturalScroll) { update(value.copy(naturalScroll = it)) }
            SettingSwitch("Tap to click", value.tapToClick) { update(value.copy(tapToClick = it)) }
            SettingSwitch("Click haptics", value.haptics) { update(value.copy(haptics = it)) }
            Text(String.format(Locale.getDefault(), "Pointer sensitivity · %.2f×", value.pointerSensitivity))
            Slider(value.pointerSensitivity, { update(value.copy(pointerSensitivity = it)) }, valueRange = .25f..3f)
            TextButton(onClick = { update(value.copy(pointerSensitivity = .65f)) }) { Text("Gentler pointer · 0.65×") }
            TextButton(onClick = {
                val candidates = listOf(Settings.ACTION_HARD_KEYBOARD_SETTINGS, Settings.ACTION_INPUT_METHOD_SETTINGS, Settings.ACTION_SETTINGS)
                val opened = candidates.any { action ->
                    runCatching { context.startActivity(Intent(action)); true }.getOrDefault(false)
                }
                if (!opened) Toast.makeText(context, "This device could not open input settings.", Toast.LENGTH_SHORT).show()
            }) { Text("This phone’s keyboard / input settings") }
            Text("This opens settings on this phone, not the receiving host. Android does not provide a universal mouse-settings screen. Pointer options may be under your device's input settings.",
                style = MaterialTheme.typography.bodySmall)
            Text(String.format(Locale.getDefault(), "Scroll sensitivity · %.2f×", value.scrollSensitivity))
            Slider(value.scrollSensitivity, { update(value.copy(scrollSensitivity = it)) }, valueRange = .25f..3f)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Bluetooth compatibility identity", style = MaterialTheme.typography.titleSmall)
            Text(
                "While MinkPad runs, this phone temporarily uses the Bluetooth name Pro Controller - MinkPad. This gives Android receiving devices the responsive HID timing used by MinkPad. Stopping or closing MinkPad restores the phone's original name.",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "MinkPad uses this phone's regular Bluetooth adapter. Existing accessory pairings are not erased, but headphones, watches, and other accessories may see the temporary name or compete for the same radio while MinkPad is active.",
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = {
                runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
            }) { Text("Open this phone's Bluetooth settings") }
        }
    }, confirmButton = { TextButton(onClick = close) { Text("Done") } })
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, update: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.weight(1f)); Switch(checked, update)
    }
}
