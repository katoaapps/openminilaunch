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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkpad.MinkPadHost
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadHostPlatform
import com.katoaapps.openminilaunch.features.minkpad.MinkPadPreferences

@Composable
internal fun MinkPadSettingsDialog(
    value: MinkPadPreferences, update: (MinkPadPreferences) -> Unit,
    host: MinkPadHost?, platform: MinkPadHostPlatform, setPlatform: (MinkPadHostPlatform) -> Unit,
    close: () -> Unit,
) {
    val context = LocalContext.current
    var hostMenu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = close,
        modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(.96f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(stringResource(R.string.minkpad_controller_settings)) },
        text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingSwitch(stringResource(R.string.minkpad_keep_awake), value.keepAwake) { update(value.copy(keepAwake = it)) }
            SettingSwitch(stringResource(R.string.minkpad_invert_scroll), value.naturalScroll) { update(value.copy(naturalScroll = it)) }
            SettingSwitch(stringResource(R.string.minkpad_tap_to_click), value.tapToClick) { update(value.copy(tapToClick = it)) }
            SettingSwitch(stringResource(R.string.minkpad_click_haptics), value.haptics) { update(value.copy(haptics = it)) }
            SettingSwitch(
                label = stringResource(R.string.minkpad_advanced_gestures),
                supportingText = stringResource(R.string.minkpad_advanced_gestures_description),
                checked = value.advancedHostGestures,
            ) { update(value.copy(advancedHostGestures = it)) }
            Text(stringResource(R.string.minkpad_pointer_sensitivity, value.pointerSensitivity))
            Slider(value.pointerSensitivity, { update(value.copy(pointerSensitivity = it)) }, valueRange = .25f..3f)
            TextButton(onClick = { update(value.copy(pointerSensitivity = .65f)) }) {
                Text(stringResource(R.string.minkpad_gentler_pointer))
            }
            TextButton(onClick = {
                val candidates = listOf(Settings.ACTION_HARD_KEYBOARD_SETTINGS, Settings.ACTION_INPUT_METHOD_SETTINGS, Settings.ACTION_SETTINGS)
                val opened = candidates.any { action ->
                    runCatching { context.startActivity(Intent(action)); true }.getOrDefault(false)
                }
                if (!opened) Toast.makeText(context, context.getString(R.string.minkpad_input_settings_failed), Toast.LENGTH_SHORT).show()
            }) { Text(stringResource(R.string.minkpad_phone_input_settings)) }
            Text(stringResource(R.string.minkpad_input_settings_description),
                style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.minkpad_scroll_sensitivity, value.scrollSensitivity))
            Slider(value.scrollSensitivity, { update(value.copy(scrollSensitivity = it)) }, valueRange = .25f..3f)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(stringResource(R.string.minkpad_operating_system), style = MaterialTheme.typography.titleSmall)
            host?.let {
                Text(stringResource(R.string.minkpad_saved_for, it.name), style = MaterialTheme.typography.bodySmall)
            } ?: Text(
                stringResource(R.string.minkpad_connect_to_save_os),
                style = MaterialTheme.typography.bodySmall,
            )
            Box {
                TextButton(onClick = { hostMenu = true }, enabled = host != null) {
                    Text(stringResource(platform.labelResource))
                }
                DropdownMenu(expanded = hostMenu, onDismissRequest = { hostMenu = false }) {
                    MinkPadHostPlatform.entries.forEach { option ->
                        DropdownMenuItem(text = { Text(stringResource(option.labelResource)) },
                            onClick = { setPlatform(option); hostMenu = false })
                    }
                }
            }
            Text(
                stringResource(R.string.minkpad_os_hint_description),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    },
        confirmButton = { TextButton(onClick = close) { Text(stringResource(R.string.done)) } },
    )
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    supportingText: String? = null,
    update: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(label)
            supportingText?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        Switch(checked, update)
    }
}
