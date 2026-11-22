package org.sableos.games

import android.content.Context
import android.view.KeyEvent

/**
 * One action a player can give its own key in Controls: its name, what it does in the game, and its
 * default keys on each phone ([defaults]). Arrow keys, the trackpad, Enter and Back always keep their
 * job as well ([KeyBindings.RESERVED]), so a player can't lock themselves out.
 */
internal data class ControlAction(
    val id: String,
    val name: String,
    val command: GameCommand,
    val defaults: (DeviceProfile) -> List<Int>,
)

/** A key that does a job but can't be rebound, shown in How to play and Controls. */
internal data class FixedKey(
    val name: String,
    val keys: (DeviceProfile) -> String,
)

/** The rebindable [actions] of one game, saved under [id], and the [fixed] keys shown beside them. */
internal data class GameControls(
    val id: String,
    val actions: List<ControlAction>,
    val fixed: List<FixedKey> = emptyList(),
) {
    fun defaults(profile: DeviceProfile): KeyBindings = KeyBindings(actions.associate { it.id to it.defaults(profile) })

    /** Every default key on [profile], so a default the player moved elsewhere stops working. */
    fun defaultKeys(profile: DeviceProfile): Set<Int> = actions.flatMap { it.defaults(profile) }.toSet()
}

/**
 * The keys a player chose for each [ControlAction], by action id. An action may have several keys
 * (W and I both turn the snake up on the Titan 2); a rebound action has the one key chosen.
 */
internal data class KeyBindings(
    val keys: Map<String, List<Int>>,
) {
    fun keysFor(id: String): List<Int> = keys[id].orEmpty()

    /** The command [keyCode] is bound to in [controls], or null. */
    fun commandFor(
        keyCode: Int,
        controls: GameControls,
    ): GameCommand? = controls.actions.firstOrNull { keyCode in keysFor(it.id) }?.command

    /**
     * Gives [actionId] the single key [keyCode]. Another action that had the key loses it, and if that
     * leaves it with none it takes this action's old keys, so the two swap and nothing is left unbound.
     */
    fun bind(
        actionId: String,
        keyCode: Int,
    ): KeyBindings {
        if (keyCode in RESERVED || keyCode in SYSTEM) return this
        val old = keysFor(actionId)
        val next = keys.mapValues { (id, list) -> if (id == actionId) list else list - keyCode }.toMutableMap()
        next.keys.filter { it != actionId && next.getValue(it).isEmpty() && keyCode in keysFor(it) }.forEach {
            next[it] = old - keyCode
        }
        next[actionId] = listOf(keyCode)
        return KeyBindings(next)
    }

    fun encode(): String = keys.entries.joinToString("|") { (id, list) -> id + "=" + list.joinToString(";") }

    companion object {
        /** Keys that keep their job everywhere: menus, arrows, the trackpad and the modifiers. */
        val RESERVED: Set<Int> =
            setOf(
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_ESCAPE,
                KeyEvent.KEYCODE_BACK,
                KeyEvent.KEYCODE_MENU,
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

        /** Keys the phone itself uses; they are never bound and pass through every menu. */
        val SYSTEM: Set<Int> =
            setOf(
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_MUTE,
                KeyEvent.KEYCODE_POWER,
                KeyEvent.KEYCODE_HOME,
                KeyEvent.KEYCODE_APP_SWITCH,
                KeyEvent.KEYCODE_CALL,
                KeyEvent.KEYCODE_ENDCALL,
                KeyEvent.KEYCODE_CAMERA,
            )

        /** Reads what [encode] wrote over [defaults]; an action missing or unreadable keeps its default. */
        fun decode(
            text: String?,
            defaults: KeyBindings,
        ): KeyBindings {
            val read =
                text
                    ?.split('|')
                    ?.mapNotNull { part ->
                        val id = part.substringBefore('=', "")
                        val codes = part.substringAfter('=', "").split(';').mapNotNull { it.trim().toIntOrNull() }
                        if (id in defaults.keys && codes.isNotEmpty() &&
                            codes.none { it in RESERVED }
                        ) {
                            id to codes
                        } else {
                            null
                        }
                    }?.toMap()
                    .orEmpty()
            val merged = defaults.keys + read
            // A file that binds one key twice is not trusted.
            val all = merged.values.flatten()
            return if (all.size == all.toSet().size) KeyBindings(merged) else defaults
        }

        /** What to call a key on the screen: "A", "Space", "Del"... */
        fun label(keyCode: Int): String =
            when (keyCode) {
                KeyEvent.KEYCODE_SPACE -> "Space"
                KeyEvent.KEYCODE_DEL -> "Del"
                KeyEvent.KEYCODE_FORWARD_DEL -> "Fwd Del"
                KeyEvent.KEYCODE_ENTER -> "Enter"
                else -> ScaffoldKeys.label(keyCode)
            }

        fun labels(keys: List<Int>): String = if (keys.isEmpty()) "None" else keys.joinToString(" ") { label(it) }
    }
}

/** Saved key choices, one entry per game. */
internal interface ControlsStore {
    fun load(
        controls: GameControls,
        profile: DeviceProfile,
    ): KeyBindings

    fun save(
        controls: GameControls,
        bindings: KeyBindings,
    )
}

internal class MemoryControlsStore : ControlsStore {
    private val saved = mutableMapOf<String, String>()

    override fun load(
        controls: GameControls,
        profile: DeviceProfile,
    ) = KeyBindings.decode(saved[controls.id], controls.defaults(profile))

    override fun save(
        controls: GameControls,
        bindings: KeyBindings,
    ) {
        saved[controls.id] = bindings.encode()
    }
}

internal class PrefsControlsStore(
    context: Context,
) : ControlsStore {
    private val prefs = context.getSharedPreferences("sable_controls", Context.MODE_PRIVATE)

    override fun load(
        controls: GameControls,
        profile: DeviceProfile,
    ) = KeyBindings.decode(prefs.getString(controls.id, null), controls.defaults(profile))

    override fun save(
        controls: GameControls,
        bindings: KeyBindings,
    ) {
        prefs.edit().putString(controls.id, bindings.encode()).apply()
    }
}
