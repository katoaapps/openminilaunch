package com.katoaapps.minkboard.data

import com.katoaapps.minkboard.core.IdFactory
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Optional compatibility harness for private, real-world Spencer exports.
 *
 * Keeping the files outside the repository avoids copying personal board content into source
 * control. Set [FIXTURE_PATHS_ENV] to a platform-separated list of JSON files to exercise them
 * through the same production parser and mapper used by the app.
 */
class SpencerExternalFixtureCompatibilityTest {
    @Test
    fun externalSpencerExportsPreserveContentThatDoesNotFitInTitles() {
        val configuredPaths = System.getenv(FIXTURE_PATHS_ENV).orEmpty()
        assumeTrue("No external Spencer fixtures configured", configuredPaths.isNotBlank())

        configuredPaths.split(File.pathSeparatorChar).forEach { configuredPath ->
            val file = File(configuredPath)
            assertTrue("Fixture does not exist: ${file.name}", file.isFile)
            val raw = file.readText()
            val legacy = SpencerLegacyJsonParser().parse(raw)
            val imported = SpencerImportAdapter(
                idFactory = sequentialIds(),
                clockMillis = { 1_700_000_000_000L },
            ).parse(raw, publicExportBoardName = "Imported ${file.nameWithoutExtension}")

            val legacyMessages = legacy.statuses.flatMap { status ->
                status.tasks.map { task -> task.message }
            }
            val importedCards = imported.boards.single().statuses
                .sortedBy { it.position }
                .flatMap { status -> status.cards.sortedBy { it.position } }

            assertEquals("Card count changed for ${file.name}", legacyMessages.size, importedCards.size)
            val expectedDetails = legacyMessages.map { message ->
                val normalized = message.replace(Regex("\\s+"), " ").trim()
                message.takeIf { normalized.length > 30 }.orEmpty()
            }
            assertEquals(expectedDetails, importedCards.map { card -> card.details })
            importedCards.zip(legacyMessages).forEach { (card, message) ->
                val normalized = message.replace(Regex("\\s+"), " ").trim()
                val expectedTitle = if (normalized.length > 30) {
                    normalized.take(30).trimEnd() + "…"
                } else {
                    normalized
                }
                assertEquals(expectedTitle, card.title)
                assertTrue(card.title.length <= 31)
                assertTrue(!card.title.contains('\n'))
            }
        }
    }

    private fun sequentialIds(): IdFactory {
        val next = AtomicInteger()
        return IdFactory { "external-${next.getAndIncrement()}" }
    }

    private companion object {
        const val FIXTURE_PATHS_ENV = "MINKBOARD_SPENCER_FIXTURES"
    }
}
