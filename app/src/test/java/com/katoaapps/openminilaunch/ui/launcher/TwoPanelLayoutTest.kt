package com.katoaapps.openminilaunch.ui.launcher

import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TwoPanelLayoutTest {
    private val landscape = Configuration.ORIENTATION_LANDSCAPE

    @Test fun regularDisplayRequiresOptInAndLargeLandscapeWindow() {
        assertNull(twoPanelGeometry(false, 700, landscape, 1200, 1f, null))
        assertNull(twoPanelGeometry(true, 599, landscape, 1200, 1f, null))
        assertNull(twoPanelGeometry(true, 700, Configuration.ORIENTATION_PORTRAIT, 1200, 1f, null))
        assertNull(twoPanelGeometry(true, 700, landscape, 700, 1f, null))
        assertNotNull(twoPanelGeometry(true, 700, landscape, 800, 1f, null))
    }

    @Test fun unfoldedBookFoldSupportsTwoPanesInPortrait() {
        val flatBookFold = FoldLayoutFeature(
            vertical = true,
            separating = false,
            leftPx = 350,
            rightPx = 350,
        )

        assertNotNull(twoPanelGeometry(
            true, 700, Configuration.ORIENTATION_PORTRAIT, 700, 1f, flatBookFold,
        ))
        assertNull(twoPanelGeometry(
            true, 599, Configuration.ORIENTATION_PORTRAIT, 700, 1f, flatBookFold,
        ))
    }

    @Test fun regularDisplayHasTwoEqualPanesAndSmallGutter() {
        val geometry = twoPanelGeometry(true, 700, landscape, 1000, 1f, null)
        assertEquals(TwoPanelGeometry(494, 12, 0, 0), geometry)
    }

    @Test fun verticalHingeLeavesBothPanesClear() {
        val geometry = twoPanelGeometry(
            enabled = true,
            smallestWidthDp = 700,
            orientation = landscape,
            windowWidthPx = 1000,
            density = 1f,
            fold = FoldLayoutFeature(
                vertical = true,
                separating = true,
                leftPx = 490,
                rightPx = 510,
            ),
        )
        assertEquals(TwoPanelGeometry(490, 20, 0, 0), geometry)
    }

    @Test fun flatHorizontalFoldStillUsesLandscapeTwoPanelLayout() {
        val horizontalFold = FoldLayoutFeature(
            vertical = false,
            separating = true,
            leftPx = 0,
            rightPx = 1000,
        )

        assertNotNull(twoPanelGeometry(true, 700, landscape, 1000, 1f, horizontalFold))
    }

    @Test fun tabletopPostureAndNarrowFoldSideFallBackToOnePage() {
        assertNull(
            twoPanelGeometry(true, 700, landscape, 1000, 1f, FoldLayoutFeature(
                vertical = false,
                separating = true,
                leftPx = 490,
                rightPx = 510,
                halfOpened = true,
            )),
        )
        assertNull(
            twoPanelGeometry(
                true,
                700,
                landscape,
                1000,
                1f,
                FoldLayoutFeature(vertical = true, separating = true, leftPx = 300, rightPx = 320),
            ),
        )
    }

    @Test fun pageMappingKeepsMinkWhenUnfoldingAndFolding() {
        assertEquals(0, pairForFocusedPage(MINK_DAY_PAGE))
        assertEquals(MINK_DAY_PAGE, newlyRevealedPage(0))
        assertEquals(1, pairForFocusedPage(HOME_PAGE))
        assertEquals(1, pairForFocusedPage(WIDGET_PAGE))
        assertEquals(WIDGET_PAGE, newlyRevealedPage(1))
    }
}
