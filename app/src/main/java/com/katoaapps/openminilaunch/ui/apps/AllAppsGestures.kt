package com.katoaapps.openminilaunch.ui.apps

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.katoaapps.openminilaunch.ui.theme.Dimens

/** Keeps the app carousel's close gesture separate from its visual layout. */
@Composable
internal fun Modifier.dismissAllAppsOnSwipeDown(onDismiss: () -> Unit): Modifier {
    val currentDismiss by rememberUpdatedState(onDismiss)
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { Dimens.dp48.toPx() }

    return pointerInput(swipeThresholdPx) {
        var verticalDistance = 0f
        detectVerticalDragGestures(
            onDragStart = { verticalDistance = 0f },
            onVerticalDrag = { _, amount -> verticalDistance += amount },
            onDragEnd = {
                if (verticalDistance >= swipeThresholdPx) currentDismiss()
            },
        )
    }
}

/** Treats directional trackpads as carousel movement instead of letter-rail focus navigation. */
@Composable
internal fun Modifier.navigateAllAppsWithDpad(
    enabled: Boolean,
    onStep: (Int) -> Unit,
    onSelect: () -> Unit,
): Modifier {
    val focusRequester = remember { FocusRequester() }
    val currentOnStep by rememberUpdatedState(onStep)
    val currentOnSelect by rememberUpdatedState(onSelect)

    LaunchedEffect(enabled) {
        if (enabled) focusRequester.requestFocus()
    }

    return focusRequester(focusRequester)
        .onPreviewKeyEvent { event ->
            val keyCode = event.nativeKeyEvent.keyCode
            val step = allAppsDpadPageStep(keyCode)
            when {
                step != null -> {
                    if (event.type == KeyEventType.KeyDown) currentOnStep(step)
                    true
                }
                isAllAppsDpadSelectKey(keyCode) -> {
                    if (event.type == KeyEventType.KeyUp) currentOnSelect()
                    true
                }
                else -> false
            }
        }
        .focusable(enabled = enabled)
}

internal fun allAppsDpadPageStep(keyCode: Int): Int? = when (keyCode) {
    AndroidKeyEvent.KEYCODE_DPAD_LEFT,
    AndroidKeyEvent.KEYCODE_DPAD_UP,
    -> -1
    AndroidKeyEvent.KEYCODE_DPAD_RIGHT,
    AndroidKeyEvent.KEYCODE_DPAD_DOWN,
    -> 1
    else -> null
}

internal fun isAllAppsDpadSelectKey(keyCode: Int): Boolean = when (keyCode) {
    AndroidKeyEvent.KEYCODE_DPAD_CENTER,
    AndroidKeyEvent.KEYCODE_ENTER,
    AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
    -> true
    else -> false
}
