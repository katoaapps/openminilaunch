package com.katoaapps.minkboard.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.katoaapps.minkboard.model.BoardDraft
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.CardDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidMinkBoardRuntimeTest {
    @Test
    fun publicRuntimeWiresCrudTemplatesBackupAndTransferCapabilities() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val runtime = AndroidMinkBoardRuntime.create(context)
        val passphrase = "runtime-test-passphrase".toCharArray()
        var transferredBoardId: BoardId? = null
        var restoredBoardId: BoardId? = null

        runtime.boards.ensureDefaultWorkspace()
        val boardId = runtime.boards.createBoard(BoardDraft("Public API test"))
        try {
            val board = checkNotNull(runtime.boards.board(boardId))
            val statusId = board.statuses.first().id
            val cardId = runtime.cards.addCard(statusId, CardDraft("Created through API"))

            assertEquals(cardId, runtime.boards.board(boardId)?.cards?.single()?.id)
            assertTrue(runtime.templates.templates.isNotEmpty())

            val snapshot = runtime.backup.createSnapshot("runtime-test")
            assertTrue(snapshot.boards.any { it.id == boardId.value })

            val archive = runtime.transfer.exportBoards(
                boardIds = setOf(boardId),
                sourceApp = "runtime-test",
                passphrase = passphrase,
            )
            assertTrue(runtime.transfer.isSupportedArchive(archive))
            transferredBoardId = runtime.transfer.importBoards(archive, passphrase).firstBoardId
            restoredBoardId = runtime.backup.importSnapshot(
                snapshot.copy(boards = snapshot.boards.filter { it.id == boardId.value }),
            ).firstBoardId

            assertTrue(transferredBoardId != null && transferredBoardId != boardId)
            assertTrue(restoredBoardId != null && restoredBoardId != boardId)
            assertTrue(runtime.boards.observeBoardSummaries().first().any { it.board.id == boardId })
        } finally {
            passphrase.fill('\u0000')
            transferredBoardId?.let { runtime.boards.deleteBoard(it) }
            restoredBoardId?.let { runtime.boards.deleteBoard(it) }
            runtime.boards.deleteBoard(boardId)
        }
    }
}
