package com.katoaapps.openminilaunch.ui.minkpad

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.features.minkpad.MinkPadRepository
import com.katoaapps.openminilaunch.features.minkpad.MinkPadStatus
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadHostPlatform
import com.katoaapps.openminilaunch.features.minkpad.input.shortcut

private enum class MinkPadDialog { SETTINGS, HELP }

@Composable
internal fun MinkPadScreen(
    repository: MinkPadRepository, onStart: () -> Unit, onDiscover: () -> Unit,
    onClose: () -> Unit, onHardwareCaptureChanged: (Boolean) -> Unit,
) {
    val state by repository.state.collectAsState()
    val preferences by repository.preferences.state.collectAsState()
    val platforms by repository.preferences.hostPlatforms.collectAsState()
    val platform = platforms[state.host?.address] ?: MinkPadHostPlatform.GENERIC
    val connected = state.status == MinkPadStatus.CONNECTED
    var mode by rememberSaveable { mutableStateOf(MinkPadControllerMode.TRADITIONAL) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var dialog by remember { mutableStateOf<MinkPadDialog?>(null) }
    var menu by remember { mutableStateOf(false) }
    var hostDialogOpen by remember { mutableStateOf(false) }
    val controlsEnabled = connected && dialog == null && !hostDialogOpen && !menu
    val keyboardEnabled = controlsEnabled && state.keyboardReady
    val mouseEnabled = controlsEnabled && state.mouseReady
    var modifiers by remember { mutableIntStateOf(0) }
    val inputReset by repository.inputReset.collectAsState()
    val activity = LocalContext.current as Activity
    DisposableEffect(connected, preferences.keepAwake) {
        if (connected && preferences.keepAwake) activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    DisposableEffect(keyboardEnabled) {
        onHardwareCaptureChanged(keyboardEnabled)
        onDispose { onHardwareCaptureChanged(false) }
    }
    LaunchedEffect(mode, platform, controlsEnabled) {
        modifiers = 0
        repository.releaseInput()
    }
    LaunchedEffect(inputReset) {
        modifiers = 0
        onHardwareCaptureChanged(false)
        onHardwareCaptureChanged(keyboardEnabled)
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.safeDrawingPadding().imePadding().padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("MinkPad · Beta", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Your pocket, everyboard.", style = MaterialTheme.typography.bodyMedium)
                }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "MinkPad options")
                    }
                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Mouse and keyboard settings") },
                            onClick = { menu = false; dialog = MinkPadDialog.SETTINGS })
                        DropdownMenuItem(text = { Text("Trackpad help") },
                            onClick = { menu = false; dialog = MinkPadDialog.HELP })
                        DropdownMenuItem(text = { Text("Close MinkPad") }, onClick = { menu = false; onClose() })
                    }
                }
            }
            MinkPadModeSwitch(mode) { mode = it; feedback = null }
            MinkPadConnectionPanel(repository, state, onStart, onDiscover) { hostDialogOpen = it }
            if (connected) Text(platform.label, style = MaterialTheme.typography.labelMedium)
            if (connected && !state.keyboardReady) Text(
                "The host has not enabled keyboard input. Mouse input can still work. Older prototype pairings may need to be forgotten and paired again.",
                style = MaterialTheme.typography.bodySmall)
            MinkPadControllerSurface(repository, preferences, mode, platform, mouseEnabled, keyboardEnabled,
                controlsEnabled && state.consumerReady, modifiers,
                onModifiers = { modifiers = it; repository.modifiers(it) },
                onHelp = { dialog = MinkPadDialog.HELP },
                onAction = { action ->
                    val shortcut = platform.shortcut(action)
                    feedback = when {
                        shortcut == null -> if (platform == MinkPadHostPlatform.GENERIC)
                            "Choose the connected host's system in mouse and keyboard settings to enable shortcut gestures."
                            else "This gesture is not mapped for ${platform.label}. See Trackpad help."
                        !repository.shortcut(shortcut.usage, shortcut.modifiers) -> "The host has not enabled keyboard shortcuts yet."
                        else -> null
                    }
                })
            if (state.capsLock) Text("Host Caps Lock is on. Turn it off for software text input.", color = MaterialTheme.colorScheme.error)
            MinkPadLiveTextInput(keyboardEnabled,
                onText = { feedback = if (repository.type(it)) null else "Text not sent. Use US-layout characters, or wait for the previous input to finish." },
                onBackspace = { repository.tapKey(42) }, onEnter = { repository.tapKey(40) }, onForwardDelete = { repository.tapKey(76) })
            feedback?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
    when (dialog) {
        MinkPadDialog.SETTINGS -> MinkPadSettingsDialog(repository, preferences, repository.preferences::update,
            state.host, platform, setPlatform = { value -> state.host?.let { repository.preferences.setHostPlatform(it.address, value) } },
            close = { dialog = null })
        MinkPadDialog.HELP -> MinkPadHelpDialog(platform, onSettings = { dialog = MinkPadDialog.SETTINGS }, close = { dialog = null })
        null -> Unit
    }
}
