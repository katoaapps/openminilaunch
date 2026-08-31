package com.katoaapps.openminilaunch.model

/** An app-published static, dynamic, cached, or launcher-pinned shortcut. */
data class LauncherShortcutTarget(
    override val label: String,
    override val packageName: String,
    val shortcutId: String,
    override val userSerial: Long,
    override val isWorkProfile: Boolean,
    override val isAvailable: Boolean = true,
    override val selectionKey: String = launcherShortcutSelectionKey(userSerial, packageName, shortcutId),
) : LauncherTarget
