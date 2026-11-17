package com.katoaapps.minkboard.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatusCustomColorTest {
    @Test
    fun colorHexRoundTripsAsOpaqueArgb() {
        assertEquals("#602C00", formatStatusColorHex(0xFF602C00.toInt()))
        assertEquals(0xFF602C00.toInt(), parseStatusColorHex("#602c00"))
    }

    @Test
    fun malformedHexIsRejected() {
        assertNull(parseStatusColorHex("#12345"))
        assertNull(parseStatusColorHex("#NOTHEX"))
        assertNull(parseStatusColorHex("#12345678"))
    }
}
