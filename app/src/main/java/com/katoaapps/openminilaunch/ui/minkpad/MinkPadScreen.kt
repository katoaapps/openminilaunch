package com.katoaapps.openminilaunch.ui.minkpad

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.features.minkpad.MinkPadRepository
import com.katoaapps.openminilaunch.features.minkpad.MinkPadSessionState
import com.katoaapps.openminilaunch.features.minkpad.MinkPadStatus
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadHostPlatform
import com.katoaapps.openminilaunch.features.minkpad.input.shortcut
import com.katoaapps.openminilaunch.ui.settings.AppearanceApplyingScreen
import kotlinx.coroutines.delay

private enum class MinkPadDialog { SETTINGS, HELP }
private enum class MinkPadScreenPhase { SETUP, CONNECTED_FEEDBACK, CONTROLLER }

@Composable
internal fun MinkPadScreen(
    repository: MinkPadRepository, onStart: () -> Unit, onDiscover: () -> Unit,
    onHardwareCaptureChanged: (Boolean) -> Unit,
) {
    val state by repository.state.collectAsState()
    val preferences by repository.preferences.state.collectAsState()
    val platforms by repository.preferences.hostPlatforms.collectAsState()
    val savedPlatform = platforms[state.host?.address]
    val platformHint = state.host?.platformHint
    val platform = savedPlatform ?: platformHint ?: MinkPadHostPlatform.GENERIC
    val connected = state.status == MinkPadStatus.CONNECTED
    var mode by rememberSaveable { mutableStateOf(MinkPadControllerMode.TRADITIONAL) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var dialog by remember { mutableStateOf<MinkPadDialog?>(null) }
    var menu by remember { mutableStateOf(false) }
    var hostDialogOpen by remember { mutableStateOf(false) }
    var controllerReady by remember { mutableStateOf(false) }
    val controlsEnabled = connected && controllerReady && dialog == null && !hostDialogOpen && !menu
    val keyboardEnabled = controlsEnabled && state.keyboardReady
    val mouseEnabled = controlsEnabled && state.mouseReady
    var modifiers by remember { mutableIntStateOf(0) }
    val inputReset by repository.inputReset.collectAsState()
    val activity = LocalContext.current as Activity
    LaunchedEffect(connected, state.host?.address) {
        controllerReady = false
        if (connected) {
            delay(CONNECTED_FEEDBACK_MILLIS)
            controllerReady = true
        }
    }
    LaunchedEffect(state.host?.address, savedPlatform, platformHint) {
        val hostAddress = state.host?.address ?: return@LaunchedEffect
        if (savedPlatform == null && platformHint != null) {
            repository.preferences.setHostPlatform(hostAddress, platformHint)
        }
    }
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
    val screenPhase = when {
        !connected -> MinkPadScreenPhase.SETUP
        !controllerReady -> MinkPadScreenPhase.CONNECTED_FEEDBACK
        else -> MinkPadScreenPhase.CONTROLLER
    }
    Surface(
        Modifier.fillMaxSize(),
        color = Color.Black,
        contentColor = Color.White,
    ) {
        AnimatedContent(
            targetState = screenPhase,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "MinkPad connection transition",
        ) { phase ->
            when (phase) {
                MinkPadScreenPhase.SETUP -> MinkPadSetupContent(
                    repository = repository,
                    state = state,
                    onStart = onStart,
                    onDiscover = onDiscover,
                    onHostDialogStateChanged = { hostDialogOpen = it },
                )

                MinkPadScreenPhase.CONNECTED_FEEDBACK -> AppearanceApplyingScreen(
                    title = "Connected to…",
                    selectionLabel = state.host?.name ?: "Receiving device",
                    description = "Preparing the MinkPad trackpad, keyboard, and remote controls",
                )

                MinkPadScreenPhase.CONTROLLER -> MinkPadConnectedSurface(
                    repository = repository,
                    state = state,
                    preferences = preferences,
                    mode = mode,
                    platform = platform,
                    mouseEnabled = mouseEnabled,
                    keyboardEnabled = keyboardEnabled,
                    mediaEnabled = controlsEnabled && state.consumerReady,
                    modifiers = modifiers,
                    feedback = feedback,
                    onFeedback = { feedback = it },
                    onFeedbackConsumed = { feedback = null },
                    onMode = { mode = it; feedback = null },
                    onConnect = repository::connect,
                    onModifiers = { modifiers = it; repository.modifiers(it) },
                    onHelp = { dialog = MinkPadDialog.HELP },
                    onSettings = { dialog = MinkPadDialog.SETTINGS },
                    onHosts = {
                        repository.refreshHosts()
                        hostDialogOpen = true
                    },
                    onDiscover = onDiscover,
                    onClose = repository::stop,
                    onMenuStateChanged = { menu = it },
                    onAction = { action ->
                        val shortcut = platform.shortcut(action)
                        feedback = when {
                            shortcut == null -> if (platform == MinkPadHostPlatform.GENERIC)
                                "Choose the connected host's system in mouse and keyboard settings to enable shortcut gestures."
                                else "This gesture is not mapped for ${platform.label}. See Trackpad help."
                            !repository.shortcut(shortcut.usage, shortcut.modifiers) ->
                                "The host has not enabled keyboard shortcuts yet."
                            else -> null
                        }
                    },
                )
            }
        }
    }
    if (connected && hostDialogOpen) MinkPadHostsDialog(repository, state) { hostDialogOpen = false }
    when (dialog) {
        MinkPadDialog.SETTINGS -> MinkPadSettingsDialog(preferences, repository.preferences::update,
            state.host, platform, setPlatform = { value -> state.host?.let { repository.preferences.setHostPlatform(it.address, value) } },
            close = { dialog = null })
        MinkPadDialog.HELP -> MinkPadHelpDialog(platform, onSettings = { dialog = MinkPadDialog.SETTINGS }, close = { dialog = null })
        null -> Unit
    }
}

@Composable
private fun MinkPadSetupContent(
    repository: MinkPadRepository,
    state: MinkPadSessionState,
    onStart: () -> Unit,
    onDiscover: () -> Unit,
    onHostDialogStateChanged: (Boolean) -> Unit,
) {
    Column(
        Modifier.safeDrawingPadding().imePadding().padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text("MinkPad · Beta", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Your pocket everyboard.", style = MaterialTheme.typography.bodyMedium)
        }
        MinkPadConnectionPanel(repository, state, onStart, onDiscover, onHostDialogStateChanged)
        if (state.status in stableSetupStatuses) MinkPadSetupOverview()
    }
}

@Composable
private fun MinkPadSetupOverview() {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("What's MinkPad?", style = MaterialTheme.typography.titleMedium)
            Text(
                "MinkPad turns this phone into a Bluetooth keyboard, trackpad, and media remote for another device.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Keyboard and pointer input travel directly over Bluetooth. MinkPad does not save what you type.",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "During a session, this phone temporarily appears to receiving devices as Pro Controller - MinkPad. Its usual Bluetooth name returns when MinkPad closes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val stableSetupStatuses = setOf(
    MinkPadStatus.IDLE,
    MinkPadStatus.READY,
    MinkPadStatus.DISCONNECTED,
    MinkPadStatus.UNSUPPORTED,
    MinkPadStatus.ERROR,
)

private const val CONNECTED_FEEDBACK_MILLIS = 900L
