@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.katoaapps.openminilaunch.ui.widgets

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Bridges a horizontal gesture owned by the native widget wall back to its Compose page strip.
 * The wall owns the Android touch stream so widget children and the surrounding page never race
 * to intercept the same gesture.
 */
internal interface WidgetHorizontalDragHandler {
    fun onDragStart()
    fun onDragBy(deltaX: Float)
    fun onDragEnd(totalDeltaX: Float, velocityX: Float)
    fun onDragCancel()
}

internal fun widgetDragDirection(
    totalDeltaX: Float,
    velocityX: Float,
    distanceThreshold: Float,
    velocityThreshold: Float,
): Int = when {
    abs(velocityX) >= velocityThreshold -> if (velocityX < 0f) 1 else -1
    abs(totalDeltaX) >= distanceThreshold -> if (totalDeltaX < 0f) 1 else -1
    else -> 0
}

@Composable
internal fun rememberWidgetPagerDragHandler(
    state: PagerState,
): WidgetHorizontalDragHandler {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    return remember(state, scope, density) {
        object : WidgetHorizontalDragHandler {
            private var startPage = 0

            override fun onDragStart() {
                startPage = state.currentPage
            }

            override fun onDragBy(deltaX: Float) {
                state.dispatchRawDelta(-deltaX)
            }

            override fun onDragEnd(totalDeltaX: Float, velocityX: Float) {
                val pageSize = state.layoutInfo.pageSize.coerceAtLeast(1)
                val distanceThreshold = maxOf(48f * density, pageSize * .18f)
                val velocityThreshold = 800f * density
                val direction = widgetDragDirection(
                    totalDeltaX,
                    velocityX,
                    distanceThreshold,
                    velocityThreshold,
                )
                val target = (startPage + direction).coerceIn(0, state.pageCount - 1)
                scope.launch { state.animateScrollToPage(target) }
            }

            override fun onDragCancel() {
                scope.launch { state.animateScrollToPage(startPage.coerceIn(0, state.pageCount - 1)) }
            }
        }
    }
}

@Composable
internal fun rememberWidgetLazyRowDragHandler(
    state: LazyListState,
    itemCount: Int,
): WidgetHorizontalDragHandler {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    return remember(state, scope, density, itemCount) {
        object : WidgetHorizontalDragHandler {
            private var startIndex = 0

            override fun onDragStart() {
                startIndex = state.firstVisibleItemIndex
            }

            override fun onDragBy(deltaX: Float) {
                state.dispatchRawDelta(-deltaX)
            }

            override fun onDragEnd(totalDeltaX: Float, velocityX: Float) {
                val itemSize = state.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1
                val distanceThreshold = maxOf(48f * density, itemSize * .18f)
                val velocityThreshold = 800f * density
                val direction = widgetDragDirection(
                    totalDeltaX,
                    velocityX,
                    distanceThreshold,
                    velocityThreshold,
                )
                val target = (startIndex + direction).coerceIn(0, itemCount - 1)
                scope.launch { state.animateScrollToItem(target) }
            }

            override fun onDragCancel() {
                scope.launch { state.animateScrollToItem(startIndex.coerceIn(0, itemCount - 1)) }
            }
        }
    }
}
