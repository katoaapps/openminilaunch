package com.katoaapps.minkboard.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.katoaapps.minkboard.core.WorkspaceImportMode
import com.katoaapps.minkboard.model.BoardDraft
import com.katoaapps.minkboard.model.BoardUpdate
import com.katoaapps.minkboard.model.CardDraft
import com.katoaapps.minkboard.model.PortableBoardSnapshot
import com.katoaapps.minkboard.model.PortableCardSnapshot
import com.katoaapps.minkboard.model.PortableStatusSnapshot
import com.katoaapps.minkboard.model.PortableSubtaskSnapshot
import com.katoaapps.minkboard.model.PortableWorkspaceSnapshot
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMinkBoardArchiveTest {
    private lateinit var database: MinkBoardDatabase
    private lateinit var repository: RoomMinkBoardRepository
    private val ids = AtomicInteger()
    private val clock = AtomicInteger(20_000)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MinkBoardDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = repository(ArchiveTestFieldCipher())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun portableSnapshotRoundTripsEveryWorkspaceField() = runBlocking {
        repository.ensureDefaultWorkspace()
        val original = repository.observeWorkspace().first().firstBoard!!
        repository.updateBoard(original.board.id, BoardUpdate("Private launch", isLocked = true))
        val cardId = repository.addCard(
            original.statuses.first().id,
            CardDraft(
                title = "Release MinkBoard",
                details = "Keep the complete encrypted graph portable.",
                dueDate = LocalDate.of(2027, 4, 5),
                isMajor = true,
            ),
        )
        val subtaskId = repository.addSubtask(cardId, "Verify archive")
        repository.setSubtaskCompleted(subtaskId, true)
        val before = repository.observeWorkspace().first()

        val archive = repository.createPortableSnapshot("MinkBoard test")
        val selected = repository.importPortableSnapshot(archive, WorkspaceImportMode.Replace)

        assertEquals(before.firstBoard!!.board.id, selected.firstBoardId)
        assertEquals(
            before.firstBoard!!.board.id,
            selected.ids.boardIds.getValue(before.firstBoard!!.board.id.value),
        )
        assertEquals(before, repository.observeWorkspace().first())
        assertEquals("MinkBoard test", archive.sourceApp)
    }

    @Test
    fun mergeRemapsEveryIdAndAppendsBoardPositions() = runBlocking {
        repository.ensureDefaultWorkspace()
        val existing = repository.observeWorkspace().first()
        val existingIds = existing.allIds()
        val incoming = portableFixture()
        val incomingIds = incoming.allIds()

        val importedBoardId = repository.importPortableSnapshot(
            incoming,
            WorkspaceImportMode.Merge,
        )

        val merged = repository.observeWorkspace().first()
        assertEquals(listOf(0, 1), merged.boards.map { it.board.position })
        val imported = merged.board(requireNotNull(importedBoardId.firstBoardId))!!
        val importedIds = imported.allIds()
        assertEquals("Imported board", imported.board.name)
        assertEquals("Imported card", imported.cards.single().title)
        assertTrue(importedIds.intersect(existingIds).isEmpty())
        assertTrue(importedIds.intersect(incomingIds).isEmpty())
        assertNotEquals("archive-board", importedBoardId.firstBoardId?.value)
        assertEquals(
            imported.board.id,
            importedBoardId.ids.boardIds.getValue("archive-board"),
        )
        assertEquals(
            imported.statuses.single().id,
            importedBoardId.ids.statusIds.getValue("archive-status"),
        )
        assertEquals(
            imported.cards.single().id,
            importedBoardId.ids.cardIds.getValue("archive-card"),
        )
        assertEquals(
            imported.cards.single().subtasks.single().id,
            importedBoardId.ids.subtaskIds.getValue("archive-subtask"),
        )
    }

    @Test
    fun replacePreservesValidatedIdsAndRemovesOldWorkspace() = runBlocking {
        repository.ensureDefaultWorkspace()
        val oldBoardId = repository.observeWorkspace().first().firstBoard!!.board.id
        val incoming = portableFixture()

        val selected = repository.importPortableSnapshot(incoming, WorkspaceImportMode.Replace)

        val workspace = repository.observeWorkspace().first()
        assertEquals("archive-board", selected.firstBoardId?.value)
        assertEquals("archive-board", selected.ids.boardIds.getValue("archive-board").value)
        assertEquals(1, workspace.boards.size)
        assertEquals(null, workspace.board(oldBoardId))
        assertEquals(incoming.allIds(), workspace.allIds())
        assertTrue(workspace.firstBoard!!.board.isLocked)
    }

    @Test
    fun replaceRejectsEmptyWorkspaceWithoutChangingLocalData() = runBlocking {
        repository.ensureDefaultWorkspace()
        val before = repository.observeWorkspace().first()
        val empty = portableFixture().copy(boards = emptyList())

        val failure = runCatching {
            repository.importPortableSnapshot(empty, WorkspaceImportMode.Replace)
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertTrue(failure?.message.orEmpty().contains("at least one board"))
        assertEquals(before, repository.observeWorkspace().first())
    }

    @Test
    fun failedReplaceRollsBackDeletionAndPartialInserts() = runBlocking {
        repository.ensureDefaultWorkspace()
        val before = repository.observeWorkspace().first()
        val failingRepository = repository(
            FailingArchiveCipher(failOnPlaintext = "Imported status"),
        )

        val failure = runCatching {
            failingRepository.importPortableSnapshot(
                portableFixture(),
                WorkspaceImportMode.Replace,
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertEquals(before, repository.observeWorkspace().first())
    }

    private fun repository(cipher: FieldCipher) = RoomMinkBoardRepository(
        database = database,
        cipher = cipher,
        idFactory = { "generated-${ids.incrementAndGet()}" },
        clockMillis = { clock.incrementAndGet().toLong() },
    )
}

private fun portableFixture() = PortableWorkspaceSnapshot(
    exportedAtEpochMillis = 12_345L,
    sourceApp = "Spencer handoff",
    boards = listOf(
        PortableBoardSnapshot(
            id = "archive-board",
            name = "Imported board",
            isLocked = true,
            position = 0,
            createdAtEpochMillis = 1_000L,
            statuses = listOf(
                PortableStatusSnapshot(
                    id = "archive-status",
                    name = "Imported status",
                    colorArgb = 0xFF336699.toInt(),
                    isDoneStatus = true,
                    position = 0,
                    cards = listOf(
                        PortableCardSnapshot(
                            id = "archive-card",
                            title = "Imported card",
                            details = "Full details",
                            dueDateIso = "2027-02-03",
                            isMajor = true,
                            position = 0,
                            createdAtEpochMillis = 2_000L,
                            updatedAtEpochMillis = 3_000L,
                            subtasks = listOf(
                                PortableSubtaskSnapshot(
                                    id = "archive-subtask",
                                    text = "Imported subtask",
                                    isCompleted = true,
                                    position = 0,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    ),
)

private fun PortableWorkspaceSnapshot.allIds(): Set<String> = buildSet {
    boards.forEach { board ->
        add(board.id)
        board.statuses.forEach { status ->
            add(status.id)
            status.cards.forEach { card ->
                add(card.id)
                card.subtasks.forEach { add(it.id) }
            }
        }
    }
}

private fun com.katoaapps.minkboard.model.MinkBoardWorkspace.allIds(): Set<String> =
    boards.flatMap { it.allIds() }.toSet()

private fun com.katoaapps.minkboard.model.BoardSnapshot.allIds(): Set<String> = buildSet {
    add(board.id.value)
    statuses.forEach { add(it.id.value) }
    cards.forEach { card ->
        add(card.id.value)
        card.subtasks.forEach { add(it.id.value) }
    }
}

private open class ArchiveTestFieldCipher : FieldCipher {
    override fun encrypt(value: String, subject: String, field: String): ByteArray =
        "$subject\u0000$field\u0000$value".toByteArray(Charsets.UTF_8)

    override fun decrypt(value: ByteArray, subject: String, field: String): String {
        val prefix = "$subject\u0000$field\u0000"
        val decoded = value.toString(Charsets.UTF_8)
        require(decoded.startsWith(prefix)) { "Ciphertext context mismatch" }
        return decoded.removePrefix(prefix)
    }
}

private class FailingArchiveCipher(
    private val failOnPlaintext: String,
) : ArchiveTestFieldCipher() {
    override fun encrypt(value: String, subject: String, field: String): ByteArray {
        check(value != failOnPlaintext) { "Injected archive import failure" }
        return super.encrypt(value, subject, field)
    }
}
