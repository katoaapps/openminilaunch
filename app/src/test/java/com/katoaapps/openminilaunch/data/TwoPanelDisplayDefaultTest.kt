package com.katoaapps.openminilaunch.data

import android.content.res.Configuration
import android.view.Surface
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TwoPanelDisplayDefaultTest {
    @Test fun landscapeNativeTabletDefaultsOnInBothOrientations() {
        assertTrue(hasLandscapeNaturalOrientation(800, 2560, 1600, Surface.ROTATION_0))
        assertTrue(hasLandscapeNaturalOrientation(800, 1600, 2560, Surface.ROTATION_90))
        assertTrue(hasLandscapeNaturalOrientation(800, 2560, 1600, Surface.ROTATION_180))
        assertTrue(hasLandscapeNaturalOrientation(800, 1600, 2560, Surface.ROTATION_270))
    }

    @Test fun portraitNativeLargeDisplayAndPhoneDoNotDefaultOn() {
        assertFalse(hasLandscapeNaturalOrientation(800, 1600, 2560, Surface.ROTATION_0))
        assertFalse(hasLandscapeNaturalOrientation(800, 2560, 1600, Surface.ROTATION_90))
        assertFalse(hasLandscapeNaturalOrientation(599, 2560, 1600, Surface.ROTATION_0))
    }

    @Test fun unknownOrInvalidDisplayDoesNotDefaultOn() {
        assertFalse(hasLandscapeNaturalOrientation(800, 0, 1600, Surface.ROTATION_0))
        assertFalse(hasLandscapeNaturalOrientation(800, 2560, 1600, -1))
    }

    @Test fun openedLandscapeFoldableCanOptInEvenWhenNaturalOrientationIsPortrait() {
        assertTrue(shouldDefaultTwoPanelMode(700, Configuration.ORIENTATION_LANDSCAPE, false))
        assertFalse(shouldDefaultTwoPanelMode(700, Configuration.ORIENTATION_PORTRAIT, false))
        assertFalse(shouldDefaultTwoPanelMode(599, Configuration.ORIENTATION_LANDSCAPE, true))
    }
}
