package com.katoaapps.openminilaunch.features.backup

import com.katoaapps.openminilaunch.features.minkboard.LauncherVisibleStatuses
import com.katoaapps.minkboard.model.StatusId
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONArray
import org.json.JSONObject

internal data class LauncherBoardHomeBackup(
    /** Null means all statuses; a non-empty list means an explicit ordered selection. */
    val visibleStatusIds: List<String>?,
)

internal sealed interface LauncherBoardHomeDecodeResult {
    data class Decoded(val backup: LauncherBoardHomeBackup) : LauncherBoardHomeDecodeResult
    data object Unsupported : LauncherBoardHomeDecodeResult
    data object Corrupt : LauncherBoardHomeDecodeResult
}

/**
 * Encrypts launcher-owned MinkBoard presentation metadata separately from the canonical board
 * envelope. Status IDs must remain private and must travel with the same passphrase as board data.
 */
internal class LauncherBoardHomeBackupCodec(
    private val random: SecureRandom = SecureRandom(),
    private val writeIterations: Int = DEFAULT_KDF_ITERATIONS,
) {
    init {
        require(writeIterations in MIN_KDF_ITERATIONS..MAX_KDF_ITERATIONS)
    }

    fun encode(visibility: LauncherVisibleStatuses, secret: CharArray): ByteArray? = runCatching {
        requireValidSecret(secret)
        val payload = JSONObject()
            .put("schemaVersion", PAYLOAD_VERSION)
            .apply {
                when (visibility) {
                    LauncherVisibleStatuses.All -> put("mode", MODE_ALL)
                    is LauncherVisibleStatuses.Selected -> {
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
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(GCM_NONCE_BYTES).also(random::nextBytes)
        val keyBytes = deriveKeyBytes(secret, salt, writeIterations)
        val ciphertext = try {
            Cipher.getInstance(CIPHER_ALGORITHM).run {
                init(
                    Cipher.ENCRYPT_MODE,
                    SecretKeySpec(keyBytes, "AES"),
                    GCMParameterSpec(GCM_TAG_BITS, nonce),
                )
                updateAAD(aad(writeIterations))
                doFinal(payload)
            }
        } finally {
            keyBytes.fill(0)
            payload.fill(0)
        }
        JSONObject()
            .put("format", ENVELOPE_FORMAT)
            .put("envelopeVersion", ENVELOPE_VERSION)
            .put("kdf", KDF_NAME)
            .put("kdfIterations", writeIterations)
            .put("salt", Base64.getEncoder().encodeToString(salt))
            .put("cipher", CIPHER_NAME)
            .put("nonce", Base64.getEncoder().encodeToString(nonce))
            .put("ciphertext", Base64.getEncoder().encodeToString(ciphertext))
            .toString()
            .toByteArray(StandardCharsets.UTF_8)
            .also { require(it.size in 1..MAX_ENVELOPE_BYTES) }
    }.getOrNull()

    fun decode(envelope: ByteArray, secret: CharArray): LauncherBoardHomeDecodeResult {
        if (envelope.isEmpty() || envelope.size > MAX_ENVELOPE_BYTES) {
            return LauncherBoardHomeDecodeResult.Corrupt
        }
        return try {
            requireValidSecret(secret)
            val root = JSONObject(String(envelope, StandardCharsets.UTF_8))
            if (root.optString("format") != ENVELOPE_FORMAT) {
                return LauncherBoardHomeDecodeResult.Unsupported
            }
            if (root.optInt("envelopeVersion", -1) != ENVELOPE_VERSION ||
                root.optString("kdf") != KDF_NAME || root.optString("cipher") != CIPHER_NAME
            ) {
                return LauncherBoardHomeDecodeResult.Unsupported
            }
            val iterations = root.getInt("kdfIterations")
            if (iterations !in MIN_KDF_ITERATIONS..MAX_KDF_ITERATIONS) {
                return LauncherBoardHomeDecodeResult.Unsupported
            }
            val salt = decodeFixed(root, "salt", SALT_BYTES)
            val nonce = decodeFixed(root, "nonce", GCM_NONCE_BYTES)
            val ciphertext = Base64.getDecoder().decode(root.getString("ciphertext"))
            require(ciphertext.size in (GCM_TAG_BITS / 8)..MAX_ENVELOPE_BYTES)
            val keyBytes = deriveKeyBytes(secret, salt, iterations)
            val plaintext = try {
                Cipher.getInstance(CIPHER_ALGORITHM).run {
                    init(
                        Cipher.DECRYPT_MODE,
                        SecretKeySpec(keyBytes, "AES"),
                        GCMParameterSpec(GCM_TAG_BITS, nonce),
                    )
                    updateAAD(aad(iterations))
                    doFinal(ciphertext)
                }
            } finally {
                keyBytes.fill(0)
            }
            try {
                val payload = JSONObject(String(plaintext, StandardCharsets.UTF_8))
                if (payload.optInt("schemaVersion", -1) != PAYLOAD_VERSION) {
                    return LauncherBoardHomeDecodeResult.Unsupported
                }
                val backup = when (payload.getString("mode")) {
                    MODE_ALL -> LauncherBoardHomeBackup(visibleStatusIds = null)
                    MODE_SELECTED -> {
                        val ids = payload.getJSONArray("statusIds")
                        require(ids.length() in 1..MAX_STATUS_IDS)
                        LauncherBoardHomeBackup(
                            visibleStatusIds = buildList {
                                repeat(ids.length()) { index ->
                                    ids.getString(index)
                                        .also { require(it.isNotBlank() && it.length <= MAX_ID_LENGTH) }
                                        .let(::add)
                                }
                            }.distinct(),
                        )
                    }
                    else -> return LauncherBoardHomeDecodeResult.Corrupt
                }
                LauncherBoardHomeDecodeResult.Decoded(backup)
            } finally {
                plaintext.fill(0)
            }
        } catch (_: AEADBadTagException) {
            LauncherBoardHomeDecodeResult.Corrupt
        } catch (_: Exception) {
            LauncherBoardHomeDecodeResult.Corrupt
        }
    }

    private fun deriveKeyBytes(secret: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(secret, salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(KDF_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun decodeFixed(root: JSONObject, name: String, expectedSize: Int): ByteArray =
        Base64.getDecoder().decode(root.getString(name)).also { require(it.size == expectedSize) }

    private fun requireValidSecret(secret: CharArray) {
        require(secret.size in MIN_PASSPHRASE_LENGTH..MAX_PASSPHRASE_LENGTH)
    }

    private fun aad(iterations: Int): ByteArray =
        "$ENVELOPE_FORMAT|$ENVELOPE_VERSION|$KDF_NAME|$iterations|$CIPHER_NAME"
            .toByteArray(StandardCharsets.UTF_8)

    private companion object {
        const val ENVELOPE_FORMAT = "openmink-minkboard-home-settings"
        const val ENVELOPE_VERSION = 1
        const val PAYLOAD_VERSION = 1
        const val MODE_ALL = "all"
        const val MODE_SELECTED = "selected"
        const val DEFAULT_KDF_ITERATIONS = 210_000
        const val MIN_KDF_ITERATIONS = 10_000
        const val MAX_KDF_ITERATIONS = 1_000_000
        const val MIN_PASSPHRASE_LENGTH = 8
        const val MAX_PASSPHRASE_LENGTH = 1_024
        const val MAX_ENVELOPE_BYTES = 64 * 1024
        const val MAX_STATUS_IDS = 64
        const val MAX_ID_LENGTH = 200
        const val KDF_NAME = "PBKDF2-HMAC-SHA256"
        const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
        const val CIPHER_NAME = "AES-256-GCM"
        const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
        const val SALT_BYTES = 16
        const val GCM_NONCE_BYTES = 12
        const val GCM_TAG_BITS = 128
        const val KEY_BITS = 256
    }
}
