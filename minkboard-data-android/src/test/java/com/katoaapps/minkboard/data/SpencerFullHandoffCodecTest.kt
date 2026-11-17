package com.katoaapps.minkboard.data

import com.katoaapps.minkboard.core.IdFactory
import java.util.concurrent.atomic.AtomicInteger
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SpencerFullHandoffCodecTest {
    @Test
    fun fullHandoffRoundTripPreservesCompleteGraphAndRemapsIds() {
        val source = completeHandoff()
        val codec = codec()

        val archive = codec.encode(source)
        val snapshot = codec.decode(archive)

        assertEquals("Spencer 4.2.0", snapshot.sourceApp)
        assertEquals(2, snapshot.boards.size)
        val kitchen = snapshot.boards.first()
        assertEquals("Kitchen خطة", kitchen.name)
        assertTrue(kitchen.isLocked)
        assertFalse(kitchen.id == source.boards.first().legacyId.toString())
        val card = kitchen.statuses.first().cards.single()
        assertEquals("Install café shelves", card.title)
        assertEquals("Preserve all details", card.details)
        assertEquals("2026-11-12", card.dueDateIso)
        assertTrue(card.isMajor)
        assertEquals(1, card.subtasks.size)
        assertTrue(card.subtasks.single().isCompleted)
        assertEquals("Measure twice", card.subtasks.single().text)
        assertNull(snapshot.boards[1].statuses.single().cards.single().dueDateIso)
    }

    @Test
    fun damageAndUnknownVersionAreRejected() {
        val codec = codec()
        val valid = JSONObject(codec.encode(completeHandoff()))
        valid.put("payloadJson", valid.getString("payloadJson") + " ")

        assertThrows(IllegalArgumentException::class.java) {
            codec.decode(valid.toString())
        }

        val unsupported = JSONObject(codec.encode(completeHandoff())).put("schemaVersion", 2)
        assertThrows(IllegalArgumentException::class.java) {
            codec.decode(unsupported.toString())
        }
    }

    @Test
    fun duplicateLegacyCardsAcrossStatusesAreRejected() {
        val source = completeHandoff()
        val board = source.boards.first()
        val firstCard = board.statuses.first().cards.single()
        val duplicateStatus = board.statuses[1].copy(
            cards = listOf(firstCard.copy(position = 0)),
        )

        assertThrows(IllegalArgumentException::class.java) {
            codec().encode(
                source.copy(
                    boards = listOf(
                        board.copy(statuses = listOf(board.statuses.first(), duplicateStatus)),
                    ),
                ),
            )
        }
    }

    @Test
    fun publicExportIsDetectedAndUsesCallerBoardName() {
        val adapter = SpencerImportAdapter(
            idFactory = sequentialIds(),
            clockMillis = { 1_700_000_000_000 },
        )
        val raw = """
            {
              "3": {
                "taskstatus_info": [3, "Doing", "#FF173529", false],
                "tasks": [[7, "Refinish cabinets", 3, "11/12/2026", true]]
              },
              "9": {
                "taskstatus_info": [9, "Done", "#FF602C00", true],
                "tasks": []
              }
            }
        """.trimIndent()

        assertEquals(SpencerImportFormat.PublicBoardExportV1, adapter.detect(raw))
        val snapshot = adapter.parse(raw, publicExportBoardName = "Imported kitchen")

        assertEquals("Imported kitchen", snapshot.boards.single().name)
        assertEquals(2, snapshot.boards.single().statuses.size)
        assertEquals("Refinish cabinets", snapshot.boards.single().statuses[0].cards.single().title)
        assertEquals("2026-11-12", snapshot.boards.single().statuses[0].cards.single().dueDateIso)
    }

    @Test
    fun publicExportRequiresNameAndUnknownNamedFormatIsRejected() {
        val adapter = SpencerImportAdapter(sequentialIds(), clockMillis = { 1 })
        val public = """{"0":{"taskstatus_info":[0,"Todo","#FF602C00",false],"tasks":[]}}"""

        assertThrows(IllegalArgumentException::class.java) { adapter.parse(public) }
        assertThrows(IllegalArgumentException::class.java) {
            adapter.detect("""{"format":"mystery","schemaVersion":1}""")
        }
    }

    private fun codec(): SpencerFullHandoffCodec = SpencerFullHandoffCodec(sequentialIds())

    private fun sequentialIds(): IdFactory {
        val next = AtomicInteger()
        return IdFactory { "new-${next.getAndIncrement()}" }
    }

    private fun completeHandoff(): SpencerFullHandoff = SpencerFullHandoff(
        exportedAtEpochMillis = 1_700_000_000_000,
        sourceAppVersion = "4.2.0",
        boards = listOf(
            SpencerHandoffBoard(
                legacyId = 4,
                name = "Kitchen خطة",
                isLocked = true,
                position = 0,
                createdAtEpochMillis = 100,
                statuses = listOf(
                    SpencerHandoffStatus(
                        legacyId = 10,
                        name = "Doing",
                        colorArgb = 0xFF173529.toInt(),
                        isDoneStatus = false,
                        position = 0,
                        cards = listOf(
                            SpencerHandoffCard(
                                legacyId = 20,
                                title = "Install café shelves",
                                details = "Preserve all details",
                                dueDateIso = "2026-11-12",
                                isMajor = true,
                                position = 0,
                                createdAtEpochMillis = 200,
                                updatedAtEpochMillis = 300,
                                subtasks = listOf(
                                    SpencerHandoffSubtask(
                                        legacyId = 30,
                                        text = "Measure twice",
                                        isCompleted = true,
                                        position = 0,
                                    ),
                                ),
                            ),
                        ),
                    ),
                    SpencerHandoffStatus(
                        legacyId = 11,
                        name = "Done",
                        colorArgb = 0xFF602C00.toInt(),
                        isDoneStatus = true,
                        position = 1,
                        cards = emptyList(),
                    ),
                ),
            ),
            SpencerHandoffBoard(
                legacyId = 5,
                name = "Launch",
                isLocked = false,
                position = 1,
                createdAtEpochMillis = 101,
                statuses = listOf(
                    SpencerHandoffStatus(
                        legacyId = 10,
                        name = "Todo",
                        colorArgb = 0xFFAD4A25.toInt(),
                        isDoneStatus = false,
                        position = 0,
                        cards = listOf(
                            SpencerHandoffCard(
                                legacyId = 20,
                                title = "Publish",
                                details = "",
                                dueDateIso = null,
                                isMajor = false,
                                position = 0,
                                createdAtEpochMillis = 201,
                                updatedAtEpochMillis = 201,
                                subtasks = emptyList(),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )
}
