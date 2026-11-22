package org.sableos.games

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaffoldKeysTest {
    @Test
    fun defaultsAreSpreadAcrossTheKeyboard() {
        val k = ScaffoldKeys()
        assertEquals(KeyEvent.KEYCODE_A, k.keyFor(ScaffoldAction.Left))
        assertEquals(KeyEvent.KEYCODE_K, k.keyFor(ScaffoldAction.Right))
        assertEquals(KeyEvent.KEYCODE_T, k.keyFor(ScaffoldAction.Up))
        assertEquals(KeyEvent.KEYCODE_V, k.keyFor(ScaffoldAction.Down))
        assertEquals(KeyEvent.KEYCODE_SPACE, k.keyFor(ScaffoldAction.Jump))
    }

    @Test
    fun arrowsOnlyWhereThePhoneHasThem() {
        val k = ScaffoldKeys()
        assertEquals(ScaffoldAction.Left, k.actionFor(KeyEvent.KEYCODE_DPAD_LEFT, DeviceProfile.Q25))
        assertEquals(ScaffoldAction.Jump, k.actionFor(KeyEvent.KEYCODE_DPAD_CENTER, DeviceProfile.Q25))
        assertNull(k.actionFor(KeyEvent.KEYCODE_DPAD_LEFT, DeviceProfile.Titan2))
        assertEquals(ScaffoldAction.Left, k.actionFor(KeyEvent.KEYCODE_A, DeviceProfile.Titan2))
    }

    @Test
    fun remapSwapsInsteadOfDoubling() {
        val k = ScaffoldKeys().bind(ScaffoldAction.Left, KeyEvent.KEYCODE_K)
        assertEquals(KeyEvent.KEYCODE_K, k.keyFor(ScaffoldAction.Left))
        assertEquals(KeyEvent.KEYCODE_A, k.keyFor(ScaffoldAction.Right))
        assertEquals(
            ScaffoldAction.entries.size,
            k.keys.values
                .toSet()
                .size,
        )
    }

    @Test
    fun reservedKeysCannotBeBound() {
        val k = ScaffoldKeys()
        assertEquals(k, k.bind(ScaffoldAction.Jump, KeyEvent.KEYCODE_ENTER))
        assertEquals(k, k.bind(ScaffoldAction.Jump, KeyEvent.KEYCODE_P))
    }

    @Test
    fun encodeDecodeRoundTripsAndBadTextGivesDefaults() {
        val k = ScaffoldKeys().bind(ScaffoldAction.Up, KeyEvent.KEYCODE_W)
        assertEquals(k, ScaffoldKeys.decode(k.encode()))
        assertEquals(ScaffoldKeys(), ScaffoldKeys.decode(null))
        assertEquals(ScaffoldKeys(), ScaffoldKeys.decode("1,2,3"))
        assertEquals(ScaffoldKeys(), ScaffoldKeys.decode("1,1,2,3,4"))
    }

    @Test
    fun labelsReadOnTheScreen() {
        assertEquals("A", ScaffoldKeys.label(KeyEvent.KEYCODE_A))
        assertEquals("Space", ScaffoldKeys.label(KeyEvent.KEYCODE_SPACE))
        assertTrue(ScaffoldKeys.label(KeyEvent.KEYCODE_5).isNotEmpty())
    }
}
