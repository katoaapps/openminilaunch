package com.katoaapps.minkpad.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MinkPadBluetoothIdentityTest {
    @Test
    fun compatibilityNameIncludesOriginalDeviceName() {
        assertEquals(
            "Pro Controller - MinkPad (Example Phone)",
            minkPadBluetoothName("Example Phone"),
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
        assertTrue(isMinkPadBluetoothName("Pro Controller - MinkPad (Example Phone)"))
        assertTrue(isMinkPadBluetoothName("Pro Controller - MinkPad"))
        assertTrue(isMinkPadBluetoothName("Pro Controller"))
        assertTrue(isMinkPadBluetoothName("MinkPad ABCD1234"))
        assertFalse(isMinkPadBluetoothName("Example Phone"))
    }

    @Test
    fun restorationUsesRememberedIdentityAfterInterruptedSession() {
        assertEquals(
            "Example Phone",
            resolvedRestoredBluetoothName(
                current = "Pro Controller - MinkPad (Example Phone)",
                remembered = "Example Phone",
                configured = "Configured Phone",
                model = "Model Phone",
            ),
        )
    }

    @Test
    fun restorationFallsBackWithoutRememberingAnotherMinkPadIdentity() {
        assertEquals(
            "Configured Phone",
            resolvedRestoredBluetoothName(
                current = "Pro Controller",
                remembered = "MinkPad ABCD1234",
                configured = "Configured Phone",
                model = "Model Phone",
            ),
        )
    }

    @Test
    fun restorationLeavesAnAlreadyNormalNameAlone() {
        assertEquals(
            "Current Phone",
            resolvedRestoredBluetoothName(
                current = "Current Phone",
                remembered = "Old Phone",
                configured = null,
                model = null,
            ),
        )
    }
}
