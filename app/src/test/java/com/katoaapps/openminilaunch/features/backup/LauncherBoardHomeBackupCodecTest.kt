package com.katoaapps.openminilaunch.features.backup

import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.minklauncher.todos.home.TodoVisibleStatuses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherBoardHomeBackupCodecTest {
    private val codec = LauncherBoardHomeBackupCodec()

    @Test fun allStatusesRoundTrip() {
        val secret = "minktest123".toCharArray()
        val envelope = checkNotNull(codec.encode(TodoVisibleStatuses.All, secret))

        assertEquals(
            LauncherBoardHomeDecodeResult.Decoded(LauncherBoardHomeBackup(null)),
            codec.decode(envelope, secret),
        )
    }

    @Test fun selectedStatusesRemainOrderedAndEncrypted() {
        val secret = "minktest123".toCharArray()
        val visibility = TodoVisibleStatuses.Selected(
            listOf(StatusId("todo-private-id"), StatusId("done-private-id")),
        )
        val envelope = checkNotNull(codec.encode(visibility, secret))
        val restored = codec.decode(envelope, secret) as LauncherBoardHomeDecodeResult.Decoded

        assertEquals(
            listOf("todo-private-id", "done-private-id"),
            restored.backup.resolveTodoVisibleStatusIds,
        )
        assertFalse(String(envelope).contains("todo-private-id"))
    }

    @Test fun wrongPassphraseFailsClosed() {
        val envelope = checkNotNull(
            codec.encode(TodoVisibleStatuses.All, "minktest123".toCharArray()),
        )

        assertTrue(
            codec.decode(envelope, "another-password".toCharArray()) is
                LauncherBoardHomeDecodeResult.Corrupt,
        )
    }
}
