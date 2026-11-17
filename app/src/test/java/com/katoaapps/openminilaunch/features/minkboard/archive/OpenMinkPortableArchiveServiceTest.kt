package com.katoaapps.openminilaunch.features.minkboard.archive

import com.katoaapps.minkboard.core.WorkspaceArchiveStore
import com.katoaapps.minkboard.core.WorkspaceImportIdMap
import com.katoaapps.minkboard.core.WorkspaceImportMode
import com.katoaapps.minkboard.core.WorkspaceImportResult
import com.katoaapps.minkboard.data.PassphrasePortableArchiveCodec
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.PortableBoardSnapshot
import com.katoaapps.minkboard.model.PortableStatusSnapshot
import com.katoaapps.minkboard.model.PortableWorkspaceSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMinkPortableArchiveServiceTest {
    @Test
    fun exportUsesCanonicalCodecAndIncludesOnlySelectedBoards() = runBlocking {
        val store = FakeArchiveStore(twoBoardSnapshot())
        val service = OpenMinkPortableArchiveService(store)
        val passphrase = "minktest123".toCharArray()

        val archive = service.exportBoards(setOf(BoardId("board-2")), passphrase)
        val decoded = PassphrasePortableArchiveCodec().decode(
            archive,
            "minktest123".toCharArray(),
        )

        assertEquals("Mink Launcher Open Source", decoded.sourceApp)
        assertEquals(listOf("board-2"), decoded.boards.map { it.id })
        assertTrue(passphrase.all { it == '\u0000' })
        archive.fill(0)
    }

    @Test
    fun importIsAdditiveAndClearsSensitiveInputs() = runBlocking {
        val store = FakeArchiveStore(twoBoardSnapshot())
        val service = OpenMinkPortableArchiveService(store)
        val archivePassphrase = "minktest123".toCharArray()
        val archive = PassphrasePortableArchiveCodec().encode(
            twoBoardSnapshot().copy(boards = listOf(twoBoardSnapshot().boards.first())),
            archivePassphrase,
        )
        archivePassphrase.fill('\u0000')
        val ownedArchive = archive.copyOf()
        val importPassphrase = "minktest123".toCharArray()

        val importedBoardId = service.importBoards(ownedArchive, importPassphrase)

        assertEquals(BoardId("imported-board"), importedBoardId)
        assertEquals(WorkspaceImportMode.Merge, store.importMode)
        assertEquals(listOf("board-1"), store.importedSnapshot?.boards?.map { it.id })
        assertTrue(ownedArchive.all { it == 0.toByte() })
        assertTrue(importPassphrase.all { it == '\u0000' })
        archive.fill(0)
    }

    @Test
    fun unsupportedImportDoesNotMutateWorkspaceAndStillClearsInputs() = runBlocking {
        val store = FakeArchiveStore(twoBoardSnapshot())
        val service = OpenMinkPortableArchiveService(store)
        val bytes = "not a minkboard archive".toByteArray()
        val passphrase = "minktest123".toCharArray()

        val result = runCatching { service.importBoards(bytes, passphrase) }

        assertTrue(result.isFailure)
        assertEquals(null, store.importedSnapshot)
        assertTrue(bytes.all { it == 0.toByte() })
        assertTrue(passphrase.all { it == '\u0000' })
    }

    private class FakeArchiveStore(
        private val snapshot: PortableWorkspaceSnapshot,
    ) : WorkspaceArchiveStore {
        var importedSnapshot: PortableWorkspaceSnapshot? = null
        var importMode: WorkspaceImportMode? = null

        override suspend fun createPortableSnapshot(sourceApp: String): PortableWorkspaceSnapshot =
            snapshot.copy(sourceApp = sourceApp)

        override suspend fun importPortableSnapshot(
            snapshot: PortableWorkspaceSnapshot,
            mode: WorkspaceImportMode,
        ): WorkspaceImportResult {
            importedSnapshot = snapshot
            importMode = mode
            return WorkspaceImportResult(
                firstBoardId = BoardId("imported-board"),
                ids = WorkspaceImportIdMap(
                    boardIds = emptyMap(),
                    statusIds = emptyMap(),
                    cardIds = emptyMap(),
                    subtaskIds = emptyMap(),
                ),
            )
        }
    }

    private fun twoBoardSnapshot(): PortableWorkspaceSnapshot = PortableWorkspaceSnapshot(
        exportedAtEpochMillis = 1_700_000_000_000,
        sourceApp = "fixture",
        boards = listOf(
            portableBoard(id = "board-1", name = "Personal", position = 0),
            portableBoard(id = "board-2", name = "Work", position = 1),
        ),
    )

    private fun portableBoard(
        id: String,
        name: String,
        position: Int,
    ) = PortableBoardSnapshot(
        id = id,
        name = name,
        isLocked = false,
        position = position,
        createdAtEpochMillis = 100,
        statuses = listOf(
            PortableStatusSnapshot(
                id = "$id-status",
                name = "To Do",
                colorArgb = 0xFF173529.toInt(),
                isDoneStatus = false,
                position = 0,
                cards = emptyList(),
            ),
        ),
    )
}
