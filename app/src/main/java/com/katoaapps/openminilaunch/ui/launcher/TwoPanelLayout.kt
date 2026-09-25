package com.katoaapps.openminilaunch.ui.launcher

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.flow.collect

/** Pixel geometry for two safe, equally sized panes within the current window. */
internal data class TwoPanelGeometry(
    val paneWidthPx: Int,
    val gapPx: Int,
    val outerLeftPx: Int,
    val outerRightPx: Int,
)

internal data class FoldLayoutFeature(
    val vertical: Boolean,
    val separating: Boolean,
    val leftPx: Int,
    val rightPx: Int,
    val halfOpened: Boolean = false,
)

internal fun pairForFocusedPage(page: Int): Int =
    if (page == MINK_DAY_PAGE) 0 else 1

internal fun newlyRevealedPage(pair: Int): Int =
    if (pair == 0) MINK_DAY_PAGE else WIDGET_PAGE

/** A narrow window or a tabletop posture retains the familiar single-page Home. */
internal fun twoPanelGeometry(
    enabled: Boolean,
    smallestWidthDp: Int,
    orientation: Int,
    windowWidthPx: Int,
    density: Float,
    fold: FoldLayoutFeature?,
): TwoPanelGeometry? {
    val unfoldedBookFold = fold?.vertical == true
    val supportsTwoPanels = orientation == Configuration.ORIENTATION_LANDSCAPE || unfoldedBookFold
    if (!enabled || smallestWidthDp < 600 || !supportsTwoPanels) {
        return null
    }
    if (windowWidthPx <= 0 || density <= 0f) return null
    if (fold?.vertical == false && fold.halfOpened) return null

    val minPaneDp = if (unfoldedBookFold) 320f else 360f
    val minPanePx = (minPaneDp * density).toInt()
    val preferredGapPx = (12f * density).toInt().coerceAtLeast(1)
    // Horizontal fold bounds span the window width, so they cannot describe a left/right gutter.
    // Once fully open, ignore that crease and split the landscape window evenly instead.
    val separatingFold = fold?.takeIf { it.vertical && it.separating }
    if (separatingFold == null) {
        val paneWidthPx = (windowWidthPx - preferredGapPx) / 2
        if (paneWidthPx < minPanePx) return null
        return TwoPanelGeometry(
            paneWidthPx = paneWidthPx,
            gapPx = windowWidthPx - paneWidthPx * 2,
            outerLeftPx = 0,
            outerRightPx = 0,
        )
    }

    if (separatingFold.leftPx < 0 || separatingFold.rightPx < separatingFold.leftPx ||
        separatingFold.rightPx > windowWidthPx
    ) return null
    val foldWidth = separatingFold.rightPx - separatingFold.leftPx
    val extraClearance = (preferredGapPx - foldWidth).coerceAtLeast(0)
    val leftEdge = (separatingFold.leftPx - extraClearance / 2).coerceAtLeast(0)
    val rightEdge = (separatingFold.rightPx + extraClearance - extraClearance / 2)
        .coerceAtMost(windowWidthPx)
    val leftSpace = leftEdge
    val rightSpace = windowWidthPx - rightEdge
    val paneWidthPx = minOf(leftSpace, rightSpace)
    if (paneWidthPx < minPanePx) return null

    val outerLeftPx = (leftSpace - paneWidthPx) / 2
    val outerRightPx = (rightSpace - paneWidthPx) / 2
    val gapPx = windowWidthPx - outerLeftPx - outerRightPx - 2 * paneWidthPx
    return TwoPanelGeometry(paneWidthPx, gapPx, outerLeftPx, outerRightPx)
}

/** Observes the active fold instead of guessing hinge position from device model. */
@Composable
internal fun currentFoldLayoutFeature(): FoldLayoutFeature? {
    val activity = LocalContext.current as? Activity ?: return null
    val fold by produceState<FoldLayoutFeature?>(initialValue = null, activity) {
        WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).collect { layout ->
            value = layout.displayFeatures
                .filterIsInstance<FoldingFeature>()
                .firstOrNull()
                ?.let { feature ->
                    FoldLayoutFeature(
                        vertical = feature.orientation == FoldingFeature.Orientation.VERTICAL,
                        separating = feature.isSeparating,
                        leftPx = feature.bounds.left,
                        rightPx = feature.bounds.right,
                        halfOpened = feature.state == FoldingFeature.State.HALF_OPENED,
                    )
                }
        }
    }
    return fold
}
