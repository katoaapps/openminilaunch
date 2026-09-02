package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.ui.settings.SettingsDestination
import com.katoaapps.openminilaunch.ui.settings.popSettingsDestination
import com.katoaapps.openminilaunch.ui.settings.pushSettingsDestination
import com.katoaapps.openminilaunch.ui.settings.settingsPathTo

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsNavigationTest {
    @Test fun pushesNestedDestinationsWithoutDuplicatingCurrentPage() {
        val overview = listOf(SettingsDestination.OVERVIEW)
        val launcher = pushSettingsDestination(overview, SettingsDestination.LAUNCHER)
        val appearance = pushSettingsDestination(launcher, SettingsDestination.APPEARANCE)

        assertEquals(
            listOf(SettingsDestination.OVERVIEW, SettingsDestination.LAUNCHER, SettingsDestination.APPEARANCE),
            appearance,
        )
        assertEquals(appearance, pushSettingsDestination(appearance, SettingsDestination.APPEARANCE))
    }

    @Test fun popsToTheActualCallingPageAndKeepsOverview() {
        val crossLinkedPermissions = listOf(
            SettingsDestination.OVERVIEW,
            SettingsDestination.MINK_DAY,
            SettingsDestination.PERMISSIONS,
        )

        assertEquals(
            listOf(SettingsDestination.OVERVIEW, SettingsDestination.MINK_DAY),
            popSettingsDestination(crossLinkedPermissions),
        )
        assertEquals(
            listOf(SettingsDestination.OVERVIEW),
            popSettingsDestination(listOf(SettingsDestination.OVERVIEW)),
        )
    }

    @Test fun deepLinksToShortcutsThroughItsLauncherParent() {
        assertEquals(
            listOf(SettingsDestination.OVERVIEW, SettingsDestination.LAUNCHER, SettingsDestination.SHORTCUTS),
            settingsPathTo(SettingsDestination.SHORTCUTS),
        )
    }

    @Test fun pinShortcutRequestsNestUnderShortcuts() {
        assertEquals(
            listOf(
                SettingsDestination.OVERVIEW,
                SettingsDestination.LAUNCHER,
                SettingsDestination.SHORTCUTS,
                SettingsDestination.PIN_SHORTCUT_REQUESTS,
            ),
            settingsPathTo(SettingsDestination.PIN_SHORTCUT_REQUESTS),
        )
    }

    @Test fun assistantAndMessagingAreTopLevelDestinations() {
        assertEquals(
            listOf(SettingsDestination.OVERVIEW, SettingsDestination.MINK_ASSISTANT),
            settingsPathTo(SettingsDestination.MINK_ASSISTANT),
        )
        assertEquals(
            listOf(SettingsDestination.OVERVIEW, SettingsDestination.MESSAGING),
            settingsPathTo(SettingsDestination.MESSAGING),
        )
    }
}
