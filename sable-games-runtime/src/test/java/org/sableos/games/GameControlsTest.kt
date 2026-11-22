package org.sableos.games

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rebindable keys: per-phone defaults, swaps, saving, and how they win over the built-in keymap. */
class GameControlsTest {
    private val snake = GameCatalog.snake.controls.first()

    private fun press(
        keyCode: Int,
        char: Char? = null,
    ) = GameKeyPress(keyCode, char)

    @Test
    fun defaultsFollowThePhone() {
        val q25 = snake.defaults(DeviceProfile.Q25)
        assertEquals(listOf(KeyEvent.KEYCODE_W), q25.keysFor("up"))
        val titan = snake.defaults(DeviceProfile.Titan2)
        assertEquals(listOf(KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_W), titan.keysFor("up"))
        assertEquals(snake.defaults(DeviceProfile.Titan2), snake.defaults(DeviceProfile.P26))
    }

    @Test
    fun aBoundKeySwapsWithTheActionThatHadIt() {
        val start = snake.defaults(DeviceProfile.Q25)
        val moved = start.bind("pause", KeyEvent.KEYCODE_N)
        assertEquals(listOf(KeyEvent.KEYCODE_N), moved.keysFor("pause"))
        assertEquals(listOf(KeyEvent.KEYCODE_P), moved.keysFor("new"))
    }

    @Test
    fun reservedKeysCantBeBound() {
        val start = snake.defaults(DeviceProfile.Q25)
        assertEquals(start, start.bind("up", KeyEvent.KEYCODE_ENTER))
        assertEquals(start, start.bind("up", KeyEvent.KEYCODE_DPAD_UP))
        assertEquals(start, start.bind("up", KeyEvent.KEYCODE_BACK))
        assertEquals(start, start.bind("up", KeyEvent.KEYCODE_VOLUME_UP))
    }

    @Test
    fun savedKeysReadBackAndBadFilesFallBack() {
        val defaults = snake.defaults(DeviceProfile.Q25)
        val bound = defaults.bind("up", KeyEvent.KEYCODE_T)
        assertEquals(bound, KeyBindings.decode(bound.encode(), defaults))
        assertEquals(defaults, KeyBindings.decode(null, defaults))
        assertEquals(defaults, KeyBindings.decode("garbage", defaults))
        // One key on two actions is not trusted.
        assertEquals(defaults, KeyBindings.decode("up=51|down=51", defaults))
        val store = MemoryControlsStore()
        store.save(snake, bound)
        assertEquals(bound, store.load(snake, DeviceProfile.Q25))
    }

    @Test
    fun theKeymapUsesTheBoundKeysFirst() {
        val bound = BoundKeys(snake, snake.defaults(DeviceProfile.Q25).bind("up", KeyEvent.KEYCODE_T))
        val cmd = { code: Int -> GameKeymap.command(GameKind.Snake, press(code), DeviceProfile.Q25, bound) }
        assertEquals(GameCommand.Move(0, -1), cmd(KeyEvent.KEYCODE_T))
        // W was up's default; moved away, it does nothing.
        assertNull(cmd(KeyEvent.KEYCODE_W))
        // The arrows always work.
        assertEquals(GameCommand.Move(0, -1), cmd(KeyEvent.KEYCODE_DPAD_UP))
        assertEquals(GameCommand.Pause, cmd(KeyEvent.KEYCODE_P))
    }

    @Test
    fun theTitanAndP26MoveWithLettersInEveryGameButWords() {
        listOf(DeviceProfile.Titan2, DeviceProfile.P26).forEach { profile ->
            val sudoku = GameCatalog.sudoku.controls.first()
            val bound = BoundKeys(sudoku, sudoku.defaults(profile))
            assertEquals(
                GameCommand.Move(-1, 0),
                GameKeymap.command(GameKind.Sudoku, press(KeyEvent.KEYCODE_J), profile, bound),
            )
        }
    }

    @Test
    fun teenPattiLookFitsEachKeyboard() {
        val tp = CardRules.teenPatti
        assertEquals(listOf(KeyEvent.KEYCODE_L), tp.defaults(DeviceProfile.Q25).keysFor("look"))
        assertEquals(listOf(KeyEvent.KEYCODE_S), tp.defaults(DeviceProfile.P26).keysFor("look"))
        val bound = BoundKeys(tp, tp.defaults(DeviceProfile.Q25).bind("pack", KeyEvent.KEYCODE_Q))
        assertEquals(
            GameCommand.Letter('P'),
            GameKeymap.command(GameKind.Indian, GameKeyPress(KeyEvent.KEYCODE_Q, 'q'), DeviceProfile.Q25, bound),
        )
        assertNull(GameKeymap.command(GameKind.Indian, GameKeyPress(KeyEvent.KEYCODE_P, 'p'), DeviceProfile.Q25, bound))
    }

    @Test
    fun everyGameHasHowToPlayAndNoKeyTwice() {
        (GameCatalog.all + GameCatalog.scaffold).forEach { spec ->
            assertTrue(spec.id, spec.howTo.isNotEmpty() && spec.howTo.all { it.rules.isNotEmpty() })
            spec.controls.forEach { c ->
                DeviceProfile.entries.forEach { profile ->
                    val keys = c.actions.flatMap { it.defaults(profile) }
                    assertEquals("${c.id} on $profile", keys.size, keys.toSet().size)
                    assertTrue(c.id, keys.none { it in KeyBindings.RESERVED })
                }
            }
        }
        assertEquals(GameCatalog.cards.howTo.size, GameCatalog.cards.controls.size)
    }

    @Test
    fun menusMoveWithArrowsOrLetters() {
        assertEquals(ShellMenuKey.Down, ShellKeys.menu(press(KeyEvent.KEYCODE_DPAD_DOWN), DeviceProfile.Q25))
        assertNull(ShellKeys.menu(press(KeyEvent.KEYCODE_K), DeviceProfile.Q25))
        assertEquals(ShellMenuKey.Down, ShellKeys.menu(press(KeyEvent.KEYCODE_K), DeviceProfile.P26))
        assertEquals(ShellMenuKey.Back, ShellKeys.menu(press(KeyEvent.KEYCODE_BACK), DeviceProfile.P26))
        assertEquals(ShellMenuKey.Confirm, ShellKeys.menu(press(KeyEvent.KEYCODE_ENTER), DeviceProfile.Q25))
    }

    @Test
    fun theKeyTableShowsThePlayersKeysAndTheFixedOnes() {
        val table = keyTable(snake, snake.defaults(DeviceProfile.Titan2), DeviceProfile.Titan2).toMap()
        assertEquals("I W", table["Up"])
        assertEquals("P", table["Pause"])
        assertEquals("Back", table["Menu"])
    }
}
