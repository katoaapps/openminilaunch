package com.katoaapps.minkboard.data

import com.katoaapps.minkboard.core.LauncherBackupEnvelopeDecodeResult
import com.katoaapps.minkboard.core.LauncherBackupEnvelopeEncodeResult
import com.katoaapps.minkboard.core.LauncherBoardBackupDesignation
import com.katoaapps.minkboard.core.LauncherMinkBoardBackup
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.PortableBoardSnapshot
import com.katoaapps.minkboard.model.PortableCardSnapshot
import com.katoaapps.minkboard.model.PortableStatusSnapshot
import com.katoaapps.minkboard.model.PortableSubtaskSnapshot
import com.katoaapps.minkboard.model.PortableWorkspaceSnapshot
import com.katoaapps.minkboard.model.StatusId
import java.security.SecureRandom
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PassphraseLauncherBackupEnvelopeCodecTest {
    private val codec = codec()
    private val secret = "correct horse battery staple".toCharArray()

    @Test
    fun encryptedRoundTripPreservesUnicodeWorkspaceAndPortableDesignation() {
        val source = populatedBackup()

        val encoded = codec.encode(source, secret) as LauncherBackupEnvelopeEncodeResult.Encrypted
        val decoded = codec.decode(encoded.envelope, secret)

        assertEquals(LauncherBackupEnvelopeDecodeResult.Decoded(source), decoded)
    }

    @Test
    fun repeatedExportsUseFreshCiphertextAndRevealNoUserAuthoredPlaintext() {
        val source = populatedBackup()

        val first = (codec.encode(source, secret) as LauncherBackupEnvelopeEncodeResult.Encrypted)
            .envelope
        val second = (codec.encode(source, secret) as LauncherBackupEnvelopeEncodeResult.Encrypted)
            .envelope
        val visible = first.toString(Charsets.UTF_8)

        assertNotEquals(first.toList(), second.toList())
        listOf(
            "Launch خطة",
            "Ship the app",
            "Preserve Unicode: café 🦦",
            "Run release checks",
            "board-1",
            "intake-status",
        ).forEach { marker -> assertFalse(visible.contains(marker)) }
        assertFalse(PassphrasePortableArchiveCodec(writeIterations = 10_000)
            .isSupportedEnvelope(first))
    }

    @Test
    fun explicitEmptyPresentRoundTripsAndDoesNotBecomeMissing() {
        val source = LauncherMinkBoardBackup.Present(
            workspace = PortableWorkspaceSnapshot(
                exportedAtEpochMillis = 4L,
                sourceApp = "MinkLauncher test",
                boards = emptyList(),
            ),
            designation = null,
        )

        val envelope = (codec.encode(source, secret) as
            LauncherBackupEnvelopeEncodeResult.Encrypted).envelope

        assertEquals(
            LauncherBackupEnvelopeDecodeResult.Decoded(source),
            codec.decode(envelope, secret),
        )
    }

    @Test
    fun wrongSecretTamperAndTruncationFailClosed() {
        val envelope = encoded(populatedBackup())

        assertEquals(
            LauncherBackupEnvelopeDecodeResult.Corrupt,
            codec.decode(envelope, "definitely wrong".toCharArray()),
        )

        val outer = JSONObject(envelope.toString(Charsets.UTF_8))
        val ciphertext = outer.getString("ciphertext").toCharArray()
        ciphertext[ciphertext.lastIndex / 2] = if (ciphertext[ciphertext.lastIndex / 2] == 'A') {
            'B'
        } else {
            'A'
        }
        outer.put("ciphertext", String(ciphertext))
        assertEquals(
            LauncherBackupEnvelopeDecodeResult.Corrupt,
            codec.decode(outer.toString().toByteArray(), secret),
        )

        assertEquals(
            LauncherBackupEnvelopeDecodeResult.Corrupt,
            codec.decode(envelope.copyOf(envelope.size / 2), secret),
        )
    }

    @Test
    fun malformedMissingAndInvalidBase64FieldsAreCorrupt() {
        assertEquals(
            LauncherBackupEnvelopeDecodeResult.Corrupt,
            codec.decode(byteArrayOf(), secret),
        )
        assertEquals(
            LauncherBackupEnvelopeDecodeResult.Corrupt,
            codec.decode("not-json".toByteArray(), secret),
        )
        assertEquals(
            LauncherBackupEnvelopeDecodeResult.Corrupt,
            codec.decode("{}".toByteArray(), secret),
        )

        val outer = JSONObject(encoded(populatedBackup()).toString(Charsets.UTF_8))
        outer.put("nonce", "not base64 %%%")
        assertEquals(
            LauncherBackupEnvelopeDecodeResult.Corrupt,
            codec.decode(outer.toString().toByteArray(), secret),
        )
    }

    @Test
    fun unsupportedEnvelopeAndPayloadVersionsRemainDistinctFromCorruption() {
        val outer = JSONObject(encoded(populatedBackup()).toString(Charsets.UTF_8))
        outer.put("envelopeVersion", 99)
        assertEquals(
            LauncherBackupEnvelopeDecodeResult.Unsupported,
            codec.decode(outer.toString().toByteArray(), secret),
        )

        val unsupportedPayload = JSONObject()
            .put("format", LauncherBackupPayloadJsonCodec.PAYLOAD_FORMAT)
            .put("schemaVersion", 99)
            .toString()
        val encrypted = LauncherBackupEnvelopeCrypto(
            writeIterations = 10_000,
        ).encode(unsupportedPayload, secret)
        assertEquals(
            LauncherBackupEnvelopeDecodeResult.Unsupported,
            codec.decode(encrypted, secret),
        )

        val nestedWorkspaceVersion = JSONObject(
            LauncherBackupPayloadJsonCodec().encode(populatedBackup()),
        )
        nestedWorkspaceVersion.getJSONObject("workspace").put("schemaVersion", 99)
        val nestedEncrypted = LauncherBackupEnvelopeCrypto(
            writeIterations = 10_000,
        ).encode(nestedWorkspaceVersion.toString(), secret)
        assertEquals(
            LauncherBackupEnvelopeDecodeResult.Unsupported,
            codec.decode(nestedEncrypted, secret),
        )

        val malformedPayload = LauncherBackupEnvelopeCrypto(
            writeIterations = 10_000,
        ).encode("{}", secret)
        assertEquals(
            LauncherBackupEnvelopeDecodeResult.Corrupt,
            codec.decode(malformedPayload, secret),
        )
    }

    @Test
    fun authenticatedMetadataTamperIsCorruptAndExcessiveKdfWorkIsRejected() {
        val outer = JSONObject(encoded(populatedBackup()).toString(Charsets.UTF_8))
        outer.put("kdfIterations", 10_001)
        assertEquals(
            LauncherBackupEnvelopeDecodeResult.Corrupt,
            codec.decode(outer.toString().toByteArray(), secret),
        )

        outer.put("kdfIterations", LauncherBackupEnvelopeCrypto.MAX_KDF_ITERATIONS + 1)
        assertEquals(
            LauncherBackupEnvelopeDecodeResult.Unsupported,
            codec.decode(outer.toString().toByteArray(), secret),
        )
    }

    @Test
    fun invalidPortableDesignationOrWorkspaceFailsBeforeEncryption() {
        val valid = populatedBackup()
        val doneStatusId = valid.workspace.boards.single().statuses.last().id

        listOf(
            valid.copy(designation = null),
            valid.copy(
                designation = LauncherBoardBackupDesignation(
                    BoardId("missing-board"),
                    StatusId("intake-status"),
                ),
            ),
            valid.copy(
                designation = LauncherBoardBackupDesignation(
                    BoardId("board-1"),
                    StatusId("missing-status"),
                ),
            ),
            valid.copy(
                designation = LauncherBoardBackupDesignation(
                    BoardId("board-1"),
                    StatusId(doneStatusId),
                ),
            ),
            LauncherMinkBoardBackup.Present(
                valid.workspace.copy(boards = emptyList()),
                valid.designation,
            ),
        ).forEach { invalid ->
            assertEquals(LauncherBackupEnvelopeEncodeResult.Failed, codec.encode(invalid, secret))
        }
    }

    @Test
    fun invalidDesignationInsideAuthenticatedPayloadIsCorrupt() {
        val validPayload = JSONObject(
            LauncherBackupPayloadJsonCodec().encode(populatedBackup()),
        )
        validPayload.getJSONObject("designation").put("intakeStatusId", "missing-status")
        val encrypted = LauncherBackupEnvelopeCrypto(
            writeIterations = 10_000,
        ).encode(validPayload.toString(), secret)

        assertEquals(
            LauncherBackupEnvelopeDecodeResult.Corrupt,
            codec.decode(encrypted, secret),
        )
    }

    @Test
    fun missingDesignationOrWorkspaceInsideAuthenticatedPayloadIsCorrupt() {
        val source = JSONObject(LauncherBackupPayloadJsonCodec().encode(populatedBackup()))
        listOf("designation", "workspace").forEach { missingField ->
            val malformed = JSONObject(source.toString()).apply { remove(missingField) }
            val encrypted = LauncherBackupEnvelopeCrypto(
                writeIterations = 10_000,
            ).encode(malformed.toString(), secret)

            assertEquals(
                LauncherBackupEnvelopeDecodeResult.Corrupt,
                codec.decode(encrypted, secret),
            )
        }
    }

    @Test
    fun passphraseAndArchiveBoundsAreEnforcedWithoutChangingCallerSecret() {
        val before = secret.copyOf()
        val envelope = encoded(populatedBackup())

        codec.decode(envelope, secret)

        assertArrayEquals(before, secret)
        assertEquals(
            LauncherBackupEnvelopeEncodeResult.Failed,
            codec.encode(populatedBackup(), "short".toCharArray()),
        )
        assertEquals(
            LauncherBackupEnvelopeDecodeResult.Corrupt,
            codec.decode(
                ByteArray(LauncherBackupEnvelopeCrypto.MAX_ARCHIVE_BYTES + 1),
                secret,
            ),
        )
        assertThrows(IllegalArgumentException::class.java) {
            LauncherBackupEnvelopeCrypto(writeIterations = 9_999)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LauncherBackupEnvelopeCrypto(
                writeIterations = LauncherBackupEnvelopeCrypto.MAX_KDF_ITERATIONS + 1,
            )
        }
    }

    @Test
    fun payloadSizeCapStopsOversizedPlaintextBeforeEncryption() {
        val crypto = LauncherBackupEnvelopeCrypto(writeIterations = 10_000)

        assertThrows(IllegalArgumentException::class.java) {
            crypto.encode("x".repeat(23 * 1024 * 1024 + 1), secret)
        }
    }

    private fun encoded(backup: LauncherMinkBoardBackup.Present): ByteArray =
        (codec.encode(backup, secret) as LauncherBackupEnvelopeEncodeResult.Encrypted).envelope

    private fun codec() = PassphraseLauncherBackupEnvelopeCodec(
        random = SecureRandom(),
        writeIterations = 10_000,
    )

    private fun populatedBackup(): LauncherMinkBoardBackup.Present {
        val workspace = PortableWorkspaceSnapshot(
            exportedAtEpochMillis = 1_700_000_000_000,
            sourceApp = "MinkLauncher test",
            boards = listOf(
                PortableBoardSnapshot(
                    id = "board-1",
                    name = "Launch خطة",
                    isLocked = true,
                    position = 0,
                    createdAtEpochMillis = 100,
                    statuses = listOf(
                        PortableStatusSnapshot(
                            id = "intake-status",
                            name = "To do",
                            colorArgb = 0xFF602C00.toInt(),
                            isDoneStatus = false,
                            position = 0,
                            cards = listOf(
                                PortableCardSnapshot(
                                    id = "card-1",
                                    title = "Ship the app",
                                    details = "Preserve Unicode: café 🦦",
                                    dueDateIso = "2027-03-04",
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
                        PortableStatusSnapshot(
                            id = "done-status",
                            name = "Done",
                            colorArgb = 0xFF336699.toInt(),
                            isDoneStatus = true,
                            position = 1,
                            cards = emptyList(),
                        ),
                    ),
                ),
            ),
        )
        return LauncherMinkBoardBackup.Present(
            workspace = workspace,
            designation = LauncherBoardBackupDesignation(
                boardId = BoardId("board-1"),
                intakeStatusId = StatusId("intake-status"),
            ),
        )
    }
}
