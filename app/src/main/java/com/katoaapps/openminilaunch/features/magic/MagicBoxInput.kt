package com.katoaapps.openminilaunch.features.magic

import android.content.res.Configuration

internal const val MAGIC_NOTE_PREFIX = '/'
internal val MAGIC_COMMAND_PREFIXES = setOf('@', '#', '-', MAGIC_NOTE_PREFIX, '+', '?')

internal data class MagicBoxInput(
    val prefix: Char?,
    val searchTerm: String,
    val plainQuery: String,
)

internal fun parseMagicBoxInput(text: String, lockedPrefix: Char? = null): MagicBoxInput {
    val prefix = lockedPrefix ?: text.firstOrNull()
    val searchTerm = if (lockedPrefix == null) {
        text.drop(1).substringBefore(' ').trim()
    } else {
        ""
    }
    val plainQuery = text.trim().takeIf { prefix !in MAGIC_COMMAND_PREFIXES }.orEmpty()
    return MagicBoxInput(prefix, searchTerm, plainQuery)
}

internal fun hasMagicBoxDraftText(text: String, lockedPrefix: Char? = null): Boolean {
    val content = when {
        lockedPrefix != null -> text
        text.firstOrNull() in MAGIC_COMMAND_PREFIXES -> text.drop(1)
        else -> text
    }
    return content.isNotBlank()
}

internal fun printableHardwareText(unicodeCodePoint: Int): String? {
    if (
        unicodeCodePoint == 0 ||
        !Character.isValidCodePoint(unicodeCodePoint) ||
        Character.isISOControl(unicodeCodePoint)
    ) {
        return null
    }
    return String(Character.toChars(unicodeCodePoint))
}

internal fun hasUsableHardwareKeyboard(keyboardType: Int, hardKeyboardHidden: Int): Boolean {
    val hasKeys = keyboardType == Configuration.KEYBOARD_QWERTY ||
        keyboardType == Configuration.KEYBOARD_12KEY
    return hasKeys && hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_YES
}

internal fun shouldAutoOpenSoftwareKeyboard(
    enabled: Boolean,
    keyboardType: Int,
    hardKeyboardHidden: Int,
): Boolean = enabled && !hasUsableHardwareKeyboard(keyboardType, hardKeyboardHidden)
