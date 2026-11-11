package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.ui.settings.SettingsDestination
import com.katoaapps.openminilaunch.ui.settings.SettingsSection
import com.katoaapps.openminilaunch.ui.settings.SettingsRoute
import com.katoaapps.openminilaunch.ui.settings.popSettingsRoute
import com.katoaapps.openminilaunch.ui.settings.openSettingsDestinationFromOverview
import com.katoaapps.openminilaunch.ui.settings.pushSettingsRoute
import com.katoaapps.openminilaunch.ui.settings.settingsPathTo
import com.katoaapps.openminilaunch.ui.settings.settingsSectionFor
import com.katoaapps.openminilaunch.ui.settings.settingsRoutesTo
import com.katoaapps.openminilaunch.ui.settings.selectSettingsSection

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsNavigationTest {
    @Test fun repeatedOverviewTapDoesNotStackDuplicateDestination() {
        val home = SettingsRoute(SettingsSection.HOME, SettingsDestination.OVERVIEW)
        val appGrid = SettingsRoute(SettingsSection.HOME, SettingsDestination.APP_GRID)
        val opened = openSettingsDestinationFromOverview(
            stack = listOf(home),
            section = SettingsSection.HOME,
            destination = SettingsDestination.APP_GRID,
        )

        assertEquals(listOf(home, appGrid), opened)
        assertEquals(
            opened,
            openSettingsDestinationFromOverview(
                stack = opened,
                section = SettingsSection.HOME,
                destination = SettingsDestination.APP_GRID,
            ),
        )
    }

    @Test fun deepLinksToHomeSectionsFromTheirSectionRoot() {
        assertEquals(
            listOf(SettingsDestination.OVERVIEW, SettingsDestination.APP_GRID),
            settingsPathTo(SettingsDestination.APP_GRID),
        )
        assertEquals(
            listOf(SettingsDestination.OVERVIEW, SettingsDestination.QUICK_DRAWER),
            settingsPathTo(SettingsDestination.QUICK_DRAWER),
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

    @Test fun privateSpaceSettingsNestUnderQuickDrawer() {
        assertEquals(
            listOf(
                SettingsDestination.OVERVIEW,
                SettingsDestination.QUICK_DRAWER,
                SettingsDestination.PRIVACY_APPS,
            ),
            settingsPathTo(SettingsDestination.PRIVACY_APPS),
        )
    }

    @Test fun demoStudioNestsUnderAbout() {
        assertEquals(
            listOf(
                SettingsDestination.OVERVIEW,
                SettingsDestination.ABOUT,
                SettingsDestination.DEMO_STUDIO,
            ),
            settingsPathTo(SettingsDestination.DEMO_STUDIO),
        )
        assertEquals(
            SettingsSection.MORE,
            settingsSectionFor(SettingsDestination.DEMO_STUDIO),
        )
    }

    @Test fun magicBoxDetailsReturnDirectlyToTheirSectionRoot() {
        assertEquals(
            listOf(SettingsDestination.OVERVIEW, SettingsDestination.FILE_SEARCH),
            settingsPathTo(SettingsDestination.FILE_SEARCH),
        )
        assertEquals(
            listOf(SettingsDestination.OVERVIEW, SettingsDestination.KEYBOARD),
            settingsPathTo(SettingsDestination.KEYBOARD),
        )
    }

    @Test fun assistantMessagingAndCallingAreTopLevelDestinations() {
        assertEquals(
            listOf(SettingsDestination.OVERVIEW, SettingsDestination.MINK_ASSISTANT),
            settingsPathTo(SettingsDestination.MINK_ASSISTANT),
        )
        assertEquals(
            listOf(SettingsDestination.OVERVIEW, SettingsDestination.MESSAGING),
            settingsPathTo(SettingsDestination.MESSAGING),
        )
        assertEquals(
            listOf(SettingsDestination.OVERVIEW, SettingsDestination.CALLING),
            settingsPathTo(SettingsDestination.CALLING),
        )
    }

    @Test fun destinationsBelongToTheExpectedPrimarySection() {
        assertEquals(SettingsSection.HOME, settingsSectionFor(SettingsDestination.MESSAGING))
        assertEquals(SettingsSection.HOME, settingsSectionFor(SettingsDestination.CALLING))
        assertEquals(SettingsSection.HOME, settingsSectionFor(SettingsDestination.QUICK_DRAWER))
        assertEquals(SettingsSection.MINI_APPS, settingsSectionFor(SettingsDestination.MINK_SPACE))
        assertEquals(SettingsSection.MAGIC_BOX, settingsSectionFor(SettingsDestination.FILE_SEARCH))
        assertEquals(SettingsSection.MAGIC_BOX, settingsSectionFor(SettingsDestination.KEYBOARD))
        assertEquals(SettingsSection.ADVANCED, settingsSectionFor(SettingsDestination.MINK_DAY))
        assertEquals(SettingsSection.MORE, settingsSectionFor(SettingsDestination.PERMISSIONS))
    }

    @Test fun minkSpaceSettingsOpenAtTheMiniAppsSectionRoot() {
        assertEquals(
            listOf(SettingsDestination.OVERVIEW),
            settingsPathTo(SettingsDestination.MINK_SPACE),
        )
        assertEquals(
            listOf(SettingsRoute(SettingsSection.MINI_APPS, SettingsDestination.OVERVIEW)),
            settingsRoutesTo(SettingsDestination.MINK_SPACE),
        )
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

    @Test fun selectingHomeReturnsToTheExistingHomeRoot() {
        val home = SettingsRoute(SettingsSection.HOME, SettingsDestination.OVERVIEW)
        val magicBox = SettingsRoute(SettingsSection.MAGIC_BOX, SettingsDestination.OVERVIEW)
        val fileSearch = SettingsRoute(SettingsSection.MAGIC_BOX, SettingsDestination.FILE_SEARCH)

        assertEquals(
            listOf(home),
            selectSettingsSection(listOf(home, magicBox, fileSearch), SettingsSection.HOME),
        )
    }

    @Test fun selectingCurrentSectionReturnsToItsRoot() {
        val home = SettingsRoute(SettingsSection.HOME, SettingsDestination.OVERVIEW)
        val shortcuts = SettingsRoute(SettingsSection.HOME, SettingsDestination.APP_GRID)

        assertEquals(
            listOf(home),
            selectSettingsSection(listOf(home, shortcuts), SettingsSection.HOME),
        )
    }

    @Test fun selectingANewSectionAddsItsRoot() {
        val home = SettingsRoute(SettingsSection.HOME, SettingsDestination.OVERVIEW)
        val advanced = SettingsRoute(SettingsSection.ADVANCED, SettingsDestination.OVERVIEW)

        assertEquals(
            listOf(home, advanced),
            selectSettingsSection(listOf(home), SettingsSection.ADVANCED),
        )
    }
}
