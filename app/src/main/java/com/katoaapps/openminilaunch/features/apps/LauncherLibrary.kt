package com.katoaapps.openminilaunch.features.apps

import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.model.LauncherTarget

/** Combines regular apps with the app-published shortcuts saved for discovery surfaces. */
internal fun launcherLibraryTargets(
    apps: List<LauncherAppTarget>,
    alwaysVisibleTargetKeys: List<String>,
    appShortcuts: List<LauncherShortcutTarget>,
    includeAppShortcuts: Boolean,
    resolveTarget: (String) -> LauncherTarget,
): List<LauncherTarget> {
    val alwaysVisibleTargets = alwaysVisibleTargetKeys.map(resolveTarget)
    val optionalAppShortcuts = if (includeAppShortcuts) appShortcuts else emptyList()
    return (apps + alwaysVisibleTargets + optionalAppShortcuts)
        .distinctBy(LauncherTarget::selectionKey)
        .sortedWith(
            compareBy<LauncherTarget, String>(String.CASE_INSENSITIVE_ORDER, LauncherTarget::label)
                .thenBy(LauncherTarget::isWorkProfile),
        )
}
