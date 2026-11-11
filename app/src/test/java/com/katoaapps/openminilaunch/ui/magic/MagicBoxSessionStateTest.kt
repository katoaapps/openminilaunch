package com.katoaapps.openminilaunch.ui.magic

import com.katoaapps.openminilaunch.features.magic.MagicBoxRoute
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MagicBoxSessionStateTest {
    @Test fun integratedConversationStartsLockedMagicBoxDraft() {
        val conversation = LauncherShortcutTarget(
            label = "May Wong",
            packageName = "com.example.messages",
            shortcutId = "may-conversation",
            userSerial = 0L,
            isWorkProfile = false,
        )
        val state = MagicBoxSessionState.empty()

        state.prepareConversationMessage(conversation)

        assertEquals("", state.textState.value.text)
        assertEquals(MagicBoxRoute.MESSAGE, state.routeState.value)
        assertEquals(
            SelectedMessageRecipient.ConversationRecipient(conversation),
            state.selectedRecipientState.value,
        )
        assertTrue(state.expanded)
    }
}
