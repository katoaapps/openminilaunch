package com.katoaapps.openminilaunch.features.minkpad.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsKeyboardLayoutTest {
    @Test fun resolvesPhoneAltNumberLayer() {
        val cases = listOf(
            Triple('0', 'q', 39),
            Triple('1', 'w', 30),
            Triple('2', 'e', 31),
            Triple('3', 'r', 32),
            Triple('4', 's', 33),
            Triple('5', 'd', 34),
            Triple('6', 'f', 35),
            Triple('7', 'x', 36),
            Triple('8', 'c', 37),
            Triple('9', 'v', 38),
        )

        cases.forEach { (resolved, base, usage) ->
            assertEquals(HidKey(usage), UsKeyboardLayout.alternateKey(resolved.code, base.code))
        }
    }

    @Test fun resolvesPhoneAltPunctuationLayer() {
        assertEquals(HidKey(56, 2), UsKeyboardLayout.alternateKey('?'.code, 'p'.code))
    }

    @Test fun leavesUnchangedAltKeyForTheHost() {
        assertNull(UsKeyboardLayout.alternateKey('q'.code, 'q'.code))
    }

    @Test fun rejectsMissingOrUnsupportedAltCharacters() {
        assertNull(UsKeyboardLayout.alternateKey(0, 'q'.code))
        assertNull(UsKeyboardLayout.alternateKey('é'.code, 'e'.code))
        assertNull(UsKeyboardLayout.alternateKey(0x1f600, 'e'.code))
    }
}
