package com.katoaapps.openminilaunch.features.magic

import android.content.res.Configuration

internal const val MAGIC_NOTE_TRIGGER = '/'
internal const val MAGIC_AI_TRIGGER = '*'

/**
 * The active Magic Box capability.
 *
 * Hotkeys select one of these routes; they are not editor content. Keeping control state separate
 * from user-authored text prevents routing characters from leaking into drafts and searches.
 */
internal enum class MagicBoxRoute(val trigger: Char?) {
    SEARCH(null),
    MESSAGE('@'),
    CALL('#'),
    TODO('-'),
    NOTE(MAGIC_NOTE_TRIGGER),
    EVENT('+'),
    APP('?'),
    ASSISTANT(MAGIC_AI_TRIGGER),
    ;

    companion object {
        fun fromTrigger(trigger: Char?): MagicBoxRoute? =
            entries.firstOrNull { it.trigger == trigger }
    }
}

internal val MAGIC_HOTKEY_TRIGGERS: Set<Char> =
    MagicBoxRoute.entries.mapNotNull(MagicBoxRoute::trigger).toSet()

internal data class MagicBoxInput(
    val route: MagicBoxRoute,
    val searchTerm: String,
    val plainQuery: String,
)

internal fun parseMagicBoxInput(
    text: String,
    route: MagicBoxRoute = MagicBoxRoute.SEARCH,
): MagicBoxInput = MagicBoxInput(
    route = route,
    searchTerm = text.trim().takeIf {
        route == MagicBoxRoute.MESSAGE || route == MagicBoxRoute.CALL || route == MagicBoxRoute.APP
    }.orEmpty(),
    plainQuery = text.trim().takeIf { route == MagicBoxRoute.SEARCH }.orEmpty(),
)

internal fun hasMagicBoxDraftText(text: String): Boolean = text.isNotBlank()

internal fun isOpenTodosCommand(text: String, route: MagicBoxRoute): Boolean =
    route == MagicBoxRoute.TODO && text.isBlank()

internal data class MagicBoxTriggerResult(
    val route: MagicBoxRoute,
    val content: String,
)

/**
 * Converts a leading hotkey into route state once, including legacy pasted commands such as
 * `-Buy milk`. Literal punctuation entered after content has begun remains ordinary text.
 */
internal fun consumeMagicBoxTrigger(
    previousText: String,
    updatedText: String,
    routeSelectionEnabled: Boolean = true,
): MagicBoxTriggerResult? {
    if (!routeSelectionEnabled || previousText.isNotEmpty()) return null
    val route = MagicBoxRoute.fromTrigger(updatedText.firstOrNull()) ?: return null
    return MagicBoxTriggerResult(route = route, content = updatedText.drop(1))
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
