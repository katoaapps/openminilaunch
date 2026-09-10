package com.katoaapps.openminilaunch.features.apps

import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.model.LauncherTarget

/** Labels and matches targets where regular apps and app-published shortcuts appear together. */
internal fun launcherDiscoveryLabel(
    target: LauncherTarget,
    packageLabel: (String) -> String,
): String = when (target) {
    is LauncherAppTarget -> target.label
    else -> "${target.label} - ${packageLabel(target.packageName)}"
}

internal fun launcherDiscoveryMatches(
    target: LauncherTarget,
    query: String,
    packageLabel: (String) -> String,
): Boolean = target.label.startsWith(query, ignoreCase = true) ||
    (target !is LauncherAppTarget && packageLabel(target.packageName).startsWith(query, ignoreCase = true))
