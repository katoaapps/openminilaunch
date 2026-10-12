package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.features.apps.*
import com.katoaapps.openminilaunch.model.*
import com.katoaapps.openminilaunch.ui.apps.allAppsDpadPageStep
import com.katoaapps.openminilaunch.ui.apps.isAllAppsDpadSelectKey

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.abs

class AllAppsNavigationTest {
    private val apps = listOf(
        LaunchableApp("Calculator", "calculator"),
        LaunchableApp("Maps", "maps.one"),
        LaunchableApp("Maps Beta", "maps.two"),
        LaunchableApp("Notes", "notes"),
    )

    @Test
    fun letterJumpUsesFirstMatchingApp() {
        assertEquals(1, appIndexForLetter(apps, 'm'))
        assertNull(appIndexForLetter(apps, 'Z'))
    }

    @Test
    fun initialFocusPrefersM() {
        assertEquals(1, initialAllAppsIndex(apps))
        assertEquals(0, initialAllAppsIndex(apps.take(1)))
    }

    @Test
    fun directionalLetterNavigationSkipsLettersWithoutAppsAndStopsAtEnds() {
        val availableLetters = setOf('C', 'M', 'N')

        assertEquals('C', adjacentAvailableAppLetter(availableLetters, 'M', -1))
        assertEquals('N', adjacentAvailableAppLetter(availableLetters, 'M', 1))
        assertEquals('C', adjacentAvailableAppLetter(availableLetters, 'C', -1))
        assertEquals('N', adjacentAvailableAppLetter(availableLetters, 'N', 1))
        assertEquals('C', adjacentAvailableAppLetter(availableLetters, null, 1))
        assertNull(adjacentAvailableAppLetter(emptySet(), 'M', 1))
    }

    @Test
    fun mIsAtExactCenterAndAAndZShareTheBaseline() {
        val width = 1000f
        val height = 500f
        val a = letterArcPosition(0, width, height)
        val m = letterArcPosition(ALL_APPS_CENTER_LETTER_INDEX, width, height)
        val z = letterArcPosition(ALL_APP_LETTERS.lastIndex, width, height)

        assertEquals(width / 2f, m.x, .001f)
        assertEquals(a.y, z.y, .001f)
        assertEquals(0, nearestLetterIndex(a.x, a.y, width, height))
        assertEquals(ALL_APPS_CENTER_LETTER_INDEX, nearestLetterIndex(m.x, m.y, width, height))
        assertEquals(ALL_APP_LETTERS.lastIndex, nearestLetterIndex(z.x, z.y, width, height))
        assert(abs(m.y - a.y) > height / 2f)
    }

    @Test
    fun directionalTrackpadMapsToPreviousAndNextLetters() {
        assertEquals(-1, allAppsDpadPageStep(KeyEvent.KEYCODE_DPAD_LEFT))
        assertEquals(-1, allAppsDpadPageStep(KeyEvent.KEYCODE_DPAD_UP))
        assertEquals(1, allAppsDpadPageStep(KeyEvent.KEYCODE_DPAD_RIGHT))
        assertEquals(1, allAppsDpadPageStep(KeyEvent.KEYCODE_DPAD_DOWN))
        assertNull(allAppsDpadPageStep(KeyEvent.KEYCODE_ENTER))
        assertEquals(true, isAllAppsDpadSelectKey(KeyEvent.KEYCODE_DPAD_CENTER))
        assertEquals(true, isAllAppsDpadSelectKey(KeyEvent.KEYCODE_ENTER))
        assertEquals(false, isAllAppsDpadSelectKey(KeyEvent.KEYCODE_SPACE))
    }
}
