package com.katoaapps.minkboard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SpencerLegacyMapperTest {
    @Test
    fun mapsPositionalLegacyDataWithoutReusingNumericIds() {
        var nextId = 0
        val mapper = SpencerLegacyMapper(
            idFactory = IdFactory { "new-${nextId++}" },
            clockMillis = { 42L },
        )
        val result = mapper.map(
            source = SpencerLegacyBoard(
                statuses = listOf(
                    SpencerLegacyStatus(
                        legacyId = 7,
                        name = "Backlog",
                        hexColor = "#FF602C00",
                        isDone = false,
                        tasks = listOf(
                            SpencerLegacyTask(3, "Refinish cabinets", "11/12/2026", true),
                            SpencerLegacyTask(4, "Choose handles", "N/A", false),
                        ),
                    ),
                ),
            ),
            requestedBoardName = "Kitchen",
        )

        assertEquals("Kitchen", result.board.name)
        assertEquals(2, result.cards.size)
        assertTrue(result.cards.first().isMajor)
        assertEquals("2026-11-12", result.cards.first().dueDate.toString())
        assertNull(result.cards.last().dueDate)
        assertTrue(result.cards.all { it.details.isEmpty() })
        assertTrue(result.board.id.value.startsWith("new-"))
    }

    @Test
    fun leavesDetailsEmptyWhenLegacyMessageFitsTheThirtyCharacterTitle() {
        val mapper = SpencerLegacyMapper(IdFactory { "new-id" }, clockMillis = { 1L })
        val message = "x".repeat(30)
        val source = SpencerLegacyBoard(
            listOf(
                SpencerLegacyStatus(
                    legacyId = 1,
                    name = "Todo",
                    hexColor = "#602C00",
                    isDone = false,
                    tasks = listOf(SpencerLegacyTask(1, message, "N/A", false)),
                ),
            ),
        )

        val card = mapper.map(source, "Import").cards.single()

        assertEquals(message, card.title)
        assertEquals("", card.details)
    }

    @Test
    fun generatesShortTitleAndPreservesCompleteLegacyMessageInDetails() {
        val mapper = SpencerLegacyMapper(IdFactory { "new-id" }, clockMillis = { 1L })
        val message = "First line of the Spencer card\nwith every original detail kept intact."
        val source = SpencerLegacyBoard(
            listOf(
                SpencerLegacyStatus(
                    legacyId = 1,
                    name = "Todo",
                    hexColor = "#602C00",
                    isDone = false,
                    tasks = listOf(SpencerLegacyTask(1, message, "N/A", false)),
                ),
            ),
        )

        val card = mapper.map(source, "Import").cards.single()

        assertEquals("First line of the Spencer card…", card.title)
        assertEquals(message, card.details)
        assertFalse(card.title.contains('\n'))
    }

    @Test
    fun acceptsLabeledLegacyNoDateAndLabeledDate() {
        var nextId = 0
        val mapper = SpencerLegacyMapper(
            idFactory = IdFactory { "new-${nextId++}" },
            clockMillis = { 1L },
        )
        val source = SpencerLegacyBoard(
            listOf(
                SpencerLegacyStatus(
                    legacyId = 1,
                    name = "Todo",
                    hexColor = "#602C00",
                    isDone = false,
                    tasks = listOf(
                        SpencerLegacyTask(1, "No date", "Due Date: N/A", false),
                        SpencerLegacyTask(2, "Has date", "Due Date: 4/24/2026", false),
                    ),
                ),
            ),
        )

        val cards = mapper.map(source, "Import").cards

        assertNull(cards[0].dueDate)
        assertEquals("2026-04-24", cards[1].dueDate.toString())
    }

    @Test
    fun rejectsAmbiguousStatusIdentityAndDoneState() {
        val mapper = SpencerLegacyMapper(IdFactory { "id" }, clockMillis = { 1L })
        val duplicateId = SpencerLegacyBoard(
            listOf(
                SpencerLegacyStatus(1, "First", "#602C00", false, emptyList()),
                SpencerLegacyStatus(1, "Second", "#173529", false, emptyList()),
            ),
        )
        val multipleDone = SpencerLegacyBoard(
            listOf(
                SpencerLegacyStatus(1, "First", "#602C00", true, emptyList()),
                SpencerLegacyStatus(2, "Second", "#173529", true, emptyList()),
            ),
        )

        assertThrows(IllegalArgumentException::class.java) { mapper.map(duplicateId, "Import") }
        assertThrows(IllegalArgumentException::class.java) { mapper.map(multipleDone, "Import") }
    }

    @Test
    fun rejectsMalformedDueDateInsteadOfDroppingIt() {
        val mapper = SpencerLegacyMapper(IdFactory { "id" }, clockMillis = { 1L })
        val source = SpencerLegacyBoard(
            listOf(
                SpencerLegacyStatus(
                    legacyId = 1,
                    name = "Todo",
                    hexColor = "#602C00",
                    isDone = false,
                    tasks = listOf(SpencerLegacyTask(1, "Task", "tomorrow-ish", false)),
                ),
            ),
        )

        assertThrows(IllegalArgumentException::class.java) { mapper.map(source, "Import") }
    }
}
