package com.katoaapps.openminilaunch.ui.minkpad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
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
    modifier: Modifier = Modifier.fillMaxWidth().height(280.dp),
    shape: Shape = MaterialTheme.shapes.extraLarge,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    showHelp: Boolean = true,
    showHint: Boolean = true,
    showGridOutline: Boolean = false,
) {
    val position = remember { mutableStateOf<Offset?>(null) }
    var hasMoved by remember { mutableStateOf(false) }
    val inputReset by repository.inputReset.collectAsState()
    val markerColor = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current
    fun vibrate() {
        if (preferences.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Surface(modifier = modifier, shape = shape, color = color) {
        Box(Modifier.fillMaxSize()) {
            // Help is a sibling overlay, not inside the gesture-capturing surface.
            Box(
                Modifier.fillMaxSize().minkPadTrackpadGestures(
                    repository, preferences, enabled, inputReset, platform,
                    onClick = { repository.click(it); vibrate() }, onHold = ::vibrate,
                    onAction = onAction, onPosition = { position.value = it; hasMoved = true },
                ),
                contentAlignment = Alignment.Center,
            ) {
                if (showHint && !hasMoved) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Trackpad", style = MaterialTheme.typography.titleLarge)
                    Text("Move a finger · Tap to click", style = MaterialTheme.typography.bodyMedium)
                }
                if (showGridOutline) Canvas(
                    Modifier
                        .matchParentSize()
                        .padding(horizontal = 22.dp, vertical = 68.dp),
                ) {
                    val lineColor = Color.White.copy(alpha = .12f)
                    val strokeWidth = 1.dp.toPx()
                    drawRoundRect(
                        color = lineColor,
                        cornerRadius = CornerRadius(14.dp.toPx()),
                        style = Stroke(width = strokeWidth),
                    )
                    drawLine(
                        color = lineColor,
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = strokeWidth,
                    )
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = strokeWidth,
                    )
                }
                Canvas(Modifier.fillMaxSize()) {
                    position.value?.let {
                        val point = Offset(size.width * it.x, size.height * it.y)
                        drawCircle(markerColor.copy(alpha = .15f), 28.dp.toPx(), point)
                        drawCircle(markerColor, 7.dp.toPx(), point)
                    }
                }
            }
            if (showHelp) {
                IconButton(onClick = onHelp, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "Trackpad help")
                }
            }
        }
    }
}
