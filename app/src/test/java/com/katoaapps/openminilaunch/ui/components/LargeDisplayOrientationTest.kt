package com.katoaapps.openminilaunch.ui.components

import android.content.pm.ActivityInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class LargeDisplayOrientationTest {
    @Test fun phonesRemainPortraitEvenWhenSettingIsEnabled() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            preferredActivityOrientation(smallestWidthDp = 599, enabled = true))
    }

    @Test fun largeDisplaysFollowUserRotationOnlyWhenEnabled() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            preferredActivityOrientation(smallestWidthDp = 600, enabled = false))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER,
            preferredActivityOrientation(smallestWidthDp = 600, enabled = true))
    }
}
