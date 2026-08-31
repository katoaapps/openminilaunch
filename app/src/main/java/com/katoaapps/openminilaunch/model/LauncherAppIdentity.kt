package com.katoaapps.openminilaunch.model

import android.content.ComponentName

data class LauncherAppIdentity(
    val userSerial: Long,
    val componentName: ComponentName,
)

fun launcherAppIdentity(selectionKey: String): LauncherAppIdentity? {
    val parts = launcherAppKeyParts(selectionKey) ?: return null
    val component = ComponentName.unflattenFromString(parts.flattenedComponent) ?: return null
    return LauncherAppIdentity(parts.userSerial, component)
}
