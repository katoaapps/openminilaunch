package org.sableos.games

import android.view.KeyEvent

/**
 * Keyboard play for the Sable games (keyboard-first, like the other Sable apps).
 *
 * The trackpad sends arrow keys, so arrows move the board cursor (Sudoku, Minesweeper) or slide the
 * tiles (2048). On a phone without arrows ([DeviceProfile.letterMoves], the Titan 2) I J K L move
 * instead, in every game but Words, where every letter types. Numbers come from the character the key
 * produces, so the Alt digit layer and a number row both work; the plain digit-layer letters (W E R S
 * D F Z X C on the Q25, W E R S D F X C V on the Titan 2) also enter 1-9 in Sudoku, where no letter
 * has another meaning. There is no Ctrl on the Q25: Ctrl chords are left alone.
 */
internal sealed interface GameCommand {
    data class Move(
        val dx: Int,
        val dy: Int,
    ) : GameCommand

    data class Digit(
        val value: Int,
    ) : GameCommand

    /** Space or Enter: reveal (Minesweeper) or nothing special. */
    data object Primary : GameCommand

    data object Flag : GameCommand

    data object Erase : GameCommand

    data object Undo : GameCommand

    data object NewGame : GameCommand

    /** A letter typed for a word game, upper case. */
    data class Letter(
        val value: Char,
    ) : GameCommand

    /** P: pause or resume a game that runs on its own (Bricks, Snake, Blocks). */
    data object Pause : GameCommand

    /** H: show a move that works (Mahjong). */
    data object Hint : GameCommand

    /** Space in Solitaire: turn the next card from the stock. */
    data object Draw : GameCommand

    /** A in Solitaire: send the selected card to its foundation. */
    data object Auto : GameCommand

    /** Space in the Indian card and board games: select, or roll. */
    data object Select : GameCommand
}

/** A key-down reduced to what the keymap needs, so the mapping is testable on the JVM. */
internal data class GameKeyPress(
    val keyCode: Int,
    /** The character the key produces with the current meta state, or null. */
    val char: Char? = null,
    val alt: Boolean = false,
    val ctrl: Boolean = false,
    val repeatCount: Int = 0,
)

internal enum class GameKind { Sudoku, Minesweeper, Game2048, Bricks, Snake, Blocks, Reversi, Solitaire, Mahjong, Words, Indian }

internal object GameKeymap {
    fun command(
        game: GameKind,
        press: GameKeyPress,
        profile: DeviceProfile = DeviceProfile.Other,
        bound: BoundKeys? = null,
    ): GameCommand? {
        if (press.ctrl) return null
        move(press.keyCode)?.let { return it }
        if (bound != null && !press.alt) {
            // The player's keys come first. A default key they moved to another action stops working.
            bound.bindings.commandFor(press.keyCode, bound.controls)?.let {
                return if (press.repeatCount > 0 && it !is GameCommand.Move) null else it
            }
            if (press.keyCode in bound.controls.defaultKeys(profile)) return null
        }
        if (profile.letterMoves && game != GameKind.Words && !press.alt) {
            DeviceProfile.LETTER_MOVES[press.keyCode]?.let { return it }
        }
        // A held key repeats only cursor moves.
        if (press.repeatCount > 0) return null
        return when (game) {
            GameKind.Sudoku -> sudoku(press, profile.letterDigits)
            GameKind.Minesweeper -> minesweeper(press)
            GameKind.Game2048 -> common(press)
            GameKind.Bricks, GameKind.Snake, GameKind.Blocks -> arcade(press)
            GameKind.Reversi -> board(press)
            GameKind.Solitaire -> solitaire(press)
            GameKind.Mahjong -> mahjong(press)
            GameKind.Words -> words(press)
            GameKind.Indian -> indian(press)
        }
    }

    /**
     * The Indian card and board games. Arrows or the trackpad move. Enter is Primary, Space is Select, Del
     * or Escape cancels, and every letter is a [GameCommand.Letter] the screen may use. A digit key (the
     * Alt layer on the Q25 included) gives [GameCommand.Digit].
     */
    private fun indian(press: GameKeyPress): GameCommand? {
        if (press.alt && press.char?.isDigit() != true) return null
        val digit = press.char?.takeIf { it in '0'..'9' }
        return when {
            digit != null -> {
                GameCommand.Digit(digit.digitToInt())
            }

            press.alt -> {
                null
            }

            press.keyCode == KeyEvent.KEYCODE_ENTER || press.keyCode == KeyEvent.KEYCODE_DPAD_CENTER -> {
                GameCommand.Primary
            }

            press.keyCode == KeyEvent.KEYCODE_SPACE -> {
                GameCommand.Select
            }

            press.keyCode == KeyEvent.KEYCODE_DEL || press.keyCode == KeyEvent.KEYCODE_ESCAPE -> {
                GameCommand.Erase
            }

            else -> {
                press.char
                    ?.takeIf {
                        it.isLetter() && it.lowercaseChar() in 'a'..'z'
                    }?.let { GameCommand.Letter(it.uppercaseChar()) }
            }
        }
    }

    private fun move(keyCode: Int): GameCommand.Move? =
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> GameCommand.Move(0, -1)
            KeyEvent.KEYCODE_DPAD_DOWN -> GameCommand.Move(0, 1)
            KeyEvent.KEYCODE_DPAD_LEFT -> GameCommand.Move(-1, 0)
            KeyEvent.KEYCODE_DPAD_RIGHT -> GameCommand.Move(1, 0)
            else -> null
        }

    private fun sudoku(
        press: GameKeyPress,
        letterDigits: Map<Int, Int>,
    ): GameCommand? {
        val digit = press.char?.takeIf { it in '1'..'9' }?.digitToInt()
        return when {
            digit != null -> GameCommand.Digit(digit)

            press.char == '0' -> GameCommand.Erase

            press.keyCode == KeyEvent.KEYCODE_DEL || press.keyCode == KeyEvent.KEYCODE_FORWARD_DEL -> GameCommand.Erase

            // Alt plus a letter types a symbol; only the digits above are taken from it.
            press.alt -> null

            press.keyCode in letterDigits -> GameCommand.Digit(letterDigits.getValue(press.keyCode))

            press.keyCode == KeyEvent.KEYCODE_U -> GameCommand.Undo

            else -> common(press)
        }
    }

    private fun minesweeper(press: GameKeyPress): GameCommand? =
        when {
            press.alt -> null

            press.keyCode == KeyEvent.KEYCODE_SPACE ||
                press.keyCode == KeyEvent.KEYCODE_ENTER ||
                press.keyCode == KeyEvent.KEYCODE_DPAD_CENTER -> GameCommand.Primary

            press.keyCode == KeyEvent.KEYCODE_F || press.keyCode == KeyEvent.KEYCODE_G -> GameCommand.Flag

            else -> common(press)
        }

    private fun isPrimary(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_ENTER ||
            keyCode == KeyEvent.KEYCODE_DPAD_CENTER

    /** Bricks, Snake and Blocks: Space or Enter launches, drops or restarts; P pauses. */
    private fun arcade(press: GameKeyPress): GameCommand? =
        when {
            press.alt -> null
            isPrimary(press.keyCode) -> GameCommand.Primary
            press.keyCode == KeyEvent.KEYCODE_P -> GameCommand.Pause
            else -> common(press)
        }

    private fun board(press: GameKeyPress): GameCommand? =
        when {
            press.alt -> null
            isPrimary(press.keyCode) -> GameCommand.Primary
            else -> common(press)
        }

    /** Solitaire: Enter picks up and puts down, Space turns the stock, A sends to a foundation, Del drops the pick. */
    private fun solitaire(press: GameKeyPress): GameCommand? =
        when {
            press.alt -> null
            press.keyCode == KeyEvent.KEYCODE_SPACE -> GameCommand.Draw
            press.keyCode == KeyEvent.KEYCODE_ENTER || press.keyCode == KeyEvent.KEYCODE_DPAD_CENTER -> GameCommand.Primary
            press.keyCode == KeyEvent.KEYCODE_A -> GameCommand.Auto
            press.keyCode == KeyEvent.KEYCODE_DEL || press.keyCode == KeyEvent.KEYCODE_ESCAPE -> GameCommand.Erase
            else -> common(press)
        }

    private fun mahjong(press: GameKeyPress): GameCommand? =
        when {
            press.alt -> null
            isPrimary(press.keyCode) -> GameCommand.Primary
            press.keyCode == KeyEvent.KEYCODE_H -> GameCommand.Hint
            else -> common(press)
        }

    /** Words: every letter types, so N is a letter here; Enter guesses (or starts over when the round is done). */
    private fun words(press: GameKeyPress): GameCommand? {
        val letter = press.char?.takeIf { !press.alt && it.isLetter() && it.lowercaseChar() in 'a'..'z' }
        return when {
            letter != null -> GameCommand.Letter(letter.uppercaseChar())
            press.keyCode == KeyEvent.KEYCODE_DEL -> GameCommand.Erase
            press.keyCode == KeyEvent.KEYCODE_ENTER || press.keyCode == KeyEvent.KEYCODE_DPAD_CENTER -> GameCommand.Primary
            else -> null
        }
    }

    private fun common(press: GameKeyPress): GameCommand? =
        when {
            press.alt -> null
            press.keyCode == KeyEvent.KEYCODE_N -> GameCommand.NewGame
            else -> null
        }

    /** The cursor after [move] on a [size] x [size] board, clamped at the edges. */
    fun moveCursor(
        row: Int,
        col: Int,
        move: GameCommand.Move,
        size: Int,
    ): Pair<Int, Int> = (row + move.dy).coerceIn(0, size - 1) to (col + move.dx).coerceIn(0, size - 1)

    /** 2048 slide direction for the native bridge: 0 left, 1 right, 2 up, 3 down. */
    fun slideDirection(move: GameCommand.Move): Int =
        when {
            move.dx < 0 -> 0
            move.dx > 0 -> 1
            move.dy < 0 -> 2
            else -> 3
        }
}

/** A game's rebindable actions and the keys the player gave them. */
internal data class BoundKeys(
    val controls: GameControls,
    val bindings: KeyBindings,
)

/** Set by the current game screen; [MainActivity] offers it every key-down first. */
internal class GameKeyHost {
    var handler: ((GameKeyPress) -> Boolean)? = null

    /** Offered every key-up, for games that need to know when a held key lets go. */
    var upHandler: ((GameKeyPress) -> Unit)? = null

    /**
     * Set by the game shell ([GameShell]); offered every key-down before [handler]. It answers true
     * (taken), false (left to the system) or null (passed on to the game).
     */
    var gate: ((GameKeyPress) -> Boolean?)? = null

    /** The game's own handling of a command, for the shell's New tab. */
    var commandHandler: ((GameCommand) -> Boolean)? = null
}

internal fun KeyEvent.toGameKeyPress(): GameKeyPress {
    val produced = unicodeChar
    return GameKeyPress(
        keyCode = keyCode,
        char = if (produced > 0) produced.toChar() else null,
        alt = isAltPressed,
        ctrl = isCtrlPressed,
        repeatCount = repeatCount,
    )
}
