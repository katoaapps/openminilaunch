package com.katoaapps.openminilaunch.features.minkpad.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MinkPadHostPlatformTest {
    @Test
    fun `infers platforms only from explicit host names`() {
        assertEquals(MinkPadHostPlatform.MAC, minkPadHostPlatformHint("Mink's MacBook Pro"))
        assertEquals(MinkPadHostPlatform.APPLE_MOBILE, minkPadHostPlatformHint("Mink's iPad"))
        assertEquals(MinkPadHostPlatform.WINDOWS, minkPadHostPlatformHint("Surface Laptop"))
        assertEquals(MinkPadHostPlatform.ANDROID, minkPadHostPlatformHint("Living Room Google TV"))
        assertEquals(MinkPadHostPlatform.ANDROID, minkPadHostPlatformHint("Pixel 10 Pro"))
    }

    @Test
    fun `leaves ambiguous host names for user selection`() {
        assertNull(minkPadHostPlatformHint("Office computer"))
        assertNull(minkPadHostPlatformHint("master tv"))
        assertNull(minkPadHostPlatformHint(null))
    }
}
