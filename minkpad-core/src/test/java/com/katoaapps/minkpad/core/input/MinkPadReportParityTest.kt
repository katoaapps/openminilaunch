package com.katoaapps.minkpad.core.input

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class MinkPadReportParityTest {
    @Test fun keyboardReportKeepsModifierAndSixKeyLayout() {
        assertArrayEquals(
            byteArrayOf(0x03, 0, 4, 5, 6, 7, 8, 9),
            UsKeyboardLayout.report(listOf(4, 5, 6, 7, 8, 9), 0x03),
        )
    }

    @Test fun moreThanSixKeysEmitsTheHidRolloverReport() {
        assertArrayEquals(
            byteArrayOf(0, 0, 1, 1, 1, 1, 1, 1),
            UsKeyboardLayout.report(listOf(4, 5, 6, 7, 8, 9, 10), 0),
        )
    }

    @Test fun emptyKeyboardReportReleasesModifiersAndKeys() {
        assertArrayEquals(ByteArray(8), UsKeyboardLayout.report(emptyList(), 0))
    }

    @Test fun consumerAndRemoteUsagesMatchOpenMink() {
        assertEquals(0x224, MinkPadConsumerAction.BACK.usage)
        assertEquals(0xCD, MinkPadConsumerAction.PLAY_PAUSE.usage)
        assertEquals(82, MinkPadAction.UP.usage)
        assertEquals(40, MinkPadAction.ENTER.usage)
    }
}
