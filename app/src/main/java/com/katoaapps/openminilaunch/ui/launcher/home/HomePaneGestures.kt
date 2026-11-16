package com.katoaapps.openminilaunch.ui.launcher.home

import android.os.SystemClock
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.katoaapps.openminilaunch.ui.theme.Dimens
import kotlin.math.abs

/** Keeps Home's pane navigation gestures separate from its visual layout. */
@Composable
internal fun Modifier.homePaneGestures(
    magicExpanded: Boolean,
    verticalGesturesEnabled: Boolean,
    observeVerticalGesturesBeforeChildren: Boolean = false,
    horizontalGesturesEnabled: Boolean,
    onPaneInteracted: () -> Unit,
    onHorizontalDrag: ((Float) -> Unit)?,
    onHorizontalDragFinished: ((Float, Long) -> Unit)?,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onDoubleTap: () -> Unit,
): Modifier {
    val density = LocalDensity.current
    val verticalSwipeThresholdPx = with(density) { Dimens.dp48.toPx() }
    val currentPaneInteracted by rememberUpdatedState(onPaneInteracted)
    val currentHorizontalDrag by rememberUpdatedState(onHorizontalDrag)
    val currentHorizontalDragFinished by rememberUpdatedState(onHorizontalDragFinished)
    val currentSwipeUp by rememberUpdatedState(onSwipeUp)
    val currentSwipeDown by rememberUpdatedState(onSwipeDown)
    val currentDoubleTap by rememberUpdatedState(onDoubleTap)

    val horizontalDragModifier = if (
        onHorizontalDrag != null && !magicExpanded && horizontalGesturesEnabled
    ) {
        Modifier.pointerInput(Unit) {
            var draggedPx = 0f
            var startedAtMillis = 0L
            detectHorizontalDragGestures(
                onDragStart = {
                    draggedPx = 0f
                    startedAtMillis = SystemClock.uptimeMillis()
                },
                onHorizontalDrag = { change, amount ->
                    draggedPx += amount
                    currentHorizontalDrag?.invoke(amount)
                    change.consume()
                },
                onDragEnd = {
                    currentHorizontalDragFinished?.invoke(
                        draggedPx,
                        SystemClock.uptimeMillis() - startedAtMillis,
                    )
                },
                onDragCancel = {
                    currentHorizontalDragFinished?.invoke(
                        draggedPx,
                        SystemClock.uptimeMillis() - startedAtMillis,
                    )
                },
            )
        }
    } else {
        Modifier
    }

    val verticalDragModifier = Modifier.pointerInput(
        verticalSwipeThresholdPx,
        magicExpanded,
        verticalGesturesEnabled,
        observeVerticalGesturesBeforeChildren,
    ) {
        if (!verticalGesturesEnabled) return@pointerInput
        if (observeVerticalGesturesBeforeChildren) {
            awaitEachGesture {
                val first = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial,
                )
                var lastPosition = first.position
                do {
                    val change = awaitPointerEvent(PointerEventPass.Initial).changes
                        .firstOrNull { it.id == first.id }
                    if (change != null) lastPosition = change.position
                } while (change?.pressed == true)

                val horizontalDistance = lastPosition.x - first.position.x
                val verticalDistance = lastPosition.y - first.position.y
                if (abs(verticalDistance) > abs(horizontalDistance)) {
                    when {
                        verticalDistance >= verticalSwipeThresholdPx -> currentSwipeDown()
                        verticalDistance <= -verticalSwipeThresholdPx && !magicExpanded -> {
                            currentSwipeUp()
                        }
                    }
                }
            }
        } else {
            var verticalDistance = 0f
            detectVerticalDragGestures(
                onDragStart = { verticalDistance = 0f },
                onVerticalDrag = { _, amount -> verticalDistance += amount },
                onDragEnd = {
                    when {
                        verticalDistance >= verticalSwipeThresholdPx -> currentSwipeDown()
                        verticalDistance <= -verticalSwipeThresholdPx && !magicExpanded -> {
                            currentSwipeUp()
                        }
                    }
                },
            )
        }
    }

    return clipToBounds()
        .then(horizontalDragModifier)
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                currentPaneInteracted()
            }
        }
        .then(verticalDragModifier)
        .pointerInput(magicExpanded) {
            if (!magicExpanded) detectTapGestures(onDoubleTap = { currentDoubleTap() })
        }
}
