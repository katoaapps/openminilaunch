package org.sableos.games

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameKeymapTest {
    private fun cmd(
        game: GameKind,
        keyCode: Int,
        char: Char? = null,
        alt: Boolean = false,
        ctrl: Boolean = false,
        repeat: Int = 0,
    ) = GameKeymap.command(game, GameKeyPress(keyCode, char, alt, ctrl, repeat))

    @Test
    fun trackpadArrowsMoveInEveryGame() {
        GameKind.entries.forEach { game ->
            assertEquals(GameCommand.Move(0, -1), cmd(game, KeyEvent.KEYCODE_DPAD_UP))
            assertEquals(GameCommand.Move(1, 0), cmd(game, KeyEvent.KEYCODE_DPAD_RIGHT, repeat = 3))
        }
    }

    @Test
    fun sudokuDigitsFromAltLayerAndLetterKeys() {
        // Alt+W produces '1' on the Q25.
        assertEquals(GameCommand.Digit(1), cmd(GameKind.Sudoku, KeyEvent.KEYCODE_W, '1', alt = true))
        assertEquals(GameCommand.Digit(9), cmd(GameKind.Sudoku, KeyEvent.KEYCODE_C, 'c'))
        assertEquals(GameCommand.Digit(5), cmd(GameKind.Sudoku, KeyEvent.KEYCODE_5, '5'))
        assertEquals(GameCommand.Erase, cmd(GameKind.Sudoku, KeyEvent.KEYCODE_DEL))
        assertEquals(GameCommand.Undo, cmd(GameKind.Sudoku, KeyEvent.KEYCODE_U, 'u'))
        assertEquals(GameCommand.NewGame, cmd(GameKind.Sudoku, KeyEvent.KEYCODE_N, 'n'))
        // Alt+letter that is not a digit types a symbol, not a command.
        assertNull(cmd(GameKind.Sudoku, KeyEvent.KEYCODE_U, '_', alt = true))
    }

    @Test
    fun minesweeperRevealAndFlag() {
        assertEquals(GameCommand.Primary, cmd(GameKind.Minesweeper, KeyEvent.KEYCODE_SPACE, ' '))
        assertEquals(GameCommand.Primary, cmd(GameKind.Minesweeper, KeyEvent.KEYCODE_ENTER))
        assertEquals(GameCommand.Flag, cmd(GameKind.Minesweeper, KeyEvent.KEYCODE_F, 'f'))
        assertNull(cmd(GameKind.Minesweeper, KeyEvent.KEYCODE_W, 'w'))
    }

    @Test
    fun heldKeysRepeatOnlyMovesAndCtrlIsLeftAlone() {
        assertNull(cmd(GameKind.Sudoku, KeyEvent.KEYCODE_W, 'w', repeat = 1))
        assertNull(cmd(GameKind.Game2048, KeyEvent.KEYCODE_DPAD_UP, ctrl = true))
    }

    @Test
    fun cursorClampsAndSlideDirections() {
        assertEquals(0 to 0, GameKeymap.moveCursor(0, 0, GameCommand.Move(-1, -1), 9))
        assertEquals(8 to 3, GameKeymap.moveCursor(8, 2, GameCommand.Move(1, 1), 9))
        assertEquals(0, GameKeymap.slideDirection(GameCommand.Move(-1, 0)))
        assertEquals(1, GameKeymap.slideDirection(GameCommand.Move(1, 0)))
        assertEquals(2, GameKeymap.slideDirection(GameCommand.Move(0, -1)))
        assertEquals(3, GameKeymap.slideDirection(GameCommand.Move(0, 1)))
    }
}
