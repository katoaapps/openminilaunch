package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.model.launcherShortcutIdentity
import com.katoaapps.openminilaunch.model.launcherShortcutSelectionKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LauncherShortcutIdentityTest {
    @Test
    fun selectionKeyRoundTripsProfilePackageAndShortcut() {
        val key = launcherShortcutSelectionKey(
            userSerial = 12L,
            packageName = "com.example.chat",
            shortcutId = "conversation:maya:work",
        )

        val identity = launcherShortcutIdentity(key)

        assertEquals(12L, identity?.userSerial)
        assertEquals("com.example.chat", identity?.packageName)
        assertEquals("conversation:maya:work", identity?.shortcutId)
    }

    @Test
    fun malformedSelectionKeysAreRejected() {
        assertNull(launcherShortcutIdentity("launcher:12:com.example/.Main"))
        assertNull(launcherShortcutIdentity("shortcut:nope:com.example.chat:maya"))
        assertNull(launcherShortcutIdentity("shortcut:12::maya"))
        assertNull(launcherShortcutIdentity("shortcut:12:com.example.chat:"))
    }
}
