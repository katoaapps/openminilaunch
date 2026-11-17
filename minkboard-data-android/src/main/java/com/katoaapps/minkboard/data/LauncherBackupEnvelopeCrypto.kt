package com.katoaapps.minkboard.data

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
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

        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(GCM_NONCE_BYTES).also(random::nextBytes)
        val keyBytes = deriveKeyBytes(passphrase, salt, writeIterations)
        val encrypted = try {
            Cipher.getInstance(CIPHER_ALGORITHM).run {
                init(
                    Cipher.ENCRYPT_MODE,
                    SecretKeySpec(keyBytes, "AES"),
                    GCMParameterSpec(GCM_TAG_BITS, nonce),
                )
                updateAAD(aad(writeIterations))
                doFinal(plaintext)
            }
        } finally {
            keyBytes.fill(0)
            plaintext.fill(0)
        }

        return JSONObject()
            .put("format", ENVELOPE_FORMAT)
            .put("envelopeVersion", ENVELOPE_VERSION)
            .put("kdf", KDF_NAME)
            .put("kdfIterations", writeIterations)
            .put("salt", Base64.getEncoder().encodeToString(salt))
            .put("cipher", CIPHER_NAME)
            .put("nonce", Base64.getEncoder().encodeToString(nonce))
            .put("ciphertext", Base64.getEncoder().encodeToString(encrypted))
            .toString()
            .toByteArray(StandardCharsets.UTF_8)
            .also {
                require(it.size <= MAX_ARCHIVE_BYTES) { "Launcher backup archive is too large" }
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

        val salt = decodeBoundedBase64(envelope, "salt", SALT_BYTES, SALT_BYTES)
        val nonce = decodeBoundedBase64(envelope, "nonce", GCM_NONCE_BYTES, GCM_NONCE_BYTES)
        val ciphertext = decodeBoundedBase64(
            envelope,
            "ciphertext",
            GCM_TAG_BITS / 8,
            MAX_ARCHIVE_BYTES,
        )
        val keyBytes = deriveKeyBytes(passphrase, salt, iterations)
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
        } catch (failure: AEADBadTagException) {
            throw InvalidLauncherBackupEnvelopeException(
                "Passphrase is incorrect or the launcher backup was damaged",
                failure,
            )
        } catch (failure: Exception) {
            throw InvalidLauncherBackupEnvelopeException(
                "Launcher backup archive could not be decrypted",
                failure,
            )
        } finally {
            keyBytes.fill(0)
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

    private fun deriveKeyBytes(
        passphrase: CharArray,
        salt: ByteArray,
        iterations: Int,
    ): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(KDF_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun decodeBoundedBase64(
        value: JSONObject,
        field: String,
        minBytes: Int,
        maxBytes: Int,
    ): ByteArray = try {
        Base64.getDecoder().decode(value.getString(field)).also { decoded ->
            require(decoded.size in minBytes..maxBytes)
        }
    } catch (failure: Exception) {
        throw InvalidLauncherBackupEnvelopeException(
            "Launcher backup archive $field is invalid",
            failure,
        )
    }

    private fun requireValidPassphrase(passphrase: CharArray) {
        require(passphrase.size in MIN_PASSPHRASE_LENGTH..MAX_PASSPHRASE_LENGTH) {
            "Passphrase must contain at least $MIN_PASSPHRASE_LENGTH characters"
        }
    }

    private fun aad(iterations: Int): ByteArray =
        "$ENVELOPE_FORMAT|$ENVELOPE_VERSION|$KDF_NAME|$iterations|$CIPHER_NAME"
            .toByteArray(StandardCharsets.UTF_8)

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
        private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val CIPHER_NAME = "AES-256-GCM"
        private const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
        private const val SALT_BYTES = 16
        private const val GCM_NONCE_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private const val KEY_BITS = 256
    }
}
