package com.katoaapps.minkboard.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatusScrollIndicatorTest {
    @Test
    fun railProgressCoversTheEntireStatusPalette() {
        assertEquals(0, statusScrollColorIndex(progress = 0f, itemCount = 5))
        assertEquals(1, statusScrollColorIndex(progress = 0.25f, itemCount = 5))
        assertEquals(2, statusScrollColorIndex(progress = 0.5f, itemCount = 5))
        assertEquals(3, statusScrollColorIndex(progress = 0.75f, itemCount = 5))
        assertEquals(4, statusScrollColorIndex(progress = 1f, itemCount = 5))
    }

    @Test
    fun railProgressHandlesEmptySingleAndOutOfRangeInputs() {
        assertNull(statusScrollColorIndex(progress = 0.5f, itemCount = 0))
        assertEquals(0, statusScrollColorIndex(progress = 0.8f, itemCount = 1))
        assertEquals(0, statusScrollColorIndex(progress = -2f, itemCount = 4))
        assertEquals(3, statusScrollColorIndex(progress = 2f, itemCount = 4))
    }
}
