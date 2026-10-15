package com.katoaapps.openminilaunch.data

import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import org.junit.Assert.assertEquals
import org.junit.Test

class MinkSpacePreferenceDefaultsTest {
    @Test
    fun missingPreferenceStartsWithTodoEnabled() {
        assertEquals(
            setOf(MinkSpaceMiniApp.TODO),
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
}
