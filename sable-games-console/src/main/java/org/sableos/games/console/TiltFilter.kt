package org.sableos.games.console

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/** A step a tilt makes: one per tilt past the threshold, ready again once the phone is back near level. */
enum class TiltStep(
    val key: String,
) {
    Left("left"),
    Right("right"),
    Up("up"),
    Down("down"),
}

/** How far the phone must lean for a full turn of the wheel or a step. */
enum class TiltSensitivity(
    val key: String,
    val fullDegrees: Double,
) {
    Low("low", 45.0),
    Mid("mid", 32.0),
    High("high", 22.0),
    ;

    fun next(): TiltSensitivity = entries[(ordinal + 1) % entries.size]

    companion object {
        fun of(value: String?): TiltSensitivity = entries.firstOrNull { it.key == value } ?: Mid
    }
}

/**
 * Turns accelerometer readings (m/s², the phone's own axes) into a tilt from -1 to 1 on each axis,
 * measured from the angle the player chose as level ("Hold it how you'll play"), and into steps.
 *
 * x is positive when the right edge goes down, y when the bottom edge goes down, so a ball rolls the
 * way the phone leans. For steering, a small lean (within [DEAD_ZONE] of a full one) counts as level,
 * so a hand that shakes doesn't steer, and the rest follows a curve ([CURVE]) that is gentle near
 * level and reaches 1 at a full lean, so small corrections stay small. Steps work like rustmix-wave's motion events: a lean past [STEP_AT] makes one
 * step, and the next needs the phone back within [REARM_AT] of level first.
 */
class TiltFilter(
    var sensitivity: TiltSensitivity = TiltSensitivity.Mid,
) {
    private var baseX = 0.0
    private var baseY = 0.0
    private var smoothX = 0.0
    private var smoothY = 0.0
    private var started = false
    private var armed = true

    var x = 0.0
        private set
    var y = 0.0
        private set

    /** The lean before the dead zone and curve, which steps use. */
    private var leanX = 0.0
    private var leanY = 0.0

    /** The latest raw reading, kept so [level] can use it. */
    private var rawX = 0.0
    private var rawY = 0.0

    /** Makes the phone's current angle the level one. */
    fun level() {
        baseX = rawX
        baseY = rawY
        smoothX = rawX
        smoothY = rawY
        x = 0.0
        y = 0.0
        leanX = 0.0
        leanY = 0.0
        armed = true
    }

    /** Feeds one reading; returns the step it makes, if any. */
    fun feed(
        ax: Double,
        ay: Double,
    ): TiltStep? {
        rawX = ax
        rawY = ay
        if (!started) {
            started = true
            level()
        }
        smoothX += (ax - smoothX) * SMOOTHING
        smoothY += (ay - smoothY) * SMOOTHING
        val full = GRAVITY * sin(Math.toRadians(sensitivity.fullDegrees))
        leanX = (-(smoothX - baseX) / full).coerceIn(-1.0, 1.0)
        leanY = ((smoothY - baseY) / full).coerceIn(-1.0, 1.0)
        x = shape(leanX)
        y = shape(leanY)
        return step()
    }

    private fun shape(lean: Double): Double {
        val past = (abs(lean) - DEAD_ZONE).coerceAtLeast(0.0) / (1 - DEAD_ZONE)
        return sign(lean) * past.pow(CURVE)
    }

    private fun step(): TiltStep? {
        val x = leanX
        val y = leanY
        val ax = abs(x)
        val ay = abs(y)
        if (!armed) {
            if (ax < REARM_AT && ay < REARM_AT) armed = true
            return null
        }
        if (maxOf(ax, ay) < STEP_AT) return null
        armed = false
        return when {
            ax >= ay && x < 0 -> TiltStep.Left
            ax >= ay -> TiltStep.Right
            y < 0 -> TiltStep.Up
            else -> TiltStep.Down
        }
    }

    companion object {
        const val GRAVITY = 9.80665
        const val SMOOTHING = 0.25
        const val DEAD_ZONE = 0.08
        const val CURVE = 1.6
        const val STEP_AT = 0.55
        const val REARM_AT = 0.25
    }
}
