package com.katoaapps.openminilaunch.features.minkpad.input

internal data class HidKey(val usage: Int, val modifiers: Int = 0)

/** Text mapping for hosts using a US keyboard layout. Unicode is rejected, never replaced. */
internal object UsKeyboardLayout {
    fun key(character: Char): HidKey? = when (character) {
        in 'a'..'z' -> HidKey(character - 'a' + 4)
        in 'A'..'Z' -> HidKey(character - 'A' + 4, 2)
        in '1'..'9' -> HidKey(character - '1' + 30)
        '0' -> HidKey(39)
        '\n', '\r' -> HidKey(40)
        '\b' -> HidKey(42)
        '\t' -> HidKey(43)
        ' ' -> HidKey(44)
        else -> {
            val plain = "-=[]\\;\'`,./"
            val shifted = "_+{}|:\"~<>?"
            val usages = intArrayOf(45,46,47,48,49,51,52,53,54,55,56)
            val p = plain.indexOf(character)
            val s = shifted.indexOf(character)
            val digit = "!@#\$%^&*()".indexOf(character)
            when {
                p >= 0 -> HidKey(usages[p])
                s >= 0 -> HidKey(usages[s], 2)
                digit >= 0 -> HidKey(30 + digit, 2)
                else -> null
            }
        }
    }

    fun report(keys: Collection<Int>, modifiers: Int): ByteArray = ByteArray(8).apply {
        this[0] = modifiers.toByte()
        if (keys.size > 6) for (i in 2..7) this[i] = 1
        else keys.forEachIndexed { index, key -> this[index + 2] = key.toByte() }
    }
}
