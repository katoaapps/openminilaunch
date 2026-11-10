package com.katoaapps.openminilaunch.ui.minkspace.media

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

private const val ADRIFT_PLACEMENT_ATTEMPTS = 96
private const val ADRIFT_FRAME_MARGIN = .025f
private const val ADRIFT_MAX_OVERLAP = .22f
private const val ADRIFT_MIN_POSITION_CHANGE = .16f
private const val ADRIFT_MIN_FRAME_AREA = .17f
private const val ADRIFT_MAX_FRAME_AREA = .205f

internal data class AdriftRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val area: Float get() = width * height
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

internal data class AdriftFramePlacement(
    val xFraction: Float,
    val yFraction: Float,
    val widthFraction: Float,
    val heightFraction: Float,
    val rotationDegrees: Float,
) {
    val bounds: AdriftRect
        get() = AdriftRect(
            left = xFraction,
            top = yFraction,
            right = xFraction + widthFraction,
            bottom = yFraction + heightFraction,
        )
}

/**
 * Finds a bounded, non-grid placement for one independently transitioning frame. Candidates are
 * scored against currently visible frames and the frame's own previous position. The clock is
 * intentionally not excluded: photos may pass behind it while the UI keeps the clock on top.
 */
internal fun planAdriftFramePlacement(
    screenAspectRatio: Float,
    imageAspectRatio: Float,
    occupied: List<AdriftFramePlacement>,
    previous: AdriftFramePlacement?,
    seed: Int,
): AdriftFramePlacement {
    val random = Random(seed)
    val safeImageAspect = normalizedAdriftAspectRatio(imageAspectRatio)
    val safeScreenAspect = screenAspectRatio.coerceAtLeast(.1f)
    val targetArea = random.nextFloat().lerp(ADRIFT_MIN_FRAME_AREA, ADRIFT_MAX_FRAME_AREA)
    var frameWidth = sqrt(targetArea * safeImageAspect / safeScreenAspect)
    var frameHeight = targetArea / frameWidth
    val maximumDimension = 1f - ADRIFT_FRAME_MARGIN * 2f
    val fitScale = min(
        1f,
        min(maximumDimension / frameWidth, maximumDimension / frameHeight),
    )
    frameWidth *= fitScale
    frameHeight *= fitScale

    var best: AdriftFramePlacement? = null
    var bestScore = Float.MAX_VALUE

    repeat(ADRIFT_PLACEMENT_ATTEMPTS) {
        val availableX = (1f - frameWidth - ADRIFT_FRAME_MARGIN * 2f).coerceAtLeast(0f)
        val availableY = (1f - frameHeight - ADRIFT_FRAME_MARGIN * 2f).coerceAtLeast(0f)
        val candidate = AdriftFramePlacement(
            xFraction = ADRIFT_FRAME_MARGIN + random.nextFloat() * availableX,
            yFraction = ADRIFT_FRAME_MARGIN + random.nextFloat() * availableY,
            widthFraction = frameWidth,
            heightFraction = frameHeight,
            rotationDegrees = random.nextFloat() * 4f - 2f,
        )
        val largestFrameOverlap = occupied.maxOfOrNull { other ->
            overlapFraction(candidate.bounds, other.bounds)
        } ?: 0f
        val previousDistance = previous?.let { old ->
            hypot(
                candidate.bounds.centerX - old.bounds.centerX,
                candidate.bounds.centerY - old.bounds.centerY,
            )
        } ?: 1f
        val overlapScore = occupied.sumOf { other ->
            overlapFraction(candidate.bounds, other.bounds).toDouble()
        }.toFloat() * 12f +
            (ADRIFT_MIN_POSITION_CHANGE - previousDistance).coerceAtLeast(0f) * 10f

        if (overlapScore < bestScore) {
            best = candidate
            bestScore = overlapScore
        }
        if (
            largestFrameOverlap <= ADRIFT_MAX_OVERLAP &&
            previousDistance >= ADRIFT_MIN_POSITION_CHANGE
        ) {
            return candidate
        }
    }

    return checkNotNull(best)
}

internal fun normalizedAdriftAspectRatio(imageAspectRatio: Float): Float =
    imageAspectRatio
        .takeIf { it.isFinite() && it > 0f }
        ?.coerceIn(.1f, 10f)
        ?: (4f / 3f)

internal fun overlapFraction(first: AdriftRect, second: AdriftRect): Float {
    val intersectionWidth = (min(first.right, second.right) - max(first.left, second.left))
        .coerceAtLeast(0f)
    val intersectionHeight = (min(first.bottom, second.bottom) - max(first.top, second.top))
        .coerceAtLeast(0f)
    val smallerArea = min(first.area, second.area)
    return if (smallerArea <= 0f) 0f else intersectionWidth * intersectionHeight / smallerArea
}

private fun Float.lerp(start: Float, end: Float): Float = start + this * (end - start)
