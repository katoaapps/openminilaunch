package com.katoaapps.openminilaunch

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
}
