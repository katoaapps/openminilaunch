package com.katoaapps.openminilaunch.features.minkpad.connection.classic

import com.katoaapps.openminilaunch.features.minkpad.hid.MinkPadReportId
import org.junit.Assert.assertEquals
import org.junit.Test

class MinkPadClassicReportMapTest {
    @Test
    fun comboDescriptorContainsKeyboardPointerAndConsumerReports() {
        val reportIds = MinkPadClassicReportMap.bytes.toList().windowed(2)
            .filter { it[0].toInt() and 0xff == 0x85 }
            .map { it[1].toInt() and 0xff }

        assertEquals(
            listOf(MinkPadReportId.KEYBOARD, MinkPadReportId.POINTER, MinkPadReportId.CONSUMER),
            reportIds,
        )
    }
}
