package com.katoaapps.openminilaunch.ui.magic

import com.katoaapps.openminilaunch.model.CommunicationRecipient
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SelectedMessageRecipientTest {
    @Test fun addressChipSurvivesSnapshot() {
        val selected = SelectedMessageRecipient.AddressRecipient(
            CommunicationRecipient("+15551234567", "Alex", "Mobile", userEntered = false),
            providerPackage = "org.example.messages",
        )
        assertEquals(selected, recipientFromSnapshot(selected.snapshot()))
    }

    @Test fun conversationChipSurvivesSnapshot() {
        val selected = SelectedMessageRecipient.ConversationRecipient(
            LauncherShortcutTarget(
                label = "Alex - Molly",
                packageName = "im.molly.app",
                shortcutId = "conversation:alex",
                userSerial = 0,
                isWorkProfile = false,
            ),
        )
        assertEquals(selected, recipientFromSnapshot(selected.snapshot()))
    }

    @Test fun invalidSnapshotDoesNotRestoreAChip() {
        assertNull(recipientFromSnapshot("not JSON"))
    }
}
