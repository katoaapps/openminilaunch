package com.katoaapps.openminilaunch.ui.magic

import com.katoaapps.openminilaunch.features.magic.MagicBoxRoute
import com.katoaapps.openminilaunch.ui.magic.tutorial.MagicBoxTourScene
import com.katoaapps.openminilaunch.ui.magic.tutorial.MagicBoxTourTarget
import com.katoaapps.openminilaunch.ui.magic.tutorial.magicBoxTourSteps
import com.katoaapps.openminilaunch.ui.magic.tutorial.route
import com.katoaapps.openminilaunch.ui.magic.tutorial.shouldStartMagicBoxFeatureTour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MagicBoxFeatureTourTest {
    @Test
    fun firstExpansionStartsEnabledUnseenTour() {
        assertTrue(
            shouldStartMagicBoxFeatureTour(
                enabled = true,
                expanded = true,
                currentTourSeen = false,
                alreadyStarted = false,
            ),
        )
    }

    @Test
    fun assistantAndCompletedToursDoNotStart() {
        assertFalse(
            shouldStartMagicBoxFeatureTour(
                enabled = false,
                expanded = true,
                currentTourSeen = false,
                alreadyStarted = false,
            ),
        )
        assertFalse(
            shouldStartMagicBoxFeatureTour(
                enabled = true,
                expanded = true,
                currentTourSeen = true,
                alreadyStarted = false,
            ),
        )
        assertFalse(
            shouldStartMagicBoxFeatureTour(
                enabled = true,
                expanded = true,
                currentTourSeen = false,
                alreadyStarted = true,
            ),
        )
    }

    @Test
    fun lessonOrderMovesFromEveryHotkeyToAppsAndSearchDestinations() {
        assertEquals(
            listOf(
                MagicBoxTourTarget.MESSAGE_HOTKEY,
                MagicBoxTourTarget.CALL_HOTKEY,
                MagicBoxTourTarget.TODO_HOTKEY,
                MagicBoxTourTarget.NOTE_HOTKEY,
                MagicBoxTourTarget.EVENT_HOTKEY,
                MagicBoxTourTarget.APP_HOTKEY,
                MagicBoxTourTarget.APP_RESULT,
                MagicBoxTourTarget.APP_RESULT,
                MagicBoxTourTarget.DOCUMENT_SEARCH,
                MagicBoxTourTarget.WEB_SEARCH,
                MagicBoxTourTarget.AI_SEARCH,
            ),
            magicBoxTourSteps.map { it.target },
        )
        assertEquals(
            listOf(
                MagicBoxTourScene.HOTKEYS,
                MagicBoxTourScene.APP_SEARCH,
                MagicBoxTourScene.SEARCH_DESTINATIONS,
            ),
            magicBoxTourSteps.map { it.scene }.distinct(),
        )
    }

    @Test
    fun tutorialScenesSelectRoutesWithoutPuttingHotkeysInTheEditor() {
        magicBoxTourSteps
            .filter { it.scene == MagicBoxTourScene.HOTKEYS }
            .forEach { step -> assertEquals(MagicBoxRoute.SEARCH, step.route()) }
        assertEquals(
            MagicBoxRoute.APP,
            magicBoxTourSteps.first { it.scene == MagicBoxTourScene.APP_SEARCH }.route(),
        )
        assertEquals(
            MagicBoxRoute.SEARCH,
            magicBoxTourSteps.first {
                it.scene == MagicBoxTourScene.SEARCH_DESTINATIONS
            }.route(),
        )
    }
}
