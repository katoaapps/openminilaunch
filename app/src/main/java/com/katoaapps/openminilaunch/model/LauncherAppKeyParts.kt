package com.katoaapps.openminilaunch.model

import android.content.ComponentName

data class LauncherAppKeyParts(
    val userSerial: Long,
    val flattenedComponent: String,
)

private const val LAUNCHER_APP_KEY_PREFIX = "launcher:"

fun launcherAppSelectionKey(userSerial: Long, componentName: ComponentName): String =
    "$LAUNCHER_APP_KEY_PREFIX$userSerial:${componentName.flattenToString()}"

fun launcherAppKeyParts(selectionKey: String): LauncherAppKeyParts? {
    if (!selectionKey.startsWith(LAUNCHER_APP_KEY_PREFIX)) return null
    val parts = selectionKey.split(':', limit = 3)
    if (parts.size != 3) return null
    val serial = parts[1].toLongOrNull()?.takeIf { it >= 0 } ?: return null
    if (parts[2].isBlank()) return null
    return LauncherAppKeyParts(serial, parts[2])
}
