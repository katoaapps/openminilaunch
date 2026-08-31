package com.katoaapps.openminilaunch.model

import android.content.ComponentName

/** A launcher activity in one Android user profile. Package name alone is not a unique app ID. */
data class LauncherAppTarget(
    override val label: String,
    override val packageName: String,
    val componentName: ComponentName,
    override val userSerial: Long,
    override val isWorkProfile: Boolean,
    override val isAvailable: Boolean = true,
    override val selectionKey: String = launcherAppSelectionKey(userSerial, componentName),
) : LauncherTarget
