package com.katoaapps.minkboard.data

import com.katoaapps.minkboard.core.MinkBoardTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoBoardArchiveFixturesTest {
    private val codec = PassphrasePortableArchiveCodec()

    @Test
    fun committedDemoArchivesDecodeToTheirCanonicalSnapshots() {
        DemoBoardArchiveFixtures.all.forEach { fixture ->
            val archive = requireNotNull(
                javaClass.classLoader?.getResourceAsStream(
                    "${DemoBoardArchiveFixtures.RESOURCE_DIRECTORY}/${fixture.fileName}",
                ),
            ) { "Missing demo archive resource: ${fixture.fileName}" }.use { it.readBytes() }

            assertTrue(fixture.fileName, codec.isSupportedEnvelope(archive))
            assertEquals(
                fixture.fileName,
                fixture.snapshot,
                codec.decode(archive, DemoBoardArchiveFixtures.PASSPHRASE.toCharArray()),
            )
        }
    }

    @Test
    fun fixturesCoverEveryTemplatePlusCustomBoards() {
        val templateFixtures = DemoBoardArchiveFixtures.all.filter { it.template != null }
        val customFixtures = DemoBoardArchiveFixtures.all.filter { it.template == null }

        assertEquals(MinkBoardTemplates.all, templateFixtures.map { it.template })
        assertEquals(2, customFixtures.size)

        templateFixtures.forEach { fixture ->
            val board = fixture.snapshot.boards.single()
            val template = requireNotNull(fixture.template)
            assertEquals(template.name, board.name)
            assertEquals(template.statuses.map { it.name }, board.statuses.map { it.name })
            assertEquals(template.statuses.map { it.colorArgb }, board.statuses.map { it.colorArgb })
            assertEquals(
                template.statuses.map { it.isDoneStatus },
                board.statuses.map { it.isDoneStatus },
            )
            assertTrue(board.cards().size in 10..20)
        }

        val commandCenter = customFixtures.single {
            it.fileName == "product-launch-command-center-demo.minkboard"
        }.snapshot.boards.single()
        assertTrue(commandCenter.statuses.size in 8..10)
        assertTrue(commandCenter.cards().size in 25..30)

        val massiveBoard = customFixtures.single {
            it.fileName == "massive-product-roadmap-demo.minkboard"
        }.snapshot.boards.single()
        assertEquals(8, massiveBoard.statuses.size)
        assertEquals(347, massiveBoard.cards().size)
        assertEquals(
            listOf(83, 57, 41, 35, 17, 29, 23, 62),
            massiveBoard.statuses.map { it.cards.size },
        )
        assertTrue(customFixtures.all { fixture ->
            fixture.snapshot.boards.single().statuses.any { it.isDoneStatus }
        })
    }

    @Test
    fun fixturesExercisePortableCardMetadata() {
        val cards = DemoBoardArchiveFixtures.all.flatMap { fixture ->
            fixture.snapshot.boards.single().cards()
        }

        assertTrue(cards.any { it.isMajor })
        assertTrue(cards.any { it.dueDateIso != null })
        assertTrue(cards.any { it.subtasks.isNotEmpty() })
        assertTrue(cards.any { card -> card.subtasks.any { it.isCompleted } })
        assertTrue(cards.all { it.details.isNotBlank() })
    }

    @Test
    fun demoPasswordIsRequired() {
        val fixture = DemoBoardArchiveFixtures.all.first()
        val archive = requireNotNull(
            javaClass.classLoader?.getResourceAsStream(
                "${DemoBoardArchiveFixtures.RESOURCE_DIRECTORY}/${fixture.fileName}",
            ),
        ).use { it.readBytes() }

        assertThrows(InvalidPortableArchiveException::class.java) {
            codec.decode(archive, "not-the-password".toCharArray())
        }
    }

    private fun com.katoaapps.minkboard.model.PortableBoardSnapshot.cards() =
        statuses.flatMap { it.cards }
}
