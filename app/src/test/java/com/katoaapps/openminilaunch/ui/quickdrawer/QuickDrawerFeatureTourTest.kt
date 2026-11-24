package com.katoaapps.openminilaunch.ui.quickdrawer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickDrawerFeatureTourTest {
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
