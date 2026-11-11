package com.katoaapps.openminilaunch.ui.launcher.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeHeaderCutoutLayoutTest {
    @Test
    fun fullWidthPagerKeepsCutoutStableWhileHomeSlides() {
        val windowCutout = listOf(IntRect(500, 0, 580, 92))

        val atRest = localizeHomeHeaderCutouts(
            windowCutouts = windowCutout,
            headerOriginInWindow = resolveHomeHeaderOriginInWindow(
                positionInWindow = Offset.Zero,
                followHorizontalWindowOffset = false,
            ),
            containerWidthPx = 1080,
            layoutDirection = LayoutDirection.Ltr,
        )
        val midwayThroughSwipe = localizeHomeHeaderCutouts(
            windowCutouts = windowCutout,
            headerOriginInWindow = resolveHomeHeaderOriginInWindow(
                positionInWindow = Offset(-420f, 0f),
                followHorizontalWindowOffset = false,
            ),
            containerWidthPx = 1080,
            layoutDirection = LayoutDirection.Ltr,
        )

        assertEquals(atRest, midwayThroughSwipe)
    }

    @Test
    fun twoPanelHeaderTracksItsRealWindowPosition() {
        val origin = resolveHomeHeaderOriginInWindow(
            positionInWindow = Offset(320f, 0f),
            followHorizontalWindowOffset = true,
        )
        val localized = localizeHomeHeaderCutouts(
            windowCutouts = listOf(IntRect(500, 0, 580, 92)),
            headerOriginInWindow = origin,
            containerWidthPx = 540,
            layoutDirection = LayoutDirection.Ltr,
        )

        assertEquals(IntOffset(320, 0), origin)
        assertEquals(listOf(IntRect(180, 0, 260, 92)), localized)
    }

    @Test
    fun centeredCutoutPlacesDateAndTimeOnOppositeSides() {
        val plan = planHomeHeaderAroundCutout(
            containerWidthPx = 720,
            contentStartPx = 24,
            contentEndPx = 696,
            rowHeightPx = 48,
            verticalPaddingPx = 8,
            measuredLeadingWidthPx = 80,
            minimumFlexibleWidthPx = 64,
            naturalDateWidthPx = 170,
            naturalTimeWidthPx = 80,
            measuredTrailingWidthPx = 96,
            groupGapPx = 8,
            cutouts = listOf(IntRect(300, 0, 420, 92)),
        )

        assertEquals(92, plan.heightPx)
        assertEquals(112, plan.dateLeftPx)
        assertEquals(180, plan.dateMaxWidthPx)
        assertEquals(512, plan.timeLeftPx)
        assertEquals(80, plan.timeMaxWidthPx)
    }

    @Test
    fun cornerCutoutUsesTheLargeSafeRegion() {
        val plan = planHomeHeaderAroundCutout(
            containerWidthPx = 720,
            contentStartPx = 24,
            contentEndPx = 696,
            rowHeightPx = 48,
            verticalPaddingPx = 8,
            measuredLeadingWidthPx = 80,
            minimumFlexibleWidthPx = 64,
            naturalDateWidthPx = 120,
            naturalTimeWidthPx = 64,
            measuredTrailingWidthPx = 96,
            groupGapPx = 8,
            cutouts = listOf(IntRect(0, 0, 110, 88)),
        )

        assertEquals(88, plan.heightPx)
        assertTrue(plan.leadingLeftPx >= 118)
        assertTrue(plan.trailingLeftPx >= plan.leadingLeftPx)
    }

    @Test
    fun topRightCutoutUsesTheLargeLeftRegion() {
        val plan = planHomeHeaderAroundCutout(
            containerWidthPx = 720,
            contentStartPx = 24,
            contentEndPx = 696,
            rowHeightPx = 48,
            verticalPaddingPx = 8,
            measuredLeadingWidthPx = 80,
            minimumFlexibleWidthPx = 64,
            naturalDateWidthPx = 120,
            naturalTimeWidthPx = 64,
            measuredTrailingWidthPx = 96,
            groupGapPx = 8,
            cutouts = listOf(IntRect(610, 0, 720, 88)),
        )

        assertEquals(88, plan.heightPx)
        assertEquals(24, plan.leadingLeftPx)
        assertTrue(plan.trailingLeftPx < 602)
    }

    @Test
    fun oversizedCutoutFallsBackBelowIt() {
        val plan = planHomeHeaderAroundCutout(
            containerWidthPx = 360,
            contentStartPx = 16,
            contentEndPx = 344,
            rowHeightPx = 48,
            verticalPaddingPx = 8,
            measuredLeadingWidthPx = 80,
            minimumFlexibleWidthPx = 64,
            naturalDateWidthPx = 120,
            naturalTimeWidthPx = 64,
            measuredTrailingWidthPx = 96,
            groupGapPx = 8,
            cutouts = listOf(IntRect(70, 0, 290, 96)),
        )

        assertEquals(104, plan.contentTopPx)
        assertEquals(160, plan.heightPx)
    }

    @Test
    fun noCutoutUsesTheNormalCompactHeaderHeight() {
        val plan = planHomeHeaderAroundCutout(
            containerWidthPx = 360,
            contentStartPx = 16,
            contentEndPx = 344,
            rowHeightPx = 48,
            verticalPaddingPx = 8,
            measuredLeadingWidthPx = 80,
            minimumFlexibleWidthPx = 64,
            naturalDateWidthPx = 120,
            naturalTimeWidthPx = 64,
            measuredTrailingWidthPx = 96,
            groupGapPx = 8,
            cutouts = emptyList(),
        )

        assertEquals(64, plan.heightPx)
        assertEquals(8, plan.contentTopPx)
    }
}
