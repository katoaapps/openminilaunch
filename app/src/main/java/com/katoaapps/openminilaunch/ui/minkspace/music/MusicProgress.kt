package com.katoaapps.openminilaunch.ui.minkspace.music

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.ui.theme.Dimens
import kotlin.math.roundToLong

@Composable
internal fun CircularMediaProgress(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val trackColor = Color.Black.copy(alpha = 0.18f)
    val progressColor = Color.Black.copy(alpha = 0.88f)
    val strokeWidthDp = Dimens.dp3
    Canvas(modifier.padding(strokeWidthDp)) {
        val strokeWidth = strokeWidthDp.toPx()
        val inset = strokeWidth / 2f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(strokeWidth, cap = StrokeCap.Round),
        )
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(strokeWidth, cap = StrokeCap.Round),
        )
    }
}

@Composable
internal fun HorizontalMediaProgress(
    progress: Float,
    positionMs: Long,
    durationMs: Long,
    enabled: Boolean,
    onSeekToFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draggedProgress by remember { mutableStateOf<Float?>(null) }
    val displayedProgress = draggedProgress ?: progress.coerceIn(0f, 1f)
    val displayedPosition = draggedProgress?.let { durationMs * it }?.roundToLong() ?: positionMs
    Column(modifier) {
        Slider(
            value = displayedProgress,
            onValueChange = { draggedProgress = it },
            onValueChangeFinished = {
                draggedProgress?.let(onSeekToFraction)
                draggedProgress = null
            },
            enabled = enabled && durationMs > 0L,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.28f),
                disabledThumbColor = Color.White.copy(alpha = 0.5f),
                disabledActiveTrackColor = Color.White.copy(alpha = 0.5f),
                disabledInactiveTrackColor = Color.White.copy(alpha = 0.2f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.dp4),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration(displayedPosition),
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = Dimens.sp11,
            )
            Text(
                text = if (durationMs > 0L) formatDuration(durationMs) else "--:--",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = Dimens.sp11,
            )
        }
    }
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
