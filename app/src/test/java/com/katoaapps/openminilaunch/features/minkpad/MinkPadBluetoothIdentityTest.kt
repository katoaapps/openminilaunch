package com.katoaapps.openminilaunch.features.minkpad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MinkPadBluetoothIdentityTest {
    @Test
    fun compatibilityNameIncludesOriginalDeviceName() {
        assertEquals(
            "Pro Controller - MinkPad (Titan 2)",
            minkPadBluetoothName("Titan 2"),
        )
    }

    @Test
    fun compatibilityNameTrimsOriginalDeviceName() {
        assertEquals(
            "Pro Controller - MinkPad (Pixel 10)",
            minkPadBluetoothName("  Pixel 10  "),
        )
    }

    @Test
    fun compatibilityNameRespectsBluetoothUtf8Limit() {
        val name = minkPadBluetoothName("Mink 🦦".repeat(100))

        assertTrue(name.toByteArray(Charsets.UTF_8).size <= 248)
        assertTrue(name.endsWith(')'))
        assertFalse(name.contains('\uFFFD'))
    }

    @Test
    fun recognizesCurrentAndLegacyMinkPadNames() {
        assertTrue(isMinkPadBluetoothName("Pro Controller - MinkPad (Titan 2)"))
        assertTrue(isMinkPadBluetoothName("Pro Controller - MinkPad"))
        assertTrue(isMinkPadBluetoothName("Pro Controller"))
        assertTrue(isMinkPadBluetoothName("MinkPad 3554A64F"))
        assertFalse(isMinkPadBluetoothName("Titan 2"))
    }
}
