package com.katoaapps.openminilaunch.ui.minkspace.music

import android.view.ViewConfiguration
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Resolves one, two, or three pointer taps without leaking timing state into a mini-app. */
internal fun Modifier.multiTapClickable(
    enabled: Boolean,
    accessibilityClickLabel: String,
    onSingleTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onTripleTap: () -> Unit,
): Modifier = composed {
    val currentSingleTap by rememberUpdatedState(onSingleTap)
    val currentDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentTripleTap by rememberUpdatedState(onTripleTap)
    val semanticModifier = Modifier.semantics(mergeDescendants = true) {
        role = Role.Button
        if (enabled) {
            onClick(label = accessibilityClickLabel) {
                currentSingleTap()
                true
            }
        } else {
            disabled()
        }
    }

    this
        .then(semanticModifier)
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            coroutineScope {
                var tapCount = 0
                var pendingAction: Job? = null
                detectTapGestures(
                    onTap = {
                        tapCount = (tapCount + 1).coerceAtMost(3)
                        pendingAction?.cancel()
                        pendingAction = launch {
                            delay(ViewConfiguration.getDoubleTapTimeout().toLong())
                            when (tapCount) {
                                1 -> currentSingleTap()
                                2 -> currentDoubleTap()
                                else -> currentTripleTap()
                            }
                            tapCount = 0
                        }
                    },
                )
            }
        }
}
