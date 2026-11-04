package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.ui.components.alphabetRailMarkers
import org.junit.Assert.assertEquals
import org.junit.Test

class AlphabetRailTest {
    @Test
    fun railAlwaysShowsEndpointsWithDotsBetween() {
        assertEquals(
            listOf(0, null, null, null, null, null, null, 25),
            alphabetRailMarkers(
                letterCount = 26,
                markerCount = 8,
                selectedIndex = 0,
            ),
        )
    }

    @Test
    fun activeMiddleLetterReplacesItsNearestDot() {
        val markers = alphabetRailMarkers(letterCount = 26, markerCount = 8, selectedIndex = 12)

        assertEquals(8, markers.size)
        assertEquals(0, markers.first())
        assertEquals(25, markers.last())
        assertEquals(12, markers[3])
    }
}
