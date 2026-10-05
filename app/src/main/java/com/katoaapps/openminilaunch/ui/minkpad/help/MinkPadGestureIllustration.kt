package com.katoaapps.openminilaunch.ui.minkpad.help

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

internal enum class MinkPadGestureMotion {
    MOVE, TAP, DOUBLE_TAP, HOLD, DRAG, VERTICAL, HORIZONTAL, SPREAD, PINCH, UP, DOWN, LEFT, RIGHT,
}

internal data class MinkPadGestureExample(
    val label: String, val fingers: Int, val motion: MinkPadGestureMotion, val caption: String,
)

/** A noninteractive teaching surface. All geometry is relative to its measured size. */
@Composable
internal fun MinkPadGestureIllustration(example: MinkPadGestureExample, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Surface(
        modifier.clearAndSetSemantics {
            contentDescription = "${example.fingers}-finger gesture: ${example.label}. ${example.caption}"
        },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 2.dp,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1.8f), contentAlignment = Alignment.Center) {
            val diameter = (maxWidth * .115f).coerceIn(20.dp, 44.dp)
            Canvas(Modifier.fillMaxSize()) { drawGestureGuide(example.motion, color) }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = maxWidth * .16f),
                horizontalArrangement = Arrangement.spacedBy(
                    maxWidth * (if (example.motion == MinkPadGestureMotion.PINCH || example.motion == MinkPadGestureMotion.SPREAD) .22f else .035f),
                    Alignment.CenterHorizontally),
            ) {
                repeat(example.fingers) {
                    // Raised touch markers symbolize fingertips, not additional clickable controls.
                    Surface(Modifier.size(diameter), shape = CircleShape, color = color,
                        shadowElevation = 6.dp) {}
                }
            }
        }
    }
}

private fun DrawScope.drawGestureGuide(motion: MinkPadGestureMotion, color: Color) {
    fun point(x: Float, y: Float) = Offset(size.width * x, size.height * y)
    fun arrow(x1: Float, y1: Float, x2: Float, y2: Float) =
        gestureArrow(point(x1, y1), point(x2, y2), color)
    val halo = color.copy(alpha = .25f)
    when (motion) {
        MinkPadGestureMotion.TAP, MinkPadGestureMotion.DOUBLE_TAP, MinkPadGestureMotion.HOLD -> {
            drawCircle(halo, size.minDimension * .2f, style = Stroke(2.dp.toPx()))
            if (motion != MinkPadGestureMotion.TAP)
                drawCircle(halo, size.minDimension * .27f, style = Stroke(2.dp.toPx()))
            if (motion == MinkPadGestureMotion.HOLD)
                drawArc(color, -90f, 285f, false, point(.5f, .5f) - Offset(size.height * .27f, size.height * .27f),
                    Size(size.height * .54f, size.height * .54f), style = Stroke(3.dp.toPx()))
        }
        MinkPadGestureMotion.MOVE, MinkPadGestureMotion.DRAG -> {
            if (motion == MinkPadGestureMotion.DRAG)
                drawRoundRect(halo, point(.25f, .30f), Size(size.width * .16f, size.height * .36f))
            arrow(.52f, .48f, .78f, .24f)
        }
        MinkPadGestureMotion.VERTICAL -> { arrow(.78f, .5f, .78f, .2f); arrow(.78f, .5f, .78f, .8f) }
        MinkPadGestureMotion.HORIZONTAL -> { arrow(.5f, .25f, .22f, .25f); arrow(.5f, .25f, .78f, .25f) }
        MinkPadGestureMotion.SPREAD -> { arrow(.35f, .3f, .18f, .3f); arrow(.65f, .3f, .82f, .3f) }
        MinkPadGestureMotion.PINCH -> { arrow(.18f, .3f, .4f, .3f); arrow(.82f, .3f, .6f, .3f) }
        MinkPadGestureMotion.UP -> arrow(.78f, .65f, .78f, .18f)
        MinkPadGestureMotion.DOWN -> arrow(.78f, .18f, .78f, .65f)
        MinkPadGestureMotion.LEFT -> arrow(.75f, .25f, .22f, .25f)
        MinkPadGestureMotion.RIGHT -> arrow(.22f, .25f, .75f, .25f)
    }
}

private fun DrawScope.gestureArrow(start: Offset, end: Offset, color: Color) {
    val direction = end - start
    val length = direction.getDistance()
    if (length == 0f) return
    val unit = direction / length
    val normal = Offset(-unit.y, unit.x)
    val head = 9.dp.toPx().coerceAtMost(size.minDimension * .09f)
    val width = 3.dp.toPx()
    drawLine(color, start, end, width)
    drawLine(color, end, end - unit * head + normal * head * .6f, width)
    drawLine(color, end, end - unit * head - normal * head * .6f, width)
}
