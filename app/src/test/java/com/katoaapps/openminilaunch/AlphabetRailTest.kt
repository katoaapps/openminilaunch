package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.ui.components.visibleAlphabetIndices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlphabetRailTest {
    @Test
    fun fullHeightShowsEveryLetter() {
        assertEquals(
            (0 until 26).toList(),
            visibleAlphabetIndices(
                letterCount = 26,
                availableHeightPx = 520,
                minimumLetterHeightPx = 13f,
                selectedIndex = 12,
            ),
        )
    }

    @Test
    fun compactHeightKeepsEndpointsAndCurrentLetterVisible() {
        val visible = visibleAlphabetIndices(
            letterCount = 26,
            availableHeightPx = 130,
            minimumLetterHeightPx = 13f,
            selectedIndex = 19,
        )

        assertEquals(10, visible.size)
        assertEquals(0, visible.first())
        assertEquals(25, visible.last())
        assertTrue(19 in visible)
        assertEquals(visible.sorted(), visible)
    }

    @Test
    fun tinyHeightStillProvidesUsableRail() {
        val visible = visibleAlphabetIndices(
            letterCount = 26,
            availableHeightPx = 20,
            minimumLetterHeightPx = 13f,
            selectedIndex = 7,
        )

        assertEquals(5, visible.size)
        assertTrue(listOf(0, 7, 25).all(visible::contains))
    }
}
