package com.katoaapps.openminilaunch.ui.minkpad

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import com.katoaapps.openminilaunch.BuildConfig
import com.katoaapps.openminilaunch.features.minkpad.MinkPadHost
import com.katoaapps.openminilaunch.features.minkpad.MinkPadRepository
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadHostPlatform
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.features.minkpad.MinkPadPreferences
import java.util.Locale

@Composable
internal fun MinkPadSettingsDialog(repository: MinkPadRepository,
    value: MinkPadPreferences, update: (MinkPadPreferences) -> Unit,
    host: MinkPadHost?, platform: MinkPadHostPlatform, setPlatform: (MinkPadHostPlatform) -> Unit,
    close: () -> Unit,
) {
    val context = LocalContext.current
    var hostMenu by remember { mutableStateOf(false) }
    var latencyExperiment by remember { mutableStateOf(repository.classicLatencyExperimentEnabled()) }
    var latencyExperimentFeedback by remember { mutableStateOf<String?>(null) }
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
            Text("Selected by you, not detected. Gesture shortcuts vary by host and app.",
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
            if (BuildConfig.DEBUG) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("Android HID latency experiment", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (latencyExperiment) {
                        "Enabled: Start temporarily names this phone Pro Controller - MinkPad so an Android host can apply its built-in 11.25 ms HID compatibility rule. Pause automatically restores the original Bluetooth name."
                    } else {
                        "When enabled, Start temporarily names this phone Pro Controller - MinkPad and Pause restores its original name. Reports and connection code stay unchanged."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                SettingSwitch("Use compatibility identity while MinkPad runs", latencyExperiment) {
                    enabled ->
                    latencyExperimentFeedback = repository.setClassicLatencyExperiment(enabled)
                    latencyExperiment = repository.classicLatencyExperimentEnabled()
                }
                if (latencyExperiment) TextButton(onClick = {
                    runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                }) { Text("Open this phone's Bluetooth settings") }
                latencyExperimentFeedback?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text(
                    "Test requirement: use Android's Bluetooth settings on both phones to remove their old system pairing. Removing a device from MinkPad's Saved devices list is not enough. Start MinkPad, then pair the newly listed Pro Controller - MinkPad entry and test continuous pointer movement. Pause MinkPad when finished. This experiment is available only in debug builds.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    "MinkPad uses this phone's regular Bluetooth adapter, not a separate radio. Existing accessory pairings are not erased, but headphones, watches, and other Bluetooth devices may see the temporary phone name, may not reconnect normally during the test, or may compete for the same radio. Disconnect other accessories for a clean latency test.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }, confirmButton = { TextButton(onClick = close) { Text("Done") } })
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, update: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.weight(1f)); Switch(checked, update)
    }
}
