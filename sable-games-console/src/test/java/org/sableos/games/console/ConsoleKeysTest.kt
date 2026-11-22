package org.sableos.games.console

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsoleKeysTest {
    @Test
    fun arrowsAndTheTrackpadAreSteps() {
        assertEquals(
            ConsoleKey.Step(TiltStep.Left),
            ConsoleKeys.inGame(KeyEvent.KEYCODE_DPAD_LEFT, null, tiltGame = false),
        )
        assertEquals(
            ConsoleKey.Step(TiltStep.Down),
            ConsoleKeys.inGame(KeyEvent.KEYCODE_DPAD_DOWN, null, tiltGame = true),
        )
    }

    @Test
    fun wasdAndPAreControlsOnlyInTiltGames() {
        assertEquals(ConsoleKey.Step(TiltStep.Up), ConsoleKeys.inGame(KeyEvent.KEYCODE_W, 'w', tiltGame = true))
        assertEquals(ConsoleKey.Named("w"), ConsoleKeys.inGame(KeyEvent.KEYCODE_W, 'w', tiltGame = false))
        assertEquals(ConsoleKey.Pause, ConsoleKeys.inGame(KeyEvent.KEYCODE_P, 'p', tiltGame = true))
        assertEquals(ConsoleKey.Named("p"), ConsoleKeys.inGame(KeyEvent.KEYCODE_P, 'p', tiltGame = false))
        assertEquals(ConsoleKey.Pause, ConsoleKeys.inGame(KeyEvent.KEYCODE_BACK, null, tiltGame = false))
    }

    @Test
    fun spaceEnterAndLettersGoToTheGame() {
        assertEquals(ConsoleKey.Named("space"), ConsoleKeys.inGame(KeyEvent.KEYCODE_SPACE, ' ', tiltGame = true))
        assertEquals(ConsoleKey.Named("enter"), ConsoleKeys.inGame(KeyEvent.KEYCODE_DPAD_CENTER, null, tiltGame = true))
        assertEquals(ConsoleKey.Named("u"), ConsoleKeys.inGame(KeyEvent.KEYCODE_U, 'U', tiltGame = true))
        assertNull(ConsoleKeys.inGame(KeyEvent.KEYCODE_VOLUME_UP, null, tiltGame = true))
    }

    @Test
    fun heldKeysSteer() {
        assertEquals(-1.0 to 0.0, ConsoleKeys.heldLean(setOf(KeyEvent.KEYCODE_DPAD_LEFT), tiltGame = false))
        assertEquals(1.0 to 0.0, ConsoleKeys.heldLean(setOf(KeyEvent.KEYCODE_D), tiltGame = true))
        assertNull(ConsoleKeys.heldLean(setOf(KeyEvent.KEYCODE_D), tiltGame = false))
        assertNull(ConsoleKeys.heldLean(emptySet(), tiltGame = true))
    }

    @Test
    fun backReachesTheScreenOnlyThroughTheBackDispatcher() {
        // The Titan 2's Back key went back twice: once as a key, once from the back dispatcher.
        assertFalse(ConsoleKeys.toScreen(KeyEvent.KEYCODE_BACK))
        assertTrue(ConsoleKeys.toScreen(KeyEvent.KEYCODE_ESCAPE))
        assertTrue(ConsoleKeys.toScreen(KeyEvent.KEYCODE_SPACE))
    }
}
