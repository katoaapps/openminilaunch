package com.katoaapps.openminilaunch.features.apps

import android.content.ComponentName
import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.model.LauncherTarget

/** Finds the launcher activity Android needs to open profile-specific App Info. */
internal fun appDetailsComponent(
    target: LauncherTarget,
    installedApps: List<LauncherAppTarget>,
): ComponentName? = when (target) {
    is LauncherAppTarget -> target.componentName
    else -> installedApps.firstOrNull { app ->
        app.packageName == target.packageName && app.userSerial == target.userSerial
    }?.componentName
}
