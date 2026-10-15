package com.katoaapps.openminilaunch.ui.minkspace.widget

import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MinkSpaceWidgetSizingTest {
    @Test fun regularPhoneUsesMeasuredPanelProportions() {
        val viewport = minkSpaceWidgetViewport(
            screenWidthDp = 412f,
            screenHeightDp = 915f,
            smallestWidthDp = 412,
            orientation = Configuration.ORIENTATION_PORTRAIT,
            twoPanelModeForLargeDisplays = false,
        )

        assertTrue(viewport.collapsed.width < viewport.expanded.width)
        assertEquals(368f, viewport.expanded.width, .01f)
        assertEquals(321.36f, viewport.expanded.height, .01f)
    }

    @Test fun landscapeTabletUsesLargeDisplayHeightCap() {
        val viewport = minkSpaceWidgetViewport(
            screenWidthDp = 1200f,
            screenHeightDp = 800f,
            smallestWidthDp = 800,
            orientation = Configuration.ORIENTATION_LANDSCAPE,
            twoPanelModeForLargeDisplays = true,
        )

        assertEquals(566f, viewport.expanded.width, .01f)
        assertEquals(420f, viewport.expanded.height, .01f)
    }

    @Test fun compatibilityDistinguishesCollapsedExpandedAndCramped() {
        val viewport = MinkSpaceWidgetViewport(
            collapsed = MinkSpaceWidgetSizeDp(220f, 280f),
            expanded = MinkSpaceWidgetSizeDp(380f, 330f),
        )

        assertEquals(
            MinkSpaceWidgetFit.COLLAPSED,
            minkSpaceWidgetFit(200f, 200f, viewport),
        )
        assertEquals(
            MinkSpaceWidgetFit.EXPANDED,
            minkSpaceWidgetFit(300f, 300f, viewport),
        )
        assertEquals(
            MinkSpaceWidgetFit.CRAMPED,
            minkSpaceWidgetFit(500f, 400f, viewport),
        )
        assertEquals(
            MinkSpaceWidgetFit.UNKNOWN,
            minkSpaceWidgetFit(0f, 200f, viewport),
        )
    }
}
