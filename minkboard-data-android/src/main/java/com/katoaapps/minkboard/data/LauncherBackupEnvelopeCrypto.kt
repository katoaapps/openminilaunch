package com.katoaapps.minkboard.data

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import org.json.JSONObject

internal class InvalidLauncherBackupEnvelopeException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

internal class UnsupportedLauncherBackupEnvelopeException(message: String) :
    IllegalArgumentException(message)

/**
 * Passphrase encryption for the launcher-specific backup payload.
 *
 * This has a deliberately distinct format and AAD from the standalone portable archive. Changing
 * either format must not make one codec accept the other product's payload by accident.
 */
internal class LauncherBackupEnvelopeCrypto(
    private val random: SecureRandom = SecureRandom(),
    private val writeIterations: Int = DEFAULT_KDF_ITERATIONS,
) {
    private val envelopeCodec = PassphraseAesGcmEnvelopeCodec(
        config = PassphraseEnvelopeConfig(
            format = ENVELOPE_FORMAT,
            envelopeVersion = ENVELOPE_VERSION,
            defaultKdfIterations = DEFAULT_KDF_ITERATIONS,
            minKdfIterations = MIN_KDF_ITERATIONS,
            maxKdfIterations = MAX_KDF_ITERATIONS,
            minPassphraseLength = MIN_PASSPHRASE_LENGTH,
            maxPassphraseLength = MAX_PASSPHRASE_LENGTH,
            maxEnvelopeBytes = MAX_ARCHIVE_BYTES,
            maxPlaintextBytes = MAX_ARCHIVE_BYTES,
        ),
        random = random,
        writeIterations = writeIterations,
    )

    init {
        require(writeIterations in MIN_KDF_ITERATIONS..MAX_KDF_ITERATIONS)
    }

    fun isSupportedEnvelope(archive: ByteArray): Boolean {
        if (archive.isEmpty() || archive.size > MAX_ARCHIVE_BYTES) return false
        return runCatching {
            val envelope = JSONObject(String(archive, StandardCharsets.UTF_8))
            envelope.optString("format") == ENVELOPE_FORMAT &&
                envelope.optInt("envelopeVersion", -1) == ENVELOPE_VERSION
        }.getOrDefault(false)
    }

    fun encode(payloadJson: String, passphrase: CharArray): ByteArray {
        requireValidPassphrase(passphrase)
        val payloadBytes = payloadJson.toByteArray(StandardCharsets.UTF_8)
        val plaintext = try {
            require(payloadBytes.size <= MAX_PAYLOAD_BYTES) { "Launcher backup payload is too large" }
            JSONObject()
                .put("checksumSha256", sha256(payloadBytes).toHex())
                .put("payloadJson", payloadJson)
                .toString()
                .toByteArray(StandardCharsets.UTF_8)
        } finally {
            payloadBytes.fill(0)
        }

        return try {
            envelopeCodec.encode(plaintext, passphrase)
        } finally {
            plaintext.fill(0)
        }
    }

    fun decode(archive: ByteArray, passphrase: CharArray): String {
        requireValidPassphrase(passphrase)
        if (archive.isEmpty() || archive.size > MAX_ARCHIVE_BYTES) {
            throw InvalidLauncherBackupEnvelopeException("Launcher backup archive size is invalid")
        }
        val envelope = try {
            JSONObject(String(archive, StandardCharsets.UTF_8))
        } catch (failure: Exception) {
            throw InvalidLauncherBackupEnvelopeException(
                "Launcher backup archive is not valid JSON",
                failure,
            )
        }

        val format = envelope.optString("format")
        if (format.isBlank()) {
            throw InvalidLauncherBackupEnvelopeException(
                "Launcher backup archive format is missing",
            )
        }
        if (format != ENVELOPE_FORMAT) {
            throw UnsupportedLauncherBackupEnvelopeException(
                "Launcher backup archive format is unsupported",
            )
        }
        if (!envelope.has("envelopeVersion")) {
            throw InvalidLauncherBackupEnvelopeException(
                "Launcher backup archive version is missing",
            )
        }
        val envelopeVersion = try {
            envelope.getInt("envelopeVersion")
        } catch (failure: Exception) {
            throw InvalidLauncherBackupEnvelopeException(
                "Launcher backup archive version is malformed",
                failure,
            )
        }
        if (envelopeVersion != ENVELOPE_VERSION) {
            throw UnsupportedLauncherBackupEnvelopeException(
                "Launcher backup archive version is unsupported",
            )
        }

        val iterations = try {
            if (envelope.getString("kdf") != KDF_NAME ||
                envelope.getString("cipher") != CIPHER_NAME
            ) {
                throw UnsupportedLauncherBackupEnvelopeException(
                    "Launcher backup archive encryption is unsupported",
                )
            }
            envelope.getInt("kdfIterations").also {
                if (it !in MIN_KDF_ITERATIONS..MAX_KDF_ITERATIONS) {
                    throw UnsupportedLauncherBackupEnvelopeException(
                        "Launcher backup archive KDF work factor is unsupported",
                    )
                }
            }
        } catch (failure: UnsupportedLauncherBackupEnvelopeException) {
            throw failure
        } catch (failure: Exception) {
            throw InvalidLauncherBackupEnvelopeException(
                "Launcher backup archive envelope is malformed",
                failure,
            )
        }

        val plaintext = when (val decoded = envelopeCodec.decode(archive, passphrase)) {
            is PassphraseEnvelopeDecodeResult.Decoded -> decoded.plaintext
            PassphraseEnvelopeDecodeResult.Unsupported -> {
                throw UnsupportedLauncherBackupEnvelopeException(
                    "Launcher backup archive encryption is unsupported",
                )
            }
            PassphraseEnvelopeDecodeResult.Corrupt -> {
                throw InvalidLauncherBackupEnvelopeException(
                    "Passphrase is incorrect or the launcher backup was damaged",
                )
            }
        }

        try {
            val wrapper = JSONObject(String(plaintext, StandardCharsets.UTF_8))
            val payloadJson = wrapper.getString("payloadJson")
            val expectedChecksum = wrapper.getString("checksumSha256")
            val payloadBytes = payloadJson.toByteArray(StandardCharsets.UTF_8)
            val actualChecksum = try {
                sha256(payloadBytes).toHex()
            } finally {
                payloadBytes.fill(0)
            }
            if (!MessageDigest.isEqual(
                    expectedChecksum.toByteArray(StandardCharsets.US_ASCII),
                    actualChecksum.toByteArray(StandardCharsets.US_ASCII),
                )
            ) {
                throw InvalidLauncherBackupEnvelopeException(
                    "Launcher backup archive checksum is invalid",
                )
            }
            return payloadJson
        } catch (failure: InvalidLauncherBackupEnvelopeException) {
            throw failure
        } catch (failure: Exception) {
            throw InvalidLauncherBackupEnvelopeException(
                "Launcher backup archive payload is invalid",
                failure,
            )
        } finally {
            plaintext.fill(0)
        }
    }

    private fun requireValidPassphrase(passphrase: CharArray) {
        require(passphrase.size in MIN_PASSPHRASE_LENGTH..MAX_PASSPHRASE_LENGTH) {
            "Passphrase must contain at least $MIN_PASSPHRASE_LENGTH characters"
        }
    }

    private fun sha256(value: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(value)

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    companion object {
        const val ENVELOPE_FORMAT = "minkboard-encrypted-launcher-backup"
        const val ENVELOPE_VERSION = 1
        const val DEFAULT_KDF_ITERATIONS = 210_000
        const val MIN_KDF_ITERATIONS = 10_000
        const val MAX_KDF_ITERATIONS = 1_000_000
        const val MIN_PASSPHRASE_LENGTH = 8
        const val MAX_PASSPHRASE_LENGTH = 1_024
        const val MAX_ARCHIVE_BYTES = 32 * 1024 * 1024

        private const val MAX_PAYLOAD_BYTES = 23 * 1024 * 1024
        private const val KDF_NAME = "PBKDF2-HMAC-SHA256"
        private const val CIPHER_NAME = "AES-256-GCM"
    }
}
