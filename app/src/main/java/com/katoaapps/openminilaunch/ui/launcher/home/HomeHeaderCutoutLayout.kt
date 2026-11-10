package com.katoaapps.openminilaunch.ui.launcher.home

import android.graphics.Rect
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.ViewCompat
import kotlin.math.max
import kotlin.math.roundToInt

internal data class HomeHeaderCutoutPlan(
    val heightPx: Int,
    val contentTopPx: Int,
    val leadingLeftPx: Int,
    val dateLeftPx: Int,
    val dateMaxWidthPx: Int,
    val timeLeftPx: Int,
    val timeMaxWidthPx: Int,
    val trailingLeftPx: Int,
    val trailingMaxWidthPx: Int,
)

/**
 * Places the date and time independently around the physical cutout. Center cameras therefore sit
 * between the two labels while corner cutouts still move the complete header to the larger side.
 */
internal fun planHomeHeaderAroundCutout(
    containerWidthPx: Int,
    contentStartPx: Int,
    contentEndPx: Int,
    rowHeightPx: Int,
    verticalPaddingPx: Int,
    measuredLeadingWidthPx: Int,
    minimumFlexibleWidthPx: Int,
    naturalDateWidthPx: Int,
    naturalTimeWidthPx: Int,
    measuredTrailingWidthPx: Int,
    groupGapPx: Int,
    cutouts: List<IntRect>,
): HomeHeaderCutoutPlan {
    val boundedContentEnd = contentEndPx.coerceIn(contentStartPx, containerWidthPx)
    val contentWidth = (boundedContentEnd - contentStartPx).coerceAtLeast(0)
    val baseHeight = rowHeightPx + verticalPaddingPx * 2
    val hasDate = naturalDateWidthPx > 0
    val hasTime = naturalTimeWidthPx > 0
    val dateGap = if (hasDate) groupGapPx else 0
    val timeGap = if (hasTime) groupGapPx else 0
    val relevantCutout = cutouts
        .filter { it.bottom > 0 && it.top < baseHeight }
        .takeIf { it.isNotEmpty() }
        ?.reduce { combined, rect ->
            IntRect(
                left = minOf(combined.left, rect.left),
                top = minOf(combined.top, rect.top),
                right = maxOf(combined.right, rect.right),
                bottom = maxOf(combined.bottom, rect.bottom),
            )
        }

    fun normalPlan(
        topPx: Int = verticalPaddingPx,
        heightPx: Int = baseHeight,
        regionStartPx: Int = contentStartPx,
        regionEndPx: Int = boundedContentEnd,
    ): HomeHeaderCutoutPlan {
        val trailingWidth = measuredTrailingWidthPx.coerceAtMost(
            (regionEndPx - regionStartPx).coerceAtLeast(0),
        )
        val trailingLeft = regionEndPx - trailingWidth
        val flexibleStart = regionStartPx + measuredLeadingWidthPx + dateGap
        val flexibleEnd = (trailingLeft - timeGap).coerceAtLeast(flexibleStart)
        val flexibleWidth = (flexibleEnd - flexibleStart).coerceAtLeast(0)
        val timeWidth = if (hasTime) naturalTimeWidthPx.coerceAtMost(flexibleWidth) else 0
        val dateWidth = if (hasDate) {
            naturalDateWidthPx.coerceAtMost(
                (flexibleWidth - timeWidth - if (hasTime) groupGapPx else 0).coerceAtLeast(0),
            )
        } else {
            0
        }
        val timeLeft = flexibleStart + dateWidth + if (hasDate && hasTime) groupGapPx else 0
        return HomeHeaderCutoutPlan(
            heightPx = heightPx,
            contentTopPx = topPx,
            leadingLeftPx = regionStartPx,
            dateLeftPx = flexibleStart,
            dateMaxWidthPx = dateWidth,
            timeLeftPx = timeLeft,
            timeMaxWidthPx = timeWidth,
            trailingLeftPx = trailingLeft,
            trailingMaxWidthPx = trailingWidth,
        )
    }

    if (relevantCutout == null || contentWidth == 0) return normalPlan()

    val cutoutLeft = (relevantCutout.left - groupGapPx).coerceIn(contentStartPx, boundedContentEnd)
    val cutoutRight = (relevantCutout.right + groupGapPx).coerceIn(contentStartPx, boundedContentEnd)
    val leftWidth = (cutoutLeft - contentStartPx).coerceAtLeast(0)
    val rightWidth = (boundedContentEnd - cutoutRight).coerceAtLeast(0)
    val alongsideHeight = max(baseHeight, relevantCutout.bottom)
    val alongsideTop = ((alongsideHeight - rowHeightPx) / 2).coerceAtLeast(0)

    if (leftWidth >= measuredLeadingWidthPx && rightWidth >= measuredTrailingWidthPx) {
        val leftFlexibleWidth = (
            cutoutLeft - contentStartPx - measuredLeadingWidthPx - dateGap
            ).coerceAtLeast(0)
        val rightFlexibleWidth = (
            boundedContentEnd - measuredTrailingWidthPx - cutoutRight - timeGap
            ).coerceAtLeast(0)
        val timeWidth = if (hasTime) naturalTimeWidthPx.coerceAtMost(rightFlexibleWidth) else 0
        val trailingLeft = boundedContentEnd - measuredTrailingWidthPx
        return HomeHeaderCutoutPlan(
            heightPx = alongsideHeight,
            contentTopPx = alongsideTop,
            leadingLeftPx = contentStartPx,
            dateLeftPx = contentStartPx + measuredLeadingWidthPx + dateGap,
            dateMaxWidthPx = if (hasDate) leftFlexibleWidth else 0,
            timeLeftPx = trailingLeft - timeGap - timeWidth,
            timeMaxWidthPx = timeWidth,
            trailingLeftPx = trailingLeft,
            trailingMaxWidthPx = measuredTrailingWidthPx,
        )
    }

    val combinedMinimumWidth = measuredLeadingWidthPx + measuredTrailingWidthPx +
        (if (hasDate) minimumFlexibleWidthPx + groupGapPx else 0) +
        (if (hasTime) minimumFlexibleWidthPx + groupGapPx else 0) +
        (if (hasDate && hasTime) groupGapPx else 0)
    if (leftWidth >= combinedMinimumWidth) {
        return normalPlan(
            topPx = alongsideTop,
            heightPx = alongsideHeight,
            regionStartPx = contentStartPx,
            regionEndPx = cutoutLeft,
        )
    }
    if (rightWidth >= combinedMinimumWidth) {
        return normalPlan(
            topPx = alongsideTop,
            heightPx = alongsideHeight,
            regionStartPx = cutoutRight,
            regionEndPx = boundedContentEnd,
        )
    }

    // Only a cutout that leaves neither side usable forces the whole header beneath it.
    val belowTop = max(baseHeight, relevantCutout.bottom) + verticalPaddingPx
    return normalPlan(
        topPx = belowTop,
        heightPx = belowTop + rowHeightPx + verticalPaddingPx,
    )
}

@Composable
internal fun CutoutAwareHomeHeaderLayout(
    modifier: Modifier,
    horizontalPaddingPx: Int,
    verticalPaddingPx: Int,
    rowHeightPx: Int,
    minimumFlexibleWidthPx: Int,
    groupGapPx: Int,
    leadingContent: @Composable () -> Unit,
    dateContent: @Composable () -> Unit,
    timeContent: @Composable () -> Unit,
    trailingContent: @Composable () -> Unit,
) {
    val windowCutouts = rememberWindowCutoutBounds()
    var positionInWindow by remember { mutableStateOf(Offset.Zero) }

    SubcomposeLayout(
        modifier = modifier.onGloballyPositioned { positionInWindow = it.positionInWindow() },
    ) { constraints ->
        val width = constraints.maxWidth
        val physicalCutouts = windowCutouts.map { rect ->
            IntRect(
                left = rect.left - positionInWindow.x.roundToInt(),
                top = rect.top - positionInWindow.y.roundToInt(),
                right = rect.right - positionInWindow.x.roundToInt(),
                bottom = rect.bottom - positionInWindow.y.roundToInt(),
            )
        }
        // The plan uses logical start/end coordinates because placement is relative in RTL layouts.
        val localCutouts = if (layoutDirection == LayoutDirection.Rtl) {
            physicalCutouts.map { rect ->
                IntRect(width - rect.right, rect.top, width - rect.left, rect.bottom)
            }
        } else {
            physicalCutouts
        }
        val availableContentWidth = (width - horizontalPaddingPx * 2).coerceAtLeast(0)
        val childConstraints = constraints.copy(
            minWidth = 0,
            maxWidth = availableContentWidth,
            minHeight = 0,
        )
        val leadingMeasurable = subcompose(HeaderSlot.Leading, leadingContent).single()
        val dateMeasurable = subcompose(HeaderSlot.Date, dateContent).single()
        val timeMeasurable = subcompose(HeaderSlot.Time, timeContent).single()
        val trailingMeasurable = subcompose(HeaderSlot.Trailing, trailingContent).single()
        val leadingPlaceable = leadingMeasurable.measure(childConstraints)
        val trailingPlaceable = trailingMeasurable.measure(childConstraints)
        val naturalDateWidth = dateMeasurable.maxIntrinsicWidth(rowHeightPx)
        val naturalTimeWidth = timeMeasurable.maxIntrinsicWidth(rowHeightPx)
        val plan = planHomeHeaderAroundCutout(
            containerWidthPx = width,
            contentStartPx = horizontalPaddingPx,
            contentEndPx = (width - horizontalPaddingPx).coerceAtLeast(horizontalPaddingPx),
            rowHeightPx = rowHeightPx,
            verticalPaddingPx = verticalPaddingPx,
            measuredLeadingWidthPx = leadingPlaceable.width,
            minimumFlexibleWidthPx = minimumFlexibleWidthPx,
            naturalDateWidthPx = naturalDateWidth,
            naturalTimeWidthPx = naturalTimeWidth,
            measuredTrailingWidthPx = trailingPlaceable.width,
            groupGapPx = groupGapPx,
            cutouts = localCutouts,
        )
        val datePlaceable = dateMeasurable.measure(
            childConstraints.copy(maxWidth = plan.dateMaxWidthPx),
        )
        val timePlaceable = timeMeasurable.measure(
            childConstraints.copy(maxWidth = plan.timeMaxWidthPx),
        )
        val childY: (Int) -> Int = { childHeight ->
            plan.contentTopPx + (rowHeightPx - childHeight) / 2
        }
        layout(width, plan.heightPx.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            leadingPlaceable.placeRelative(plan.leadingLeftPx, childY(leadingPlaceable.height))
            datePlaceable.placeRelative(plan.dateLeftPx, childY(datePlaceable.height))
            timePlaceable.placeRelative(plan.timeLeftPx, childY(timePlaceable.height))
            trailingPlaceable.placeRelative(plan.trailingLeftPx, childY(trailingPlaceable.height))
        }
    }
}

private enum class HeaderSlot { Leading, Date, Time, Trailing }

@Composable
private fun rememberWindowCutoutBounds(): List<IntRect> {
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    var bounds by remember(view) { mutableStateOf(emptyList<IntRect>()) }

    DisposableEffect(
        view,
        configuration.orientation,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
    ) {
        fun refreshCutouts(target: View) {
            bounds = ViewCompat.getRootWindowInsets(target)
                ?.displayCutout
                ?.boundingRects
                .orEmpty()
                .filterNot(Rect::isEmpty)
                .map { IntRect(it.left, it.top, it.right, it.bottom) }
        }

        val layoutListener = View.OnLayoutChangeListener { target, _, _, _, _, _, _, _, _ ->
            refreshCutouts(target)
        }
        view.addOnLayoutChangeListener(layoutListener)
        view.post { refreshCutouts(view) }
        onDispose { view.removeOnLayoutChangeListener(layoutListener) }
    }

    return bounds
}
