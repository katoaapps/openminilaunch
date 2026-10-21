package com.katoaapps.openminilaunch.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FontAppearanceTest {
    @Test fun fontSizeAlwaysSnapsToASupportedStep() {
        assertEquals(.85f, normalizeFontSizeScale(.78f))
        assertEquals(1.15f, normalizeFontSizeScale(1.12f))
        assertEquals(1.45f, normalizeFontSizeScale(2f))
    }

    @Test fun invalidStoredFontSizesReturnToDefault() {
        assertEquals(DEFAULT_FONT_SIZE_SCALE, normalizeFontSizeScale(Float.NaN))
        assertEquals(DEFAULT_FONT_SIZE_SCALE, normalizeFontSizeScale(Float.POSITIVE_INFINITY))
    }
}
