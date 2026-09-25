package com.katoaapps.openminilaunch.ui.launcher

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

/** Keeps Home's pane navigation gestures separate from its visual layout. */
@Composable
internal fun Modifier.homePaneGestures(
    magicExpanded: Boolean,
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

    val horizontalDragModifier = if (onHorizontalDrag != null && !magicExpanded) {
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

    return clipToBounds()
        .then(horizontalDragModifier)
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                currentPaneInteracted()
            }
        }
        .pointerInput(verticalSwipeThresholdPx, magicExpanded) {
            var verticalDistance = 0f
            detectVerticalDragGestures(
                onDragStart = { verticalDistance = 0f },
                onVerticalDrag = { _, amount -> verticalDistance += amount },
                onDragEnd = {
                    when {
                        verticalDistance >= verticalSwipeThresholdPx -> currentSwipeDown()
                        verticalDistance <= -verticalSwipeThresholdPx && !magicExpanded -> currentSwipeUp()
                    }
                },
            )
        }
        .pointerInput(magicExpanded) {
            if (!magicExpanded) detectTapGestures(onDoubleTap = { currentDoubleTap() })
        }
}
