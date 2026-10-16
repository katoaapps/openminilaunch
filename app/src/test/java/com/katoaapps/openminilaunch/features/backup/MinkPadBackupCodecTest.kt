package com.katoaapps.openminilaunch.features.backup

import com.katoaapps.openminilaunch.features.minkpad.MinkPadPreferences
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MinkPadBackupCodecTest {
    @Test
    fun `round trips advanced host gesture preference`() {
        val restored = MinkPadBackupCodec.decode(
            MinkPadBackupCodec.encode(MinkPadPreferences(advancedHostGestures = false)),
        )

        assertFalse(restored.advancedHostGestures)
    }

    @Test
    fun `older backups keep advanced host gestures enabled`() {
        val restored = MinkPadBackupCodec.decode(JSONObject())

        assertTrue(restored.advancedHostGestures)
    }
}
