package com.katoaapps.openminilaunch.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFeatureTourTest {
    @Test
    fun unfinishedOnboardingDoesNotShowHomeTour() {
        assertFalse(
            shouldShowHomeFeatureTour(
                onboardingComplete = false,
                currentTourSeen = false,
            ),
        )
    }

    @Test
    fun everyReturningUserGetsFullReleaseTour() {
        assertTrue(
            shouldShowHomeFeatureTour(
                onboardingComplete = true,
                currentTourSeen = false,
            ),
        )
    }

    @Test
    fun userWhoNeverSawHomeTourGetsFullTargetOrder() {
        assertTrue(
            shouldShowHomeFeatureTour(
                onboardingComplete = true,
                currentTourSeen = false,
            ),
        )
        assertEquals(
            listOf(
                HomeTourTarget.APP_GRID,
                HomeTourTarget.MINK_SPACE,
                HomeTourTarget.MAGIC_BOX,
                HomeTourTarget.KEYBOARD_ACTION,
                HomeTourTarget.HEADER,
                HomeTourTarget.GESTURES,
            ),
            homeFeatureTourSteps().map(HomeTourStep::target),
        )
    }

    @Test
    fun completedCurrentTourDoesNotShowAgain() {
        assertFalse(
            shouldShowHomeFeatureTour(
                onboardingComplete = true,
                currentTourSeen = true,
            ),
        )
    }
}
