package com.katoaapps.openminilaunch.model

private const val LEGACY_LAUNCHER_SHORTCUT_KEY_PREFIX = "legacy-shortcut:"

fun legacyLauncherShortcutSelectionKey(id: String): String =
    "$LEGACY_LAUNCHER_SHORTCUT_KEY_PREFIX$id"

fun legacyLauncherShortcutId(selectionKey: String): String? = selectionKey
    .takeIf { it.startsWith(LEGACY_LAUNCHER_SHORTCUT_KEY_PREFIX) }
    ?.removePrefix(LEGACY_LAUNCHER_SHORTCUT_KEY_PREFIX)
    ?.takeIf { it.isNotBlank() && ':' !in it }
