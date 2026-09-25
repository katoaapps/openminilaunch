package com.katoaapps.openminilaunch.ui.apps

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
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
