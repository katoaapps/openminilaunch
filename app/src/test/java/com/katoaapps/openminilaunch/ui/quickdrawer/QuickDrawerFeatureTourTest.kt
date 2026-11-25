package com.katoaapps.openminilaunch.ui.quickdrawer

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickDrawerFeatureTourTest {
    @Test
    fun helpCardUsesTheSideThatCanFitItsMeasuredHeight() {
        val screenHeight = 500f
        val cardHeight = 180f
        val margin = 18f
        val gap = 12f

        assertEquals(
            212f,
            quickDrawerTourCardTop(
                target = Rect(0f, 100f, 100f, 200f),
                screenHeightPx = screenHeight,
                cardHeightPx = cardHeight,
                edgeMarginPx = margin,
                targetGapPx = gap,
            ),
            0f,
        )
        assertEquals(
            138f,
            quickDrawerTourCardTop(
                target = Rect(0f, 330f, 100f, 400f),
                screenHeightPx = screenHeight,
                cardHeightPx = cardHeight,
                edgeMarginPx = margin,
                targetGapPx = gap,
            ),
            0f,
        )
    }

    @Test
    fun helpCardStaysInsideCompactOverlayWhenNeitherSideFits() {
        val top = quickDrawerTourCardTop(
            target = Rect(0f, 130f, 100f, 210f),
            screenHeightPx = 300f,
            cardHeightPx = 180f,
            edgeMarginPx = 18f,
            targetGapPx = 12f,
        )

        assertTrue(top >= 18f)
        assertTrue(top + 180f <= 282f)
    }

    @Test
    fun baseTourOnlyIncludesControlsThatAlwaysExist() {
        assertEquals(
            listOf(
                QuickDrawerTourTarget.ANNOUNCEMENT,
                QuickDrawerTourTarget.QUICK_APPS,
                QuickDrawerTourTarget.GAMES,
                QuickDrawerTourTarget.MINKPAD_LITE,
                QuickDrawerTourTarget.ALL_APPS,
            ),
            quickDrawerTourSteps(
                listOf(
                    QuickDrawerDestination.MINKPAD_LITE,
                    QuickDrawerDestination.ALL_APPS,
                ),
            ).map(QuickDrawerTourStep::target),
        )
    }

    @Test
    fun privateAndWorkLessonsFollowVisibleDestinations() {
        assertEquals(
            listOf(
                QuickDrawerTourTarget.ANNOUNCEMENT,
                QuickDrawerTourTarget.QUICK_APPS,
                QuickDrawerTourTarget.GAMES,
                QuickDrawerTourTarget.ALL_APPS,
                QuickDrawerTourTarget.PRIVATE_SPACE,
                QuickDrawerTourTarget.WORK_SPACE,
            ),
            quickDrawerTourSteps(
                listOf(
                    QuickDrawerDestination.ALL_APPS,
                    QuickDrawerDestination.PRIVATE_SPACE,
                    QuickDrawerDestination.WORK_SPACE,
                ),
            ).map(QuickDrawerTourStep::target),
        )
    }

    @Test
    fun firstOpenAndExplicitReplayStartAfterOnboarding() {
        assertTrue(
            shouldStartQuickDrawerFeatureTour(
                onboardingComplete = true,
                currentTourSeen = false,
                replayRequested = false,
            ),
        )
        assertTrue(
            shouldStartQuickDrawerFeatureTour(
                onboardingComplete = true,
                currentTourSeen = true,
                replayRequested = true,
            ),
        )
        assertFalse(
            shouldStartQuickDrawerFeatureTour(
                onboardingComplete = false,
                currentTourSeen = false,
                replayRequested = true,
            ),
        )
    }
}
