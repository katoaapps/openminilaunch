package com.katoaapps.openminilaunch.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiColorsTest {
    @Test
    fun readableContentUsesDarkInkOnLightBackground() {
        val background = Color(0xFFF4F1E8)

        assertEquals(Color(0xFF172019), readableContentColor(background))
        assertTrue(contrastRatio(background, readableContentColor(background)) >= 4.5f)
    }

    @Test
    fun readableContentUsesLightInkOnDarkBackground() {
        val background = Color(0xFF101512)

        assertEquals(Color(0xFFFAF9F6), readableContentColor(background))
        assertTrue(contrastRatio(background, readableContentColor(background)) >= 4.5f)
    }

    @Test
    fun preferredContentIsRetainedWhenItIsReadable() {
        val background = Color(0xFF602C00)
        val preferred = Color(0xFFF7E7CE)

        assertEquals(preferred, ensureReadableContentColor(background, preferred))
    }

    @Test
    fun preferredContentFallsBackWhenItIsNotReadable() {
        val background = Color(0xFFF4F1E8)
        val preferred = Color(0xFFF7E7CE)

        assertEquals(Color(0xFF172019), ensureReadableContentColor(background, preferred))
    }

    @Test
    fun resolvedContentKeepsReadablePreferenceAndRepairsUnsafePreference() {
        val darkBackground = Color(0xFF101512)
        val lightBackground = Color(0xFFF4F1E8)
        val preferredLight = Color(0xFFFAF9F6)

        assertEquals(preferredLight, resolveContentColor(darkBackground, preferredLight))
        assertEquals(Color(0xFF172019), resolveContentColor(lightBackground, preferredLight))
        assertTrue(contrastRatio(lightBackground, resolveContentColor(lightBackground, preferredLight)) >= 4.5f)
    }
}
