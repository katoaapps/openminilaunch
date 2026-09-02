package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.model.legacyLauncherShortcutId
import com.katoaapps.openminilaunch.model.legacyLauncherShortcutSelectionKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LegacyLauncherShortcutIdentityTest {
    @Test fun roundTripsLegacyShortcutId() {
        val id = "6d8ee53e-ff58-42fd-a14d-283461d4e11b"

        assertEquals(id, legacyLauncherShortcutId(legacyLauncherShortcutSelectionKey(id)))
    }

    @Test fun rejectsUnrelatedOrMalformedKeys() {
        assertNull(legacyLauncherShortcutId("shortcut:0:com.example:one"))
        assertNull(legacyLauncherShortcutId("legacy-shortcut:"))
        assertNull(legacyLauncherShortcutId("legacy-shortcut:one:two"))
    }
}
