package com.katoaapps.minkboard.core

import com.katoaapps.minkboard.model.Board
import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardDraft
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.BoardUpdate
import com.katoaapps.minkboard.model.CardDraft
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.CardUpdate
import com.katoaapps.minkboard.model.MinkBoardWorkspace
import com.katoaapps.minkboard.model.PortableBoardSnapshot
import com.katoaapps.minkboard.model.PortableStatusSnapshot
import com.katoaapps.minkboard.model.PortableWorkspaceSnapshot
import com.katoaapps.minkboard.model.StatusDeletionPolicy
import com.katoaapps.minkboard.model.StatusDraft
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.minkboard.model.StatusUpdate
import com.katoaapps.minkboard.model.SubtaskId
import java.util.ArrayDeque
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MinkBoardLauncherFacadeTest {
    @Test
    fun observationResolvesSummaryAndReportsBrokenBindings() = runBlocking {
        val fixture = fixture()

        assertEquals(LauncherBoardState.Unbound, fixture.facade.observeLauncherBoard().first())

        fixture.integration.designation.value = fixture.designation
        fixture.integration.publishHostSnapshot()
        val ready = fixture.facade.observeLauncherBoard().first() as LauncherBoardState.Ready
        assertEquals("To-Dos", ready.summary.boardName)
        assertEquals(1, ready.summary.intakeCardCount)
        assertEquals(1, ready.summary.unfinishedCardCount)
        assertEquals(1, ready.summary.completedCardCount)
        assertEquals(2, ready.summary.totalCardCount)

        fixture.repository.workspace.value = MinkBoardWorkspace.Empty
        fixture.integration.publishHostSnapshot()
        val broken = fixture.facade.observeLauncherBoard().first() as LauncherBoardState.BrokenBinding
        assertEquals(LauncherBindingProblem.BOARD_MISSING, broken.problem)
    }

    @Test
    fun observationUsesOnlyTheTransactionStableAggregateSnapshot() = runBlocking {
        val fixture = fixture()
        val committedWorkspace = fixture.repository.workspace.value

        // Deliberately disagree with the aggregate's two halves. If the facade still combined the
        // repository and designation flows, either half could leak into the observed state.
        fixture.repository.workspace.value = MinkBoardWorkspace.Empty
        fixture.integration.designation.value = null
        fixture.integration.hostSnapshot.value = LauncherBoardHostSnapshot(
            workspace = committedWorkspace,
            designation = fixture.designation,
        )
        val ready = fixture.facade.observeLauncherBoard().first() as LauncherBoardState.Ready
        assertEquals(fixture.designation, ready.designation)

        fixture.repository.workspace.value = committedWorkspace
        fixture.integration.designation.value = fixture.designation
        fixture.integration.hostSnapshot.value = LauncherBoardHostSnapshot(
            workspace = MinkBoardWorkspace.Empty,
            designation = null,
        )
        assertEquals(LauncherBoardState.Unbound, fixture.facade.observeLauncherBoard().first())
    }

    @Test
    fun projectionPlanningCannotMixAnOldGraphWithANewDesignationGeneration() = runBlocking {
        val fixture = fixture()
        val oldWorkspace = fixture.repository.workspace.value
        val newWorkspace = MinkBoardWorkspace(
            oldWorkspace.boards.map { board ->
                board.copy(
                    cards = board.cards.map { card ->
                        if (card.id == CardId("todo-card")) {
                            card.copy(title = "New canonical title", updatedAtEpochMillis = 2L)
                        } else {
                            card
                        }
                    },
                )
            },
        )
        val newDesignation = fixture.designation.copy(
            generation = "new-generation",
            designatedAtEpochMillis = 2L,
        )

        // These direct values deliberately form the unsafe old-graph/new-generation pair. Only
        // the transaction-stable aggregate contains the graph that belongs to the new generation.
        fixture.repository.workspace.value = oldWorkspace
        fixture.integration.designation.value = newDesignation
        fixture.integration.hostSnapshot.value = LauncherBoardHostSnapshot(
            workspace = newWorkspace,
            designation = newDesignation,
        )

        val result = fixture.facade.planLegacyProjection(emptyList())
            as LauncherLegacyProjectionState.Ready
        val expectedProjection = fixture.normalizer.project(newWorkspace.boards.single())
        val unsafeProjection = fixture.normalizer.project(oldWorkspace.boards.single())

        assertEquals("new-generation", result.receiptToken.bindingGeneration)
        assertEquals(expectedProjection, result.projection)
        assertEquals(expectedProjection.fingerprint, result.receiptToken.fingerprint)
        assertTrue(result.projection.fingerprint != unsafeProjection.fingerprint)
    }

    @Test
    fun freshCreationIsIdempotentAndHasNoFakeLegacyCompatibility() = runBlocking {
        val fixture = fixture(MinkBoardWorkspace.Empty)

        val first = fixture.facade.createAndDesignateFreshBoard(
            LauncherFreshBoardRequest("My launcher board"),
        ) as LauncherFreshBoardDesignationResult.Created

        assertEquals("My launcher board", fixture.repository.workspace.value.boards.single().board.name)
        assertEquals(2L, first.designation.designatedAtEpochMillis)
        assertEquals(null, first.designation.legacyCompatibility)
        assertEquals(
            LauncherLegacyProjectionState.NotApplicable,
            fixture.facade.planLegacyProjection(emptyList()),
        )
        assertEquals(
            LauncherBindingMutationResult.CompatibilityNotApplicable,
            fixture.facade.finishLegacyCompatibility(),
        )

        val repeated = fixture.facade.createAndDesignateFreshBoard(
            LauncherFreshBoardRequest("Must not create another board"),
        ) as LauncherFreshBoardDesignationResult.AlreadyDesignated
        assertEquals(first.designation, repeated.designation)
        assertEquals(1, fixture.repository.workspace.value.boards.size)
    }

    @Test
    fun explicitExistingDesignationRejectsDoneAndKeepsTheFirstDesignation() = runBlocking {
        val fixture = fixture()

        assertEquals(
            LauncherExistingBoardDesignationResult.InvalidTarget(
                LauncherBindingProblem.INTAKE_STATUS_IS_DONE,
            ),
            fixture.facade.designateExistingBoard(
                BoardId("launcher-board"),
                StatusId("done"),
                expectedGeneration = null,
            ),
        )

        val designated = fixture.facade.designateExistingBoard(
            BoardId("launcher-board"),
            StatusId("todo"),
            expectedGeneration = null,
        ) as LauncherExistingBoardDesignationResult.Designated
        assertEquals(null, designated.designation.legacyCompatibility)

        val repeated = fixture.facade.designateExistingBoard(
            BoardId("launcher-board"),
            StatusId("todo"),
            expectedGeneration = "stale-but-same-target",
        ) as LauncherExistingBoardDesignationResult.AlreadyDesignated
        assertEquals(designated.designation, repeated.designation)
    }

    @Test
    fun explicitRedesignationRejectsStaleGenerationAndInvalidatesOldProjectionToken() = runBlocking {
        val fixture = fixture(workspaceWithSecondBoard())
        fixture.integration.designation.value = fixture.designation
        fixture.integration.publishHostSnapshot()
        val oldProjection = fixture.facade.planLegacyProjection(
            listOf(
                LegacyLauncherTodo("todo-card", "Open", false),
                LegacyLauncherTodo("done-card", "Closed", true),
            ),
        ) as LauncherLegacyProjectionState.Ready

        assertEquals(
            LauncherExistingBoardDesignationResult.StaleDesignation(fixture.designation),
            fixture.facade.designateExistingBoard(
                BoardId("second-board"),
                StatusId("second-todo"),
                expectedGeneration = "stale-generation",
            ),
        )
        assertEquals(fixture.designation, fixture.integration.designation.value)

        val replaced = fixture.facade.designateExistingBoard(
            BoardId("second-board"),
            StatusId("second-todo"),
            expectedGeneration = fixture.designation.generation,
        ) as LauncherExistingBoardDesignationResult.Designated
        assertTrue(replaced.designation.generation != fixture.designation.generation)
        assertEquals(null, replaced.designation.legacyCompatibility)
        assertEquals(
            LauncherBindingMutationResult.StaleProjectionToken,
            fixture.facade.recordLegacyProjectionWritten(oldProjection.receiptToken),
        )
    }

    @Test
    fun explicitDesignationRepairsABrokenExistingDesignationWithMatchingGeneration() = runBlocking {
        val fixture = fixture()
        val broken = fixture.designation.copy(boardId = BoardId("deleted-board"))
        fixture.integration.designation.value = broken
        fixture.integration.publishHostSnapshot()

        assertTrue(fixture.facade.observeLauncherBoard().first() is LauncherBoardState.BrokenBinding)
        val repaired = fixture.facade.designateExistingBoard(
            BoardId("launcher-board"),
            StatusId("todo"),
            expectedGeneration = broken.generation,
        ) as LauncherExistingBoardDesignationResult.Designated

        assertTrue(repaired.designation.generation != broken.generation)
        assertEquals(null, repaired.designation.legacyCompatibility)
        assertTrue(fixture.facade.observeLauncherBoard().first() is LauncherBoardState.Ready)
    }

    @Test
    fun normalizationIsIdempotentAndNeverConvertsMissingOrCorruptToEmpty() = runBlocking {
        val missing = fixture(MinkBoardWorkspace.Empty)
        assertEquals(
            LauncherNormalizationResult.NoLegacyData,
            missing.facade.normalizeLegacyTodos(LegacyTodoSource.Missing, 1),
        )
        assertEquals(
            LauncherNormalizationResult.CorruptLegacyData,
            missing.facade.normalizeLegacyTodos(LegacyTodoSource.Corrupt, 1),
        )
        assertEquals(0, missing.integration.commitCount)

        val source = LegacyTodoSource.Available(
            listOf(LegacyLauncherTodo(UUID_ONE, "Milk", completed = false)),
        )
        val migrated = missing.facade.normalizeLegacyTodos(source, 1)
        assertTrue(migrated is LauncherNormalizationResult.Migrated)
        assertEquals(1, missing.integration.commitCount)

        val repeated = missing.facade.normalizeLegacyTodos(source, 1)
        assertTrue(repeated is LauncherNormalizationResult.AlreadyNormalized)
        assertEquals(1, missing.integration.commitCount)
    }

    @Test
    fun normalizationValidatesTheAtomicPostCommitOwnerInsteadOfTheReturnedDesignation() = runBlocking {
        val fixture = fixture(MinkBoardWorkspace.Empty)
        val replacement = fixture.designation.copy(
            generation = "replacement-generation",
            designatedAtEpochMillis = 9L,
            legacyCompatibility = null,
        )
        fixture.integration.hostSnapshotAfterMigrationCommit = LauncherBoardHostSnapshot(
            workspace = workspace(),
            designation = replacement,
        )

        val result = fixture.facade.normalizeLegacyTodos(
            LegacyTodoSource.Available(
                listOf(LegacyLauncherTodo(UUID_ONE, "Milk", completed = false)),
            ),
            sourceSchemaVersion = 1,
        ) as LauncherNormalizationResult.AlreadyNormalized

        assertEquals(replacement, result.designation)
    }

    @Test
    fun magicBoxAddsOnlyToTheBoundIntakeStatus() = runBlocking {
        val fixture = fixture()

        assertEquals(LauncherCardAddResult.Unbound, fixture.facade.addToIntake("Milk"))
        fixture.integration.designation.value = fixture.designation

        val result = fixture.facade.addToIntake("Milk") as LauncherCardAddResult.Added
        assertEquals(CardId("added-card"), result.cardId)
        assertEquals(fixture.designation.intakeStatusId, fixture.integration.lastAddedStatus)
        assertEquals(CardDraft("Milk"), fixture.integration.lastAddedDraft)
        assertEquals(LauncherCardAddResult.InvalidText, fixture.facade.addToIntake("   "))

        fixture.facade.addToIntake("Title\nDetails that must not be dropped")
        assertEquals(
            CardDraft("Title", "\nDetails that must not be dropped"),
            fixture.integration.lastAddedDraft,
        )

        fixture.repository.workspace.value = MinkBoardWorkspace.Empty
        assertEquals(
            LauncherCardAddResult.BrokenBinding(LauncherBindingProblem.BOARD_MISSING),
            fixture.facade.addToIntake("Cannot be misrouted"),
        )
    }

    @Test
    fun projectionMakesDivergenceAndFinalCompatibilityExplicit() = runBlocking {
        val fixture = fixture()
        val source = listOf(
            LegacyLauncherTodo("todo-card", "Open", false),
            LegacyLauncherTodo("done-card", "Closed", true),
        )
        fixture.integration.designation.value = fixture.designation.copy(
            legacyCompatibility = requireNotNull(fixture.designation.legacyCompatibility).copy(
                sourceFingerprint = fixture.normalizer.fingerprint(source),
            ),
        )
        fixture.integration.publishHostSnapshot()

        val first = fixture.facade.planLegacyProjection(source) as LauncherLegacyProjectionState.Ready
        assertEquals(LegacyProjectionDecision.ADOPT_CANONICAL_PROJECTION, first.decision)

        val changedByOldBuild = listOf(LegacyLauncherTodo("old", "Changed", false))
        val diverged = fixture.facade.planLegacyProjection(changedByOldBuild)
            as LauncherLegacyProjectionState.Ready
        assertEquals(LegacyProjectionDecision.DIVERGED, diverged.decision)

        val current = requireNotNull(fixture.integration.designation.value)
        fixture.integration.designation.value = current.copy(
            legacyCompatibility = requireNotNull(current.legacyCompatibility).copy(
                state = LauncherTodoCompatibilityState.CANONICAL_FINAL,
            ),
        )
        fixture.integration.publishHostSnapshot()
        assertEquals(
            LauncherLegacyProjectionState.CompatibilityComplete,
            fixture.facade.planLegacyProjection(source),
        )
    }

    @Test
    fun projectionReceiptAndFinalizationUseTheBoundBoard() = runBlocking {
        val fixture = fixture()
        fixture.integration.designation.value = fixture.designation
        fixture.integration.publishHostSnapshot()
        val projection = fixture.facade.planLegacyProjection(
            listOf(
                LegacyLauncherTodo("todo-card", "Open", false),
                LegacyLauncherTodo("done-card", "Closed", true),
            ),
        ) as LauncherLegacyProjectionState.Ready

        assertEquals(
            LauncherBindingMutationResult.Updated,
            fixture.facade.recordLegacyProjectionWritten(projection.receiptToken),
        )
        assertEquals(fixture.designation.boardId, fixture.integration.projectionBoardId)
        assertEquals(projection.projection.fingerprint, fixture.integration.projectionFingerprint)

        assertEquals(
            LauncherBindingMutationResult.StaleProjectionToken,
            fixture.facade.recordLegacyProjectionWritten(projection.receiptToken),
        )
        assertEquals(projection.projection.fingerprint, fixture.integration.projectionFingerprint)

        assertEquals(
            LauncherBindingMutationResult.Updated,
            fixture.facade.finishLegacyCompatibility(),
        )
        assertEquals(fixture.designation.boardId, fixture.integration.finishedBoardId)

        fixture.integration.finishSucceeds = false
        assertEquals(
            LauncherBindingMutationResult.StaleBindingGeneration,
            fixture.facade.finishLegacyCompatibility(),
        )
    }

    @Test
    fun backupReconciliationPreservesAbsentEmptyAndCorruptSemantics() {
        assertEquals(
            LauncherBackupRestorePlan.LeaveDestinationUntouched,
            LauncherBackupReconciler.plan(
                LauncherMinkBoardBackup.Absent,
                LegacyTodoSource.Missing,
            ),
        )
        assertEquals(
            LauncherBackupRestorePlan.CorruptLegacyData,
            LauncherBackupReconciler.plan(
                LauncherMinkBoardBackup.Absent,
                LegacyTodoSource.Corrupt,
            ),
        )
        assertTrue(
            LauncherBackupReconciler.plan(
                LauncherMinkBoardBackup.Absent,
                LegacyTodoSource.Available(emptyList()),
            ) is LauncherBackupRestorePlan.NormalizeLegacy,
        )
        assertEquals(
            LauncherBackupRestorePlan.ExplicitlyEmptyWorkspace,
            LauncherBackupReconciler.plan(
                LauncherMinkBoardBackup.Present(emptyPortableWorkspace(), null),
                LegacyTodoSource.Available(
                    listOf(LegacyLauncherTodo("legacy", "Must be ignored", false)),
                ),
            ),
        )
        assertEquals(
            LauncherBackupRestorePlan.InvalidRichBackup,
            LauncherBackupReconciler.plan(
                LauncherMinkBoardBackup.Present(portableWorkspace(), designation = null),
                LegacyTodoSource.Missing,
            ),
        )
        assertEquals(
            LauncherBackupRestorePlan.InvalidRichBackup,
            LauncherBackupReconciler.plan(
                LauncherMinkBoardBackup.Present(
                    portableWorkspace(),
                    LauncherBoardBackupDesignation(
                        boardId = BoardId("missing"),
                        intakeStatusId = StatusId("portable-status"),
                    ),
                ),
                LegacyTodoSource.Missing,
            ),
        )
    }

    @Test
    fun richBackupImportRemapsTheDesignatedLauncherBoard() = runBlocking {
        val fixture = fixture()
        val snapshot = portableWorkspace()
        val portableDesignation = LauncherBoardBackupDesignation(
            boardId = BoardId("portable-board"),
            intakeStatusId = StatusId("portable-status"),
        )
        val plan = LauncherBackupRestorePlan.ImportWorkspace(snapshot, portableDesignation)
        val restoreContext = LauncherBackupRestoreContext(
            restoredAtEpochMillis = 9_876L,
            localLegacyCompatibility = LauncherLocalLegacyCompatibility(
                sourceSchemaVersion = 4,
                sourceFingerprint = "7".repeat(64),
            ),
        )

        val outcome = fixture.facade.importRichBackup(
            plan,
            WorkspaceImportMode.Merge,
            restoreContext,
        )

        assertEquals(WorkspaceImportMode.Merge, fixture.archive.lastImportMode)
        assertEquals(snapshot, fixture.archive.lastImportedSnapshot)
        assertEquals(
            LauncherBoardDesignation(
                generation = "restored-generation",
                boardId = BoardId("local-board"),
                intakeStatusId = StatusId("local-status"),
                designatedAtEpochMillis = restoreContext.restoredAtEpochMillis,
                legacyCompatibility = LauncherLegacyCompatibilityReceipt(
                    state = LauncherTodoCompatibilityState.CANONICAL_COMPAT,
                    sourceSchemaVersion = 4,
                    sourceFingerprint = "7".repeat(64),
                    lastProjectionFingerprint = null,
                    migratedAtEpochMillis = restoreContext.restoredAtEpochMillis,
                ),
            ),
            outcome.launcherDesignation,
        )
    }

    @Test
    fun richBackupImportWithoutALocalLegacySourceDoesNotInventAReceipt() = runBlocking {
        val fixture = fixture()
        val outcome = fixture.facade.importRichBackup(
            LauncherBackupRestorePlan.ImportWorkspace(
                portableWorkspace(),
                LauncherBoardBackupDesignation(
                    BoardId("portable-board"),
                    StatusId("portable-status"),
                ),
            ),
            WorkspaceImportMode.Replace,
            LauncherBackupRestoreContext(restoredAtEpochMillis = 4_321L),
        )

        assertEquals(4_321L, outcome.launcherDesignation.designatedAtEpochMillis)
        assertEquals(null, outcome.launcherDesignation.legacyCompatibility)
    }

    @Test
    fun inMemoryBackupIncludesOnlyAValidDesignatedBoard() = runBlocking {
        val fixture = fixture()
        fixture.archive.exportSnapshot = portableWorkspace()

        assertEquals(
            LauncherBackupCreationResult.UnboundWorkspace,
            fixture.facade.createInMemoryBackup("launcher"),
        )

        fixture.integration.designation.value = fixture.designation
        val missingDesignation = fixture.facade.createInMemoryBackup("launcher")
            as LauncherBackupCreationResult.BrokenBinding
        assertEquals(LauncherBindingProblem.BOARD_MISSING, missingDesignation.problem)

        fixture.archive.exportSnapshot = portableWorkspace(
            boardId = fixture.designation.boardId.value,
            statusId = fixture.designation.intakeStatusId.value,
        )
        val designated = fixture.facade.createInMemoryBackup("launcher")
            as LauncherBackupCreationResult.Ready
        assertEquals(
            LauncherBoardBackupDesignation(
                boardId = fixture.designation.boardId,
                intakeStatusId = fixture.designation.intakeStatusId,
            ),
            designated.backup.designation,
        )
    }

    @Test
    fun encryptedBackupCreationUsesOnlyAReadyInMemoryBackup() = runBlocking {
        val fixture = fixture()
        val codec = FakeLauncherBackupEnvelopeCodec()
        val workflow = MinkBoardLauncherBackupWorkflow(fixture.facade, codec)
        val secret = "launcher recovery secret".toCharArray()

        assertEquals(
            LauncherEncryptedBackupCreationResult.UnboundWorkspace,
            workflow.createEncryptedBackup("OpenMink", secret),
        )
        assertEquals(null, codec.encodedBackup)

        fixture.integration.designation.value = fixture.designation
        assertEquals(
            LauncherEncryptedBackupCreationResult.BrokenBinding(
                LauncherBindingProblem.BOARD_MISSING,
            ),
            workflow.createEncryptedBackup("OpenMink", secret),
        )
        assertEquals(null, codec.encodedBackup)

        fixture.archive.exportSnapshot = portableWorkspace(
            boardId = fixture.designation.boardId.value,
            statusId = fixture.designation.intakeStatusId.value,
        )
        codec.encodeResult = LauncherBackupEnvelopeEncodeResult.Encrypted(byteArrayOf(4, 2))

        val ready = workflow.createEncryptedBackup("OpenMink", secret)
            as LauncherEncryptedBackupCreationResult.Ready
        assertArrayEquals(byteArrayOf(4, 2), ready.envelope)
        assertEquals("OpenMink", requireNotNull(codec.encodedBackup).workspace.sourceApp)
        assertArrayEquals(secret.concatToString().encodeToByteArray(), codec.encodedSecret)

        codec.encodeResult = LauncherBackupEnvelopeEncodeResult.Failed
        assertEquals(
            LauncherEncryptedBackupCreationResult.EncryptionFailed,
            workflow.createEncryptedBackup("OpenMink", secret),
        )
    }

    @Test
    fun onlyAMissingOuterFieldFallsBackToLegacyData() {
        val fixture = fixture(MinkBoardWorkspace.Empty)
        val codec = FakeLauncherBackupEnvelopeCodec()
        val workflow = MinkBoardLauncherBackupWorkflow(fixture.facade, codec)
        val legacy = LegacyTodoSource.Available(
            listOf(LegacyLauncherTodo("legacy", "Keep me", false)),
        )

        val missing = workflow.prepareRestore(
            LauncherMinkBoardBackupField.Missing,
            secret = null,
            legacyTodos = legacy,
        ) as LauncherBackupPreparationResult.Prepared
        assertTrue(missing.plan is LauncherBackupRestorePlan.NormalizeLegacy)
        assertEquals(0, codec.decodeCalls)

        assertEquals(
            LauncherBackupPreparationResult.MissingSecret,
            workflow.prepareRestore(
                LauncherMinkBoardBackupField.Encrypted(byteArrayOf(5)),
                secret = null,
                legacyTodos = legacy,
            ),
        )
        assertEquals(0, codec.decodeCalls)

        codec.decodeResult = LauncherBackupEnvelopeDecodeResult.Corrupt
        assertEquals(
            LauncherBackupPreparationResult.CorruptEnvelope,
            workflow.prepareRestore(
                LauncherMinkBoardBackupField.Encrypted(byteArrayOf()),
                "secret".toCharArray(),
                legacy,
            ),
        )
        assertEquals(1, codec.decodeCalls)

        codec.decodeResult = LauncherBackupEnvelopeDecodeResult.Unsupported
        assertEquals(
            LauncherBackupPreparationResult.UnsupportedEnvelope,
            workflow.prepareRestore(
                LauncherMinkBoardBackupField.Encrypted(byteArrayOf(1)),
                "secret".toCharArray(),
                legacy,
            ),
        )
        assertEquals(2, codec.decodeCalls)
    }

    @Test
    fun missingFieldPreservesAbsentEmptyAndCorruptLegacySemantics() {
        val workflow = MinkBoardLauncherBackupWorkflow(
            fixture().facade,
            FakeLauncherBackupEnvelopeCodec(),
        )

        val untouched = workflow.prepareRestore(
            LauncherMinkBoardBackupField.Missing,
            secret = null,
            legacyTodos = LegacyTodoSource.Missing,
        ) as LauncherBackupPreparationResult.Prepared
        assertEquals(LauncherBackupRestorePlan.LeaveDestinationUntouched, untouched.plan)

        val emptyLegacy = workflow.prepareRestore(
            LauncherMinkBoardBackupField.Missing,
            secret = null,
            legacyTodos = LegacyTodoSource.Available(emptyList()),
        ) as LauncherBackupPreparationResult.Prepared
        assertTrue(emptyLegacy.plan is LauncherBackupRestorePlan.NormalizeLegacy)

        assertEquals(
            LauncherBackupPreparationResult.CorruptLegacyData,
            workflow.prepareRestore(
                LauncherMinkBoardBackupField.Missing,
                secret = null,
                legacyTodos = LegacyTodoSource.Corrupt,
            ),
        )
    }

    @Test
    fun decodedRichBackupWinsOverLegacyAndPresentEmptyIsExplicit() {
        val fixture = fixture()
        val codec = FakeLauncherBackupEnvelopeCodec()
        val workflow = MinkBoardLauncherBackupWorkflow(fixture.facade, codec)
        val legacy = LegacyTodoSource.Available(
            listOf(LegacyLauncherTodo("legacy", "Must not win", false)),
        )
        codec.decodeResult = LauncherBackupEnvelopeDecodeResult.Decoded(
            LauncherMinkBoardBackup.Present(
                portableWorkspace(),
                LauncherBoardBackupDesignation(
                    BoardId("portable-board"),
                    StatusId("portable-status"),
                ),
            ),
        )

        val rich = workflow.prepareRestore(
            LauncherMinkBoardBackupField.Encrypted(byteArrayOf(9)),
            "secret".toCharArray(),
            legacy,
        ) as LauncherBackupPreparationResult.Prepared
        assertTrue(rich.plan is LauncherBackupRestorePlan.ImportWorkspace)

        codec.decodeResult = LauncherBackupEnvelopeDecodeResult.Decoded(
            LauncherMinkBoardBackup.Present(emptyPortableWorkspace(), designation = null),
        )
        val empty = workflow.prepareRestore(
            LauncherMinkBoardBackupField.Encrypted(byteArrayOf(8)),
            "secret".toCharArray(),
            legacy,
        ) as LauncherBackupPreparationResult.Prepared
        assertEquals(LauncherBackupRestorePlan.ExplicitlyEmptyWorkspace, empty.plan)
    }

    @Test
    fun decodedInvalidRichBackupIsRejectedBeforeApply() {
        val fixture = fixture()
        val codec = FakeLauncherBackupEnvelopeCodec()
        codec.decodeResult = LauncherBackupEnvelopeDecodeResult.Decoded(
            LauncherMinkBoardBackup.Present(
                portableWorkspace(),
                designation = null,
            ),
        )
        val workflow = MinkBoardLauncherBackupWorkflow(fixture.facade, codec)

        assertEquals(
            LauncherBackupPreparationResult.InvalidRichBackup,
            workflow.prepareRestore(
                LauncherMinkBoardBackupField.Encrypted(byteArrayOf(7)),
                "secret".toCharArray(),
                LegacyTodoSource.Available(
                    listOf(LegacyLauncherTodo("legacy", "Must not be imported", false)),
                ),
            ),
        )
        assertEquals(null, fixture.archive.lastImportedSnapshot)
    }

    @Test
    fun applyNoOpsAndRejectedPlansNeverMutateTheDestination() = runBlocking {
        val fixture = fixture()
        val workflow = MinkBoardLauncherBackupWorkflow(
            fixture.facade,
            FakeLauncherBackupEnvelopeCodec(),
        )
        val before = fixture.repository.workspace.value
        val context = LauncherBackupRestoreContext(restoredAtEpochMillis = 10L)

        assertEquals(
            LauncherBackupApplyResult.LeftDestinationUntouched,
            workflow.apply(
                LauncherBackupPreparationResult.Prepared(
                    LauncherBackupRestorePlan.LeaveDestinationUntouched,
                ),
                WorkspaceImportMode.Replace,
                context,
            ),
        )
        assertEquals(
            LauncherBackupApplyResult.ExplicitlyEmptyWorkspace,
            workflow.apply(
                LauncherBackupPreparationResult.Prepared(
                    LauncherBackupRestorePlan.ExplicitlyEmptyWorkspace,
                ),
                WorkspaceImportMode.Replace,
                context,
            ),
        )
        assertEquals(
            LauncherBackupApplyResult.RejectedInvalidRichBackup,
            workflow.apply(
                LauncherBackupPreparationResult.Prepared(
                    LauncherBackupRestorePlan.InvalidRichBackup,
                ),
                WorkspaceImportMode.Replace,
                context,
            ),
        )
        assertEquals(
            LauncherBackupApplyResult.RejectedCorruptLegacyData,
            workflow.apply(
                LauncherBackupPreparationResult.Prepared(
                    LauncherBackupRestorePlan.CorruptLegacyData,
                ),
                WorkspaceImportMode.Replace,
                context,
            ),
        )
        assertEquals(before, fixture.repository.workspace.value)
        assertEquals(null, fixture.archive.lastImportedSnapshot)
    }

    @Test
    fun legacyApplyRequiresTheHostSchemaVersionAndThenNormalizes() = runBlocking {
        val fixture = fixture(MinkBoardWorkspace.Empty)
        val workflow = MinkBoardLauncherBackupWorkflow(
            fixture.facade,
            FakeLauncherBackupEnvelopeCodec(),
        )
        val prepared = LauncherBackupPreparationResult.Prepared(
            LauncherBackupRestorePlan.NormalizeLegacy(
                LegacyTodoSource.Available(
                    listOf(LegacyLauncherTodo("legacy", "Migrate me", false)),
                ),
            ),
        )
        val context = LauncherBackupRestoreContext(restoredAtEpochMillis = 10L)

        assertEquals(
            LauncherBackupApplyResult.MissingLegacySchemaVersion,
            workflow.apply(prepared, WorkspaceImportMode.Merge, context),
        )
        assertEquals(MinkBoardWorkspace.Empty, fixture.repository.workspace.value)
        assertEquals(
            LauncherBackupApplyResult.MissingLegacySchemaVersion,
            workflow.apply(
                prepared,
                WorkspaceImportMode.Merge,
                context,
                legacySourceSchemaVersion = 0,
            ),
        )
        assertEquals(MinkBoardWorkspace.Empty, fixture.repository.workspace.value)

        val applied = workflow.apply(
            prepared,
            WorkspaceImportMode.Merge,
            context,
            legacySourceSchemaVersion = 7,
        ) as LauncherBackupApplyResult.LegacyNormalized
        val migrated = applied.result as LauncherNormalizationResult.Migrated
        assertEquals(7, requireNotNull(migrated.designation.legacyCompatibility).sourceSchemaVersion)
        assertEquals("Migrate me", fixture.repository.workspace.value.boards.single().cards.single().title)
    }

    @Test
    fun richApplyUsesTheExistingAtomicImportBoundary() = runBlocking {
        val fixture = fixture()
        val workflow = MinkBoardLauncherBackupWorkflow(
            fixture.facade,
            FakeLauncherBackupEnvelopeCodec(),
        )
        val snapshot = portableWorkspace()
        val prepared = LauncherBackupPreparationResult.Prepared(
            LauncherBackupRestorePlan.ImportWorkspace(
                snapshot,
                LauncherBoardBackupDesignation(
                    BoardId("portable-board"),
                    StatusId("portable-status"),
                ),
            ),
        )
        val context = LauncherBackupRestoreContext(restoredAtEpochMillis = 22L)

        val applied = workflow.apply(
            prepared,
            WorkspaceImportMode.Replace,
            context,
        ) as LauncherBackupApplyResult.WorkspaceImported

        assertEquals(snapshot, fixture.archive.lastImportedSnapshot)
        assertEquals(WorkspaceImportMode.Replace, fixture.archive.lastImportMode)
        assertEquals(22L, applied.outcome.launcherDesignation.designatedAtEpochMillis)
        assertEquals(null, applied.outcome.launcherDesignation.legacyCompatibility)
    }

    private fun fixture(
        workspace: MinkBoardWorkspace = workspace(),
    ): Fixture {
        val repository = FakeRepository(workspace)
        val integration = FakeIntegrationStore(repository)
        val archive = FakeArchiveStore(integration)
        val ids = ArrayDeque((1..30).map { "generated-$it" })
        val normalizer = LauncherTodoNormalizer(
            idFactory = IdFactory { ids.removeFirst() },
            clockMillis = { 1234L },
        )
        val designation = designation()
        return Fixture(
            repository,
            integration,
            archive,
            MinkBoardLauncherFacade(integration, archive, normalizer),
            designation,
            normalizer,
        )
    }

    private data class Fixture(
        val repository: FakeRepository,
        val integration: FakeIntegrationStore,
        val archive: FakeArchiveStore,
        val facade: MinkBoardLauncherFacade,
        val designation: LauncherBoardDesignation,
        val normalizer: LauncherTodoNormalizer,
    )

    private class FakeRepository(initial: MinkBoardWorkspace) : MinkBoardRepository {
        val workspace = MutableStateFlow(initial)

        override fun observeWorkspace(): Flow<MinkBoardWorkspace> = workspace
        override suspend fun addCard(statusId: StatusId, draft: CardDraft): CardId = unsupported()

        override suspend fun ensureDefaultWorkspace() = unsupported()
        override suspend fun createBoard(draft: BoardDraft): BoardId = unsupported()
        override suspend fun createBoardFromTemplate(draft: BoardDraft, templateId: BoardTemplateId): BoardId = unsupported()
        override suspend fun renameBoard(boardId: BoardId, name: String) = unsupported()
        override suspend fun updateBoard(boardId: BoardId, update: BoardUpdate) = unsupported()
        override suspend fun reorderBoards(orderedBoardIds: List<BoardId>) = unsupported()
        override suspend fun deleteBoard(boardId: BoardId) = unsupported()
        override suspend fun addStatus(boardId: BoardId, draft: StatusDraft): StatusId = unsupported()
        override suspend fun updateStatus(statusId: StatusId, update: StatusUpdate) = unsupported()
        override suspend fun reorderStatuses(boardId: BoardId, orderedStatusIds: List<StatusId>) = unsupported()
        override suspend fun deleteStatus(statusId: StatusId, policy: StatusDeletionPolicy) = unsupported()
        override suspend fun cloneCard(cardId: CardId): CardId = unsupported()
        override suspend fun updateCard(cardId: CardId, update: CardUpdate) = unsupported()
        override suspend fun moveCard(cardId: CardId, destination: StatusId, position: Int) = unsupported()
        override suspend fun deleteCard(cardId: CardId) = unsupported()
        override suspend fun addSubtask(cardId: CardId, text: String): SubtaskId = unsupported()
        override suspend fun updateSubtask(subtaskId: SubtaskId, text: String) = unsupported()
        override suspend fun setSubtaskCompleted(subtaskId: SubtaskId, completed: Boolean) = unsupported()
        override suspend fun deleteSubtask(subtaskId: SubtaskId) = unsupported()

        private fun unsupported(): Nothing = error("Not used by this facade test")
    }

    private class FakeIntegrationStore(
        private val repository: FakeRepository,
    ) : LauncherBoardIntegrationStore {
        val designation = MutableStateFlow<LauncherBoardDesignation?>(null)
        val hostSnapshot = MutableStateFlow(
            LauncherBoardHostSnapshot(repository.workspace.value, designation = null),
        )
        var commitCount = 0
        var projectionBoardId: BoardId? = null
        var projectionFingerprint: String? = null
        var finishedBoardId: BoardId? = null
        var finishSucceeds = true
        var lastAddedStatus: StatusId? = null
        var lastAddedDraft: CardDraft? = null
        var hostSnapshotAfterMigrationCommit: LauncherBoardHostSnapshot? = null
        private var designationCounter = 0

        override fun observeLauncherBoardHostSnapshot(): Flow<LauncherBoardHostSnapshot> =
            hostSnapshot

        override suspend fun launcherBoardHostSnapshot(): LauncherBoardHostSnapshot =
            hostSnapshot.value

        override suspend fun launcherBoardDesignation(): LauncherBoardDesignation? = designation.value

        fun publishHostSnapshot() {
            hostSnapshot.value = LauncherBoardHostSnapshot(
                workspace = repository.workspace.value,
                designation = designation.value,
            )
        }

        override suspend fun createAndDesignateFreshBoard(
            request: LauncherFreshBoardRequest,
        ): LauncherFreshBoardDesignationResult {
            designation.value?.let { existing ->
                return LauncherFreshBoardDesignationResult.AlreadyDesignated(existing)
            }
            val boardId = BoardId("fresh-board")
            val todoId = StatusId("fresh-todo")
            val doneId = StatusId("fresh-done")
            repository.workspace.value = MinkBoardWorkspace(
                repository.workspace.value.boards + BoardSnapshot(
                    board = Board(boardId, request.boardName, request.isLocked, 0, 2L),
                    statuses = listOf(
                        BoardStatus(todoId, boardId, "To do", 0, false, 0),
                        BoardStatus(doneId, boardId, "Done", 0, true, 1),
                    ),
                    cards = emptyList(),
                ),
            )
            val created = LauncherBoardDesignation(
                generation = "fresh-generation",
                boardId = boardId,
                intakeStatusId = todoId,
                designatedAtEpochMillis = 2L,
                legacyCompatibility = null,
            )
            designation.value = created
            publishHostSnapshot()
            return LauncherFreshBoardDesignationResult.Created(created)
        }

        override suspend fun designateExistingBoard(
            boardId: BoardId,
            intakeStatusId: StatusId,
            expectedGeneration: String?,
        ): LauncherExistingBoardDesignationResult {
            val existing = designation.value
            if (existing?.boardId == boardId && existing.intakeStatusId == intakeStatusId) {
                return designationTargetProblem(boardId, intakeStatusId)?.let { problem ->
                    LauncherExistingBoardDesignationResult.BrokenExistingDesignation(
                        existing,
                        problem,
                    )
                } ?: LauncherExistingBoardDesignationResult.AlreadyDesignated(existing)
            }
            if (
                (existing == null && expectedGeneration != null) ||
                (existing != null && existing.generation != expectedGeneration)
            ) {
                return LauncherExistingBoardDesignationResult.StaleDesignation(existing)
            }
            designationTargetProblem(boardId, intakeStatusId)?.let { problem ->
                return LauncherExistingBoardDesignationResult.InvalidTarget(problem)
            }
            val created = LauncherBoardDesignation(
                generation = "existing-generation-${++designationCounter}",
                boardId = boardId,
                intakeStatusId = intakeStatusId,
                designatedAtEpochMillis = 3L,
                legacyCompatibility = null,
            )
            designation.value = created
            publishHostSnapshot()
            return LauncherExistingBoardDesignationResult.Designated(created)
        }

        private fun designationTargetProblem(
            boardId: BoardId,
            intakeStatusId: StatusId,
        ): LauncherBindingProblem? {
            val board = repository.workspace.value.board(boardId)
                ?: return LauncherBindingProblem.BOARD_MISSING
            val intake = board.statuses.firstOrNull { it.id == intakeStatusId }
                ?: return LauncherBindingProblem.INTAKE_STATUS_MISSING
            if (intake.boardId != boardId) {
                return LauncherBindingProblem.INTAKE_STATUS_BELONGS_TO_ANOTHER_BOARD
            }
            if (intake.isDoneStatus) return LauncherBindingProblem.INTAKE_STATUS_IS_DONE
            return null
        }

        override suspend fun commitLauncherTodoMigration(
            plan: LauncherTodoMigrationPlan,
        ): LauncherBoardDesignation {
            designation.value?.let { return it }
            commitCount += 1
            repository.workspace.value = MinkBoardWorkspace(
                repository.workspace.value.boards + plan.board,
            )
            designation.value = plan.designation
            publishHostSnapshot()
            hostSnapshotAfterMigrationCommit?.let { replacement ->
                hostSnapshot.value = replacement
            }
            return plan.designation
        }

        override suspend fun addCardToBoundIntake(
            draft: CardDraft,
        ): LauncherBoundIntakeWriteResult {
            val currentDesignation = designation.value
                ?: return LauncherBoundIntakeWriteResult.Unbound
            val board = repository.workspace.value.board(currentDesignation.boardId)
                ?: return LauncherBoundIntakeWriteResult.BrokenBinding(
                    LauncherBindingProblem.BOARD_MISSING,
                )
            val intake = board.statuses.firstOrNull { it.id == currentDesignation.intakeStatusId }
                ?: return LauncherBoundIntakeWriteResult.BrokenBinding(
                    LauncherBindingProblem.INTAKE_STATUS_MISSING,
                )
            if (intake.isDoneStatus) {
                return LauncherBoundIntakeWriteResult.BrokenBinding(
                    LauncherBindingProblem.INTAKE_STATUS_IS_DONE,
                )
            }
            lastAddedStatus = intake.id
            lastAddedDraft = draft
            return LauncherBoundIntakeWriteResult.Added(CardId("added-card"))
        }

        override suspend fun recordLegacyProjectionWritten(
            generation: String,
            boardId: BoardId,
            fingerprint: String,
            expectedPreviousFingerprint: String?,
        ): Boolean {
            val current = designation.value ?: return false
            val compatibility = current.legacyCompatibility ?: return false
            if (current.generation != generation ||
                compatibility.lastProjectionFingerprint != expectedPreviousFingerprint
            ) {
                return false
            }
            projectionBoardId = boardId
            projectionFingerprint = fingerprint
            designation.value = current.copy(
                legacyCompatibility = compatibility.copy(
                    lastProjectionFingerprint = fingerprint,
                ),
            )
            publishHostSnapshot()
            return true
        }

        override suspend fun finishLegacyCompatibility(
            generation: String,
            boardId: BoardId,
        ): Boolean {
            val current = designation.value ?: return false
            val compatibility = current.legacyCompatibility ?: return false
            if (!finishSucceeds || current.generation != generation) return false
            finishedBoardId = boardId
            designation.value = current.copy(
                legacyCompatibility = compatibility.copy(
                    state = LauncherTodoCompatibilityState.CANONICAL_FINAL,
                ),
            )
            publishHostSnapshot()
            return true
        }
    }

    private class FakeArchiveStore(
        private val integration: FakeIntegrationStore,
    ) : LauncherWorkspaceIntegrationStore {
        var exportSnapshot = portableWorkspace()
        var lastImportedSnapshot: PortableWorkspaceSnapshot? = null
        var lastImportMode: WorkspaceImportMode? = null

        override suspend fun createLauncherBackupSnapshot(
            sourceApp: String,
        ): LauncherBackupCreationResult {
            val snapshot = exportSnapshot.copy(sourceApp = sourceApp)
            val designation = integration.designation.value
            if (snapshot.boards.isEmpty() && designation == null) {
                return LauncherBackupCreationResult.Ready(
                    LauncherMinkBoardBackup.Present(snapshot, designation = null),
                )
            }
            designation ?: return LauncherBackupCreationResult.UnboundWorkspace
            val board = snapshot.boards.firstOrNull { it.id == designation.boardId.value }
                ?: return LauncherBackupCreationResult.BrokenBinding(
                    LauncherBindingProblem.BOARD_MISSING,
                )
            val intake = board.statuses.firstOrNull { it.id == designation.intakeStatusId.value }
                ?: return LauncherBackupCreationResult.BrokenBinding(
                    LauncherBindingProblem.INTAKE_STATUS_MISSING,
                )
            if (intake.isDoneStatus) {
                return LauncherBackupCreationResult.BrokenBinding(
                    LauncherBindingProblem.INTAKE_STATUS_IS_DONE,
                )
            }
            return LauncherBackupCreationResult.Ready(
                LauncherMinkBoardBackup.Present(
                    workspace = snapshot,
                    designation = LauncherBoardBackupDesignation(
                        designation.boardId,
                        designation.intakeStatusId,
                    ),
                ),
            )
        }

        override suspend fun importLauncherBackupSnapshot(
            plan: LauncherBackupRestorePlan.ImportWorkspace,
            mode: WorkspaceImportMode,
            context: LauncherBackupRestoreContext,
        ): LauncherWorkspaceImportOutcome.Imported {
            lastImportedSnapshot = plan.workspace
            lastImportMode = mode
            val importResult = WorkspaceImportResult(
                firstBoardId = BoardId("local-board"),
                ids = WorkspaceImportIdMap(
                    boardIds = mapOf("portable-board" to BoardId("local-board")),
                    statusIds = mapOf("portable-status" to StatusId("local-status")),
                    cardIds = emptyMap(),
                    subtaskIds = emptyMap(),
                ),
            )
            val resolved = LauncherBackupReconciler.resolveLauncherDesignation(
                plan.portableDesignation,
                importResult,
            ) as LauncherBackupDesignationResolution.Resolved
            val localCompatibility = context.localLegacyCompatibility
            val localDesignation = LauncherBoardDesignation(
                generation = "restored-generation",
                boardId = resolved.designation.boardId,
                intakeStatusId = resolved.designation.intakeStatusId,
                designatedAtEpochMillis = context.restoredAtEpochMillis,
                legacyCompatibility = localCompatibility?.let { legacy ->
                    LauncherLegacyCompatibilityReceipt(
                        state = LauncherTodoCompatibilityState.CANONICAL_COMPAT,
                        sourceSchemaVersion = legacy.sourceSchemaVersion,
                        sourceFingerprint = legacy.sourceFingerprint,
                        lastProjectionFingerprint = null,
                        migratedAtEpochMillis = context.restoredAtEpochMillis,
                    )
                },
            )
            integration.designation.value = localDesignation
            integration.publishHostSnapshot()
            return LauncherWorkspaceImportOutcome.Imported(importResult, localDesignation)
        }
    }

    private class FakeLauncherBackupEnvelopeCodec : LauncherBackupEnvelopeCodec {
        var encodeResult: LauncherBackupEnvelopeEncodeResult =
            LauncherBackupEnvelopeEncodeResult.Encrypted(byteArrayOf(1))
        var decodeResult: LauncherBackupEnvelopeDecodeResult =
            LauncherBackupEnvelopeDecodeResult.Corrupt
        var encodedBackup: LauncherMinkBoardBackup.Present? = null
        var encodedSecret: ByteArray? = null
        var decodeCalls: Int = 0

        override fun encode(
            backup: LauncherMinkBoardBackup.Present,
            secret: CharArray,
        ): LauncherBackupEnvelopeEncodeResult {
            encodedBackup = backup
            encodedSecret = secret.concatToString().encodeToByteArray()
            return encodeResult
        }

        override fun decode(
            envelope: ByteArray,
            secret: CharArray,
        ): LauncherBackupEnvelopeDecodeResult {
            decodeCalls++
            return decodeResult
        }
    }

    private companion object {
        const val UUID_ONE = "123e4567-e89b-12d3-a456-426614174000"

        fun designation() = LauncherBoardDesignation(
            generation = "binding-generation",
            boardId = BoardId("launcher-board"),
            intakeStatusId = StatusId("todo"),
            designatedAtEpochMillis = 1L,
            legacyCompatibility = LauncherLegacyCompatibilityReceipt(
                state = LauncherTodoCompatibilityState.CANONICAL_COMPAT,
                sourceSchemaVersion = 1,
                sourceFingerprint = "5".repeat(64),
                lastProjectionFingerprint = null,
                migratedAtEpochMillis = 1L,
            ),
        )

        fun workspace(): MinkBoardWorkspace {
            val boardId = BoardId("launcher-board")
            val todo = BoardStatus(StatusId("todo"), boardId, "To do", 0, false, 0)
            val done = BoardStatus(StatusId("done"), boardId, "Done", 0, true, 1)
            return MinkBoardWorkspace(
                listOf(
                    BoardSnapshot(
                        board = Board(boardId, "To-Dos", false, 0, 1L),
                        statuses = listOf(todo, done),
                        cards = listOf(
                            card("todo-card", todo.id, "Open"),
                            card("done-card", done.id, "Closed"),
                        ),
                    ),
                ),
            )
        }

        fun workspaceWithSecondBoard(): MinkBoardWorkspace {
            val first = workspace()
            val boardId = BoardId("second-board")
            return MinkBoardWorkspace(
                first.boards + BoardSnapshot(
                    board = Board(boardId, "Second", false, 1, 2L),
                    statuses = listOf(
                        BoardStatus(
                            StatusId("second-todo"),
                            boardId,
                            "To do",
                            0,
                            false,
                            0,
                        ),
                        BoardStatus(
                            StatusId("second-done"),
                            boardId,
                            "Done",
                            0,
                            true,
                            1,
                        ),
                    ),
                    cards = emptyList(),
                ),
            )
        }

        fun card(id: String, statusId: StatusId, title: String) = BoardCard(
            id = CardId(id),
            statusId = statusId,
            title = title,
            details = "",
            dueDate = null,
            isMajor = false,
            position = 0,
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )

        fun emptyPortableWorkspace() = PortableWorkspaceSnapshot(
            exportedAtEpochMillis = 1L,
            sourceApp = "launcher",
            boards = emptyList(),
        )

        fun portableWorkspace(
            boardId: String = "portable-board",
            statusId: String = "portable-status",
        ) = PortableWorkspaceSnapshot(
            exportedAtEpochMillis = 1L,
            sourceApp = "launcher",
            boards = listOf(
                PortableBoardSnapshot(
                    id = boardId,
                    name = "To-Dos",
                    isLocked = false,
                    position = 0,
                    createdAtEpochMillis = 1L,
                    statuses = listOf(
                        PortableStatusSnapshot(
                            id = statusId,
                            name = "To do",
                            colorArgb = 0,
                            isDoneStatus = false,
                            position = 0,
                            cards = emptyList(),
                        ),
                    ),
                ),
            ),
        )
    }
}
