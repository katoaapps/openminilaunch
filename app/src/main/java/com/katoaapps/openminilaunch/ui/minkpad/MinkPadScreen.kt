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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.R
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
    onBrowseNearby: () -> Unit,
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
    val chooseHostOsFeedback = stringResource(R.string.minkpad_choose_host_os_feedback)
    val unmappedGestureFeedback = stringResource(
        R.string.minkpad_unmapped_gesture_feedback,
        stringResource(platform.labelResource),
    )
    val shortcutsNotReady = stringResource(R.string.minkpad_shortcuts_not_ready)
    LaunchedEffect(connected, state.host?.address) {
        controllerReady = false
        if (connected) {
            mode = if (state.host?.prefersRemoteControls == true) {
                MinkPadControllerMode.REMOTE
            } else {
                MinkPadControllerMode.TRADITIONAL
            }
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
                    onBrowseNearby = onBrowseNearby,
                    onHostDialogStateChanged = { hostDialogOpen = it },
                )

                MinkPadScreenPhase.CONNECTED_FEEDBACK -> AppearanceApplyingScreen(
                    title = stringResource(R.string.minkpad_connected_to),
                    selectionLabel = state.host?.name ?: stringResource(R.string.minkpad_receiving_device),
                    description = stringResource(R.string.minkpad_preparing_controls),
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
                                chooseHostOsFeedback
                                else unmappedGestureFeedback
                            !repository.shortcut(shortcut.usage, shortcut.modifiers) ->
                                shortcutsNotReady
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
    onBrowseNearby: () -> Unit,
    onHostDialogStateChanged: (Boolean) -> Unit,
) {
    Column(
        Modifier.safeDrawingPadding().imePadding().padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(stringResource(R.string.minkpad_name), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.minkpad_tagline), style = MaterialTheme.typography.bodyMedium)
        }
        MinkPadConnectionPanel(repository, state, onStart, onDiscover, onBrowseNearby, onHostDialogStateChanged)
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
            Text(stringResource(R.string.minkpad_whats_minkpad), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.minkpad_overview),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.minkpad_privacy_summary),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(R.string.minkpad_identity_summary),
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
