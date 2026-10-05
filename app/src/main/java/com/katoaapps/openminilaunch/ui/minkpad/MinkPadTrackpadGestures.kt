package com.katoaapps.openminilaunch.ui.minkpad

import android.os.SystemClock
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import com.katoaapps.openminilaunch.features.minkpad.MinkPadPreferences
import com.katoaapps.openminilaunch.features.minkpad.MinkPadRepository
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadGestureAction
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadHostPlatform
import kotlin.math.abs

/** Captures only the pad. Highest finger count owns the stream, including its release tail. */
internal fun Modifier.minkPadTrackpadGestures(
    repository: MinkPadRepository,
    preferences: MinkPadPreferences,
    enabled: Boolean,
    inputReset: Long,
    platform: MinkPadHostPlatform,
    onClick: (Int) -> Unit,
    onHold: () -> Unit,
    onAction: (MinkPadGestureAction) -> Unit,
    onPosition: (Offset) -> Unit,
): Modifier = pointerInput(enabled, preferences, inputReset, platform) {
    if (!enabled) return@pointerInput
    var lastTapTime = 0L
    var lastTapPosition = Offset.Zero
    awaitEachGesture {
        val first = awaitFirstDown()
        val initialFingers = currentEvent.changes.filter { it.pressed }
        currentEvent.changes.forEach { it.consume() }
        val doubleTap = initialFingers.size == 1 && preferences.tapToClick &&
            first.uptimeMillis - lastTapTime in 1..viewConfiguration.doubleTapTimeoutMillis &&
            (first.position - lastTapPosition).getDistance() < viewConfiguration.touchSlop * 2
        var held = doubleTap
        if (held) repository.mouseButton(1, true)
        var travel = 0f
        var motion = Offset.Zero
        var maxFingers = initialFingers.size.coerceAtLeast(1)
        var previousCount = maxFingers
        var endedAt = first.uptimeMillis
        var twoFinger = if (initialFingers.size == 2)
            MinkPadTwoFingerGesture((initialFingers[0].position - initialFingers[1].position).getDistance())
            else null
        try {
            while (true) {
                // A stationary hold has no MOVE event. Use the platform long-press timeout.
                val canHold = maxFingers == 1 && !held && travel < viewConfiguration.touchSlop
                val remaining = first.uptimeMillis + viewConfiguration.longPressTimeoutMillis - SystemClock.uptimeMillis()
                val event = if (canHold) withTimeoutOrNull(remaining.coerceAtLeast(1)) { awaitPointerEvent() }
                    else awaitPointerEvent()
                if (event == null) {
                    repository.mouseButton(1, true)
                    held = true
                    onHold()
                    lastTapTime = 0
                    continue
                }
                val fingers = event.changes.filter { it.pressed }
                val count = fingers.size
                endedAt = event.changes.maxOf { it.uptimeMillis }
                val previousMax = maxFingers
                maxFingers = maxOf(maxFingers, count)
                if (maxFingers > 1 && held) {
                    repository.mouseButton(1, false)
                    held = false
                }
                val delta = fingers.fold(Offset.Zero) { sum, finger -> sum + finger.positionChange() } /
                    count.coerceAtLeast(1).toFloat()
                if (count > 0 && count == previousCount && count == maxFingers) {
                    travel += delta.getDistance()
                    motion += delta
                    onPosition(Offset(fingers.first().position.x / size.width, fingers.first().position.y / size.height))
                    when (count) {
                        1 -> {
                            repository.pointer(delta.x * preferences.pointerSensitivity, delta.y * preferences.pointerSensitivity)
                        }
                        2 -> {
                            val span = (fingers[0].position - fingers[1].position).getDistance()
                            val update = twoFinger?.update(delta, span, viewConfiguration.touchSlop)
                            if (update != null && update.scroll != Offset.Zero) {
                                val sign = if (preferences.naturalScroll) -1f else 1f
                                val scale = preferences.scrollSensitivity * sign / 32f
                                repository.scroll(update.scroll.y * scale, update.scroll.x * scale)
                            }
                            if (update?.zoom == 1) onAction(MinkPadGestureAction.ZOOM_IN)
                            if (update?.zoom == -1) onAction(MinkPadGestureAction.ZOOM_OUT)
                        }
                    }
                } else {
                    // Reset movement on entry to a higher-finger gesture, not on its release tail.
                    if (count > previousMax) {
                        motion = Offset.Zero
                        travel = 0f
                    }
                    if (count == 2 && maxFingers == 2) {
                        twoFinger = MinkPadTwoFingerGesture((fingers[0].position - fingers[1].position).getDistance())
                    }
                }
                event.changes.forEach { it.consume() }
                previousCount = count
                if (count == 0) break
            }
            val isTap = twoFinger?.recognized != true && travel < viewConfiguration.touchSlop &&
                endedAt - first.uptimeMillis < MAX_TAP_DURATION_MS
            when {
                maxFingers == 1 && isTap && preferences.tapToClick && !held -> onClick(1)
                maxFingers == 2 && isTap && preferences.tapToClick -> onClick(2)
                maxFingers == 3 && isTap -> onAction(MinkPadGestureAction.BACK)
                maxFingers == 3 && motion.getDistance() >= viewConfiguration.touchSlop * 3 -> {
                    val action = if (abs(motion.x) > abs(motion.y)) {
                        if (motion.x < 0) MinkPadGestureAction.NEXT_APP else MinkPadGestureAction.PREVIOUS_APP
                    } else {
                        if (motion.y < 0) MinkPadGestureAction.OVERVIEW else MinkPadGestureAction.DISMISS_OVERVIEW
                    }
                    onAction(action)
                }
                maxFingers == 4 && isTap -> onAction(MinkPadGestureAction.APP_LIST)
            }
            lastTapTime = if (maxFingers == 1 && isTap && !doubleTap && !held) endedAt else 0
            lastTapPosition = first.position
        } finally {
            if (held) repository.mouseButton(1, false)
        }
    }
}

private const val MAX_TAP_DURATION_MS = 500L
