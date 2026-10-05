package com.katoaapps.openminilaunch.ui.minkpad

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

/** Lock each two-finger stream to scroll or zoom, never both. Keep initial scroll displacement. */
internal class MinkPadTwoFingerGesture(private val initialSpan: Float) {
    private enum class Mode { UNDECIDED, SCROLL, ZOOM }
    private var mode = Mode.UNDECIDED
    val recognized get() = mode != Mode.UNDECIDED
    private var scroll = Offset.Zero
    private var zoomSpan = initialSpan

    data class Update(val scroll: Offset = Offset.Zero, val zoom: Int = 0)

    fun update(delta: Offset, span: Float, slop: Float): Update {
        if (mode == Mode.UNDECIDED) {
            scroll += delta
            mode = when {
                abs(span - initialSpan) > slop * 2 && abs(span - initialSpan) > scroll.getDistance() ->
                    Mode.ZOOM
                scroll.getDistance() > slop -> Mode.SCROLL
                else -> return Update()
            }
        }
        return when (mode) {
            Mode.SCROLL -> {
                val movement = if (scroll != Offset.Zero) scroll else delta
                scroll = Offset.Zero
                Update(scroll = movement)
            }
            Mode.ZOOM -> {
                // Discrete keyboard zoom, not a native precision-trackpad gesture. Bound report traffic.
                val threshold = maxOf(slop * 3, initialSpan * .12f)
                val step = when {
                    span - zoomSpan >= threshold -> 1
                    span - zoomSpan <= -threshold -> -1
                    else -> 0
                }
                if (step != 0) zoomSpan = span
                Update(zoom = step)
            }
            Mode.UNDECIDED -> Update()
        }
    }
}
