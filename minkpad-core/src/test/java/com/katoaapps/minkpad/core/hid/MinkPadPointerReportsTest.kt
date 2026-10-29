package com.katoaapps.minkpad.core.hid

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class MinkPadPointerReportsTest {
    @Test fun emitsPointerScrollAndPanInDescriptorOrder() {
        val reports = mutableListOf<ByteArray>()
        val pointer = MinkPadPointerReports { reports += it.copyOf() }

        assertEquals(1, pointer.move(12f, -9f, 3f, -2f))
        assertArrayEquals(byteArrayOf(0, 12, -9, 3, -2), reports.single())
    }

    @Test fun splitsMovementAtSignedEightBitProtocolBounds() {
        val reports = mutableListOf<ByteArray>()
        val pointer = MinkPadPointerReports { reports += it.copyOf() }

        assertEquals(3, pointer.move(300f, 0f, 0f, 0f))
        assertEquals(listOf(127, 127, 46), reports.map { it[1].toInt() })
    }

    @Test fun resetReleasesButtonsAndMotion() {
        val reports = mutableListOf<ByteArray>()
        val pointer = MinkPadPointerReports { reports += it.copyOf() }
        pointer.button(1, true)
        pointer.reset()

        assertArrayEquals(ByteArray(5), reports.last())
        assertArrayEquals(ByteArray(5), pointer.latestReport)
    }
}
