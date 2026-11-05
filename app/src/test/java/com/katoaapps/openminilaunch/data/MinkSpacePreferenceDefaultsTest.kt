package com.katoaapps.openminilaunch.data

import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import org.junit.Assert.assertEquals
import org.junit.Test

class MinkSpacePreferenceDefaultsTest {
    @Test
    fun missingPreferenceStartsWithTodoAndVCardEnabled() {
        assertEquals(
            setOf(MinkSpaceMiniApp.TODO, MinkSpaceMiniApp.VCARD),
            enabledMinkSpaceMiniAppsFromStableIds(null),
        )
    }

    @Test
    fun savedEmptyPreferenceKeepsEveryMiniAppDisabled() {
        assertEquals(
            emptySet<MinkSpaceMiniApp>(),
            enabledMinkSpaceMiniAppsFromStableIds(emptySet()),
        )
    }

    @Test
    fun savedPreferenceDoesNotReEnableTodo() {
        assertEquals(
            setOf(MinkSpaceMiniApp.MUSIC),
            enabledMinkSpaceMiniAppsFromStableIds(setOf("music")),
        )
    }

    @Test
    fun unavailableAndUnknownMiniAppsAreIgnored() {
        assertEquals(
            setOf(MinkSpaceMiniApp.CALCULATOR),
            enabledMinkSpaceMiniAppsFromStableIds(setOf("calculator", "recorder", "future-app")),
        )
    }

    @Test
    fun visibleReorderKeepsUnavailableMiniAppsInTheirExistingSlots() {
        assertEquals(
            listOf(
                MinkSpaceMiniApp.WIDGET,
                MinkSpaceMiniApp.TODO,
                MinkSpaceMiniApp.CALCULATOR,
                MinkSpaceMiniApp.RECORDER,
                MinkSpaceMiniApp.MEDIA,
                MinkSpaceMiniApp.MUSIC,
                MinkSpaceMiniApp.VCARD,
            ),
            mergeVisibleMinkSpaceMiniAppOrder(
                currentOrder = MinkSpaceMiniApp.defaultOrder,
                requestedVisibleOrder = listOf(
                    MinkSpaceMiniApp.WIDGET,
                    MinkSpaceMiniApp.TODO,
                    MinkSpaceMiniApp.CALCULATOR,
                    MinkSpaceMiniApp.MEDIA,
                    MinkSpaceMiniApp.MUSIC,
                    MinkSpaceMiniApp.VCARD,
                ),
            ),
        )
    }

    @Test
    fun portableRestoreKeepsWidgetInItsCurrentSlot() {
        val currentOrder = listOf(
            MinkSpaceMiniApp.TODO,
            MinkSpaceMiniApp.WIDGET,
            MinkSpaceMiniApp.CALCULATOR,
            MinkSpaceMiniApp.RECORDER,
            MinkSpaceMiniApp.MEDIA,
            MinkSpaceMiniApp.MUSIC,
            MinkSpaceMiniApp.VCARD,
        )

        assertEquals(
            listOf(
                MinkSpaceMiniApp.VCARD,
                MinkSpaceMiniApp.WIDGET,
                MinkSpaceMiniApp.MUSIC,
                MinkSpaceMiniApp.RECORDER,
                MinkSpaceMiniApp.MEDIA,
                MinkSpaceMiniApp.CALCULATOR,
                MinkSpaceMiniApp.TODO,
            ),
            mergePortableMinkSpaceMiniAppOrder(
                currentOrder = currentOrder,
                restoredOrder = listOf(
                    MinkSpaceMiniApp.WIDGET,
                    MinkSpaceMiniApp.VCARD,
                    MinkSpaceMiniApp.MUSIC,
                    MinkSpaceMiniApp.MEDIA,
                    MinkSpaceMiniApp.CALCULATOR,
                    MinkSpaceMiniApp.TODO,
                ),
            ),
        )
    }

    @Test
    fun portableRestoreKeepsConfiguredWidgetEnabled() {
        assertEquals(
            setOf(MinkSpaceMiniApp.MUSIC, MinkSpaceMiniApp.WIDGET),
            mergePortableMinkSpaceEnabledMiniApps(
                currentEnabledMiniApps = setOf(MinkSpaceMiniApp.TODO, MinkSpaceMiniApp.WIDGET),
                restoredEnabledMiniApps = setOf(MinkSpaceMiniApp.MUSIC),
            ),
        )
    }

    @Test
    fun portableRestoreDoesNotEnableWidgetFromAnotherInstallation() {
        assertEquals(
            setOf(MinkSpaceMiniApp.MUSIC),
            mergePortableMinkSpaceEnabledMiniApps(
                currentEnabledMiniApps = setOf(MinkSpaceMiniApp.TODO),
                restoredEnabledMiniApps = setOf(MinkSpaceMiniApp.MUSIC, MinkSpaceMiniApp.WIDGET),
            ),
        )
    }
}
