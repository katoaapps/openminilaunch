package com.katoaapps.openminilaunch.model

/** A shortcut delivered through Android's pre-O INSTALL_SHORTCUT broadcast. */
data class LegacyLauncherShortcutTarget(
    val id: String,
    override val label: String,
    override val packageName: String,
    val intentUri: String,
    override val userSerial: Long,
    override val isAvailable: Boolean = true,
    override val isWorkProfile: Boolean = false,
    override val selectionKey: String = legacyLauncherShortcutSelectionKey(id),
) : LauncherTarget
