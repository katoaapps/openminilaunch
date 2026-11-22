package org.sableos.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** DESIGN-KF-D part 2: one functional alphabet index, type-to-jump, meaningful mini-player. */
class SableAlphabetAndMiniPlayerTest {
    private val people = listOf("#hashtag", "1Password", "Alice", "Amir", "Ben", "Émile", "Zoe")

    @Test
    fun railShowsOnlySectionsThatExist() {
        val index = SableAlphabetIndex.build(people)
        assertEquals(listOf("#", "A", "B", "E", "Z"), index.railKeys)
        // Every visible rail letter jumps somewhere (ALPHABET_RAIL_MUST_FUNCTION_IF_VISIBLE).
        index.railKeys.forEach { key -> assertNotNull(index.indexOf(key)) }
        assertEquals(2, index.indexOf("A"))
        assertEquals(5, index.indexOf("E"))
    }

    @Test
    fun keyboardJumpLandsOnLetterOrNextSection() {
        val index = SableAlphabetIndex.build(people)
        assertEquals(4, index.indexForChar('b'))
        assertEquals(5, index.indexForChar('C')) // no C or D: next section is E
        assertEquals(6, index.indexForChar('y')) // next after Y is Z
        assertEquals(0, index.indexForChar('7')) // digits go to "#"
        assertEquals(5, index.indexForChar('é'))
        val noOther = SableAlphabetIndex.build(listOf("Alpha", "Beta"))
        assertEquals(1, noOther.indexForChar('z')) // past the end: last section
        assertNull(noOther.indexForChar('3'))
        assertNull(SableAlphabetIndex.build(emptyList()).indexForChar('a'))
    }

    @Test
    fun pinnedFavouritesFormOneSectionAndLettersSkipThem() {
        // Starred contacts first (Zed, Amy), then the alphabetical list.
        val labels = listOf("Zed", "Amy", "Bob", "Cara", "Yuri")
        val index = SableAlphabetIndex.build(labels, pinnedCount = 2)
        assertEquals(listOf("★", "B", "C", "Y"), index.railKeys)
        assertEquals(2, index.indexForChar('a')) // next lettered section, not the starred Amy
        assertEquals(4, index.indexForChar('z')) // past the end: last lettered section, never ★
        assertEquals(0, index.indexOf(SableAlphabetIndex.PINNED))
        assertNull(SableAlphabetIndex.build(listOf("Zed"), pinnedCount = 1).indexForChar('a'))
    }

    @Test
    fun activeSectionAndTouchSlots() {
        val index = SableAlphabetIndex.build(people)
        assertEquals("A", index.sectionKeyAt(3))
        assertEquals("Z", index.sectionKeyAt(99))
        assertEquals("#", index.sectionAt(0f)?.key)
        assertEquals("Z", index.sectionAt(1f)?.key)
        assertEquals("B", index.sectionAt(0.5f)?.key)
        assertNull(index.sectionAt(Float.NaN))
    }

    @Test
    fun accentedLettersShareTheirBaseSection() {
        assertEquals("E", SableAlphabetIndex.keyFor("Élodie"))
        assertEquals("#", SableAlphabetIndex.keyFor("  "))
        assertEquals("#", SableAlphabetIndex.keyFor("42"))
    }

    @Test
    fun typeToJumpRefinesPrefixAndCycles() {
        val labels = listOf("Calculator", "Calendar", "Camera", "Clock", "Maps")
        val jump = SableTypeToJump(timeoutMs = 1000)
        assertEquals(0, jump.onChar('c', 0, labels))
        assertEquals(0, jump.onChar('a', 100, labels)) // "ca"
        assertEquals(0, jump.onChar('l', 200, labels)) // "cal"
        assertEquals(1, jump.onChar('e', 300, labels)) // "cale"
        // Timeout resets the prefix.
        assertEquals(4, jump.onChar('m', 5000, labels))
        // Same letter repeated cycles.
        jump.reset()
        assertEquals(0, jump.onChar('c', 0, labels, currentIndex = -1))
        assertEquals(1, jump.onChar('c', 100, labels, currentIndex = 0))
        assertEquals(2, jump.onChar('c', 200, labels, currentIndex = 1))
        assertEquals(3, jump.onChar('c', 300, labels, currentIndex = 2))
        assertEquals(0, jump.onChar('c', 400, labels, currentIndex = 3))
        assertNull(SableTypeToJump().onChar('x', 0, labels))
        assertEquals(0, SableTypeToJump().onChar('e', 0, listOf("Émile")))
    }

    @Test
    fun miniPlayerAlwaysCommunicatesState() {
        val playing =
            SableMiniPlayerPolicy.present(
                SableMiniPlayerInput("Song", "Artist", SablePlaybackState.Playing, 30_000, 120_000),
            )!!
        assertEquals("Song", playing.primary)
        assertEquals("Artist", playing.secondary)
        assertEquals("playing", playing.stateLabel)
        assertEquals(SableMiniPlayerToggle.Pause, playing.toggle)
        assertEquals("Pause", playing.toggleLabel)
        assertEquals(0.25f, playing.progress!!, 0.001f)
        assertTrue(playing.contentDescription.contains("playing"))

        val paused =
            SableMiniPlayerPolicy.present(SableMiniPlayerInput("Song", null, SablePlaybackState.Paused))!!
        assertEquals("paused", paused.stateLabel)
        assertEquals(SableMiniPlayerToggle.Play, paused.toggle)
        assertNull(paused.progress) // unknown duration: no fake progress
        assertNull(paused.secondary)
    }

    @Test
    fun miniPlayerHidesWhenNothingMeaningfulAndRadioHasNoProgress() {
        assertNull(SableMiniPlayerPolicy.present(SableMiniPlayerInput(null, null, SablePlaybackState.Paused)))
        assertNull(SableMiniPlayerPolicy.present(SableMiniPlayerInput("x", "y", SablePlaybackState.Idle)))
        val radio =
            SableMiniPlayerPolicy.present(
                SableMiniPlayerInput("", "Radio One", SablePlaybackState.Playing, 10, 100, live = true),
            )!!
        assertEquals("Radio One", radio.primary)
        assertEquals("live", radio.stateLabel)
        assertNull(radio.progress)
        val buffering =
            SableMiniPlayerPolicy.present(SableMiniPlayerInput("Ep 1", "Show", SablePlaybackState.Buffering))!!
        assertEquals(SableMiniPlayerToggle.Pause, buffering.toggle)
        assertEquals("buffering", buffering.stateLabel)
        assertEquals(
            "playback error",
            SableMiniPlayerPolicy.present(SableMiniPlayerInput("a", null, SablePlaybackState.Error))!!.stateLabel,
        )
    }
}
