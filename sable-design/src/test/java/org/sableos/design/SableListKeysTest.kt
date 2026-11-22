package org.sableos.design

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** BlackBerry single-key list movement shared by Sable lists. */
class SableListKeysTest {
    private fun cmd(
        code: Int,
        shift: Boolean = false,
        chord: Boolean = false,
        owned: Set<SableListMove> = emptySet(),
    ) = SableListKeys.command(code, shift, chord, owned)

    @Test
    fun blackBerryKeys() {
        assertEquals(SableListMove.Top, cmd(KeyEvent.KEYCODE_T))
        assertEquals(SableListMove.Bottom, cmd(KeyEvent.KEYCODE_B))
        assertEquals(SableListMove.PageDown, cmd(KeyEvent.KEYCODE_SPACE))
        assertEquals(SableListMove.PageUp, cmd(KeyEvent.KEYCODE_SPACE, shift = true))
        assertEquals(SableListMove.Next, cmd(KeyEvent.KEYCODE_N))
        assertEquals(SableListMove.Previous, cmd(KeyEvent.KEYCODE_P))
    }

    @Test
    fun chordsShiftedLettersAndOwnedKeysAreLeftAlone() {
        assertNull("Alt+T types (", cmd(KeyEvent.KEYCODE_T, chord = true))
        assertNull(cmd(KeyEvent.KEYCODE_SPACE, chord = true))
        assertNull(cmd(KeyEvent.KEYCODE_B, shift = true))
        assertNull(cmd(KeyEvent.KEYCODE_A))
        assertNull(cmd(KeyEvent.KEYCODE_N, owned = setOf(SableListMove.Next)))
        assertEquals(SableListMove.Top, cmd(KeyEvent.KEYCODE_T, owned = setOf(SableListMove.Next)))
    }

    @Test
    fun targetsStayInsideTheList() {
        fun t(
            move: SableListMove,
            focused: Int,
        ) = SableListKeys.target(move, focused, firstVisible = 10, lastVisible = 16, itemCount = 40)
        assertEquals(0, t(SableListMove.Top, 12))
        assertEquals(39, t(SableListMove.Bottom, 12))
        assertEquals(13, t(SableListMove.Next, 12))
        assertEquals(11, t(SableListMove.Previous, 12))
        assertEquals(18, t(SableListMove.PageDown, 12))
        assertEquals(6, t(SableListMove.PageUp, 12))
        assertEquals("no focus: start at the first visible row", 10, t(SableListMove.Next, -1))
        assertEquals(39, SableListKeys.target(SableListMove.PageDown, 38, 30, 39, 40))
        assertEquals(0, SableListKeys.target(SableListMove.Previous, 0, 0, 5, 40))
        assertNull(SableListKeys.target(SableListMove.Top, -1, 0, 0, 0))
    }

    @Test
    fun scrollingScreensGoWhereTheKeysSay() {
        val d = { move: SableListMove, v: Int -> SableScrollKeys.destination(move, v, max = 1000, viewport = 400) }
        assertEquals(0, d(SableListMove.Top, 500))
        assertEquals(1000, d(SableListMove.Bottom, 500))
        assertEquals(860, d(SableListMove.PageDown, 500))
        assertEquals(140, d(SableListMove.PageUp, 500))
        assertEquals(1000, d(SableListMove.PageDown, 900))
        assertNull("already at the top", d(SableListMove.Top, 0))
        assertNull("N and P are not scroll moves", d(SableListMove.Next, 10))
    }
}
