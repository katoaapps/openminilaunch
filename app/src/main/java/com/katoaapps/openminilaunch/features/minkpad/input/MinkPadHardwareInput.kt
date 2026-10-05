package com.katoaapps.openminilaunch.features.minkpad.input

import android.view.KeyEvent
import com.katoaapps.openminilaunch.features.minkpad.MinkPadRepository

/** Captures keys only while the visible controller is connected. */
internal class MinkPadHardwareInput(private val repository: MinkPadRepository) {
    private val heldModifierKeys = mutableSetOf<Int>()

    fun reset() { heldModifierKeys.clear() }

    fun handle(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) return false
        if (event.action != KeyEvent.ACTION_DOWN && event.action != KeyEvent.ACTION_UP) return false
        val down = event.action == KeyEvent.ACTION_DOWN
        val modifierBit = modifierBits[event.keyCode]
        val resolvedCharacter = event.takeIf { it.isAltPressed && !it.isCtrlPressed && !it.isMetaPressed }
            ?.unicodeChar?.takeIf { it != 0 }?.toChar()?.takeIf { !it.isLetterOrDigit() }
        val resolvedSymbol = resolvedCharacter?.let(UsKeyboardLayout::key)
        val usage = resolvedSymbol?.usage ?: usage(event.keyCode) ?: if (modifierBit != null) 0 else return false
        val repeated = down && event.repeatCount > 0
        // The host repeats a held HID key until its release report. Forwarding Android's
        // synthetic repeat KeyEvents creates extra presses and makes the result host-dependent.
        if (repeated) return true
        var modifiers = eventModifiers(event)
        if (modifierBit != null) {
            if (down) heldModifierKeys.add(event.keyCode) else heldModifierKeys.remove(event.keyCode)
            val family = modifierBit or (if (modifierBit < 16) modifierBit shl 4 else modifierBit shr 4)
            modifiers = modifiers and family.inv()
            heldModifierKeys.forEach { modifiers = modifiers or modifierBits.getValue(it) }
        }
        if (event.keyCode in symbolKeys) {
            // Android already resolved these explicit symbols; do not also send its local Alt layer.
            modifiers = (modifiers and 0x44.inv()) or 2
        } else if (resolvedSymbol != null) {
            // Compact hardware keyboards commonly use an Android Alt/Sym layer for punctuation.
            // Send the resolved US HID symbol rather than forwarding the phone's Alt modifier.
            modifiers = (modifiers and 0x66.inv()) or resolvedSymbol.modifiers
        }
        repository.key(usage, down, modifiers)
        return true
    }

    private fun eventModifiers(event: KeyEvent): Int {
        val meta = event.metaState
        var value = 0
        if (event.isCtrlPressed) value = value or if (meta and KeyEvent.META_CTRL_RIGHT_ON != 0) 16 else 1
        if (event.isShiftPressed) value = value or if (meta and KeyEvent.META_SHIFT_RIGHT_ON != 0) 32 else 2
        if (event.isAltPressed) value = value or if (meta and KeyEvent.META_ALT_RIGHT_ON != 0) 64 else 4
        if (event.isMetaPressed) value = value or if (meta and KeyEvent.META_META_RIGHT_ON != 0) 128 else 8
        heldModifierKeys.forEach { value = value or modifierBits.getValue(it) }
        return value
    }

    private fun usage(code: Int): Int? = when (code) {
        in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z -> code - KeyEvent.KEYCODE_A + 4
        in KeyEvent.KEYCODE_1..KeyEvent.KEYCODE_9 -> code - KeyEvent.KEYCODE_1 + 30
        KeyEvent.KEYCODE_0 -> 39
        KeyEvent.KEYCODE_AT -> 31
        KeyEvent.KEYCODE_POUND -> 32
        KeyEvent.KEYCODE_STAR -> 37
        KeyEvent.KEYCODE_PLUS -> 46
        KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> 40
        KeyEvent.KEYCODE_ESCAPE -> 41
        KeyEvent.KEYCODE_DEL -> 42
        KeyEvent.KEYCODE_TAB -> 43
        KeyEvent.KEYCODE_SPACE -> 44
        KeyEvent.KEYCODE_MINUS -> 45
        KeyEvent.KEYCODE_EQUALS -> 46
        KeyEvent.KEYCODE_LEFT_BRACKET -> 47
        KeyEvent.KEYCODE_RIGHT_BRACKET -> 48
        KeyEvent.KEYCODE_BACKSLASH -> 49
        KeyEvent.KEYCODE_SEMICOLON -> 51
        KeyEvent.KEYCODE_APOSTROPHE -> 52
        KeyEvent.KEYCODE_GRAVE -> 53
        KeyEvent.KEYCODE_COMMA -> 54
        KeyEvent.KEYCODE_PERIOD -> 55
        KeyEvent.KEYCODE_SLASH -> 56
        KeyEvent.KEYCODE_CAPS_LOCK -> 57
        in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12 -> code - KeyEvent.KEYCODE_F1 + 58
        KeyEvent.KEYCODE_INSERT -> 73
        KeyEvent.KEYCODE_MOVE_HOME -> 74
        KeyEvent.KEYCODE_PAGE_UP -> 75
        KeyEvent.KEYCODE_FORWARD_DEL -> 76
        KeyEvent.KEYCODE_MOVE_END -> 77
        KeyEvent.KEYCODE_PAGE_DOWN -> 78
        KeyEvent.KEYCODE_DPAD_RIGHT -> 79
        KeyEvent.KEYCODE_DPAD_LEFT -> 80
        KeyEvent.KEYCODE_DPAD_DOWN -> 81
        KeyEvent.KEYCODE_DPAD_UP -> 82
        KeyEvent.KEYCODE_NUM_LOCK -> 83
        KeyEvent.KEYCODE_NUMPAD_DIVIDE -> 84
        KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> 85
        KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> 86
        KeyEvent.KEYCODE_NUMPAD_ADD -> 87
        KeyEvent.KEYCODE_NUMPAD_ENTER -> 88
        in KeyEvent.KEYCODE_NUMPAD_1..KeyEvent.KEYCODE_NUMPAD_9 -> code - KeyEvent.KEYCODE_NUMPAD_1 + 89
        KeyEvent.KEYCODE_NUMPAD_0 -> 98
        KeyEvent.KEYCODE_NUMPAD_DOT -> 99
        else -> null
    }

    companion object {
        private val symbolKeys = setOf(KeyEvent.KEYCODE_AT, KeyEvent.KEYCODE_POUND, KeyEvent.KEYCODE_STAR, KeyEvent.KEYCODE_PLUS)
        private val modifierBits = mapOf(
            KeyEvent.KEYCODE_CTRL_LEFT to 1, KeyEvent.KEYCODE_CTRL_RIGHT to 16,
            KeyEvent.KEYCODE_SHIFT_LEFT to 2, KeyEvent.KEYCODE_SHIFT_RIGHT to 32,
            KeyEvent.KEYCODE_ALT_LEFT to 4, KeyEvent.KEYCODE_ALT_RIGHT to 64,
            KeyEvent.KEYCODE_META_LEFT to 8, KeyEvent.KEYCODE_META_RIGHT to 128,
        )
    }
}
