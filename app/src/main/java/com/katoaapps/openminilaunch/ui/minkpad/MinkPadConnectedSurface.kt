package com.katoaapps.openminilaunch.ui.minkpad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.features.minkpad.MinkPadPreferences
import com.katoaapps.openminilaunch.features.minkpad.MinkPadRepository
import com.katoaapps.openminilaunch.features.minkpad.MinkPadSessionState
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadGestureAction
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadHostPlatform

/** Full-screen controller shown only while a native HID host is connected. */
@Composable
internal fun MinkPadConnectedSurface(
    repository: MinkPadRepository,
    state: MinkPadSessionState,
    preferences: MinkPadPreferences,
    mode: MinkPadControllerMode,
    platform: MinkPadHostPlatform,
    mouseEnabled: Boolean,
    keyboardEnabled: Boolean,
    mediaEnabled: Boolean,
    modifiers: Int,
    feedback: String?,
    onFeedback: (String?) -> Unit,
    onFeedbackConsumed: () -> Unit,
    onMode: (MinkPadControllerMode) -> Unit,
    onConnect: (String) -> Unit,
    onModifiers: (Int) -> Unit,
    onHelp: () -> Unit,
    onSettings: () -> Unit,
    onHosts: () -> Unit,
    onDiscover: () -> Unit,
    onClose: () -> Unit,
    onMenuStateChanged: (Boolean) -> Unit,
    onAction: (MinkPadGestureAction) -> Unit,
) {
    var topExpanded by rememberSaveable { mutableStateOf(false) }
    var bottomExpanded by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf<MinkPadConnectedMenu?>(null) }
    var topDockHeightPx by remember { mutableIntStateOf(0) }
    var bottomTrayHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val topDockHeight = with(density) { topDockHeightPx.toDp() }
    val bottomTrayHeight = with(density) { bottomTrayHeightPx.toDp() }
    val topTriggerOffset by animateDpAsState(
        targetValue = if (topExpanded) (topDockHeight - 20.dp).coerceAtLeast(0.dp) else 0.dp,
        label = "MinkPad top control offset",
    )
    val bottomTriggerOffset by animateDpAsState(
        targetValue = if (bottomExpanded) -(bottomTrayHeight - 20.dp).coerceAtLeast(0.dp) else 0.dp,
        label = "MinkPad bottom control offset",
    )
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(menu) { onMenuStateChanged(menu != null) }
    DisposableEffect(Unit) { onDispose { onMenuStateChanged(false) } }
    LaunchedEffect(feedback) {
        val message = feedback ?: return@LaunchedEffect
        snackbarHost.showSnackbar(message)
        onFeedbackConsumed()
    }
    LaunchedEffect(state.capsLock) {
        if (state.capsLock) snackbarHost.showSnackbar("Host Caps Lock is on. Turn it off for software text input.")
    }

    Box(Modifier.fillMaxSize()) {
        when (mode) {
            MinkPadControllerMode.TRADITIONAL -> MinkPadTrackpad(
                repository = repository,
                preferences = preferences,
                enabled = mouseEnabled,
                platform = platform,
                onHelp = onHelp,
                onAction = onAction,
                modifier = Modifier.fillMaxSize(),
                shape = RectangleShape,
                color = Color.Black,
                showHelp = false,
                showHint = false,
                showGridOutline = true,
            )

            MinkPadControllerMode.REMOTE -> Surface(
                Modifier.fillMaxSize(),
                color = Color.Black,
                contentColor = Color.White,
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 76.dp),
                ) {
                    MinkPadRemoteControls(repository, keyboardEnabled, mediaEnabled)
                }
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            AnimatedVisibility(
                visible = topExpanded,
                modifier = Modifier.align(Alignment.TopCenter),
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            ) {
                MinkPadTopDock(
                    state = state,
                    mode = mode,
                    menu = menu,
                    onMode = onMode,
                    onMenu = { menu = it },
                    onConnect = onConnect,
                    onHosts = onHosts,
                    onDiscover = onDiscover,
                    onHelp = onHelp,
                    onSettings = onSettings,
                    onClose = onClose,
                    modifier = Modifier.onGloballyPositioned { topDockHeightPx = it.size.height },
                )
            }

            FilledTonalIconButton(
                onClick = { topExpanded = !topExpanded },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .offset(y = topTriggerOffset)
                    .size(48.dp),
            ) {
                Icon(
                    if (topExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (topExpanded) "Hide MinkPad controls" else "Show MinkPad controls",
                )
            }

            AnimatedVisibility(
                visible = bottomExpanded,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { bottomTrayHeightPx = it.size.height },
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                ) {
                    Column(
                        Modifier.padding(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MinkPadHostShortcutGrid(
                            platform = platform,
                            enabled = keyboardEnabled,
                            onAction = onAction,
                        )
                        HorizontalDivider()
                        MinkPadLiveTextInput(
                            enabled = keyboardEnabled,
                            onText = {
                                onFeedback(
                                    if (repository.type(it)) null
                                    else "Text not sent. Use US-layout characters, or wait for the previous input to finish.",
                                )
                            },
                            onBackspace = { repository.tapKey(42) },
                            onEnter = { repository.tapKey(40) },
                            onForwardDelete = { repository.tapKey(76) },
                        )
                        MinkPadKeyboardGrid(keyboardEnabled, modifiers, onModifiers, repository::perform)
                    }
                }
            }

            FilledTonalIconButton(
                onClick = { bottomExpanded = !bottomExpanded },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .offset(y = bottomTriggerOffset)
                    .size(48.dp),
            ) {
                Icon(
                    if (bottomExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.Keyboard,
                    contentDescription = if (bottomExpanded) "Hide keyboard and host controls"
                    else "Show keyboard and host controls",
                )
            }

            SnackbarHost(
                hostState = snackbarHost,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = if (bottomExpanded) bottomTrayHeight + 28.dp else 68.dp,
                    ),
            )
        }
    }
}
