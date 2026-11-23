package com.katoaapps.openminilaunch.ui.onboarding

import com.katoaapps.openminilaunch.model.HomeLayoutStyle
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
                HomeTourTarget.HEADER,
                HomeTourTarget.GESTURES,
                HomeTourTarget.KEYBOARD_ACTION,
                HomeTourTarget.MAGIC_BOX,
            ),
            homeFeatureTourSteps(HomeLayoutStyle.MINK).map(HomeTourStep::target),
        )
    }

    @Test
    fun classicTourTeachesPagesAndRailBeforeMagicBox() {
        assertEquals(
            listOf(
                HomeTourTarget.CLASSIC_PAGES,
                HomeTourTarget.CLASSIC_FOLDER_TITLE,
                HomeTourTarget.CLASSIC_FOLDER_HOME,
                HomeTourTarget.CLASSIC_FOLDER_EDIT,
                HomeTourTarget.CLASSIC_RAIL,
                HomeTourTarget.HEADER,
                HomeTourTarget.GESTURES,
                HomeTourTarget.KEYBOARD_ACTION,
                HomeTourTarget.MAGIC_BOX,
            ),
            homeFeatureTourSteps(HomeLayoutStyle.CLASSIC).map(HomeTourStep::target),
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
