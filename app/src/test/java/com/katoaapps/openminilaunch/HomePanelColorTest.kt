package com.katoaapps.openminilaunch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomePanelColorTest {
    @Test fun parsesSixDigitHexWithOrWithoutHash() {
        assertEquals(0xFF602C00.toInt(), parseHomePanelHex("#602C00"))
        assertEquals(0xFF173529.toInt(), parseHomePanelHex("173529"))
    }

    @Test fun rejectsIncompleteOrInvalidHex() {
        assertNull(parseHomePanelHex("#12345"))
        assertNull(parseHomePanelHex("#GG0000"))
    }

    @Test fun formatsOpaqueColorWithoutAlpha() {
        assertEquals("#602C00", formatHomePanelHex(0xFF602C00.toInt()))
    }
}
