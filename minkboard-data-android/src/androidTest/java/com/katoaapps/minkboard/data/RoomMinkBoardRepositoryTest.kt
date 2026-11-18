package com.katoaapps.minkboard.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.katoaapps.minkboard.core.IdFactory
import com.katoaapps.minkboard.core.LauncherTodoCompatibilityState
import com.katoaapps.minkboard.core.LauncherTodoMigrationOutcome
import com.katoaapps.minkboard.core.LauncherTodoNormalizer
import com.katoaapps.minkboard.core.LegacyLauncherTodo
import com.katoaapps.minkboard.core.LegacyTodoSource
import com.katoaapps.minkboard.core.MinkBoardTemplates
import com.katoaapps.minkboard.model.BoardDraft
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardUpdate
import com.katoaapps.minkboard.model.CardDraft
import com.katoaapps.minkboard.model.CardUpdate
import com.katoaapps.minkboard.model.FocusSession
import com.katoaapps.minkboard.model.FocusSessionState
import com.katoaapps.minkboard.model.FocusTimerId
import com.katoaapps.minkboard.model.StatusDeletionPolicy
import com.katoaapps.minkboard.model.StatusDraft
import com.katoaapps.minkboard.model.StatusUpdate
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMinkBoardRepositoryTest {
    private lateinit var database: MinkBoardDatabase
    private lateinit var repository: RoomMinkBoardRepository
    private lateinit var cipher: TestFieldCipher
    private val ids = AtomicInteger()
    private val clock = AtomicInteger(1_000)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MinkBoardDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cipher = TestFieldCipher()
        repository = RoomMinkBoardRepository(
            database = database,
            cipher = cipher,
            idFactory = { "test-${ids.incrementAndGet()}" },
            clockMillis = { clock.incrementAndGet().toLong() },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun cardAndSubtaskMutationsPreserveOrderingAndCompletion() = runBlocking {
        repository.ensureDefaultWorkspace()
        var board = repository.observeWorkspace().first().firstBoard!!
        val toDo = board.statuses.first()
        val done = board.statuses.single { it.isDoneStatus }
        val cardId = repository.addCard(toDo.id, CardDraft("Refinish cabinets"))
        val firstSubtask = repository.addSubtask(cardId, "Sand")
        val secondSubtask = repository.addSubtask(cardId, "Seal")

        repository.setSubtaskCompleted(firstSubtask, true)
        repository.deleteSubtask(firstSubtask)
        repository.moveCard(cardId, done.id)

        board = repository.observeWorkspace().first().firstBoard!!
        val card = board.cards.single { it.id == cardId }
        assertEquals(done.id, card.statusId)
        assertEquals(1, board.completedCardCount)
        assertEquals(listOf(secondSubtask), card.subtasks.map { it.id })
        assertEquals(listOf(0), card.subtasks.map { it.position })
        assertTrue(card.updatedAtEpochMillis > card.createdAtEpochMillis)
    }

    @Test
    fun templateCreationPersistsExactIndependentStatusGraphs() = runBlocking {
        repository.ensureDefaultWorkspace()
        val firstId = repository.createBoardFromTemplate(
            BoardDraft("First project"),
            MinkBoardTemplates.AgileDevelopment.id,
        )
        val secondId = repository.createBoardFromTemplate(
            BoardDraft("Second project"),
            MinkBoardTemplates.AgileDevelopment.id,
        )

        val workspace = repository.observeWorkspace().first()
        val first = workspace.board(firstId)!!
        val second = workspace.board(secondId)!!
        val expected = MinkBoardTemplates.AgileDevelopment.statuses
        assertEquals(expected.map { it.name }, first.statuses.map { it.name })
        assertEquals(expected.map { it.colorArgb }, first.statuses.map { it.colorArgb })
        assertEquals(expected.map { it.isDoneStatus }, first.statuses.map { it.isDoneStatus })
        assertEquals(first.statuses.indices.toList(), first.statuses.map { it.position })
        assertTrue(first.statuses.map { it.id }.toSet().intersect(second.statuses.map { it.id }.toSet()).isEmpty())
    }

    @Test
    fun boardCatalogAndActiveBoardDoNotDecryptUnrelatedCards() = runBlocking {
        repository.ensureDefaultWorkspace()
        val first = repository.observeWorkspace().first().firstBoard!!
        val secondId = repository.createBoard(BoardDraft("Large unrelated board"))
        val second = repository.board(secondId)!!
        val corruptCardId = "corrupt-unrelated-card"
        database.boardDao().insertCard(
            CardEntity(
                id = corruptCardId,
                statusId = second.statuses.first().id.value,
                encryptedTitle = "not-valid-ciphertext".toByteArray(),
                encryptedDetails = "not-valid-ciphertext".toByteArray(),
                encryptedDueDate = null,
                isMajor = false,
                position = 0,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L,
            ),
        )

        val summaries = repository.observeBoardSummaries().first()
        assertEquals(2, summaries.size)
        assertEquals(1, summaries.single { it.board.id == secondId }.cardCount)
        assertEquals(first, repository.board(first.board.id))
        assertTrue(runCatching { repository.board(secondId) }.isFailure)
    }

    @Test
    fun movingCardAcrossBoardsIsRejectedWithoutChangingIt() = runBlocking {
        repository.ensureDefaultWorkspace()
        val first = repository.observeWorkspace().first().firstBoard!!
        val cardId = repository.addCard(first.statuses.first().id, CardDraft("Stay here"))
        val secondId = repository.createBoard(BoardDraft("Second board"))
        val second = repository.observeWorkspace().first().board(secondId)!!

        val failure = runCatching {
            repository.moveCard(cardId, second.statuses.first().id)
        }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)

        val unchanged = repository.observeWorkspace().first().firstBoard!!.cards.single()
        assertEquals(first.statuses.first().id, unchanged.statusId)
    }

    @Test
    fun movingCardsPersistsExactPositionsWithinAndAcrossStatuses() = runBlocking {
        repository.ensureDefaultWorkspace()
        val board = repository.observeWorkspace().first().firstBoard!!
        val source = board.statuses[0]
        val destination = board.statuses[1]
        val firstId = repository.addCard(source.id, CardDraft("First"))
        val secondId = repository.addCard(source.id, CardDraft("Second"))
        val thirdId = repository.addCard(source.id, CardDraft("Third"))
        val existingDestinationId = repository.addCard(destination.id, CardDraft("Already there"))

        repository.moveCard(firstId, source.id, 0)

        var updated = repository.observeWorkspace().first().firstBoard!!
        assertEquals(
            listOf(firstId, thirdId, secondId),
            updated.cardsIn(source.id).map { it.id },
        )
        assertEquals(listOf(0, 1, 2), updated.cardsIn(source.id).map { it.position })

        repository.moveCard(firstId, destination.id, 0)

        updated = repository.observeWorkspace().first().firstBoard!!
        assertEquals(listOf(thirdId, secondId), updated.cardsIn(source.id).map { it.id })
        assertEquals(listOf(0, 1), updated.cardsIn(source.id).map { it.position })
        assertEquals(
            listOf(firstId, existingDestinationId),
            updated.cardsIn(destination.id).map { it.id },
        )
        assertEquals(listOf(0, 1), updated.cardsIn(destination.id).map { it.position })
    }

    @Test
    fun newCardsAreInsertedAtTopWithoutPositionGaps() = runBlocking {
        repository.ensureDefaultWorkspace()
        val status = repository.observeWorkspace().first().firstBoard!!.statuses.first()
        val firstId = repository.addCard(status.id, CardDraft("First"))
        val secondId = repository.addCard(status.id, CardDraft("Second"))
        val thirdId = repository.addCard(status.id, CardDraft("Third"))

        val cards = repository.observeWorkspace().first().firstBoard!!.cardsIn(status.id)
        assertEquals(listOf(thirdId, secondId, firstId), cards.map { it.id })
        assertEquals(listOf(0, 1, 2), cards.map { it.position })
    }

    @Test
    fun remappingLargeBoardOnlyDecryptsChangedCardFields() = runBlocking {
        repository.ensureDefaultWorkspace()
        val board = repository.observeWorkspace().first().firstBoard!!
        val status = board.statuses.first()
        val cardIds = (1..40).map { number ->
            repository.addCard(status.id, CardDraft("Card $number", details = "Details $number"))
        }

        repository.board(board.board.id)
        val decryptionsAfterFirstMap = cipher.decryptCount
        repository.board(board.board.id)
        assertEquals(decryptionsAfterFirstMap, cipher.decryptCount)

        val changedId = cardIds.last()
        repository.updateCard(
            changedId,
            CardUpdate(
                title = "Changed title",
                details = "Details 40",
                dueDate = null,
                isMajor = false,
            ),
        )
        repository.board(board.board.id)

        assertTrue(cipher.decryptCount - decryptionsAfterFirstMap <= 3)
    }

    @Test
    fun boardManagementRenamesReordersDeletesAndKeepsOneBoard() = runBlocking {
        repository.ensureDefaultWorkspace()
        val firstBoard = repository.observeWorkspace().first().firstBoard!!
        val firstId = firstBoard.board.id
        val deletedCardId = repository.addCard(firstBoard.statuses.first().id, CardDraft("Cascade"))
        val deletedSubtaskId = repository.addSubtask(deletedCardId, "Cascade too")
        val secondId = repository.createBoard(BoardDraft("Second board"))
        val thirdId = repository.createBoard(BoardDraft("Third board"))

        repository.renameBoard(secondId, "Renamed board")
        repository.reorderBoards(listOf(thirdId, firstId, secondId))

        var workspace = repository.observeWorkspace().first()
        assertEquals(listOf(thirdId, firstId, secondId), workspace.boards.map { it.board.id })
        assertEquals(listOf(0, 1, 2), workspace.boards.map { it.board.position })
        assertEquals("Renamed board", workspace.board(secondId)!!.board.name)

        repository.deleteBoard(firstId)
        workspace = repository.observeWorkspace().first()
        assertEquals(listOf(thirdId, secondId), workspace.boards.map { it.board.id })
        assertEquals(listOf(0, 1), workspace.boards.map { it.board.position })
        assertEquals(null, database.boardDao().card(deletedCardId.value))
        assertEquals(null, database.boardDao().subtask(deletedSubtaskId.value))

        repository.deleteBoard(secondId)
        val failure = runCatching { repository.deleteBoard(thirdId) }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        assertEquals(listOf(thirdId), repository.observeWorkspace().first().boards.map { it.board.id })
    }

    @Test
    fun deletingManyBoardsIsAtomicAndKeepsOneBoard() = runBlocking {
        repository.ensureDefaultWorkspace()
        val first = repository.observeWorkspace().first().firstBoard!!
        val deletedCardId = repository.addCard(first.statuses.first().id, CardDraft("Cascade"))
        val secondId = repository.createBoard(BoardDraft("Second board"))
        val thirdId = repository.createBoard(BoardDraft("Third board"))

        repository.deleteBoards(setOf(first.board.id, secondId))

        assertEquals(
            listOf(thirdId),
            repository.observeWorkspace().first().boards.map { it.board.id },
        )
        assertEquals(null, database.boardDao().card(deletedCardId.value))

        val failure = runCatching {
            repository.deleteBoards(setOf(thirdId))
        }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        assertEquals(
            listOf(thirdId),
            repository.observeWorkspace().first().boards.map { it.board.id },
        )
    }

    @Test
    fun reorderOperationsRequireEveryCurrentIdExactlyOnce() = runBlocking {
        repository.ensureDefaultWorkspace()
        val first = repository.observeWorkspace().first().firstBoard!!
        val secondId = repository.createBoard(BoardDraft("Second board"))

        val boardFailure = runCatching {
            repository.reorderBoards(listOf(first.board.id, first.board.id))
        }.exceptionOrNull()
        assertTrue(boardFailure is IllegalArgumentException)
        assertEquals(
            listOf(first.board.id, secondId),
            repository.observeWorkspace().first().boards.map { it.board.id },
        )

        val originalStatusIds = first.statuses.map { it.id }
        val statusFailure = runCatching {
            repository.reorderStatuses(
                first.board.id,
                originalStatusIds.dropLast(1) + originalStatusIds.first(),
            )
        }.exceptionOrNull()
        assertTrue(statusFailure is IllegalArgumentException)
        assertEquals(
            originalStatusIds,
            repository.observeWorkspace().first().firstBoard!!.statuses.map { it.id },
        )

        repository.reorderStatuses(first.board.id, originalStatusIds.reversed())
        assertEquals(
            originalStatusIds.reversed(),
            repository.observeWorkspace().first().firstBoard!!.statuses.map { it.id },
        )
    }

    @Test
    fun updatingStatusRenamesRecolorsAndKeepsAtMostOneDoneStatus() = runBlocking {
        repository.ensureDefaultWorkspace()
        val board = repository.observeWorkspace().first().firstBoard!!
        val target = board.statuses.first { !it.isDoneStatus }
        val cardId = repository.addCard(target.id, CardDraft("Keep the card"))

        repository.updateStatus(
            target.id,
            StatusUpdate(
                name = "Finished for real",
                colorArgb = 0xFF336699.toInt(),
                isDoneStatus = true,
            ),
        )

        val updated = repository.observeWorkspace().first().firstBoard!!
        val updatedStatus = updated.statuses.single { it.id == target.id }
        assertEquals("Finished for real", updatedStatus.name)
        assertEquals(0xFF336699.toInt(), updatedStatus.colorArgb)
        assertTrue(updatedStatus.isDoneStatus)
        assertEquals(1, updated.statuses.count { it.isDoneStatus })
        assertEquals(target.id, updated.cards.single { it.id == cardId }.statusId)
    }

    @Test
    fun deletingStatusCanMoveCardsWithoutLosingTheirOrderOrSubtasks() = runBlocking {
        repository.ensureDefaultWorkspace()
        val board = repository.observeWorkspace().first().firstBoard!!
        val source = board.statuses[0]
        val destination = board.statuses[1]
        val existingId = repository.addCard(destination.id, CardDraft("Already there"))
        val firstMovedId = repository.addCard(source.id, CardDraft("First moved"))
        val secondMovedId = repository.addCard(source.id, CardDraft("Second moved"))
        val subtaskId = repository.addSubtask(firstMovedId, "Moves too")

        repository.deleteStatus(
            source.id,
            StatusDeletionPolicy.MoveCardsTo(destination.id),
        )

        val updated = repository.observeWorkspace().first().firstBoard!!
        assertFalse(updated.statuses.any { it.id == source.id })
        assertEquals(
            listOf(existingId, secondMovedId, firstMovedId),
            updated.cardsIn(destination.id).map { it.id },
        )
        assertEquals(listOf(0, 1, 2), updated.cardsIn(destination.id).map { it.position })
        assertEquals(
            listOf(subtaskId),
            updated.cards.single { it.id == firstMovedId }.subtasks.map { it.id },
        )
        assertEquals(updated.statuses.indices.toList(), updated.statuses.map { it.position })
    }

    @Test
    fun deletingStatusCanDeleteItsCardsAndRejectsCrossBoardDestinations() = runBlocking {
        repository.ensureDefaultWorkspace()
        val firstBoard = repository.observeWorkspace().first().firstBoard!!
        val source = firstBoard.statuses.first()
        val cardId = repository.addCard(source.id, CardDraft("Remove me"))
        val subtaskId = repository.addSubtask(cardId, "Remove me too")
        val secondBoardId = repository.createBoard(BoardDraft("Second board"))
        val secondBoard = repository.observeWorkspace().first().board(secondBoardId)!!

        val crossBoardFailure = runCatching {
            repository.deleteStatus(
                source.id,
                StatusDeletionPolicy.MoveCardsTo(secondBoard.statuses.first().id),
            )
        }.exceptionOrNull()
        assertTrue(crossBoardFailure is IllegalArgumentException)
        assertTrue(
            repository.observeWorkspace().first().board(firstBoard.board.id)!!
                .cards.any { it.id == cardId },
        )

        repository.deleteStatus(source.id, StatusDeletionPolicy.DeleteCards)
        val updated = repository.observeWorkspace().first().board(firstBoard.board.id)!!
        assertFalse(updated.statuses.any { it.id == source.id })
        assertFalse(updated.cards.any { it.id == cardId })
        assertEquals(null, database.boardDao().card(cardId.value))
        assertEquals(null, database.boardDao().subtask(subtaskId.value))
    }

    @Test
    fun cloningCardDeepCopiesContentWithFreshIndependentIds() = runBlocking {
        repository.ensureDefaultWorkspace()
        val status = repository.observeWorkspace().first().firstBoard!!.statuses.first()
        val originalId = repository.addCard(
            status.id,
            CardDraft(
                title = "Original",
                details = "Sensitive details",
                dueDate = LocalDate.of(2027, 3, 14),
                isMajor = true,
            ),
        )
        val firstSubtaskId = repository.addSubtask(originalId, "First step")
        repository.setSubtaskCompleted(firstSubtaskId, true)
        repository.addSubtask(originalId, "Second step")

        val cloneId = repository.cloneCard(originalId, "Original (duplicate)")
        var board = repository.observeWorkspace().first().firstBoard!!
        val original = board.cards.single { it.id == originalId }
        val clone = board.cards.single { it.id == cloneId }

        assertNotEquals(original.id, clone.id)
        assertEquals(original.statusId, clone.statusId)
        assertEquals("Original (duplicate)", clone.title)
        assertEquals(original.details, clone.details)
        assertEquals(original.dueDate, clone.dueDate)
        assertEquals(original.isMajor, clone.isMajor)
        assertEquals(original.position + 1, clone.position)
        assertEquals(original.subtasks.map { it.text }, clone.subtasks.map { it.text })
        assertEquals(
            original.subtasks.map { it.isCompleted },
            clone.subtasks.map { it.isCompleted },
        )
        assertTrue(original.subtasks.map { it.id }.toSet().intersect(clone.subtasks.map { it.id }.toSet()).isEmpty())
        assertTrue(clone.subtasks.all { it.cardId == cloneId })

        repository.updateCard(
            cloneId,
            CardUpdate(
                title = "Changed clone",
                details = clone.details,
                dueDate = clone.dueDate,
                isMajor = clone.isMajor,
            ),
        )
        repository.setSubtaskCompleted(clone.subtasks.first().id, false)
        repository.updateSubtask(clone.subtasks.last().id, "Changed clone step")

        board = repository.observeWorkspace().first().firstBoard!!
        assertEquals("Original", board.cards.single { it.id == originalId }.title)
        assertTrue(board.cards.single { it.id == originalId }.subtasks.first().isCompleted)
        assertEquals("Changed clone", board.cards.single { it.id == cloneId }.title)
        assertFalse(board.cards.single { it.id == cloneId }.subtasks.first().isCompleted)
        assertEquals(
            "Changed clone step",
            board.cards.single { it.id == cloneId }.subtasks.last().text,
        )
        assertEquals("Second step", board.cards.single { it.id == originalId }.subtasks.last().text)
    }

    @Test
    fun aBoardMustKeepAtLeastOneStatus() = runBlocking {
        repository.ensureDefaultWorkspace()
        var board = repository.observeWorkspace().first().firstBoard!!
        board.statuses.drop(1).forEach { status ->
            repository.deleteStatus(status.id, StatusDeletionPolicy.DeleteCards)
        }
        board = repository.observeWorkspace().first().firstBoard!!

        val failure = runCatching {
            repository.deleteStatus(board.statuses.single().id, StatusDeletionPolicy.DeleteCards)
        }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        assertEquals(1, repository.observeWorkspace().first().firstBoard!!.statuses.size)
    }

    @Test
    fun boardLockAndFocusSessionRoundTripWithoutDuplicatingCardText() = runBlocking {
        repository.ensureDefaultWorkspace()
        val board = repository.observeWorkspace().first().firstBoard!!
        val cardId = repository.addCard(
            board.statuses.first().id,
            CardDraft("Private focus", details = "Visible only while the board is unlocked"),
        )
        repository.updateBoard(board.board.id, BoardUpdate("Protected", isLocked = true))
        val session = FocusSession(
            id = FocusTimerId("timer"),
            boardId = board.board.id,
            cardId = cardId,
            startedAtEpochMillis = 2_000L,
            endsAtEpochMillis = 302_000L,
            state = FocusSessionState.Running,
        )

        val running = repository.replaceFocusTimer(session)
        assertEquals("Private focus", running.cardTitle)
        assertEquals("Visible only while the board is unlocked", running.cardDetails)
        assertTrue(running.boardIsLocked)
        assertEquals(session, repository.observeFocusSession().first())
        assertEquals(1, database.query("SELECT COUNT(*) FROM focus_sessions", null).use {
            it.moveToFirst(); it.getInt(0)
        })

        val finished = repository.finishFocusTimer(session.id)
        assertEquals(FocusSessionState.Finished, finished?.session?.state)
        assertTrue(repository.clearFocusTimer(session.id))
        assertEquals(null, repository.currentFocusTimer())
    }

    @Test
    fun launcherMigrationCommitsEncryptedBoardAndReceiptTogether() = runBlocking {
        val normalizer = LauncherTodoNormalizer(
            idFactory = IdFactory { UUID.randomUUID().toString() },
            clockMillis = { 9_000L },
        )
        val source = listOf(
            LegacyLauncherTodo(
                id = "123e4567-e89b-12d3-a456-426614174000",
                text = "Unfinished",
                completed = false,
            ),
            LegacyLauncherTodo(
                id = "123e4567-e89b-12d3-a456-426614174001",
                text = "Finished",
                completed = true,
            ),
        )
        val plan = (
            normalizer.plan(LegacyTodoSource.Available(source), sourceSchemaVersion = 1) as
                LauncherTodoMigrationOutcome.Ready
            ).plan

        val designation = repository.commitLauncherTodoMigration(plan)
        val board = repository.observeWorkspace().first().board(designation.boardId)!!

        assertEquals(plan.designation, designation)
        assertEquals(designation, repository.launcherBoardDesignation())
        assertEquals(designation, repository.observeLauncherBoardHostSnapshot().first().designation)
        assertEquals(listOf("To do", "Done"), board.statuses.map { it.name })
        assertEquals(listOf("Unfinished", "Finished"), board.cards.map { it.title })
        assertEquals(1, board.completedCardCount)

        val projection = normalizer.project(board)
        assertTrue(
            repository.recordLegacyProjectionWritten(
                generation = designation.generation,
                boardId = designation.boardId,
                fingerprint = projection.fingerprint,
                expectedPreviousFingerprint = null,
            ),
        )
        assertFalse(
            repository.recordLegacyProjectionWritten(
                generation = designation.generation,
                boardId = designation.boardId,
                fingerprint = "a".repeat(64),
                expectedPreviousFingerprint = null,
            ),
        )
        assertEquals(
            projection.fingerprint,
            repository.launcherBoardDesignation()?.legacyCompatibility
                ?.lastProjectionFingerprint,
        )

        assertTrue(
            repository.finishLegacyCompatibility(designation.generation, designation.boardId),
        )
        assertEquals(
            LauncherTodoCompatibilityState.CANONICAL_FINAL,
            repository.launcherBoardDesignation()?.legacyCompatibility?.state,
        )
    }

    @Test
    fun launcherMigrationIsIdempotentAndInvalidReceiptCannotPartiallyInsert() = runBlocking {
        val normalizer = LauncherTodoNormalizer(
            idFactory = IdFactory { UUID.randomUUID().toString() },
            clockMillis = { 10_000L },
        )
        val plan = (
            normalizer.plan(
                LegacyTodoSource.Available(listOf(LegacyLauncherTodo("legacy", "One", false))),
                sourceSchemaVersion = 1,
            ) as LauncherTodoMigrationOutcome.Ready
            ).plan
        val invalid = plan.copy(
            designation = plan.designation.copy(boardId = BoardId("wrong-board")),
        )

        val failure = runCatching { repository.commitLauncherTodoMigration(invalid) }
            .exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(repository.observeWorkspace().first().boards.isEmpty())
        assertEquals(null, repository.launcherBoardDesignation())

        val first = repository.commitLauncherTodoMigration(plan)
        val second = repository.commitLauncherTodoMigration(
            plan.copy(
                designation = plan.designation.copy(
                    legacyCompatibility = plan.designation.legacyCompatibility?.copy(
                        sourceFingerprint = "0".repeat(64),
                    ),
                ),
            ),
        )
        assertEquals(first, second)
        assertEquals(1, repository.observeWorkspace().first().boards.size)
    }

    @Test
    fun launcherMigrationRemapsOnlyIdsThatCollideWithExistingData() = runBlocking {
        val legacyCardId = "123e4567-e89b-12d3-a456-426614174000"
        repository.ensureDefaultWorkspace()
        val existingStatus = repository.observeWorkspace().first().firstBoard!!.statuses.first()
        database.boardDao().insertCard(
            CardEntity(
                id = legacyCardId,
                statusId = existingStatus.id.value,
                encryptedTitle = TestFieldCipher().encrypt(
                    "Existing card",
                    "card:$legacyCardId",
                    "title",
                ),
                encryptedDetails = TestFieldCipher().encrypt(
                    "",
                    "card:$legacyCardId",
                    "details",
                ),
                encryptedDueDate = null,
                isMajor = false,
                position = 0,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L,
            ),
        )
        val normalizer = LauncherTodoNormalizer(
            idFactory = IdFactory { UUID.randomUUID().toString() },
            clockMillis = { 11_000L },
        )
        val plan = (
            normalizer.plan(
                LegacyTodoSource.Available(
                    listOf(LegacyLauncherTodo(legacyCardId, "Migrated card", false)),
                ),
                sourceSchemaVersion = 1,
            ) as LauncherTodoMigrationOutcome.Ready
            ).plan

        val binding = repository.commitLauncherTodoMigration(plan)
        val migratedBoard = repository.observeWorkspace().first().board(binding.boardId)!!

        assertNotEquals(legacyCardId, migratedBoard.cards.single().id.value)
        assertEquals("Migrated card", migratedBoard.cards.single().title)
        assertEquals("Existing card", database.boardDao().card(legacyCardId)?.let { entity ->
            TestFieldCipher().decrypt(entity.encryptedTitle, "card:$legacyCardId", "title")
        })
        assertEquals(2, repository.observeWorkspace().first().boards.size)
    }
}

private class TestFieldCipher : FieldCipher {
    var decryptCount: Int = 0
        private set

    override fun encrypt(value: String, subject: String, field: String): ByteArray =
        "$subject\u0000$field\u0000$value".toByteArray(Charsets.UTF_8)

    override fun decrypt(value: ByteArray, subject: String, field: String): String {
        decryptCount++
        val prefix = "$subject\u0000$field\u0000"
        val decoded = value.toString(Charsets.UTF_8)
        require(decoded.startsWith(prefix)) { "Ciphertext context mismatch" }
        return decoded.removePrefix(prefix)
    }
}
