package com.katoaapps.minkboard.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.katoaapps.minkboard.core.IdFactory
import com.katoaapps.minkboard.core.LauncherBackupApplyResult
import com.katoaapps.minkboard.core.LauncherBackupCreationResult
import com.katoaapps.minkboard.core.LauncherBackupPreparationResult
import com.katoaapps.minkboard.core.LauncherBackupRestoreContext
import com.katoaapps.minkboard.core.LauncherEncryptedBackupCreationResult
import com.katoaapps.minkboard.core.LauncherFreshBoardDesignationResult
import com.katoaapps.minkboard.core.LauncherFreshBoardRequest
import com.katoaapps.minkboard.core.LauncherMinkBoardBackup
import com.katoaapps.minkboard.core.LauncherMinkBoardBackupField
import com.katoaapps.minkboard.core.LauncherTodoNormalizer
import com.katoaapps.minkboard.core.LegacyTodoSource
import com.katoaapps.minkboard.core.MinkBoardLauncherBackupWorkflow
import com.katoaapps.minkboard.core.MinkBoardLauncherFacade
import com.katoaapps.minkboard.core.WorkspaceImportMode
import com.katoaapps.minkboard.model.BoardDraft
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardUpdate
import com.katoaapps.minkboard.model.CardDraft
import com.katoaapps.minkboard.model.PortableWorkspaceSnapshot
import java.security.SecureRandom
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherBackupEndToEndTest {
    private val databases = mutableListOf<MinkBoardDatabase>()
    private val secret = "correct horse battery staple".toCharArray()

    @After
    fun tearDown() {
        databases.forEach(MinkBoardDatabase::close)
        databases.clear()
        secret.fill('\u0000')
    }

    @Test
    fun encryptedMergeRestoresCompleteGraphAndRemapsDesignationWithoutCompatibility() = runBlocking {
        val source = harness("source", 10_000L)
        val sourceState = populateSource(source)
        val sourceBackup = source.repository.readyBackup()
        val encrypted = source.workflow.createEncryptedBackup("OpenMink", secret)
            as LauncherEncryptedBackupCreationResult.Ready

        val destination = harness("merge-destination", 20_000L)
        val destinationDesignation = destination.repository.createAndDesignateFreshBoard(
            LauncherFreshBoardRequest("Existing destination"),
        ) as LauncherFreshBoardDesignationResult.Created
        val boardsBefore = destination.repository.observeWorkspace().first().boards.size

        val prepared = destination.workflow.prepareRestore(
            minkBoardField = LauncherMinkBoardBackupField.Encrypted(encrypted.envelope),
            secret = secret,
            legacyTodos = LegacyTodoSource.Missing,
        ) as LauncherBackupPreparationResult.Prepared
        val applied = destination.workflow.apply(
            prepared = prepared,
            mode = WorkspaceImportMode.Merge,
            context = LauncherBackupRestoreContext(restoredAtEpochMillis = 90_000L),
        ) as LauncherBackupApplyResult.WorkspaceImported

        val outcome = applied.outcome
        val restored = outcome.launcherDesignation
        val boardMap = outcome.importResult.ids.boardIds
        val statusMap = outcome.importResult.ids.statusIds

        assertEquals(boardMap[sourceState.designation.boardId.value], restored.boardId)
        assertEquals(
            statusMap[sourceState.designation.intakeStatusId.value],
            restored.intakeStatusId,
        )
        assertNotEquals(sourceState.designation.boardId, restored.boardId)
        assertNotEquals(sourceState.designation.intakeStatusId, restored.intakeStatusId)
        assertNotEquals(destinationDesignation.designation.generation, restored.generation)
        assertNotEquals(sourceState.designation.generation, restored.generation)
        assertEquals(90_000L, restored.designatedAtEpochMillis)
        assertNull(restored.legacyCompatibility)

        val workspace = destination.repository.observeWorkspace().first()
        assertEquals(boardsBefore + sourceBackup.workspace.boards.size, workspace.boards.size)
        assertCompleteRestoredGraph(workspace, restored.boardId)
        val mappedSideBoard = checkNotNull(boardMap[sourceState.sideBoardId.value])
        assertEquals("Release notes", workspace.board(mappedSideBoard)?.board?.name)
    }

    @Test
    fun encryptedReplaceAtomicallyReplacesGraphAndCreatesFreshLocalDesignation() = runBlocking {
        val source = harness("replace-source", 30_000L)
        val sourceState = populateSource(source)
        val sourceBackup = source.repository.readyBackup()
        val encrypted = source.workflow.createEncryptedBackup("PlayMink", secret)
            as LauncherEncryptedBackupCreationResult.Ready

        val destination = harness("replace-destination", 40_000L)
        val previous = destination.repository.createAndDesignateFreshBoard(
            LauncherFreshBoardRequest("Discarded destination"),
        ) as LauncherFreshBoardDesignationResult.Created
        val previousBoardId = previous.designation.boardId

        val prepared = destination.workflow.prepareRestore(
            LauncherMinkBoardBackupField.Encrypted(encrypted.envelope),
            secret,
            LegacyTodoSource.Missing,
        ) as LauncherBackupPreparationResult.Prepared
        val applied = destination.workflow.apply(
            prepared = prepared,
            mode = WorkspaceImportMode.Replace,
            context = LauncherBackupRestoreContext(restoredAtEpochMillis = 91_000L),
        ) as LauncherBackupApplyResult.WorkspaceImported

        val outcome = applied.outcome
        val restored = outcome.launcherDesignation
        assertEquals(sourceState.designation.boardId, restored.boardId)
        assertEquals(sourceState.designation.intakeStatusId, restored.intakeStatusId)
        assertEquals(
            sourceState.designation.boardId,
            outcome.importResult.ids.boardIds[sourceState.designation.boardId.value],
        )
        assertEquals(
            sourceState.designation.intakeStatusId,
            outcome.importResult.ids.statusIds[sourceState.designation.intakeStatusId.value],
        )
        assertNotEquals(previous.designation.generation, restored.generation)
        assertNotEquals(sourceState.designation.generation, restored.generation)
        assertEquals(91_000L, restored.designatedAtEpochMillis)
        assertNull(restored.legacyCompatibility)

        val workspace = destination.repository.observeWorkspace().first()
        assertEquals(sourceBackup.workspace.boards.size, workspace.boards.size)
        assertNull(workspace.board(previousBoardId))
        assertCompleteRestoredGraph(workspace, restored.boardId)
        assertNotNull(workspace.board(sourceState.sideBoardId))
    }

    @Test
    fun decodeFailuresAndUnsupportedEnvelopeLeaveRoomGraphAndDesignationUntouched() = runBlocking {
        val source = harness("failure-source", 50_000L)
        populateSource(source)
        val encrypted = source.workflow.createEncryptedBackup("OpenMink", secret)
            as LauncherEncryptedBackupCreationResult.Ready

        val destination = harness("failure-destination", 60_000L)
        destination.repository.createAndDesignateFreshBoard(
            LauncherFreshBoardRequest("Must survive"),
        )
        val before = destination.repository.launcherBoardHostSnapshot()

        assertEquals(
            LauncherBackupPreparationResult.CorruptEnvelope,
            destination.workflow.prepareRestore(
                LauncherMinkBoardBackupField.Encrypted(encrypted.envelope),
                "wrong recovery secret".toCharArray(),
                LegacyTodoSource.Missing,
            ),
        )

        val tampered = JSONObject(encrypted.envelope.toString(Charsets.UTF_8)).apply {
            val chars = getString("ciphertext").toCharArray()
            val index = chars.lastIndex / 2
            chars[index] = if (chars[index] == 'A') 'B' else 'A'
            put("ciphertext", String(chars))
        }.toString().toByteArray()
        assertEquals(
            LauncherBackupPreparationResult.CorruptEnvelope,
            destination.workflow.prepareRestore(
                LauncherMinkBoardBackupField.Encrypted(tampered),
                secret,
                LegacyTodoSource.Missing,
            ),
        )

        val unsupported = JSONObject(encrypted.envelope.toString(Charsets.UTF_8))
            .put("envelopeVersion", 99)
            .toString()
            .toByteArray()
        assertEquals(
            LauncherBackupPreparationResult.UnsupportedEnvelope,
            destination.workflow.prepareRestore(
                LauncherMinkBoardBackupField.Encrypted(unsupported),
                secret,
                LegacyTodoSource.Missing,
            ),
        )

        assertEquals(before, destination.repository.launcherBoardHostSnapshot())
    }

    @Test
    fun encryptedExplicitEmptyWorkspaceIsANonDestructiveWorkflowNoOp() = runBlocking {
        val destination = harness("empty-destination", 70_000L)
        destination.repository.createAndDesignateFreshBoard(
            LauncherFreshBoardRequest("Keep this board"),
        )
        val before = destination.repository.launcherBoardHostSnapshot()
        val empty = LauncherMinkBoardBackup.Present(
            workspace = PortableWorkspaceSnapshot(
                exportedAtEpochMillis = 1L,
                sourceApp = "MinkLauncher",
                boards = emptyList(),
            ),
            designation = null,
        )
        val codec = testCodec()
        val encoded = codec.encode(empty, secret)
            as com.katoaapps.minkboard.core.LauncherBackupEnvelopeEncodeResult.Encrypted

        val prepared = destination.workflow.prepareRestore(
            LauncherMinkBoardBackupField.Encrypted(encoded.envelope),
            secret,
            LegacyTodoSource.Missing,
        ) as LauncherBackupPreparationResult.Prepared
        val result = destination.workflow.apply(
            prepared,
            WorkspaceImportMode.Replace,
            LauncherBackupRestoreContext(restoredAtEpochMillis = 92_000L),
        )

        assertEquals(LauncherBackupApplyResult.ExplicitlyEmptyWorkspace, result)
        assertEquals(before, destination.repository.launcherBoardHostSnapshot())
    }

    private suspend fun populateSource(harness: Harness): SourceState {
        val created = harness.repository.createAndDesignateFreshBoard(
            LauncherFreshBoardRequest("Product launch"),
        ) as LauncherFreshBoardDesignationResult.Created
        harness.repository.updateBoard(
            created.designation.boardId,
            BoardUpdate(name = "Product launch", isLocked = true),
        )
        val cardId = harness.repository.addCard(
            created.designation.intakeStatusId,
            CardDraft(
                title = "Ship MinkBoard 🦦",
                details = "Preserve complete encrypted details.",
                dueDate = LocalDate.of(2027, 3, 4),
                isMajor = true,
            ),
        )
        val completedSubtask = harness.repository.addSubtask(cardId, "Run release checks")
        harness.repository.setSubtaskCompleted(completedSubtask, true)
        harness.repository.addSubtask(cardId, "Publish the notes")

        val sideBoardId = harness.repository.createBoard(BoardDraft("Release notes"))
        val sideBoard = harness.repository.observeWorkspace().first().board(sideBoardId)!!
        harness.repository.addCard(
            sideBoard.statuses.first { !it.isDoneStatus }.id,
            CardDraft("Document launcher restore"),
        )
        return SourceState(created.designation, sideBoardId)
    }

    private fun assertCompleteRestoredGraph(
        workspace: com.katoaapps.minkboard.model.MinkBoardWorkspace,
        boardId: BoardId,
    ) {
        val board = checkNotNull(workspace.board(boardId))
        assertEquals("Product launch", board.board.name)
        assertTrue(board.board.isLocked)
        assertEquals(listOf("To do", "Doing", "Done"), board.statuses.map { it.name })
        val card = board.cards.single { it.title == "Ship MinkBoard 🦦" }
        assertEquals("Preserve complete encrypted details.", card.details)
        assertEquals(LocalDate.of(2027, 3, 4), card.dueDate)
        assertTrue(card.isMajor)
        assertEquals(listOf("Run release checks", "Publish the notes"), card.subtasks.map { it.text })
        assertTrue(card.subtasks.first().isCompleted)
        assertFalse(card.subtasks.last().isCompleted)
    }

    private fun harness(prefix: String, clockStart: Long): Harness {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, MinkBoardDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        databases += database
        val sequence = AtomicInteger()
        val repository = RoomMinkBoardRepository(
            database = database,
            cipher = EndToEndTestCipher(),
            idFactory = { "$prefix-${sequence.incrementAndGet()}" },
            clockMillis = { clockStart + sequence.incrementAndGet() },
        )
        val facade = MinkBoardLauncherFacade(
            integrationStore = repository,
            workspaceIntegrationStore = repository,
            normalizer = LauncherTodoNormalizer(
                idFactory = IdFactory { "$prefix-normalizer-${sequence.incrementAndGet()}" },
                clockMillis = { clockStart + sequence.incrementAndGet() },
            ),
        )
        return Harness(
            repository = repository,
            workflow = MinkBoardLauncherBackupWorkflow(facade, testCodec()),
        )
    }

    private fun testCodec() = PassphraseLauncherBackupEnvelopeCodec(
        random = SecureRandom(),
        writeIterations = 10_000,
    )

    private suspend fun RoomMinkBoardRepository.readyBackup(): LauncherMinkBoardBackup.Present =
        (createLauncherBackupSnapshot("connected test") as LauncherBackupCreationResult.Ready).backup

    private data class Harness(
        val repository: RoomMinkBoardRepository,
        val workflow: MinkBoardLauncherBackupWorkflow,
    )

    private data class SourceState(
        val designation: com.katoaapps.minkboard.core.LauncherBoardDesignation,
        val sideBoardId: BoardId,
    )
}

private class EndToEndTestCipher : FieldCipher {
    override fun encrypt(value: String, subject: String, field: String): ByteArray =
        "$subject\u0000$field\u0000$value".toByteArray(Charsets.UTF_8)

    override fun decrypt(value: ByteArray, subject: String, field: String): String {
        val prefix = "$subject\u0000$field\u0000"
        val decoded = value.toString(Charsets.UTF_8)
        require(decoded.startsWith(prefix)) { "Ciphertext context mismatch" }
        return decoded.removePrefix(prefix)
    }
}
