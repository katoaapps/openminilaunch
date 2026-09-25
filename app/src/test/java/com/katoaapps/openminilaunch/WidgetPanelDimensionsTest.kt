package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.model.WidgetGridSize
import com.katoaapps.openminilaunch.ui.widgets.widgetPanelDimensions
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetPanelDimensionsTest {
    @Test fun widgetSizingUsesItsPaneRatherThanTheDisplayWidth() {
        val gridSize = WidgetGridSize(columns = 2, rows = 2)

        val tabletPane = widgetPanelDimensions(contentWidthDp = 400f, gridSize = gridSize)
        val foldedPane = widgetPanelDimensions(contentWidthDp = 312f, gridSize = gridSize)

        assertEquals(200f, tabletPane.widthDp, .001f)
        assertEquals(200f, tabletPane.heightDp, .001f)
        assertEquals(156f, foldedPane.widthDp, .001f)
        assertEquals(156f, foldedPane.heightDp, .001f)
    }

    @Test fun widgetSizingMatchesTheRenderedMinimumsAndHeightCap() {
        val narrow = widgetPanelDimensions(200f, WidgetGridSize(columns = 1, rows = 1))
        val wideAndTall = widgetPanelDimensions(800f, WidgetGridSize(columns = 4, rows = 5))

        assertEquals(96f, narrow.widthDp, .001f)
        assertEquals(72f, narrow.heightDp, .001f)
        assertEquals(800f, wideAndTall.widthDp, .001f)
        assertEquals(420f, wideAndTall.heightDp, .001f)
    }
}
