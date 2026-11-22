package org.sableos.games

import android.content.Context
import android.view.KeyEvent

/** The five things Pip does. */
internal enum class ScaffoldAction { Left, Right, Up, Down, Jump }

/**
 * The keys for [ScaffoldAction]s. The defaults are A (left), K (right), T (up), V (down) and Space (jump):
 * spread across the keyboard so they are not under one thumb, and the same on every keyboard phone.
 * Players can remap them. Where the phone has arrow keys or a trackpad that sends them (not the
 * Titan 2, see [DeviceProfile.letterMoves]) the arrows and the trackpad press work as well, with the
 * trackpad click as jump.
 */
internal data class ScaffoldKeys(
    val keys: Map<ScaffoldAction, Int> = DEFAULTS,
) {
    init {
        require(keys.keys.containsAll(ScaffoldAction.entries)) { "every action needs a key" }
    }

    fun keyFor(action: ScaffoldAction): Int = keys.getValue(action)

    /** The action a key-down means, or null. Remapped keys come first; the arrows only where the phone has them. */
    fun actionFor(
        keyCode: Int,
        profile: DeviceProfile,
    ): ScaffoldAction? {
        keys.entries.firstOrNull { it.value == keyCode }?.let { return it.key }
        if (profile.letterMoves) return null
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> ScaffoldAction.Left
            KeyEvent.KEYCODE_DPAD_RIGHT -> ScaffoldAction.Right
            KeyEvent.KEYCODE_DPAD_UP -> ScaffoldAction.Up
            KeyEvent.KEYCODE_DPAD_DOWN -> ScaffoldAction.Down
            KeyEvent.KEYCODE_DPAD_CENTER -> ScaffoldAction.Jump
            else -> null
        }
    }

    /** Gives [action] the key [keyCode]; the action that had that key takes this action's old key, so no key is used twice. */
    fun bind(
        action: ScaffoldAction,
        keyCode: Int,
    ): ScaffoldKeys {
        if (keyCode in RESERVED) return this
        val old = keys.getValue(action)
        val other = keys.entries.firstOrNull { it.value == keyCode && it.key != action }?.key
        val next = keys.toMutableMap()
        next[action] = keyCode
        if (other != null) next[other] = old
        return ScaffoldKeys(next)
    }

    fun encode(): String = ScaffoldAction.entries.joinToString(",") { keys.getValue(it).toString() }

    companion object {
        val DEFAULTS: Map<ScaffoldAction, Int> =
            mapOf(
                ScaffoldAction.Left to KeyEvent.KEYCODE_A,
                ScaffoldAction.Right to KeyEvent.KEYCODE_K,
                ScaffoldAction.Up to KeyEvent.KEYCODE_T,
                ScaffoldAction.Down to KeyEvent.KEYCODE_V,
                ScaffoldAction.Jump to KeyEvent.KEYCODE_SPACE,
            )

        /** Keys that keep their job in the menus and can't be taken for a move. */
        val RESERVED: Set<Int> =
            setOf(
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_ESCAPE,
                KeyEvent.KEYCODE_BACK,
                KeyEvent.KEYCODE_P,
                KeyEvent.KEYCODE_DEL,
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_SHIFT_LEFT,
                KeyEvent.KEYCODE_SHIFT_RIGHT,
                KeyEvent.KEYCODE_ALT_LEFT,
                KeyEvent.KEYCODE_ALT_RIGHT,
                KeyEvent.KEYCODE_CTRL_LEFT,
                KeyEvent.KEYCODE_CTRL_RIGHT,
                KeyEvent.KEYCODE_SYM,
            )

        /** Reads what [encode] wrote; anything unusable gives the defaults. */
        fun decode(text: String?): ScaffoldKeys {
            val codes = text?.split(',')?.map { it.trim().toIntOrNull() }.orEmpty()
            if (codes.size != ScaffoldAction.entries.size || codes.any { it == null } ||
                codes.toSet().size != codes.size
            ) {
                return ScaffoldKeys()
            }
            return ScaffoldKeys(ScaffoldAction.entries.zip(codes.map { it!! }).toMap())
        }

        /** What to call a key on the screen: "A", "Space", "Shift"... */
        fun label(keyCode: Int): String =
            when (keyCode) {
                KeyEvent.KEYCODE_SPACE -> {
                    "Space"
                }

                in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z -> {
                    ('A' + (keyCode - KeyEvent.KEYCODE_A)).toString()
                }

                in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                    ('0' + (keyCode - KeyEvent.KEYCODE_0)).toString()
                }

                KeyEvent.KEYCODE_COMMA -> {
                    ","
                }

                KeyEvent.KEYCODE_PERIOD -> {
                    "."
                }

                KeyEvent.KEYCODE_TAB -> {
                    "Tab"
                }

                else -> {
                    KeyEvent
                        .keyCodeToString(
                            keyCode,
                        ).removePrefix("KEYCODE_")
                        .lowercase()
                        .replaceFirstChar { it.uppercase() }
                }
            }
    }
}

/** Progress that survives closing the app: the best score on each level, the levels opened and the keys. */
internal interface ScaffoldStore {
    var keys: ScaffoldKeys

    /** The highest level number the player may start, 1..[ScaffoldLevels.COUNT]. */
    var unlocked: Int

    fun best(level: Int): Int

    /** Keeps [score] as level [level]'s best if it beats the old one; true when it did. */
    fun record(
        level: Int,
        score: Int,
    ): Boolean

    /** Best bolts collected on level [level]. */
    fun bolts(level: Int): Int

    fun recordBolts(
        level: Int,
        count: Int,
    )
}

internal class MemoryScaffoldStore : ScaffoldStore {
    override var keys = ScaffoldKeys()
    override var unlocked = 1
    private val scores = IntArray(ScaffoldLevels.COUNT + 1)
    private val boltCounts = IntArray(ScaffoldLevels.COUNT + 1)

    override fun best(level: Int): Int = scores[level]

    override fun record(
        level: Int,
        score: Int,
    ): Boolean = (score > scores[level]).also { if (it) scores[level] = score }

    override fun bolts(level: Int): Int = boltCounts[level]

    override fun recordBolts(
        level: Int,
        count: Int,
    ) {
        if (count > boltCounts[level]) boltCounts[level] = count
    }
}

internal class PrefsScaffoldStore(
    context: Context,
) : ScaffoldStore {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override var keys: ScaffoldKeys
        get() = ScaffoldKeys.decode(prefs.getString(KEYS, null))
        set(value) = prefs.edit().putString(KEYS, value.encode()).apply()

    override var unlocked: Int
        get() = prefs.getInt(UNLOCKED, 1).coerceIn(1, ScaffoldLevels.COUNT)
        set(value) = prefs.edit().putInt(UNLOCKED, value.coerceIn(1, ScaffoldLevels.COUNT)).apply()

    override fun best(level: Int): Int = prefs.getInt("best_$level", 0)

    override fun record(
        level: Int,
        score: Int,
    ): Boolean {
        if (score <= best(level)) return false
        prefs.edit().putInt("best_$level", score).apply()
        return true
    }

    override fun bolts(level: Int): Int = prefs.getInt("bolts_$level", 0)

    override fun recordBolts(
        level: Int,
        count: Int,
    ) {
        if (count > bolts(level)) prefs.edit().putInt("bolts_$level", count).apply()
    }

    private companion object {
        const val FILE = "sable_scaffold"
        const val KEYS = "keys"
        const val UNLOCKED = "unlocked"
    }
}
