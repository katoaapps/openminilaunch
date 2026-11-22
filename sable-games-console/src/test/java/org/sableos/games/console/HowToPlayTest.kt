package org.sableos.games.console

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** How to play in the console comes from each game's game.toml. */
class HowToPlayTest {
    @Test
    fun theSummaryIsTheText() {
        assertEquals("Roll to the flag.", howToPlay("Roll to the flag.", "Tilt rolls."))
    }

    @Test
    fun aGameWithNoTextSaysSo() {
        assertEquals("", howToPlay("", ""))
        assertNull(howToPlay(" ", "Tilt rolls."))
    }

    @Test
    fun longTextIsCut() {
        assertEquals(600, howToPlay("x".repeat(900), "")!!.length)
    }
}
