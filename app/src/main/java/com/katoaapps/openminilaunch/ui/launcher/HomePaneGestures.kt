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

/** Keeps Home's pane navigation gestures separate from its visual layout. */
@Composable
internal fun Modifier.homePaneGestures(
    magicExpanded: Boolean,
    onPaneInteracted: () -> Unit,
    onHorizontalDrag: ((Float) -> Unit)?,
    onHorizontalDragFinished: ((Float, Long) -> Unit)?,
    onSwipeDown: () -> Unit,
    onDoubleTap: () -> Unit,
): Modifier {
    val currentPaneInteracted by rememberUpdatedState(onPaneInteracted)
    val currentHorizontalDrag by rememberUpdatedState(onHorizontalDrag)
    val currentHorizontalDragFinished by rememberUpdatedState(onHorizontalDragFinished)
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
        .pointerInput(Unit) {
            var distance = 0f
            detectVerticalDragGestures(
                onDragStart = { distance = 0f },
                onVerticalDrag = { _, amount -> if (amount > 0) distance += amount },
                onDragEnd = { if (distance > 140f) currentSwipeDown() },
            )
        }
        .pointerInput(magicExpanded) {
            if (!magicExpanded) detectTapGestures(onDoubleTap = { currentDoubleTap() })
        }
}
