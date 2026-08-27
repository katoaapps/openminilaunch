package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.data.restoredMessageSendMode
import com.katoaapps.openminilaunch.model.MessageSendMode
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageSendModeTest {
    @Test
    fun `old always ask preference migrates to system chooser`() {
        assertEquals(MessageSendMode.SYSTEM_CHOOSER, restoredMessageSendMode("ALWAYS_ASK"))
    }

    @Test
    fun `old messaging app preference migrates to preferred app`() {
        assertEquals(MessageSendMode.PREFERRED_APP, restoredMessageSendMode("MESSAGING_APP"))
        assertEquals(MessageSendMode.PREFERRED_APP, restoredMessageSendMode("DEFAULT_MESSENGER"))
    }

    @Test
    fun `missing or invalid preference uses system chooser`() {
        assertEquals(MessageSendMode.SYSTEM_CHOOSER, restoredMessageSendMode(null))
        assertEquals(MessageSendMode.SYSTEM_CHOOSER, restoredMessageSendMode("UNKNOWN"))
    }
}
