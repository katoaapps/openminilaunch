package com.katoaapps.minkboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.katoaapps.minkboard.model.BoardStatus
import kotlin.math.roundToInt

internal data class StatusScrollMetrics(
    val progress: Float,
    val visibleFraction: Float,
    val estimatedMaxScrollPx: Float,
)

/**
 * Keeps the draggable board-position rail above a small app-owned touch guard. The guard does not
 * attempt to override Android's system navigation gesture; it prevents board gestures from
 * beginning immediately beside that gesture area.
 */
@Composable
internal fun StatusScrollDock(
    state: LazyListState,
    statuses: List<BoardStatus>,
    itemSpacing: Dp,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        StatusScrollIndicator(
            state = state,
            statuses = statuses,
            itemSpacing = itemSpacing,
            contentDescription = contentDescription,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BOTTOM_TOUCH_GUARD_HEIGHT)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
                },
        )
    }
}

/** A compact board-position rail shared by the detailed board and its spatial map. */
@Composable
internal fun StatusScrollIndicator(
    state: LazyListState,
    statuses: List<BoardStatus>,
    itemSpacing: Dp,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val itemSpacingPx = with(density) { itemSpacing.toPx() }
    val metrics by remember(state, statuses.size, itemSpacingPx) {
        derivedStateOf { statusScrollMetrics(state, statuses.size, itemSpacingPx) }
    }
    val currentMetrics by rememberUpdatedState(metrics)
    val thumbColor by remember(statuses) {
        derivedStateOf {
            statuses.getOrNull(statusScrollColorIndex(metrics.progress, statuses.size) ?: -1)
                ?.let { Color(it.colorArgb) }
                ?: Color.Unspecified
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .height(32.dp)
            .fillMaxWidth()
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(metrics.progress, 0f..1f)
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val thumbWidth = maxWidth * metrics.visibleFraction
        val thumbTravel = maxWidth - thumbWidth
        val thumbOffset = thumbTravel * metrics.progress

        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(99.dp),
                ),
        )
        Box(
            Modifier
                .offset(x = thumbOffset)
                .width(thumbWidth)
                .height(8.dp)
                .background(
                    color = if (thumbColor == Color.Unspecified) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        thumbColor
                    },
                    shape = RoundedCornerShape(99.dp),
                ),
        )
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(state) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        val thumbTravelPx = size.width * (1f - currentMetrics.visibleFraction)
                        val scrollPerTrackPixel = currentMetrics.estimatedMaxScrollPx /
                            thumbTravelPx.coerceAtLeast(1f)
                        state.dispatchRawDelta(dragAmount * scrollPerTrackPixel)
                    }
                },
        )
    }
}

/**
 * Maps the rail's full travel to the complete status palette. A viewport can show several
 * statuses at once, so the last status may never become the list's first visible item. Progress
 * mapping guarantees that both the first and final rail positions use their corresponding colors.
 */
internal fun statusScrollColorIndex(progress: Float, itemCount: Int): Int? {
    if (itemCount <= 0) return null
    if (itemCount == 1) return 0
    return (progress.coerceIn(0f, 1f) * (itemCount - 1))
        .roundToInt()
        .coerceIn(0, itemCount - 1)
}

internal fun statusScrollMetrics(
    state: LazyListState,
    itemCount: Int,
    itemSpacingPx: Float,
): StatusScrollMetrics {
    val layout = state.layoutInfo
    val visibleItems = layout.visibleItemsInfo.filter { it.index < itemCount && it.size > 0 }
    val averageSize = visibleItems.map { it.size }
        .average()
        .takeIf { !it.isNaN() }
        ?.toFloat()
        ?: 1f
    val viewportSize = (layout.viewportEndOffset - layout.viewportStartOffset)
        .coerceAtLeast(1)
        .toFloat()
    val estimatedTotalSize = (
        averageSize * itemCount.coerceAtLeast(1) +
            itemSpacingPx * (itemCount - 1).coerceAtLeast(0)
        ).coerceAtLeast(viewportSize)
    val visibleFraction = (viewportSize / estimatedTotalSize).coerceIn(0.12f, 1f)
    val first = visibleItems.firstOrNull()
    val estimatedScroll = if (first == null) {
        0f
    } else {
        first.index * (averageSize + itemSpacingPx) +
            (layout.viewportStartOffset - first.offset).coerceAtLeast(0)
    }
    val maxScroll = (estimatedTotalSize - viewportSize).coerceAtLeast(1f)
    val progress = when {
        !state.canScrollBackward -> 0f
        !state.canScrollForward -> 1f
        else -> (estimatedScroll / maxScroll).coerceIn(0f, 1f)
    }
    return StatusScrollMetrics(progress, visibleFraction, maxScroll)
}

private val BOTTOM_TOUCH_GUARD_HEIGHT = 20.dp
