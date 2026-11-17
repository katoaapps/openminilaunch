package com.katoaapps.minkboard.core

import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.StatusId
import java.util.ArrayDeque
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherTodoNormalizerTest {
    @Test
    fun missingAndCorruptSourcesNeverBecomeEmptyMigrations() {
        val normalizer = normalizer()

        assertEquals(
            LauncherTodoMigrationOutcome.NoLegacyData,
            normalizer.plan(LegacyTodoSource.Missing, sourceSchemaVersion = 1),
        )
        assertEquals(
            LauncherTodoMigrationOutcome.CorruptLegacyData,
            normalizer.plan(LegacyTodoSource.Corrupt, sourceSchemaVersion = 1),
        )
    }

    @Test
    fun migrationCreatesOnlyToDoAndDoneAndPreservesGroupOrder() {
        val firstId = "123e4567-e89b-12d3-a456-426614174000"
        val secondId = "123e4567-e89b-12d3-a456-426614174001"
        val thirdId = "123e4567-e89b-12d3-a456-426614174002"
        val source = listOf(
            LegacyLauncherTodo(firstId, "First", completed = false),
            LegacyLauncherTodo(secondId, "Already done", completed = true),
            LegacyLauncherTodo(thirdId, "Second", completed = false),
        )

        val plan = readyPlan(normalizer().plan(LegacyTodoSource.Available(source), 3))
        val statuses = plan.board.statuses.sortedBy { it.position }

        assertEquals(listOf("To do", "Done"), statuses.map { it.name })
        assertFalse(statuses.first().isDoneStatus)
        assertTrue(statuses.last().isDoneStatus)
        assertEquals(
            listOf(firstId, thirdId),
            plan.board.cardsIn(statuses.first().id).map { it.id.value },
        )
        assertEquals(
            listOf(secondId),
            plan.board.cardsIn(statuses.last().id).map { it.id.value },
        )
        assertEquals(statuses.first().id, plan.designation.intakeStatusId)
        assertEquals(1234L, plan.designation.designatedAtEpochMillis)
        assertEquals(
            LauncherTodoCompatibilityState.CANONICAL_COMPAT,
            plan.designation.legacyCompatibility?.state,
        )
    }

    @Test
    fun emptyAvailableSourceStillCreatesAnEmptyBoundBoard() {
        val plan = readyPlan(
            normalizer().plan(LegacyTodoSource.Available(emptyList()), sourceSchemaVersion = 1),
        )

        assertEquals(2, plan.board.statuses.size)
        assertTrue(plan.board.cards.isEmpty())
        assertEquals(plan.board.board.id, plan.designation.boardId)
    }

    @Test
    fun duplicateOrMalformedLegacyIdsAreRegeneratedWithoutDroppingCards() {
        val repeatedId = "123e4567-e89b-12d3-a456-426614174000"
        val source = listOf(
            LegacyLauncherTodo(repeatedId, "One", completed = false),
            LegacyLauncherTodo(repeatedId, "Two", completed = false),
            LegacyLauncherTodo("not-a-uuid", "Three", completed = false),
        )

        val plan = readyPlan(normalizer().plan(LegacyTodoSource.Available(source), 1))
        val ids = plan.board.cards.map { it.id.value }

        assertEquals(3, ids.distinct().size)
        assertTrue(repeatedId in ids)
        assertTrue("not-a-uuid" !in ids)
    }

    @Test
    fun multilineAndOversizedTextIsPreservedAcrossTitleAndDetails() {
        val longLine = "a".repeat(MinkBoardRules.MAX_CARD_TITLE_LENGTH + 25)
        val multiline = "Title\nDetails stay here"
        val source = listOf(
            LegacyLauncherTodo("one", longLine, completed = false),
            LegacyLauncherTodo("two", multiline, completed = false),
        )

        val normalizer = normalizer()
        val plan = readyPlan(normalizer.plan(LegacyTodoSource.Available(source), 1))
        val cards = plan.board.cards

        assertEquals(longLine, cards[0].title + cards[0].details)
        assertEquals("Title", cards[1].title)
        assertEquals("\nDetails stay here", cards[1].details)
        val projected = normalizer.project(plan.board).items.map { it.text }
        assertEquals(
            longLine.take(MinkBoardRules.MAX_CARD_TITLE_LENGTH) + "\n" +
                longLine.drop(MinkBoardRules.MAX_CARD_TITLE_LENGTH),
            projected[0],
        )
        assertEquals(longLine, projected[0].replaceFirst("\n", ""))
        assertEquals(multiline, projected[1])
    }

    @Test
    fun nativeCardDetailsReceiveAReadableSeparatorWithoutLosingCharacters() {
        val boardId = BoardId("board")
        val todo = status("todo", boardId, position = 0)
        val snapshot = BoardSnapshot(
            board = com.katoaapps.minkboard.model.Board(boardId, "To-Dos", false, 0, 1L),
            statuses = listOf(todo),
            cards = listOf(
                card("plain", todo.id, "Buy milk", 0, details = "at store"),
                card("space", todo.id, "Keep", 1, details = " existing space"),
                card("newline", todo.id, "Keep", 2, details = "\nexisting newline"),
                card("unicode", todo.id, "Café", 3, details = "مرحبا 👋"),
            ),
        )

        assertEquals(
            listOf(
                "Buy milk\nat store",
                "Keep existing space",
                "Keep\nexisting newline",
                "Café\nمرحبا 👋",
            ),
            normalizer().project(snapshot).items.map { it.text },
        )
    }

    @Test
    fun nativeDetailsProjectionStabilizesAfterOneMigrationCycle() {
        val boardId = BoardId("board")
        val todo = status("todo", boardId, position = 0)
        val native = BoardSnapshot(
            board = com.katoaapps.minkboard.model.Board(boardId, "To-Dos", false, 0, 1L),
            statuses = listOf(todo),
            cards = listOf(
                card(
                    id = "123e4567-e89b-12d3-a456-426614174000",
                    statusId = todo.id,
                    title = "Buy milk",
                    position = 0,
                    details = "at store",
                ),
            ),
        )
        val normalizer = normalizer()
        val first = normalizer.project(native)
        val migrated = readyPlan(
            normalizer.plan(LegacyTodoSource.Available(first.items), sourceSchemaVersion = 1),
        )
        val second = normalizer.project(migrated.board)

        assertEquals("Buy milk\nat store", first.items.single().text)
        assertEquals(first.items, second.items)
        assertEquals(first.fingerprint, second.fingerprint)
    }

    @Test
    fun maximumNativeTitleAndDetailsPreserveEveryCharacterAcrossProjectionAndMigration() {
        val boardId = BoardId("board")
        val todo = status("todo", boardId, position = 0)
        val title = "t".repeat(MinkBoardRules.MAX_CARD_TITLE_LENGTH)
        val details = "d".repeat(MinkBoardRules.MAX_CARD_DETAILS_LENGTH)
        val native = BoardSnapshot(
            board = com.katoaapps.minkboard.model.Board(boardId, "To-Dos", false, 0, 1L),
            statuses = listOf(todo),
            cards = listOf(
                card(
                    id = "123e4567-e89b-12d3-a456-426614174000",
                    statusId = todo.id,
                    title = title,
                    position = 0,
                    details = details,
                ),
            ),
        )
        val normalizer = normalizer()

        val first = normalizer.project(native)
        val migrated = readyPlan(
            normalizer.plan(LegacyTodoSource.Available(first.items), sourceSchemaVersion = 1),
        )
        val migratedCard = migrated.board.cards.single()
        val second = normalizer.project(migrated.board)

        assertEquals(title, migratedCard.title)
        assertEquals(details, migratedCard.details)
        assertEquals(first.items, second.items)
        assertEquals(first.fingerprint, second.fingerprint)
    }

    @Test
    fun malformedAvailableTextBecomesTypedCorruptInput() {
        val normalizer = normalizer()
        val tooLarge = "x".repeat(
            MinkBoardRules.MAX_CARD_TITLE_LENGTH + MinkBoardRules.MAX_CARD_DETAILS_LENGTH + 1,
        )

        listOf("   ", tooLarge).forEach { malformedText ->
            assertEquals(
                LauncherTodoMigrationOutcome.CorruptLegacyData,
                normalizer.plan(
                    LegacyTodoSource.Available(
                        listOf(LegacyLauncherTodo("legacy", malformedText, completed = false)),
                    ),
                    sourceSchemaVersion = 1,
                ),
            )
        }
    }

    @Test
    fun projectionTreatsOnlyTheDesignatedDoneStatusAsCompleted() {
        val boardId = BoardId("board")
        val todo = status("todo", boardId, position = 0)
        val waiting = status("waiting", boardId, position = 1)
        val done = status("done", boardId, position = 2, isDone = true)
        val snapshot = BoardSnapshot(
            board = com.katoaapps.minkboard.model.Board(boardId, "To-Dos", false, 0, 1L),
            statuses = listOf(todo, waiting, done),
            cards = listOf(
                card("a", todo.id, "To do", 0),
                card("b", waiting.id, "Waiting", 0),
                card("c", done.id, "Done", 0),
            ),
        )

        val projection = normalizer().project(snapshot)

        assertEquals(listOf("a", "b", "c"), projection.items.map { it.id })
        assertEquals(listOf(false, false, true), projection.items.map { it.completed })
    }

    @Test
    fun projectionDecisionDetectsAnOlderWriterWithoutOverwritingIt() {
        val normalizer = normalizer()
        val source = listOf(
            LegacyLauncherTodo("123e4567-e89b-12d3-a456-426614174000", "Original", false),
        )
        val plan = readyPlan(normalizer.plan(LegacyTodoSource.Available(source), 1))
        val canonical = normalizer.project(plan.board)

        assertEquals(
            LegacyProjectionDecision.ADOPT_CANONICAL_PROJECTION,
            normalizer.projectionDecision(
                source,
                requireNotNull(plan.designation.legacyCompatibility),
                canonical,
            ),
        )

        val writtenCompatibility = requireNotNull(plan.designation.legacyCompatibility).copy(
            lastProjectionFingerprint = canonical.fingerprint,
        )
        assertEquals(
            LegacyProjectionDecision.UNCHANGED,
            normalizer.projectionDecision(canonical.items, writtenCompatibility, canonical),
        )

        val editedBoard = plan.board.copy(
            cards = plan.board.cards.map { it.copy(title = "Edited") },
        )
        val editedCanonical = normalizer.project(editedBoard)
        assertEquals(
            LegacyProjectionDecision.WRITE_CANONICAL_PROJECTION,
            normalizer.projectionDecision(canonical.items, writtenCompatibility, editedCanonical),
        )
        assertEquals(
            LegacyProjectionDecision.ADOPT_CANONICAL_PROJECTION,
            normalizer.projectionDecision(
                editedCanonical.items,
                writtenCompatibility,
                editedCanonical,
            ),
        )
        assertEquals(
            LegacyProjectionDecision.DIVERGED,
            normalizer.projectionDecision(
                listOf(LegacyLauncherTodo("a", "Changed by an old build", false)),
                writtenCompatibility,
                canonical,
            ),
        )
    }

    @Test
    fun fingerprintIsOrderAndBoundarySensitive() {
        val normalizer = normalizer()
        val first = listOf(
            LegacyLauncherTodo("a", "bc", false),
            LegacyLauncherTodo("d", "e", true),
        )
        val reordered = first.reversed()
        val ambiguousWithoutLengths = listOf(
            LegacyLauncherTodo("ab", "c", false),
            LegacyLauncherTodo("d", "e", true),
        )

        assertNotEquals(normalizer.fingerprint(first), normalizer.fingerprint(reordered))
        assertNotEquals(normalizer.fingerprint(first), normalizer.fingerprint(ambiguousWithoutLengths))
    }

    private fun normalizer(): LauncherTodoNormalizer {
        val ids = ArrayDeque((1..30).map { "generated-$it" })
        return LauncherTodoNormalizer(
            idFactory = IdFactory { ids.removeFirst() },
            clockMillis = { 1234L },
        )
    }

    private fun readyPlan(outcome: LauncherTodoMigrationOutcome): LauncherTodoMigrationPlan =
        (outcome as LauncherTodoMigrationOutcome.Ready).plan

    private fun status(
        id: String,
        boardId: BoardId,
        position: Int,
        isDone: Boolean = false,
    ) = BoardStatus(StatusId(id), boardId, id, 0, isDone, position)

    private fun card(
        id: String,
        statusId: StatusId,
        title: String,
        position: Int,
        details: String = "",
    ) = BoardCard(
        id = CardId(id),
        statusId = statusId,
        title = title,
        details = details,
        dueDate = null,
        isMajor = false,
        position = position,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )
}
