package com.katoaapps.openminilaunch.ui.keyboard

import com.katoaapps.openminilaunch.model.LauncherKeyboardPreferences
import com.katoaapps.openminilaunch.model.LauncherKeyboardStyle
import com.katoaapps.openminilaunch.ui.theme.contrastRatio
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherKeyboardColorsTest {
    @Test
    fun similarThemeColorsStillProduceReadableKeyLegends() {
        val colors = resolveLauncherKeyboardColors(
            preferences = LauncherKeyboardPreferences(style = LauncherKeyboardStyle.FLAT_PRIMARY),
            themePrimaryArgb = 0xFFF4F1E8.toInt(),
            themeSecondaryArgb = 0xFFF7E7CE.toInt(),
        )

        assertTrue(contrastRatio(colors.key, colors.content) >= 4.5f)
    }

    @Test
    fun readableThemePairKeepsItsIntendedContrast() {
        val colors = resolveLauncherKeyboardColors(
            preferences = LauncherKeyboardPreferences(style = LauncherKeyboardStyle.FLAT_PRIMARY),
            themePrimaryArgb = 0xFF602C00.toInt(),
            themeSecondaryArgb = 0xFFF7E7CE.toInt(),
        )

        assertTrue(contrastRatio(colors.key, colors.content) >= 4.5f)
    }
}
