package com.katoaapps.openminilaunch.features.minkboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherHomeBoardAccessPolicyTest {
    @Test
    fun `unlocked board is visible without an access grant`() {
        assertTrue(canRevealLauncherHomeBoard(isLocked = false, accessGranted = false))
    }

    @Test
    fun `locked board remains hidden without an access grant`() {
        assertFalse(canRevealLauncherHomeBoard(isLocked = true, accessGranted = false))
    }

    @Test
    fun `locked board is visible after an access grant`() {
        assertTrue(canRevealLauncherHomeBoard(isLocked = true, accessGranted = true))
    }
}
