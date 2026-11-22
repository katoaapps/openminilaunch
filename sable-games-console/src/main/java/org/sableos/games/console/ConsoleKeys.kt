package org.sableos.games.console

import android.view.KeyEvent

/** What a key does in the console, worked out without Android so it can be tested. */
sealed interface ConsoleKey {
    /** Arrows, the trackpad and (in tilt games) W A S D: a step, like a tilt. */
    data class Step(
        val step: TiltStep,
    ) : ConsoleKey

    /** Back, or P in a tilt game. */
    data object Pause : ConsoleKey

    /** Space, Enter, the trackpad click or a letter, passed to the game's `key()` by name. */
    data class Named(
        val name: String,
    ) : ConsoleKey
}

object ConsoleKeys {
    private val ARROWS =
        mapOf(
            KeyEvent.KEYCODE_DPAD_LEFT to TiltStep.Left,
            KeyEvent.KEYCODE_DPAD_RIGHT to TiltStep.Right,
            KeyEvent.KEYCODE_DPAD_UP to TiltStep.Up,
            KeyEvent.KEYCODE_DPAD_DOWN to TiltStep.Down,
        )
    private val WASD =
        mapOf(
            KeyEvent.KEYCODE_A to TiltStep.Left,
            KeyEvent.KEYCODE_D to TiltStep.Right,
            KeyEvent.KEYCODE_W to TiltStep.Up,
            KeyEvent.KEYCODE_S to TiltStep.Down,
        )

    /**
     * Whether a key event goes straight to the showing screen's key handler. Back does not: it reaches
     * the screen through the back dispatcher, once a press.
     */
    fun toScreen(keyCode: Int): Boolean = keyCode != KeyEvent.KEYCODE_BACK

    /** The key's meaning while a game runs; [tiltGame] makes W A S D and P controls, not letters. */
    fun inGame(
        keyCode: Int,
        char: Char?,
        tiltGame: Boolean,
    ): ConsoleKey? {
        ARROWS[keyCode]?.let { return ConsoleKey.Step(it) }
        if (tiltGame) WASD[keyCode]?.let { return ConsoleKey.Step(it) }
        return when {
            keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE -> ConsoleKey.Pause
            tiltGame && keyCode == KeyEvent.KEYCODE_P -> ConsoleKey.Pause
            keyCode == KeyEvent.KEYCODE_SPACE -> ConsoleKey.Named("space")
            keyCode in ENTER_KEYS -> ConsoleKey.Named("enter")
            keyCode == KeyEvent.KEYCODE_DEL -> ConsoleKey.Named("delete")
            char != null && char.isLetterOrDigit() -> ConsoleKey.Named(char.lowercaseChar().toString())
            else -> null
        }
    }

    /** The lean a held arrow (or A/D, W/S in a tilt game) gives, for games that steer. */
    fun heldLean(
        held: Set<Int>,
        tiltGame: Boolean,
    ): Pair<Double, Double>? {
        var x = 0.0
        var y = 0.0
        var any = false
        for (code in held) {
            val step = ARROWS[code] ?: (if (tiltGame) WASD[code] else null) ?: continue
            any = true
            when (step) {
                TiltStep.Left -> x -= 1.0
                TiltStep.Right -> x += 1.0
                TiltStep.Up -> y -= 1.0
                TiltStep.Down -> y += 1.0
            }
        }
        return if (any) x.coerceIn(-1.0, 1.0) to y.coerceIn(-1.0, 1.0) else null
    }

    val ENTER_KEYS = setOf(KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_DPAD_CENTER)
}
