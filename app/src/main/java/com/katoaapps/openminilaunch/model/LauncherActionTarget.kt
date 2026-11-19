package com.katoaapps.openminilaunch.model

/** A launcher-owned action that can share app and shortcut surfaces without impersonating an app. */
data class LauncherActionTarget(
    override val label: String,
    override val packageName: String,
    override val selectionKey: String,
    override val userSerial: Long = 0L,
    override val isWorkProfile: Boolean = false,
    override val isAvailable: Boolean = true,
) : LauncherTarget
