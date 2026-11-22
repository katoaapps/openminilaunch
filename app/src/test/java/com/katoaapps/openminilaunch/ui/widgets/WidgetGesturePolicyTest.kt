package com.katoaapps.openminilaunch.ui.widgets

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetGesturePolicyTest {
    @Test
    fun movementInsideTouchSlopRemainsUndecided() {
        assertEquals(
            WidgetGestureAxis.UNDECIDED,
            classifyWidgetGesture(dx = 7f, dy = 5f, touchSlop = 8),
        )
    }

    @Test
    fun clearlyHorizontalMovementRoutesToPageStrip() {
        assertEquals(
            WidgetGestureAxis.HORIZONTAL,
            classifyWidgetGesture(dx = -24f, dy = 8f, touchSlop = 8),
        )
    }

    @Test
    fun clearlyVerticalMovementRemainsWithWidgetWall() {
        assertEquals(
            WidgetGestureAxis.VERTICAL,
            classifyWidgetGesture(dx = 8f, dy = 24f, touchSlop = 8),
        )
    }

    @Test
    fun ambiguousDiagonalStartWaitsForDirectionalIntent() {
        assertEquals(
            WidgetGestureAxis.UNDECIDED,
            classifyWidgetGesture(dx = 12f, dy = 12f, touchSlop = 8),
        )
        assertEquals(
            WidgetGestureAxis.HORIZONTAL,
            classifyWidgetGesture(dx = 28f, dy = 14f, touchSlop = 8),
        )
        assertEquals(
            WidgetGestureAxis.VERTICAL,
            classifyWidgetGesture(dx = 14f, dy = 28f, touchSlop = 8),
        )
    }

    @Test
    fun releaseBelowDistanceAndVelocityThresholdReturnsToStart() {
        assertEquals(0, widgetDragDirection(40f, 400f, 48f, 800f))
    }

    @Test
    fun finalFlingVelocityWinsOverEarlierDragDistance() {
        assertEquals(1, widgetDragDirection(90f, -900f, 48f, 800f))
        assertEquals(-1, widgetDragDirection(-90f, 900f, 48f, 800f))
    }
}
