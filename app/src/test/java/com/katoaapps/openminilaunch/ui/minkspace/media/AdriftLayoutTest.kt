package com.katoaapps.openminilaunch.ui.minkspace.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdriftLayoutTest {
    @Test fun everyMemoryIsDealtBeforeTheDeckResets() {
        val items = (1..9).toList()
        val deck = AdriftMemoryDeck(items, slotCount = 4, seed = 42)
        val observed = deck.currentItems().toMutableSet()

        repeat(items.size - 4) { replacement ->
            deck.advance(replacement % 4).let(observed::add)
        }

        assertEquals(items.toSet(), observed)
    }

    @Test fun twentyFiveMemoryDeckNeverDuplicatesTheVisibleFour() {
        val deck = AdriftMemoryDeck((1..25).toList(), slotCount = 4, seed = 7)

        repeat(100) { replacement ->
            deck.advance(replacement % 4)
            assertEquals(4, deck.currentItems().distinct().size)
        }
    }

    @Test fun smallLibraryClonesMemoriesAcrossFourSlots() {
        val deck = AdriftMemoryDeck(listOf("one", "two"), slotCount = 4, seed = 7)
        val visible = deck.currentItems()

        assertEquals(4, visible.size)
        assertEquals(setOf("one", "two"), visible.toSet())
    }

    @Test fun plannedFramesPreserveAspectRatioStayOnScreenAndLimitOverlap() {
        val screenAspectRatio = 9f / 16f
        val imageAspectRatios = listOf(1.5f, .75f, 2f, 1f)
        val placements = mutableListOf<AdriftFramePlacement>()

        imageAspectRatios.forEachIndexed { index, imageAspectRatio ->
            val placement = planAdriftFramePlacement(
                screenAspectRatio = screenAspectRatio,
                imageAspectRatio = imageAspectRatio,
                occupied = placements,
                previous = null,
                seed = 31 + index * 137,
            )
            val renderedAspectRatio =
                placement.widthFraction * screenAspectRatio / placement.heightFraction

            assertEquals(imageAspectRatio, renderedAspectRatio, .001f)
            assertTrue(placement.bounds.left >= 0f)
            assertTrue(placement.bounds.top >= 0f)
            assertTrue(placement.bounds.right <= 1f)
            assertTrue(placement.bounds.bottom <= 1f)
            placements.forEach { other ->
                assertTrue(overlapFraction(placement.bounds, other.bounds) <= .65f)
            }
            placements += placement
        }
    }

    @Test fun replacementMovesAwayFromItsPreviousPosition() {
        val original = planAdriftFramePlacement(
            screenAspectRatio = 16f / 9f,
            imageAspectRatio = 4f / 3f,
            occupied = emptyList(),
            previous = null,
            seed = 7,
        )
        val replacement = planAdriftFramePlacement(
            screenAspectRatio = 16f / 9f,
            imageAspectRatio = 4f / 3f,
            occupied = emptyList(),
            previous = original,
            seed = 108,
        )

        val horizontalChange = replacement.bounds.centerX - original.bounds.centerX
        val verticalChange = replacement.bounds.centerY - original.bounds.centerY
        assertTrue(horizontalChange * horizontalChange + verticalChange * verticalChange >= .025f)
    }

    @Test fun fourTypicalPhotosOccupyAboutThreeQuartersOfTheDisplay() {
        listOf(9f / 16f, 16f / 9f).forEach { screenAspectRatio ->
            val placements = mutableListOf<AdriftFramePlacement>()
            repeat(4) { index ->
                placements += planAdriftFramePlacement(
                    screenAspectRatio = screenAspectRatio,
                    imageAspectRatio = 4f / 3f,
                    occupied = placements,
                    previous = null,
                    seed = 91 + index * 137,
                )
            }

            val combinedFrameArea = placements.sumOf { it.bounds.area.toDouble() }.toFloat()
            assertTrue(combinedFrameArea in .68f..82f)
        }
    }
}
