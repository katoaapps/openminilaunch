package com.katoaapps.openminilaunch.ui.minkpad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.features.minkpad.MinkPadPreferences
import com.katoaapps.openminilaunch.features.minkpad.MinkPadRepository
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadGestureAction
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadHostPlatform

@Composable
internal fun MinkPadTrackpad(
    repository: MinkPadRepository, preferences: MinkPadPreferences, enabled: Boolean,
    platform: MinkPadHostPlatform, onHelp: () -> Unit, onAction: (MinkPadGestureAction) -> Unit,
) {
    val position = remember { mutableStateOf<Offset?>(null) }
    var hasMoved by remember { mutableStateOf(false) }
    val inputReset by repository.inputReset.collectAsState()
    val markerColor = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current
    fun vibrate() {
        if (preferences.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Box(Modifier.fillMaxWidth().height(280.dp)) {
            // Help is a sibling overlay, not inside the gesture-capturing surface.
            Box(
                Modifier.fillMaxSize().minkPadTrackpadGestures(
                    repository, preferences, enabled, inputReset, platform,
                    onClick = { repository.click(it); vibrate() }, onHold = ::vibrate,
                    onAction = onAction, onPosition = { position.value = it; hasMoved = true },
                ),
                contentAlignment = Alignment.Center,
            ) {
                if (!hasMoved) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Trackpad", style = MaterialTheme.typography.titleLarge)
                    Text("Move a finger · Tap to click", style = MaterialTheme.typography.bodyMedium)
                }
                Canvas(Modifier.fillMaxSize()) {
                    position.value?.let {
                        val point = Offset(size.width * it.x, size.height * it.y)
                        drawCircle(markerColor.copy(alpha = .15f), 28.dp.toPx(), point)
                        drawCircle(markerColor, 7.dp.toPx(), point)
                    }
                }
            }
            IconButton(onClick = onHelp, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "Trackpad help")
            }
        }
    }
}
