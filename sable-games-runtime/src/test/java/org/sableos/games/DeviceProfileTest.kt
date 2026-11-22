package org.sableos.games

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceProfileTest {
    private fun detect(
        manufacturer: String = "",
        model: String = "",
        device: String = "",
        hasQwerty: Boolean = false,
        hasArrows: Boolean = false,
    ) = DeviceProfile.detect(manufacturer, model, device, hasQwerty, hasArrows)

    private fun cmd(
        game: GameKind,
        keyCode: Int,
        char: Char? = null,
        profile: DeviceProfile,
        alt: Boolean = false,
        repeat: Int = 0,
    ) = GameKeymap.command(game, GameKeyPress(keyCode, char, alt, false, repeat), profile)

    @Test
    fun knownPhonesByName() {
        assertEquals(DeviceProfile.Titan2, detect("Unihertz", "Titan 2", hasQwerty = true))
        assertEquals(DeviceProfile.Titan2, detect("Unihertz", "TITAN2"))
        assertEquals(DeviceProfile.Titan2, detect("Unihertz", "Titan_2"))
        assertEquals(DeviceProfile.Titan2, detect(device = "titan2", hasQwerty = true, hasArrows = true))
        assertEquals(DeviceProfile.Q25, detect("Zinwa", "Q25", hasQwerty = true, hasArrows = true))
        assertEquals(DeviceProfile.Q25, detect("ZINWA", "zinwa_q25"))
        assertEquals(DeviceProfile.P26, detect("Zinwa", "P26", hasQwerty = true))
        assertEquals(DeviceProfile.P26, detect("ZINWA", "Passport 2026"))
        assertEquals(DeviceProfile.P26, detect(device = "p26"))
        // A model number that only contains the letters is not the P26.
        assertEquals(DeviceProfile.Other, detect("Samsung", "SM-P260"))
    }

    @Test
    fun otherPhonesByTheirKeyboard() {
        assertEquals(DeviceProfile.KeyboardNoArrows, detect("Unihertz", "Titan Slim", hasQwerty = true))
        assertEquals(DeviceProfile.Other, detect("BlackBerry", "BBF100-1", hasQwerty = true, hasArrows = true))
        assertEquals(DeviceProfile.Other, detect("Google", "Pixel 9"))
        // The first Titan is not a Titan 2.
        assertEquals(DeviceProfile.Other, detect("Unihertz", "Titan"))
    }

    @Test
    fun titan2LettersMoveTheCursor() {
        val t = DeviceProfile.Titan2
        assertEquals(GameCommand.Move(0, -1), cmd(GameKind.Sudoku, KeyEvent.KEYCODE_I, 'i', t))
        assertEquals(GameCommand.Move(-1, 0), cmd(GameKind.Minesweeper, KeyEvent.KEYCODE_J, 'j', t))
        assertEquals(GameCommand.Move(0, 1), cmd(GameKind.Reversi, KeyEvent.KEYCODE_K, 'k', t))
        assertEquals(GameCommand.Move(1, 0), cmd(GameKind.Indian, KeyEvent.KEYCODE_L, 'l', t))
        // A held letter repeats the move, like a held arrow.
        assertEquals(GameCommand.Move(1, 0), cmd(GameKind.Game2048, KeyEvent.KEYCODE_L, 'l', t, repeat = 2))
        // Words types every letter.
        assertEquals(GameCommand.Letter('I'), cmd(GameKind.Words, KeyEvent.KEYCODE_I, 'i', t))
        // Alt+letter is a symbol or digit, not a move.
        assertNull(cmd(GameKind.Reversi, KeyEvent.KEYCODE_K, '\'', t, alt = true))
    }

    @Test
    fun q25LettersStayLetters() {
        assertEquals(GameCommand.Letter('L'), cmd(GameKind.Indian, KeyEvent.KEYCODE_L, 'l', DeviceProfile.Q25))
        assertNull(cmd(GameKind.Sudoku, KeyEvent.KEYCODE_I, 'i', DeviceProfile.Q25))
    }

    @Test
    fun sudokuDigitLettersFollowTheKeyboard() {
        assertEquals(GameCommand.Digit(7), cmd(GameKind.Sudoku, KeyEvent.KEYCODE_Z, 'z', DeviceProfile.Q25))
        assertEquals(GameCommand.Digit(9), cmd(GameKind.Sudoku, KeyEvent.KEYCODE_C, 'c', DeviceProfile.Q25))
        assertEquals(GameCommand.Digit(7), cmd(GameKind.Sudoku, KeyEvent.KEYCODE_X, 'x', DeviceProfile.Titan2))
        assertEquals(GameCommand.Digit(9), cmd(GameKind.Sudoku, KeyEvent.KEYCODE_V, 'v', DeviceProfile.Titan2))
        assertNull(cmd(GameKind.Sudoku, KeyEvent.KEYCODE_Z, 'z', DeviceProfile.Titan2))
        // Alt+Q is 0 on the Titan 2: it erases, as 0 does.
        val t = DeviceProfile.Titan2
        assertEquals(GameCommand.Erase, cmd(GameKind.Sudoku, KeyEvent.KEYCODE_Q, '0', t, alt = true))
        assertEquals(GameCommand.Digit(8), cmd(GameKind.Sudoku, KeyEvent.KEYCODE_C, '8', t, alt = true))
    }
}
