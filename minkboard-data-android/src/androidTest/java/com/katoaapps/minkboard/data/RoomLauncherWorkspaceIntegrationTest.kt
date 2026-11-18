package com.katoaapps.minkboard.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.katoaapps.minkboard.core.IdFactory
import com.katoaapps.minkboard.core.LauncherBackupCreationResult
import com.katoaapps.minkboard.core.LauncherBackupRestoreContext
import com.katoaapps.minkboard.core.LauncherBackupRestorePlan
import com.katoaapps.minkboard.core.LauncherBoardBackupDesignation
import com.katoaapps.minkboard.core.LauncherBoardHostSnapshot
import com.katoaapps.minkboard.core.LauncherBindingProblem
import com.katoaapps.minkboard.core.LauncherBoundIntakeWriteResult
import com.katoaapps.minkboard.core.LauncherExistingBoardDesignationResult
import com.katoaapps.minkboard.core.LauncherFreshBoardDesignationResult
import com.katoaapps.minkboard.core.LauncherFreshBoardRequest
import com.katoaapps.minkboard.core.LauncherLocalLegacyCompatibility
import com.katoaapps.minkboard.core.LauncherMinkBoardBackup
import com.katoaapps.minkboard.core.LauncherTodoCompatibilityState
import com.katoaapps.minkboard.core.LauncherTodoMigrationOutcome
import com.katoaapps.minkboard.core.LauncherTodoNormalizer
import com.katoaapps.minkboard.core.LegacyLauncherTodo
import com.katoaapps.minkboard.core.LegacyTodoSource
import com.katoaapps.minkboard.core.WorkspaceImportMode
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardDraft
import com.katoaapps.minkboard.model.CardDraft
import com.katoaapps.minkboard.model.PortableWorkspaceSnapshot
import com.katoaapps.minkboard.model.StatusId
import java.util.concurrent.atomic.AtomicInteger
import java.util.Collections
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomLauncherWorkspaceIntegrationTest {
    private lateinit var database: MinkBoardDatabase
    private lateinit var repository: RoomMinkBoardRepository
    private val ids = AtomicInteger()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MinkBoardDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomMinkBoardRepository(
            database = database,
            cipher = LauncherArchiveTestCipher(),
            idFactory = { "integration-${ids.incrementAndGet()}" },
            clockMillis = { 50_000L + ids.incrementAndGet() },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun launcherBackupCapturesWorkspaceAndPortableBindingTogether() = runBlocking {
        val local = commitLauncherWorkspace()

        val result = repository.createLauncherBackupSnapshot("MinkLauncher test")

        assertTrue(result is LauncherBackupCreationResult.Ready)
        val present = (result as LauncherBackupCreationResult.Ready).backup
        assertEquals(local.boardId, present.designation?.boardId)
        assertEquals(local.intakeStatusId, present.designation?.intakeStatusId)
        assertEquals(local.boardId.value, present.workspace.boards.single().id)
        assertEquals("MinkLauncher test", present.workspace.sourceApp)
    }

    @Test
    fun launcherHostSnapshotInitiallyEmitsOneCoherentEmptyState() = runBlocking {
        val snapshot = repository.observeLauncherBoardHostSnapshot().first()

        assertTrue(snapshot.workspace.boards.isEmpty())
        assertEquals(null, snapshot.designation)
    }

    @Test
    fun migrationHostSnapshotsNeverExposeGraphAndDesignationFromDifferentCommits() = runBlocking {
        val emissions = captureHostSnapshotsAround(
            action = { commitLauncherWorkspace() },
            finalState = { it.designation?.legacyCompatibility != null },
        )

        assertTrue(emissions.first().workspace.boards.isEmpty())
        assertEquals(null, emissions.first().designation)
        emissions.forEach(::assertCoherentHostSnapshot)
        assertTrue(
            emissions.all { snapshot ->
                snapshot.workspace.boards.isEmpty() == (snapshot.designation == null)
            },
        )
    }

    @Test
    fun freshCreateHostSnapshotsNeverExposeAnOrphanGraphOrDesignation() = runBlocking {
        val emissions = captureHostSnapshotsAround(
            action = {
                repository.createAndDesignateFreshBoard(
                    LauncherFreshBoardRequest("Atomic fresh board"),
                )
            },
            finalState = { it.designation?.legacyCompatibility == null && it.designation != null },
        )

        emissions.forEach(::assertCoherentHostSnapshot)
        assertTrue(
            emissions.all { snapshot ->
                snapshot.workspace.boards.isEmpty() == (snapshot.designation == null)
            },
        )
    }

    @Test
    fun boundIntakeWriteResolvesReceiptAndAddsCardAtomically() = runBlocking {
        val binding = commitLauncherWorkspace()

        val result = repository.addCardToBoundIntake(CardDraft("Added through Magic Box"))

        assertTrue(result is LauncherBoundIntakeWriteResult.Added)
        val board = repository.observeWorkspace().first().board(binding.boardId)!!
        assertEquals(
            "Added through Magic Box",
            board.cards.single { it.id == (result as LauncherBoundIntakeWriteResult.Added).cardId }
                .title,
        )
        assertEquals(binding.intakeStatusId, board.cards.last().statusId)
    }

    @Test
    fun freshBoardAndDesignationCommitTogetherWithoutLegacyMetadata() = runBlocking {
        val result = repository.createAndDesignateFreshBoard(
            LauncherFreshBoardRequest("Launcher board"),
        ) as LauncherFreshBoardDesignationResult.Created

        val workspace = repository.observeWorkspace().first()
        val board = workspace.board(result.designation.boardId)!!
        assertEquals("Launcher board", board.board.name)
        assertEquals(listOf("To do", "Doing", "Done"), board.statuses.map { it.name })
        assertFalse(board.statuses.single { it.id == result.designation.intakeStatusId }.isDoneStatus)
        assertEquals(null, result.designation.legacyCompatibility)
        assertEquals(result.designation, repository.launcherBoardDesignation())

        val added = repository.addCardToBoundIntake(CardDraft("Fresh Magic Box item"))
            as LauncherBoundIntakeWriteResult.Added
        assertEquals(
            result.designation.intakeStatusId,
            repository.observeWorkspace().first().board(result.designation.boardId)!!
                .cards.single { it.id == added.cardId }.statusId,
        )
    }

    @Test
    fun freshDesignationAppendsWithoutMutatingExistingUnboundWorkspace() = runBlocking {
        repository.ensureDefaultWorkspace()
        val existing = repository.observeWorkspace().first().boards.single()

        val result = repository.createAndDesignateFreshBoard(
            LauncherFreshBoardRequest("Launcher board"),
        ) as LauncherFreshBoardDesignationResult.Created

        val workspace = repository.observeWorkspace().first()
        assertEquals(2, workspace.boards.size)
        assertEquals(existing, workspace.boards.first())
        assertEquals(result.designation.boardId, workspace.boards.last().board.id)
    }

    @Test
    fun launcherHostSnapshotHydratesOnlyItsDesignatedBoard() = runBlocking {
        val created = repository.createAndDesignateFreshBoard(
            LauncherFreshBoardRequest("Launcher board"),
        ) as LauncherFreshBoardDesignationResult.Created
        val unrelatedId = repository.createBoard(BoardDraft("Unrelated Pro board"))
        val unrelated = repository.board(unrelatedId)!!
        val corruptCardId = "corrupt-unrelated-launcher-card"
        database.boardDao().insertCard(
            CardEntity(
                id = corruptCardId,
                statusId = unrelated.statuses.first().id.value,
                encryptedTitle = "not-valid-ciphertext".toByteArray(),
                encryptedDetails = "not-valid-ciphertext".toByteArray(),
                encryptedDueDate = null,
                isMajor = false,
                position = 0,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L,
            ),
        )

        val snapshot = repository.launcherBoardHostSnapshot()

        assertEquals(created.designation, snapshot.designation)
        assertEquals(
            listOf(created.designation.boardId),
            snapshot.workspace.boards.map { it.board.id },
        )
    }

    @Test
    fun explicitExistingDesignationCreatesNoAdditionalBoardOrLegacyReceipt() = runBlocking {
        repository.ensureDefaultWorkspace()
        val board = repository.observeWorkspace().first().boards.single()
        val intake = board.statuses.first { !it.isDoneStatus }

        val result = repository.designateExistingBoard(
            board.board.id,
            intake.id,
            expectedGeneration = null,
        )
            as LauncherExistingBoardDesignationResult.Designated

        assertEquals(board.board.id, result.designation.boardId)
        assertEquals(intake.id, result.designation.intakeStatusId)
        assertEquals(null, result.designation.legacyCompatibility)
        assertEquals(1, repository.observeWorkspace().first().boards.size)

        val repeated = repository.designateExistingBoard(
            board.board.id,
            intake.id,
            expectedGeneration = "stale-but-same-target",
        ) as LauncherExistingBoardDesignationResult.AlreadyDesignated
        assertEquals(result.designation, repeated.designation)
        assertEquals(result.designation, repository.launcherBoardDesignation())
    }

    @Test
    fun repeatedFreshDesignationIsIdempotent() = runBlocking {
        val created = repository.createAndDesignateFreshBoard(
            LauncherFreshBoardRequest("Launcher board"),
        ) as LauncherFreshBoardDesignationResult.Created

        val repeated = repository.createAndDesignateFreshBoard(
            LauncherFreshBoardRequest("Ignored second name"),
        ) as LauncherFreshBoardDesignationResult.AlreadyDesignated

        assertEquals(created.designation, repeated.designation)
        assertEquals(1, repository.observeWorkspace().first().boards.size)
    }

    @Test
    fun designationInsertFailureRollsBackFreshBoardGraph() = runBlocking {
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_fresh_designation_insert
            BEFORE INSERT ON launcher_board_designation
            BEGIN
                SELECT RAISE(ABORT, 'injected designation insert failure');
            END
            """.trimIndent(),
        )

        val failure = runCatching {
            repository.createAndDesignateFreshBoard(LauncherFreshBoardRequest("Never committed"))
        }.exceptionOrNull()

        assertTrue(failure != null)
        assertTrue(repository.observeWorkspace().first().boards.isEmpty())
        assertEquals(null, repository.launcherBoardDesignation())
    }

    @Test
    fun explicitDesignationRejectsMissingForeignAndDoneTargetsWithoutMutation() = runBlocking {
        val missingBoard = repository.designateExistingBoard(
            BoardId("missing-board"),
            StatusId("missing-status"),
            expectedGeneration = null,
        ) as LauncherExistingBoardDesignationResult.InvalidTarget
        assertEquals(LauncherBindingProblem.BOARD_MISSING, missingBoard.problem)

        repository.ensureDefaultWorkspace()
        val first = repository.observeWorkspace().first().boards.single()
        val missingStatus = repository.designateExistingBoard(
            first.board.id,
            StatusId("missing-status"),
            expectedGeneration = null,
        ) as LauncherExistingBoardDesignationResult.InvalidTarget
        assertEquals(LauncherBindingProblem.INTAKE_STATUS_MISSING, missingStatus.problem)

        repository.createBoard(BoardDraft("Second"))
        val workspace = repository.observeWorkspace().first()
        val second = workspace.boards.last()
        val foreign = repository.designateExistingBoard(
            first.board.id,
            second.statuses.first { !it.isDoneStatus }.id,
            expectedGeneration = null,
        ) as LauncherExistingBoardDesignationResult.InvalidTarget
        assertEquals(
            LauncherBindingProblem.INTAKE_STATUS_BELONGS_TO_ANOTHER_BOARD,
            foreign.problem,
        )

        val done = repository.designateExistingBoard(
            first.board.id,
            first.statuses.single { it.isDoneStatus }.id,
            expectedGeneration = null,
        ) as LauncherExistingBoardDesignationResult.InvalidTarget
        assertEquals(LauncherBindingProblem.INTAKE_STATUS_IS_DONE, done.problem)
        assertEquals(null, repository.launcherBoardDesignation())
    }

    @Test
    fun staleRedesignationAndUnexpectedGenerationWhileUnboundDoNotMutateAnything() = runBlocking {
        repository.ensureDefaultWorkspace()
        val initialWorkspace = repository.observeWorkspace().first()
        val board = initialWorkspace.boards.single()
        val intake = board.statuses.first { !it.isDoneStatus }

        val unboundStale = repository.designateExistingBoard(
            board.board.id,
            intake.id,
            expectedGeneration = "caller-assumed-a-designation",
        ) as LauncherExistingBoardDesignationResult.StaleDesignation
        assertEquals(null, unboundStale.current)
        assertEquals(null, repository.launcherBoardDesignation())

        val designated = repository.designateExistingBoard(
            board.board.id,
            intake.id,
            expectedGeneration = null,
        ) as LauncherExistingBoardDesignationResult.Designated
        repository.createBoard(BoardDraft("Second"))
        val second = repository.observeWorkspace().first().boards.last()
        val before = repository.observeWorkspace().first()

        val stale = repository.designateExistingBoard(
            second.board.id,
            second.statuses.first { !it.isDoneStatus }.id,
            expectedGeneration = "stale-generation",
        ) as LauncherExistingBoardDesignationResult.StaleDesignation

        assertEquals(designated.designation, stale.current)
        assertEquals(designated.designation, repository.launcherBoardDesignation())
        assertEquals(before, repository.observeWorkspace().first())
    }

    @Test
    fun matchingGenerationRepairsBrokenBoardAndIntakeWithFreshGeneration() = runBlocking {
        repository.ensureDefaultWorkspace()
        val board = repository.observeWorkspace().first().boards.single()
        val validIntake = board.statuses.first { !it.isDoneStatus }
        val dao = database.boardDao()
        val brokenBoard = LauncherBoardDesignationEntity(
            generation = "broken-board-generation",
            boardId = "deleted-board",
            intakeStatusId = "deleted-intake",
            designatedAtEpochMillis = 1L,
            compatibilityState = LauncherTodoCompatibilityState.CANONICAL_COMPAT.name,
            sourceSchemaVersion = 1,
            sourceFingerprint = "a".repeat(64),
            lastProjectionFingerprint = null,
            migratedAtEpochMillis = 1L,
        )
        dao.insertLauncherBoardDesignation(brokenBoard)

        val unchangedBrokenBoard = repository.designateExistingBoard(
            BoardId(brokenBoard.boardId),
            StatusId(brokenBoard.intakeStatusId),
            expectedGeneration = "irrelevant-for-identical-target",
        ) as LauncherExistingBoardDesignationResult.BrokenExistingDesignation
        assertEquals(LauncherBindingProblem.BOARD_MISSING, unchangedBrokenBoard.problem)

        val boardRepair = repository.designateExistingBoard(
            board.board.id,
            validIntake.id,
            expectedGeneration = brokenBoard.generation,
        ) as LauncherExistingBoardDesignationResult.Designated
        assertNotEquals(brokenBoard.generation, boardRepair.designation.generation)
        assertEquals(null, boardRepair.designation.legacyCompatibility)

        val brokenIntake = LauncherBoardDesignationEntity(
            generation = "broken-intake-generation",
            boardId = board.board.id.value,
            intakeStatusId = "deleted-intake",
            designatedAtEpochMillis = 2L,
            compatibilityState = LauncherTodoCompatibilityState.CANONICAL_COMPAT.name,
            sourceSchemaVersion = 1,
            sourceFingerprint = "b".repeat(64),
            lastProjectionFingerprint = null,
            migratedAtEpochMillis = 2L,
        )
        dao.upsertLauncherBoardDesignation(brokenIntake)

        val unchangedBrokenIntake = repository.designateExistingBoard(
            BoardId(brokenIntake.boardId),
            StatusId(brokenIntake.intakeStatusId),
            expectedGeneration = "irrelevant-for-identical-target",
        ) as LauncherExistingBoardDesignationResult.BrokenExistingDesignation
        assertEquals(
            LauncherBindingProblem.INTAKE_STATUS_MISSING,
            unchangedBrokenIntake.problem,
        )

        val intakeRepair = repository.designateExistingBoard(
            board.board.id,
            validIntake.id,
            expectedGeneration = brokenIntake.generation,
        ) as LauncherExistingBoardDesignationResult.Designated
        assertNotEquals(brokenIntake.generation, intakeRepair.designation.generation)
        assertEquals(validIntake.id, intakeRepair.designation.intakeStatusId)
        assertEquals(null, intakeRepair.designation.legacyCompatibility)
    }

    @Test
    fun redesignationInvalidatesOldProjectionAndFinalizationTokens() = runBlocking {
        val previous = commitLauncherWorkspace()
        repository.createBoard(BoardDraft("Second"))
        val second = repository.observeWorkspace().first().boards.last()

        val redesigned = repository.designateExistingBoard(
            second.board.id,
            second.statuses.first { !it.isDoneStatus }.id,
            expectedGeneration = previous.generation,
        ) as LauncherExistingBoardDesignationResult.Designated

        assertNotEquals(previous.generation, redesigned.designation.generation)
        assertEquals(null, redesigned.designation.legacyCompatibility)
        assertFalse(
            repository.recordLegacyProjectionWritten(
                generation = previous.generation,
                boardId = previous.boardId,
                fingerprint = "c".repeat(64),
                expectedPreviousFingerprint = null,
            ),
        )
        assertFalse(repository.finishLegacyCompatibility(previous.generation, previous.boardId))
        assertEquals(redesigned.designation, repository.launcherBoardDesignation())
    }

    @Test
    fun redesignationHostSnapshotsNeverMixGenerationAndTargetBoard() = runBlocking {
        val previous = commitLauncherWorkspace()
        repository.createBoard(BoardDraft("Second"))
        val second = repository.observeWorkspace().first().boards.last()
        var nextGeneration: String? = null

        val emissions = captureHostSnapshotsAround(
            action = {
                val result = repository.designateExistingBoard(
                    second.board.id,
                    second.statuses.first { !it.isDoneStatus }.id,
                    expectedGeneration = previous.generation,
                ) as LauncherExistingBoardDesignationResult.Designated
                nextGeneration = result.designation.generation
            },
            finalState = { it.designation?.generation == nextGeneration },
        )

        emissions.forEach(::assertCoherentHostSnapshot)
        assertTrue(
            emissions.all { snapshot ->
                val designation = checkNotNull(snapshot.designation)
                (designation.generation == previous.generation &&
                    designation.boardId == previous.boardId) ||
                    (designation.generation == nextGeneration &&
                        designation.boardId == second.board.id)
            },
        )
    }

    @Test
    fun emptyWorkspaceIsPresentEmptyButPopulatedUnboundWorkspaceIsRejected() = runBlocking {
        val empty = repository.createLauncherBackupSnapshot("MinkLauncher test")
        assertTrue(empty is LauncherBackupCreationResult.Ready)
        assertEquals(
            emptyList<Any>(),
            (empty as LauncherBackupCreationResult.Ready).backup.workspace.boards,
        )
        assertEquals(null, empty.backup.designation)

        repository.ensureDefaultWorkspace()
        assertTrue(
            repository.createLauncherBackupSnapshot("MinkLauncher test") is
                LauncherBackupCreationResult.UnboundWorkspace,
        )
    }

    @Test
    fun mergeImportRemapsBindingAndCreatesFreshLocalCompatibilityMetadata() = runBlocking {
        val sourceBinding = commitLauncherWorkspace()
        val backup = readyBackup()
        val context = restoreContext()

        val imported = repository.importLauncherBackupSnapshot(
            plan = LauncherBackupRestorePlan.ImportWorkspace(
                workspace = backup.workspace,
                portableDesignation = checkNotNull(backup.designation),
            ),
            mode = WorkspaceImportMode.Merge,
            context = context,
        )

        val designation = imported.launcherDesignation
        assertNotEquals(sourceBinding.boardId, designation.boardId)
        assertEquals(
            imported.importResult.ids.boardIds.getValue(sourceBinding.boardId.value),
            designation.boardId,
        )
        assertEquals(
            imported.importResult.ids.statusIds.getValue(sourceBinding.intakeStatusId.value),
            designation.intakeStatusId,
        )
        assertEquals(
            LauncherTodoCompatibilityState.CANONICAL_COMPAT,
            designation.legacyCompatibility?.state,
        )
        assertEquals(
            context.localLegacyCompatibility?.sourceSchemaVersion,
            designation.legacyCompatibility?.sourceSchemaVersion,
        )
        assertEquals(
            context.localLegacyCompatibility?.sourceFingerprint,
            designation.legacyCompatibility?.sourceFingerprint,
        )
        assertEquals(null, designation.legacyCompatibility?.lastProjectionFingerprint)
        assertEquals(
            context.restoredAtEpochMillis,
            designation.legacyCompatibility?.migratedAtEpochMillis,
        )
        assertEquals(designation, repository.launcherBoardDesignation())
        assertEquals(2, repository.observeWorkspace().first().boards.size)
    }

    @Test
    fun restoringFreshDesignationDoesNotManufactureLegacyCompatibilityMetadata() = runBlocking {
        val source = repository.createAndDesignateFreshBoard(
            LauncherFreshBoardRequest("Fresh launcher board"),
        ) as LauncherFreshBoardDesignationResult.Created
        val backup = readyBackup()

        val restored = repository.importLauncherBackupSnapshot(
            plan = LauncherBackupRestorePlan.ImportWorkspace(
                workspace = backup.workspace,
                portableDesignation = checkNotNull(backup.designation),
            ),
            mode = WorkspaceImportMode.Replace,
            context = LauncherBackupRestoreContext(restoredAtEpochMillis = 88_000L),
        ).launcherDesignation

        assertEquals(source.designation.boardId, restored.boardId)
        assertEquals(source.designation.intakeStatusId, restored.intakeStatusId)
        assertNotEquals(source.designation.generation, restored.generation)
        assertEquals(88_000L, restored.designatedAtEpochMillis)
        assertEquals(null, restored.legacyCompatibility)
    }

    @Test
    fun replaceHostSnapshotsNeverMixRestoredGraphAndDesignationGeneration() = runBlocking {
        val previous = commitLauncherWorkspace()
        val backup = readyBackup()
        repository.renameBoard(previous.boardId, "Locally changed board")
        var restoredGeneration: String? = null

        val emissions = captureHostSnapshotsAround(
            action = {
                val restored = repository.importLauncherBackupSnapshot(
                    plan = LauncherBackupRestorePlan.ImportWorkspace(
                        workspace = backup.workspace,
                        portableDesignation = checkNotNull(backup.designation),
                    ),
                    mode = WorkspaceImportMode.Replace,
                    context = restoreContext(),
                )
                restoredGeneration = restored.launcherDesignation.generation
            },
            finalState = { it.designation?.generation == restoredGeneration },
        )

        emissions.forEach(::assertCoherentHostSnapshot)
        assertTrue(
            emissions.all { snapshot ->
                val designation = checkNotNull(snapshot.designation)
                val name = checkNotNull(snapshot.workspace.board(designation.boardId)).board.name
                (designation.generation == previous.generation && name == "Locally changed board") ||
                    (designation.generation == restoredGeneration && name == "To-Dos")
            },
        )
    }

    @Test
    fun directHostSnapshotReadsOneAggregateAfterReplaceAndRedesignation() = runBlocking {
        val original = commitLauncherWorkspace()
        val backup = readyBackup()
        repository.renameBoard(original.boardId, "Changed after backup")

        val restored = repository.importLauncherBackupSnapshot(
            plan = LauncherBackupRestorePlan.ImportWorkspace(
                workspace = backup.workspace,
                portableDesignation = checkNotNull(backup.designation),
            ),
            mode = WorkspaceImportMode.Replace,
            context = restoreContext(),
        ).launcherDesignation
        val afterReplace = repository.launcherBoardHostSnapshot()
        assertEquals(restored, afterReplace.designation)
        assertEquals("To-Dos", afterReplace.workspace.board(restored.boardId)?.board?.name)
        assertCoherentHostSnapshot(afterReplace)

        repository.createBoard(BoardDraft("Second"))
        val second = repository.observeWorkspace().first().boards.last()
        val redesigned = repository.designateExistingBoard(
            boardId = second.board.id,
            intakeStatusId = second.statuses.first { !it.isDoneStatus }.id,
            expectedGeneration = restored.generation,
        ) as LauncherExistingBoardDesignationResult.Designated
        val afterRedesignation = repository.launcherBoardHostSnapshot()
        assertEquals(redesigned.designation, afterRedesignation.designation)
        assertEquals(
            "Second",
            afterRedesignation.workspace.board(redesigned.designation.boardId)?.board?.name,
        )
        assertCoherentHostSnapshot(afterRedesignation)
    }

    @Test
    fun explicitlyEmptyRichImportCannotReplaceLocalWorkspaceOrBinding() = runBlocking {
        val beforeBinding = commitLauncherWorkspace()
        val beforeWorkspace = repository.observeWorkspace().first()
        val empty = PortableWorkspaceSnapshot(
            exportedAtEpochMillis = 1L,
            sourceApp = "Empty launcher backup",
            boards = emptyList(),
        )

        val failure = runCatching {
            repository.importLauncherBackupSnapshot(
                plan = LauncherBackupRestorePlan.ImportWorkspace(
                    workspace = empty,
                    portableDesignation = LauncherBoardBackupDesignation(
                        BoardId("missing-board"),
                        StatusId("missing-status"),
                    ),
                ),
                mode = WorkspaceImportMode.Replace,
                context = restoreContext(),
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals(beforeWorkspace, repository.observeWorkspace().first())
        assertEquals(beforeBinding, repository.launcherBoardDesignation())
    }

    @Test
    fun bindingPersistenceFailureRollsBackReplaceImportAndDeletion() = runBlocking {
        val beforeBinding = commitLauncherWorkspace()
        val beforeWorkspace = repository.observeWorkspace().first()
        val backup = readyBackup()
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_launcher_binding_insert
            BEFORE INSERT ON launcher_board_designation
            BEGIN
                SELECT RAISE(ABORT, 'injected binding insert failure');
            END
            """.trimIndent(),
        )
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_launcher_binding_update
            BEFORE UPDATE ON launcher_board_designation
            BEGIN
                SELECT RAISE(ABORT, 'injected binding update failure');
            END
            """.trimIndent(),
        )

        val failure = runCatching {
            repository.importLauncherBackupSnapshot(
                plan = LauncherBackupRestorePlan.ImportWorkspace(
                    workspace = backup.workspace,
                    portableDesignation = checkNotNull(backup.designation),
                ),
                mode = WorkspaceImportMode.Replace,
                context = restoreContext(),
            )
        }.exceptionOrNull()

        assertTrue(failure != null)
        assertEquals(beforeWorkspace, repository.observeWorkspace().first())
        assertEquals(beforeBinding, repository.launcherBoardDesignation())
    }

    @Test
    fun replaceWithPreservedIdsRejectsProjectionTokenFromPreviousBindingGeneration() = runBlocking {
        val previousBinding = commitLauncherWorkspace()
        val backup = readyBackup()

        val imported = repository.importLauncherBackupSnapshot(
            plan = LauncherBackupRestorePlan.ImportWorkspace(
                workspace = backup.workspace,
                portableDesignation = checkNotNull(backup.designation),
            ),
            mode = WorkspaceImportMode.Replace,
            context = restoreContext(),
        )

        assertEquals(previousBinding.boardId, imported.launcherDesignation.boardId)
        assertNotEquals(previousBinding.generation, imported.launcherDesignation.generation)
        assertFalse(
            repository.recordLegacyProjectionWritten(
                generation = previousBinding.generation,
                boardId = previousBinding.boardId,
                fingerprint = "c".repeat(64),
                expectedPreviousFingerprint = null,
            ),
        )
        assertEquals(
            null,
            repository.launcherBoardDesignation()?.legacyCompatibility
                ?.lastProjectionFingerprint,
        )
        assertFalse(
            repository.finishLegacyCompatibility(
                generation = previousBinding.generation,
                boardId = previousBinding.boardId,
            ),
        )
        assertEquals(
            LauncherTodoCompatibilityState.CANONICAL_COMPAT,
            repository.launcherBoardDesignation()?.legacyCompatibility?.state,
        )
    }

    private suspend fun captureHostSnapshotsAround(
        action: suspend () -> Unit,
        finalState: (LauncherBoardHostSnapshot) -> Boolean,
    ): List<LauncherBoardHostSnapshot> = coroutineScope {
        val emissions = Collections.synchronizedList(mutableListOf<LauncherBoardHostSnapshot>())
        val initialEmission = CompletableDeferred<Unit>()
        val collector = launch(Dispatchers.Default) {
            repository.observeLauncherBoardHostSnapshot().collect { snapshot ->
                emissions += snapshot
                if (!initialEmission.isCompleted) initialEmission.complete(Unit)
            }
        }
        withTimeout(5_000L) { initialEmission.await() }
        action()
        withTimeout(5_000L) {
            while (synchronized(emissions) { emissions.none(finalState) }) delay(10L)
        }
        collector.cancelAndJoin()
        synchronized(emissions) { emissions.toList() }
    }

    private fun assertCoherentHostSnapshot(snapshot: LauncherBoardHostSnapshot) {
        val designation = snapshot.designation ?: return
        val board = snapshot.workspace.board(designation.boardId)
        assertTrue("Designated board must exist in the same snapshot", board != null)
        val intake = board!!.statuses.firstOrNull { it.id == designation.intakeStatusId }
        assertTrue("Designated intake must exist in the same snapshot", intake != null)
        assertFalse("Designated intake cannot be Done", intake!!.isDoneStatus)
    }

    private suspend fun commitLauncherWorkspace() = repository.commitLauncherTodoMigration(
        (
            LauncherTodoNormalizer(
                idFactory = IdFactory { "source-${ids.incrementAndGet()}" },
                clockMillis = { 30_000L },
            ).plan(
                LegacyTodoSource.Available(
                    listOf(LegacyLauncherTodo("legacy-card", "Portable To-Do", false)),
                ),
                sourceSchemaVersion = 2,
            ) as LauncherTodoMigrationOutcome.Ready
            ).plan,
    )

    private suspend fun readyBackup(): LauncherMinkBoardBackup.Present =
        (repository.createLauncherBackupSnapshot("MinkLauncher test") as
            LauncherBackupCreationResult.Ready).backup

    private fun restoreContext() = LauncherBackupRestoreContext(
        restoredAtEpochMillis = 77_000L,
        localLegacyCompatibility = LauncherLocalLegacyCompatibility(
            sourceSchemaVersion = 7,
            sourceFingerprint = "b".repeat(64),
        ),
    )
}

private class LauncherArchiveTestCipher : FieldCipher {
    override fun encrypt(value: String, subject: String, field: String): ByteArray =
        "$subject\u0000$field\u0000$value".toByteArray(Charsets.UTF_8)

    override fun decrypt(value: ByteArray, subject: String, field: String): String {
        val prefix = "$subject\u0000$field\u0000"
        val decoded = value.toString(Charsets.UTF_8)
        require(decoded.startsWith(prefix)) { "Ciphertext context mismatch" }
        return decoded.removePrefix(prefix)
    }
}
