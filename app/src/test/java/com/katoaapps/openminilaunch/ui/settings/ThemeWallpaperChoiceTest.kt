package com.katoaapps.openminilaunch.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeWallpaperChoiceTest {
    @Test
    fun deviceWallpaperRequiresConfirmationBeforeAThemeCanReplaceIt() {
        assertTrue(
            shouldConfirmThemeWallpaperReplacement(
                appPhotoSelected = false,
                deviceWallpaperSelected = true,
            ),
        )
    }

    @Test
    fun legacyAppPhotoAlsoRequiresConfirmation() {
        assertTrue(
            shouldConfirmThemeWallpaperReplacement(
                appPhotoSelected = true,
                deviceWallpaperSelected = false,
            ),
        )
    }

    @Test
    fun existingThemeWallpaperCanBeReplacedWithoutConfirmation() {
        assertFalse(
            shouldConfirmThemeWallpaperReplacement(
                appPhotoSelected = false,
                deviceWallpaperSelected = false,
            ),
        )
    }
}
