package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.model.LauncherAppKeyParts
import com.katoaapps.openminilaunch.model.launcherAppKeyParts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LauncherAppIdentityTest {
    @Test
    fun profileSerialAndComponentRemainDistinct() {
        assertEquals(
            LauncherAppKeyParts(10L, "com.example.mail/.MainActivity"),
            launcherAppKeyParts("launcher:10:com.example.mail/.MainActivity"),
        )
        assertEquals(
            LauncherAppKeyParts(11L, "com.example.mail/.MainActivity"),
            launcherAppKeyParts("launcher:11:com.example.mail/.MainActivity"),
        )
    }

    @Test
    fun legacyPackageAndMalformedKeysAreNotParsedAsProfileTargets() {
        assertNull(launcherAppKeyParts("com.example.mail"))
        assertNull(launcherAppKeyParts("launcher:not-a-number:com.example.mail/.MainActivity"))
        assertNull(launcherAppKeyParts("launcher:-1:com.example.mail/.MainActivity"))
        assertNull(launcherAppKeyParts("launcher:10:"))
    }
}
