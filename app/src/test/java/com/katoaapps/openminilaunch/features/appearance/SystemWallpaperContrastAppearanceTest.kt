package com.katoaapps.openminilaunch.features.appearance

import androidx.compose.ui.graphics.Color
import com.katoaapps.openminilaunch.ui.theme.readableContentColor
import org.junit.Assert.assertEquals
import org.junit.Test

class SystemWallpaperContrastAppearanceTest {
    @Test
    fun darkTextHintProducesDarkLauncherContent() {
        val appearance = systemWallpaperContrastAppearance(
            primaryColorArgb = 0xFFFFD600.toInt(),
            supportsDarkText = true,
        )

        assertEquals(
            Color(0xFF172019),
            readableContentColor(Color(appearance.averageColorArgb)),
        )
    }

    @Test
    fun missingDarkTextHintProducesLightLauncherContent() {
        val appearance = systemWallpaperContrastAppearance(
            primaryColorArgb = 0xFFFFD600.toInt(),
            supportsDarkText = false,
        )

        assertEquals(
            Color(0xFFFAF9F6),
            readableContentColor(Color(appearance.averageColorArgb)),
        )
    }

    @Test
    fun olderAndroidFallsBackToWallpaperPaletteColor() {
        val yellow = 0xFFFFD600.toInt()
        val appearance = systemWallpaperContrastAppearance(
            primaryColorArgb = yellow,
            supportsDarkText = null,
        )

        assertEquals(yellow, appearance.averageColorArgb)
        assertEquals(yellow, appearance.headerColorArgb)
    }
}
