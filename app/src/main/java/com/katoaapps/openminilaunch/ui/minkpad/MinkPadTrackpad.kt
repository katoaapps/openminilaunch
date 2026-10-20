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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.R
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
    var touchPositions by remember { mutableStateOf(emptyList<Offset>()) }
    var hasInteracted by remember { mutableStateOf(false) }
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
                    onAction = onAction,
                    onTouches = {
                        touchPositions = it
                        if (it.isNotEmpty()) hasInteracted = true
                    },
                ),
                contentAlignment = Alignment.Center,
            ) {
                if (showHint && !hasInteracted) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.minkpad_trackpad), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.minkpad_trackpad_hint), style = MaterialTheme.typography.bodyMedium)
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
                MinkPadTouchIndicators(touchPositions, markerColor)
            }
            if (showHelp) {
                IconButton(onClick = onHelp, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    Icon(
                        Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = stringResource(R.string.minkpad_trackpad_help),
                    )
                }
            }
        }
    }
}

@Composable
private fun MinkPadTouchIndicators(positions: List<Offset>, color: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val multiTouch = positions.size > 1
        positions.forEach { position ->
            val center = Offset(size.width * position.x, size.height * position.y)
            drawCircle(
                color = color.copy(alpha = if (multiTouch) .22f else .12f),
                radius = 30.dp.toPx(),
                center = center,
            )
            drawCircle(
                color = color.copy(alpha = if (multiTouch) .9f else .65f),
                radius = 20.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx()),
            )
            drawCircle(color, 5.dp.toPx(), center)
        }
    }
}
