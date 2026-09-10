package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.features.apps.launcherLibraryTargets
import com.katoaapps.openminilaunch.features.apps.launcherDiscoveryLabel
import com.katoaapps.openminilaunch.features.apps.launcherDiscoveryMatches
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherLibraryTest {
    @Test fun savedShortcutsAreDeduplicatedAndSortedForDiscovery() {
        val alpha = shortcut("Alpha", "shortcut:alpha")
        val beta = shortcut("beta", "shortcut:beta")

        val targets = launcherLibraryTargets(
            apps = emptyList(),
            alwaysVisibleTargetKeys = listOf(beta.selectionKey, alpha.selectionKey, beta.selectionKey),
            appShortcuts = emptyList(),
            includeAppShortcuts = false,
            resolveTarget = { key -> if (key == alpha.selectionKey) alpha else beta },
        )

        assertEquals(listOf(alpha, beta), targets)
    }

    @Test fun developerShortcutsAreOptionalButExplicitlyAddedShortcutsRemainVisible() {
        val savedWebsite = shortcut("Website", "shortcut:website")
        val developerAction = shortcut("Compose", "shortcut:compose")

        val hidden = launcherLibraryTargets(
            apps = emptyList(),
            alwaysVisibleTargetKeys = listOf(savedWebsite.selectionKey),
            appShortcuts = listOf(developerAction),
            includeAppShortcuts = false,
            resolveTarget = { savedWebsite },
        )
        val included = launcherLibraryTargets(
            apps = emptyList(),
            alwaysVisibleTargetKeys = listOf(savedWebsite.selectionKey),
            appShortcuts = listOf(developerAction),
            includeAppShortcuts = true,
            resolveTarget = { savedWebsite },
        )

        assertEquals(listOf(savedWebsite), hidden)
        assertEquals(listOf(developerAction, savedWebsite), included)
    }

    @Test fun shortcutDiscoveryShowsAndMatchesItsPublisherAfterItsOwnName() {
        val search = shortcut("Search", "shortcut:search")
        val appLabel: (String) -> String = { "Maps" }

        assertEquals("Search - Maps", launcherDiscoveryLabel(search, appLabel))
        assertEquals(true, launcherDiscoveryMatches(search, "sea", appLabel))
        assertEquals(true, launcherDiscoveryMatches(search, "map", appLabel))
        assertEquals(false, launcherDiscoveryMatches(search, "music", appLabel))
    }

    private fun shortcut(label: String, selectionKey: String) = LauncherShortcutTarget(
        label = label,
        packageName = "com.example",
        shortcutId = label,
        userSerial = 0,
        isWorkProfile = false,
        selectionKey = selectionKey,
    )
}
