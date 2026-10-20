package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.ui.settings.SettingsDestination
import com.katoaapps.openminilaunch.ui.settings.SettingsSection
import com.katoaapps.openminilaunch.ui.settings.SettingsRoute
import com.katoaapps.openminilaunch.ui.settings.popSettingsRoute
import com.katoaapps.openminilaunch.ui.settings.pushSettingsRoute
import com.katoaapps.openminilaunch.ui.settings.settingsPathTo
import com.katoaapps.openminilaunch.ui.settings.settingsSectionFor
import com.katoaapps.openminilaunch.ui.settings.settingsRoutesTo

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsNavigationTest {
    @Test fun deepLinksToHomeSectionsFromTheirSectionRoot() {
        assertEquals(
            listOf(SettingsDestination.OVERVIEW, SettingsDestination.SHORTCUTS),
            settingsPathTo(SettingsDestination.SHORTCUTS),
        )
    }

    @Test fun appearanceSectionsNestUnderAppearance() {
        assertEquals(
            listOf(
                SettingsDestination.OVERVIEW,
                SettingsDestination.LAUNCHER,
                SettingsDestination.APPEARANCE,
                SettingsDestination.APPEARANCE_WALLPAPER,
            ),
            settingsPathTo(SettingsDestination.APPEARANCE_WALLPAPER),
        )
        assertEquals(
            listOf(
                SettingsDestination.OVERVIEW,
                SettingsDestination.LAUNCHER,
                SettingsDestination.APPEARANCE,
                SettingsDestination.APPEARANCE_LAYOUT_CLOCK,
            ),
            settingsPathTo(SettingsDestination.APPEARANCE_LAYOUT_CLOCK),
        )
    }

    @Test fun pinShortcutRequestsNestUnderShortcuts() {
        assertEquals(
            listOf(
                SettingsDestination.OVERVIEW,
                SettingsDestination.SHORTCUTS,
                SettingsDestination.PIN_SHORTCUT_REQUESTS,
            ),
            settingsPathTo(SettingsDestination.PIN_SHORTCUT_REQUESTS),
        )
    }

    @Test fun magicBoxDetailsReturnDirectlyToTheirSectionRoot() {
        assertEquals(
            listOf(SettingsDestination.OVERVIEW, SettingsDestination.FILE_SEARCH),
            settingsPathTo(SettingsDestination.FILE_SEARCH),
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

    @Test fun destinationsBelongToTheExpectedPrimarySection() {
        assertEquals(SettingsSection.HOME, settingsSectionFor(SettingsDestination.MESSAGING))
        assertEquals(SettingsSection.MAGIC_BOX, settingsSectionFor(SettingsDestination.FILE_SEARCH))
        assertEquals(SettingsSection.ADVANCED, settingsSectionFor(SettingsDestination.MINK_DAY))
        assertEquals(SettingsSection.MORE, settingsSectionFor(SettingsDestination.PERMISSIONS))
    }

    @Test fun deepLinkRoutesKeepTheirSectionAcrossTheWholeBackStack() {
        assertEquals(
            listOf(
                SettingsRoute(SettingsSection.ADVANCED, SettingsDestination.OVERVIEW),
                SettingsRoute(SettingsSection.ADVANCED, SettingsDestination.MINK_DAY),
            ),
            settingsRoutesTo(SettingsDestination.MINK_DAY),
        )
    }

    @Test fun sectionChangesParticipateInBackNavigation() {
        val home = SettingsRoute(SettingsSection.HOME, SettingsDestination.OVERVIEW)
        val advanced = SettingsRoute(SettingsSection.ADVANCED, SettingsDestination.OVERVIEW)
        val minkDay = SettingsRoute(SettingsSection.ADVANCED, SettingsDestination.MINK_DAY)
        val stack = pushSettingsRoute(pushSettingsRoute(listOf(home), advanced), minkDay)

        assertEquals(listOf(home, advanced), popSettingsRoute(stack))
        assertEquals(listOf(home), popSettingsRoute(popSettingsRoute(stack)))
        assertEquals(listOf(home), popSettingsRoute(listOf(home)))
    }
}
