package com.katoaapps.openminilaunch.features.backup

import com.katoaapps.minkboard.data.PassphraseAesGcmEnvelopeCodec
import com.katoaapps.minkboard.data.PassphraseEnvelopeConfig
import com.katoaapps.minkboard.data.PassphraseEnvelopeDecodeResult
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.minklauncher.todos.home.TodoVisibleStatuses
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import org.json.JSONArray
import org.json.JSONObject

internal data class LauncherBoardHomeBackup(
    /** Null means all statuses; a non-empty list means an explicit ordered selection. */
    val resolveTodoVisibleStatusIds: List<String>?,
)

internal sealed interface LauncherBoardHomeDecodeResult {
    data class Decoded(val backup: LauncherBoardHomeBackup) : LauncherBoardHomeDecodeResult
    data object Unsupported : LauncherBoardHomeDecodeResult
    data object Corrupt : LauncherBoardHomeDecodeResult
}

/**
 * Encrypts launcher-owned presentation metadata separately from the canonical board envelope.
 * The shared data module owns the low-level passphrase cryptography; this class owns only the
 * OpenMink JSON schema and its validation.
 */
internal class LauncherBoardHomeBackupCodec(
    random: SecureRandom = SecureRandom(),
    writeIterations: Int = DEFAULT_KDF_ITERATIONS,
) {
    private val envelopeCodec = PassphraseAesGcmEnvelopeCodec(
        config = ENVELOPE_CONFIG,
        random = random,
        writeIterations = writeIterations,
    )

    fun encode(visibility: TodoVisibleStatuses, secret: CharArray): ByteArray? = runCatching {
        val payload = JSONObject()
            .put("schemaVersion", PAYLOAD_VERSION)
            .apply {
                when (visibility) {
                    TodoVisibleStatuses.All -> put("mode", MODE_ALL)
                    is TodoVisibleStatuses.Selected -> {
                        put("mode", MODE_SELECTED)
                        put(
                            "statusIds",
                            JSONArray().apply {
                                visibility.statusIds
                                    .distinctBy(StatusId::value)
                                    .take(MAX_STATUS_IDS)
                                    .forEach { statusId -> put(statusId.value) }
                            },
                        )
                    }
                }
            }
            .toString()
            .toByteArray(StandardCharsets.UTF_8)
        try {
            envelopeCodec.encode(payload, secret)
        } finally {
            payload.fill(0)
        }
    }.getOrNull()

    fun decode(envelope: ByteArray, secret: CharArray): LauncherBoardHomeDecodeResult =
        when (val decrypted = envelopeCodec.decode(envelope, secret)) {
            PassphraseEnvelopeDecodeResult.Unsupported -> LauncherBoardHomeDecodeResult.Unsupported
            PassphraseEnvelopeDecodeResult.Corrupt -> LauncherBoardHomeDecodeResult.Corrupt
            is PassphraseEnvelopeDecodeResult.Decoded -> parsePayload(decrypted.plaintext)
        }

    private fun parsePayload(plaintext: ByteArray): LauncherBoardHomeDecodeResult {
        return try {
            val payload = JSONObject(String(plaintext, StandardCharsets.UTF_8))
            if (payload.optInt("schemaVersion", -1) != PAYLOAD_VERSION) {
                return LauncherBoardHomeDecodeResult.Unsupported
            }
            when (payload.getString("mode")) {
            MODE_ALL -> LauncherBoardHomeDecodeResult.Decoded(
                LauncherBoardHomeBackup(resolveTodoVisibleStatusIds = null),
            )
            MODE_SELECTED -> {
                val ids = payload.getJSONArray("statusIds")
                require(ids.length() in 1..MAX_STATUS_IDS)
                LauncherBoardHomeDecodeResult.Decoded(
                    LauncherBoardHomeBackup(
                        resolveTodoVisibleStatusIds = buildList {
                            repeat(ids.length()) { index ->
                                ids.getString(index)
                                    .also { require(it.isNotBlank() && it.length <= MAX_ID_LENGTH) }
                                    .let(::add)
                            }
                        }.distinct(),
                    ),
                )
            }
                else -> LauncherBoardHomeDecodeResult.Corrupt
            }
        } catch (_: Exception) {
            LauncherBoardHomeDecodeResult.Corrupt
        } finally {
            plaintext.fill(0)
        }
    }

    private companion object {
        const val ENVELOPE_FORMAT = "openmink-minkboard-home-settings"
        const val PAYLOAD_VERSION = 1
        const val MODE_ALL = "all"
        const val MODE_SELECTED = "selected"
        const val DEFAULT_KDF_ITERATIONS = 210_000
        const val MAX_ENVELOPE_BYTES = 64 * 1024
        const val MAX_STATUS_IDS = 64
        const val MAX_ID_LENGTH = 200

        val ENVELOPE_CONFIG = PassphraseEnvelopeConfig(
            format = ENVELOPE_FORMAT,
            maxEnvelopeBytes = MAX_ENVELOPE_BYTES,
            maxPlaintextBytes = MAX_ENVELOPE_BYTES,
        )
    }
}
