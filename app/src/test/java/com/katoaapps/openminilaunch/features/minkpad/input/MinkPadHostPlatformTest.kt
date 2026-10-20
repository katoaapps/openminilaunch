package com.katoaapps.openminilaunch.features.minkpad.input

import android.bluetooth.BluetoothClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun `recognizes TV hosts without treating every audio video device as a remote`() {
        assertTrue(minkPadRemoteControlsHint("Living Room Google TV", null))
        assertTrue(minkPadRemoteControlsHint("master tv", null))
        assertTrue(minkPadRemoteControlsHint(null, BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX))
        assertFalse(minkPadRemoteControlsHint("Living Room speaker", BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER))
        assertFalse(minkPadRemoteControlsHint("Activity tracker", null))
    }

    @Test
    fun `uses Android HID consumer back for remote controls`() {
        assertEquals(0x224, MinkPadConsumerAction.BACK.usage)
    }

    @Test
    fun `uses standard HID consumer usages for channel controls`() {
        assertEquals(0x9C, MinkPadConsumerAction.CHANNEL_UP.usage)
        assertEquals(0x9D, MinkPadConsumerAction.CHANNEL_DOWN.usage)
    }

    @Test
    fun `maps Mac vertical overview gestures to Mission Control and App Expose`() {
        val swipeUp = MinkPadHostPlatform.MAC.shortcut(MinkPadGestureAction.OVERVIEW)
        val swipeDown = MinkPadHostPlatform.MAC.shortcut(MinkPadGestureAction.DISMISS_OVERVIEW)

        assertEquals(82, swipeUp?.usage)
        assertEquals(1, swipeUp?.modifiers)
        assertEquals(81, swipeDown?.usage)
        assertEquals(1, swipeDown?.modifiers)
    }

    @Test
    fun `maps Mac horizontal swipes to adjacent Spaces`() {
        val swipeLeft = MinkPadHostPlatform.MAC.shortcut(MinkPadGestureAction.NEXT_APP)
        val swipeRight = MinkPadHostPlatform.MAC.shortcut(MinkPadGestureAction.PREVIOUS_APP)

        assertEquals(79, swipeLeft?.usage)
        assertEquals(1, swipeLeft?.modifiers)
        assertEquals(80, swipeRight?.usage)
        assertEquals(1, swipeRight?.modifiers)
    }
}
