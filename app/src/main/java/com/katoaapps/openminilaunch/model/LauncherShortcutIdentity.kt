package com.katoaapps.openminilaunch.model

data class LauncherShortcutIdentity(
    val userSerial: Long,
    val packageName: String,
    val shortcutId: String,
)

private const val LAUNCHER_SHORTCUT_KEY_PREFIX = "shortcut:"

fun launcherShortcutSelectionKey(userSerial: Long, packageName: String, shortcutId: String): String =
    "$LAUNCHER_SHORTCUT_KEY_PREFIX$userSerial:$packageName:$shortcutId"

fun launcherShortcutIdentity(selectionKey: String): LauncherShortcutIdentity? {
    if (!selectionKey.startsWith(LAUNCHER_SHORTCUT_KEY_PREFIX)) return null
    val parts = selectionKey.split(':', limit = 4)
    if (parts.size != 4) return null
    val serial = parts[1].toLongOrNull()?.takeIf { it >= 0 } ?: return null
    val packageName = parts[2].takeIf(String::isNotBlank) ?: return null
    val shortcutId = parts[3].takeIf(String::isNotBlank) ?: return null
    return LauncherShortcutIdentity(serial, packageName, shortcutId)
}
