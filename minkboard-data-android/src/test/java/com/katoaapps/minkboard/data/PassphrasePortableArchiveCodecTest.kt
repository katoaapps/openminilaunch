package com.katoaapps.minkboard.data

import com.katoaapps.minkboard.model.PortableBoardSnapshot
import com.katoaapps.minkboard.model.PortableCardSnapshot
import com.katoaapps.minkboard.model.PortableStatusSnapshot
import com.katoaapps.minkboard.model.PortableSubtaskSnapshot
import com.katoaapps.minkboard.model.PortableWorkspaceSnapshot
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PassphrasePortableArchiveCodecTest {
    private val codec = PassphrasePortableArchiveCodec(writeIterations = 10_000)
    private val passphrase = "correct horse battery staple".toCharArray()

    @Test
    fun jsonRoundTripPreservesEveryField() {
        val json = PortableArchiveJsonCodec()
        val source = fullSnapshot()

        assertEquals(source, json.decode(json.encode(source)))
    }

    @Test
    fun encryptedRoundTripPreservesEveryField() {
        val source = fullSnapshot()

        val archive = codec.encode(source, passphrase)

        assertEquals(source, codec.decode(archive, passphrase))
    }

    @Test
    fun repeatedExportsUseFreshSaltAndNonce() {
        val source = fullSnapshot()

        val first = codec.encode(source, passphrase)
        val second = codec.encode(source, passphrase)

        assertNotEquals(String(first), String(second))
        assertEquals(source, codec.decode(first, passphrase))
        assertEquals(source, codec.decode(second, passphrase))
    }

    @Test
    fun wrongPassphraseAndDamageFailClosed() {
        val archive = codec.encode(fullSnapshot(), passphrase)

        assertThrows(InvalidPortableArchiveException::class.java) {
            codec.decode(archive, "definitely wrong".toCharArray())
        }

        val damaged = archive.copyOf().also { bytes ->
            val index = bytes.indexOfLast { it == 'A'.code.toByte() }
                .takeIf { it >= 0 }
                ?: bytes.lastIndex - 2
            bytes[index] = if (bytes[index] == 'A'.code.toByte()) 'B'.code.toByte() else 'A'.code.toByte()
        }
        assertThrows(InvalidPortableArchiveException::class.java) {
            codec.decode(damaged, passphrase)
        }
    }

    @Test
    fun unsupportedEnvelopeAndShortPassphraseAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            codec.encode(fullSnapshot(), "short".toCharArray())
        }
        assertThrows(InvalidPortableArchiveException::class.java) {
            codec.decode("{}".toByteArray(), passphrase)
        }
    }

    @Test
    fun callerPassphraseIsNotModified() {
        val before = passphrase.copyOf()

        codec.encode(fullSnapshot(), passphrase)

        assertArrayEquals(before, passphrase)
    }

    @Test
    fun formatProbeRecognizesOnlyEncryptedMinkBoardEnvelope() {
        val archive = codec.encode(fullSnapshot(), passphrase)

        assertEquals(true, codec.isSupportedEnvelope(archive))
        assertEquals(false, codec.isSupportedEnvelope("{}".toByteArray()))
        assertEquals(
            false,
            codec.isSupportedEnvelope("{\"format\":\"spencer-full-handoff\"}".toByteArray()),
        )
    }

    private fun fullSnapshot(): PortableWorkspaceSnapshot = PortableWorkspaceSnapshot(
        exportedAtEpochMillis = 1_700_000_000_000,
        sourceApp = "MinkBoard 0.1.0",
        boards = listOf(
            PortableBoardSnapshot(
                id = "board-1",
                name = "Launch خطة",
                isLocked = true,
                position = 0,
                createdAtEpochMillis = 100,
                statuses = listOf(
                    PortableStatusSnapshot(
                        id = "status-1",
                        name = "Done ✓",
                        colorArgb = 0xFF602C00.toInt(),
                        isDoneStatus = true,
                        position = 0,
                        cards = listOf(
                            PortableCardSnapshot(
                                id = "card-1",
                                title = "Ship the app",
                                details = "Preserve Unicode: café 🦦",
                                dueDateIso = null,
                                isMajor = true,
                                position = 0,
                                createdAtEpochMillis = 200,
                                updatedAtEpochMillis = 300,
                                subtasks = listOf(
                                    PortableSubtaskSnapshot(
                                        id = "subtask-1",
                                        text = "Run release checks",
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
}
