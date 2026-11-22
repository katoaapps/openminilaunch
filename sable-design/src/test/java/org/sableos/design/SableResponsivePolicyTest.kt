package org.sableos.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** DESIGN-KF-D part 2: responsive rules (navigation, dense rows, labels, focus, latest content). */
class SableResponsivePolicyTest {
    private val square = SableLayoutMetrics(widthDp = 360, heightDp = 360, fontScale = 1f)
    private val squareLargeFont = SableLayoutMetrics(widthDp = 360, heightDp = 360, fontScale = 1.5f)
    private val tablet = SableLayoutMetrics(widthDp = 900, heightDp = 700, fontScale = 1f)

    private fun measure(fontScale: Float): (String) -> Float =
        { label -> SableResponsive.estimateLabelWidthDp(label, textSizeSp = 16f, fontScale = fontScale) }

    private fun dests(vararg labels: String) = labels.map { SableDestination(it, it) }

    @Test
    fun squareDisplayIsCompactAndLimitsTopDestinationsToThree() {
        assertEquals(SableWidthClass.Compact, square.widthClass)
        assertTrue(square.squareish)
        assertEquals(3, square.maxTopDestinations)
        assertEquals(1, square.maxPrimaryTrailingActions)
        assertEquals(SableWidthClass.Expanded, tablet.widthClass)
    }

    @Test
    fun largeFontShrinksTextWidthAndStaysCompact() {
        assertEquals(240f, squareLargeFont.textWidthDp, 0.01f)
        assertTrue(squareLargeFont.largeFont)
        // A 700dp window at font scale 2 has the text room of 350dp: still compact.
        assertEquals(SableWidthClass.Compact, SableLayoutMetrics(700, 700, 2f).widthClass)
        // Nonsense font scales fall back to 1.
        assertEquals(360f, SableLayoutMetrics(360, 360, Float.NaN).textWidthDp, 0.01f)
    }

    @Test
    fun threeShortDestinationsStayInOneRowWithoutMore() {
        val layout =
            SableNavigationPolicy.layout(dests("agenda", "month", "new"), "agenda", square, labelWidthDp = measure(1f))
        assertEquals(3, layout.visible.size)
        assertFalse(layout.hasOverflow)
    }

    @Test
    fun fourDestinationsOnCompactUseMoreInsteadOfOverflowingRow() {
        val layout =
            SableNavigationPolicy.layout(
                dests("music", "podcasts", "radio", "now playing"),
                "music",
                square,
                labelWidthDp = measure(1f),
            )
        assertTrue(layout.hasOverflow)
        assertTrue(layout.slotCount <= SableResponsive.MAX_COMPACT_TOP_DESTINATIONS)
        assertEquals(listOf("music", "podcasts"), layout.visible.map { it.id })
        assertEquals(listOf("radio", "now playing"), layout.overflow.map { it.id })
    }

    @Test
    fun selectedDestinationIsAlwaysVisible() {
        val layout =
            SableNavigationPolicy.layout(
                dests("priority", "messages", "email", "people"),
                "people",
                square,
                labelWidthDp = measure(1f),
            )
        assertTrue(layout.visible.any { it.id == "people" })
        assertEquals(listOf("priority", "people"), layout.visible.map { it.id })
        assertEquals(listOf("messages", "email"), layout.overflow.map { it.id })
    }

    @Test
    fun noVisibleLabelIsWiderThanItsSlotAtLargeFontScale() {
        val labels = dests("songs", "artists", "albums", "favorites", "playlists")
        val m = measure(1.5f)
        val layout = SableNavigationPolicy.layout(labels, "songs", squareLargeFont, labelWidthDp = m)
        val slot = 360f / layout.slotCount
        val shown =
            layout.visible.map { it.label } +
                if (layout.hasOverflow) listOf(SableNavigationPolicy.MORE_LABEL) else emptyList()
        shown.forEach { label ->
            assertTrue(
                "$label clipped",
                m(label) + SableNavigationPolicy.TAB_HORIZONTAL_PADDING_DP <= slot || layout.visible.size == 1,
            )
        }
        assertEquals(5, layout.visible.size + layout.overflow.size)
    }

    @Test
    fun expandedWindowShowsAllFiveDestinations() {
        val layout =
            SableNavigationPolicy.layout(
                dests("songs", "artists", "albums", "favorites", "playlists"),
                null,
                tablet,
                labelWidthDp = measure(1f),
            )
        assertFalse(layout.hasOverflow)
        assertEquals(5, layout.visible.size)
    }

    @Test
    fun denseRowKeepsOnePrimaryTrailingActionOnCompact() {
        val actions =
            listOf(
                SableRowAction("favorite", "Favorite"),
                SableRowAction("play", "Play", primary = true),
                SableRowAction("queue", "Add to queue"),
                SableRowAction("remove", "Remove", destructive = true),
            )
        val split = SableDenseRowPolicy.split(actions, square)
        assertEquals(listOf("play"), split.trailing.map { it.id })
        assertEquals(listOf("favorite", "queue", "remove"), split.overflow.map { it.id })
        // Nothing is lost.
        assertEquals(actions.size, split.trailing.size + split.overflow.size)
    }

    @Test
    fun denseRowOnCompactShowsNoTrailingActionWhenNoneIsPrimary() {
        val split = SableDenseRowPolicy.split(listOf(SableRowAction("edit", "Edit")), square)
        assertTrue(split.trailing.isEmpty())
        assertEquals(1, split.overflow.size)
    }

    @Test
    fun denseRowOnWideWindowMayShowMoreButLargeFontLimitsToOne() {
        val actions =
            listOf(
                SableRowAction("play", "Play", primary = true),
                SableRowAction("fav", "Favorite"),
                SableRowAction("q", "Queue"),
            )
        assertEquals(3, SableDenseRowPolicy.split(actions, tablet).trailing.size)
        assertEquals(1, SableDenseRowPolicy.split(actions, tablet.copy(fontScale = 1.4f)).trailing.size)
    }

    @Test
    fun longLabelsShortenAtWordBoundaryWithEllipsis() {
        assertEquals("Sable Media…", SableLabelPolicy.shorten("Sable Media Library Player", 14))
        assertEquals("Short", SableLabelPolicy.shorten("Short", 14))
        assertEquals("Supercalifra…", SableLabelPolicy.shorten("Supercalifragilistic", 13))
        assertEquals("…", SableLabelPolicy.shorten("Anything", 1))
        assertEquals("Full name", SableLabelPolicy.accessibleLabel("Full name", "Full…"))
        assertNull(SableLabelPolicy.accessibleLabel("Same", "Same"))
    }

    @Test
    fun focusRestorationPrefersSameItemThenOldPosition() {
        val memory = SableFocusMemory()
        assertNull(memory.restore("apps", listOf("a", "b")))
        memory.remember("apps", "c", 2)
        assertEquals(0, memory.restore("apps", listOf("c", "x")))
        // The item was uninstalled: focus the item now at the old position.
        assertEquals(2, memory.restore("apps", listOf("a", "b", "d", "e")))
        assertEquals(1, memory.restore("apps", listOf("a", "b")))
        assertNull(memory.restore("apps", emptyList()))
        memory.forget("apps")
        assertNull(memory.rememberedKey("apps"))
    }

    @Test
    fun latestContentIsVisibleOnOpenAndReturnActionAppearsWhenScrolledAway() {
        assertEquals(0, SableLatestContentPolicy.initialIndex(10, SableListOrder.NewestFirst))
        assertEquals(9, SableLatestContentPolicy.initialIndex(10, SableListOrder.NewestLast))
        assertEquals(0, SableLatestContentPolicy.initialIndex(0, SableListOrder.NewestLast))
        assertFalse(SableLatestContentPolicy.showReturnToCurrent(0, 4, 10, SableListOrder.NewestFirst))
        assertTrue(SableLatestContentPolicy.showReturnToCurrent(3, 7, 10, SableListOrder.NewestFirst))
        assertTrue(SableLatestContentPolicy.showReturnToCurrent(0, 4, 10, SableListOrder.NewestLast))
        assertFalse(SableLatestContentPolicy.showReturnToCurrent(0, 0, 1, SableListOrder.NewestLast))
    }

    @Test
    fun shortWindowsGetTheCompactHeadersAndType() {
        // Zinwa Q25: 720x720 at 280 dpi is 411 dp, about 380 dp under the status bar.
        assertTrue(SableResponsive.isShort(380))
        assertTrue(SableResponsive.isShort(599))
        assertFalse(SableResponsive.isShort(600))
        assertFalse(SableResponsive.isShort(800))
        assertFalse(SableResponsive.isShort(0))
    }
}
