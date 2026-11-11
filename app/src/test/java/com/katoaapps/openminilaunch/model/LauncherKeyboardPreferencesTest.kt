package com.katoaapps.openminilaunch.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherKeyboardPreferencesTest {
    @Test
    fun defaultPortraitHeightUsesFortyPercentOfScreen() {
        val height = resolveLauncherKeyboardHeightDp(
            preferences = LauncherKeyboardPreferences(),
            screenWidthDp = 400,
            screenHeightDp = 800,
        )

        assertEquals(320f, height, .001f)
    }

    @Test
    fun landscapeHeightKeepsEstablishedDefault() {
        val height = resolveLauncherKeyboardHeightDp(
            preferences = LauncherKeyboardPreferences(),
            screenWidthDp = 800,
            screenHeightDp = 400,
        )

        assertEquals(DEFAULT_KEYBOARD_HEIGHT_DP, height, .001f)
    }

    @Test
    fun portraitPresetCanUseThirtyPercent() {
        val height = resolveLauncherKeyboardHeightDp(
            preferences = LauncherKeyboardPreferences(
                portraitHeightPreset = LauncherKeyboardHeightPreset.COMPACT,
            ),
            screenWidthDp = 400,
            screenHeightDp = 800,
        )

        assertEquals(240f, height, .001f)
    }

    @Test
    fun tallPortraitPresetUsesHalfTheScreen() {
        val height = resolveLauncherKeyboardHeightDp(
            preferences = LauncherKeyboardPreferences(
                portraitHeightPreset = LauncherKeyboardHeightPreset.TALL,
            ),
            screenWidthDp = 800,
            screenHeightDp = 1600,
        )

        assertEquals(800f, height, .001f)
    }
}
