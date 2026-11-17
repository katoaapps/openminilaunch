package com.katoaapps.minkboard.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorContrastTest {
    @Test
    fun goldStatusUsesReadableDarkText() {
        val gold = Color(0xFFD6A300)
        val content = readableContentColor(gold)

        assertEquals(Color(0xFF172019), content)
        assertTrue(contrastRatio(gold, content) >= 4.5f)
    }
}
