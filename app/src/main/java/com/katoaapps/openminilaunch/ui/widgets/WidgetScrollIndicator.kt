package com.katoaapps.openminilaunch.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import com.katoaapps.openminilaunch.ui.theme.Dimens

private data class WidgetScrollMetrics(
    val progress: Float,
    val visibleFraction: Float,
    val estimatedMaxScrollPx: Float,
)

@Composable
internal fun WidgetScrollIndicator(
    state: LazyListState,
    widgetCount: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val itemSpacingPx = with(density) { Dimens.dp20.toPx() }
    val footerSizePx = with(density) { Dimens.dp32.toPx() }
    val metrics by remember(state, widgetCount, itemSpacingPx, footerSizePx) {
        derivedStateOf { widgetScrollMetrics(state, widgetCount, itemSpacingPx, footerSizePx) }
    }
    val currentMetrics by rememberUpdatedState(metrics)

    BoxWithConstraints(
        modifier = modifier
            .width(Dimens.dp38)
            .fillMaxHeight(.68f)
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(metrics.progress, 0f..1f)
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        val thumbHeight = maxHeight * metrics.visibleFraction
        val thumbTravel = maxHeight - thumbHeight
        val thumbOffset = thumbTravel * metrics.progress

        Box(
            Modifier
                .align(Alignment.Center)
                .width(Dimens.dp4)
                .fillMaxHeight()
                .background(
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = .16f),
                    shape = RoundedCornerShape(Dimens.dp4),
                ),
        )
        Box(
            Modifier
                .offset(y = thumbOffset)
                .width(Dimens.dp8)
                .height(thumbHeight)
                .background(
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = .88f),
                    shape = RoundedCornerShape(Dimens.dp8),
                ),
        )
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(state) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        val thumbTravelPx = size.height * (1f - currentMetrics.visibleFraction)
                        val scrollPerTrackPixel = currentMetrics.estimatedMaxScrollPx /
                            thumbTravelPx.coerceAtLeast(1f)
                        state.dispatchRawDelta(dragAmount * scrollPerTrackPixel)
                    }
                },
        )
    }
}

private fun widgetScrollMetrics(
    state: LazyListState,
    widgetCount: Int,
    itemSpacingPx: Float,
    footerSizePx: Float,
): WidgetScrollMetrics {
    val layout = state.layoutInfo
    val visibleWidgets = layout.visibleItemsInfo.filter { it.index < widgetCount && it.size > 0 }
    val averageSize = visibleWidgets.map { it.size }.average().takeIf { !it.isNaN() }?.toFloat() ?: 1f
    val viewportSize = (layout.viewportEndOffset - layout.viewportStartOffset).coerceAtLeast(1).toFloat()
    val estimatedTotalSize = (
        averageSize * widgetCount.coerceAtLeast(1) +
            itemSpacingPx * widgetCount.coerceAtLeast(1) +
            footerSizePx
        ).coerceAtLeast(viewportSize)
    val visibleFraction = (viewportSize / estimatedTotalSize).coerceIn(.16f, 1f)
    val first = visibleWidgets.firstOrNull()
    val estimatedScroll = if (first == null) 0f else {
        first.index * (averageSize + itemSpacingPx) +
            (layout.viewportStartOffset - first.offset).coerceAtLeast(0)
    }
    val maxScroll = (estimatedTotalSize - viewportSize).coerceAtLeast(1f)
    val progress = when {
        !state.canScrollBackward -> 0f
        !state.canScrollForward -> 1f
        else -> (estimatedScroll / maxScroll).coerceIn(0f, 1f)
    }
    return WidgetScrollMetrics(progress, visibleFraction, maxScroll)
}
