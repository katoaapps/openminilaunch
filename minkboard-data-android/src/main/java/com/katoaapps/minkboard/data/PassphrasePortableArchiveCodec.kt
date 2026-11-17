package com.katoaapps.minkboard.data

import com.katoaapps.minkboard.core.PortableArchiveValidator
import com.katoaapps.minkboard.model.PortableWorkspaceSnapshot
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

class InvalidPortableArchiveException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

/**
 * Encodes portable workspace data in a passphrase-derived AES-256-GCM envelope.
 *
 * Local Room ciphertext is never copied: it is installation-bound to Android Keystore. The
 * snapshot is re-encrypted with a user-recoverable passphrase for cross-device portability.
 */
class PassphrasePortableArchiveCodec(
    private val validator: PortableArchiveValidator = PortableArchiveValidator(),
    private val random: SecureRandom = SecureRandom(),
    private val writeIterations: Int = DEFAULT_KDF_ITERATIONS,
) {
    private val json = PortableArchiveJsonCodec()

    init {
        require(writeIterations in MIN_KDF_ITERATIONS..MAX_KDF_ITERATIONS)
    }

    /** Cheap format probe used before asking the user for a passphrase. */
    fun isSupportedEnvelope(archive: ByteArray): Boolean {
        if (archive.isEmpty() || archive.size > MAX_ARCHIVE_BYTES) return false
        return runCatching {
            val envelope = JSONObject(String(archive, StandardCharsets.UTF_8))
            envelope.optString("format") == ENVELOPE_FORMAT
        }.getOrDefault(false)
    }

    fun encode(snapshot: PortableWorkspaceSnapshot, passphrase: CharArray): ByteArray {
        requireValidPassphrase(passphrase)
        validator.validate(snapshot)
        val snapshotJson = json.encode(snapshot)
        val checksum = sha256(snapshotJson.toByteArray(StandardCharsets.UTF_8)).toHex()
        val payload = JSONObject()
            .put("checksumSha256", checksum)
            .put("snapshotJson", snapshotJson)
            .toString()
            .toByteArray(StandardCharsets.UTF_8)

        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(GCM_NONCE_BYTES).also(random::nextBytes)
        val key = deriveKey(passphrase, salt, writeIterations)
        val encrypted = try {
            Cipher.getInstance(CIPHER_ALGORITHM).run {
                init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, nonce))
                updateAAD(aad(writeIterations))
                doFinal(payload)
            }
        } finally {
            key.encoded.fill(0)
            payload.fill(0)
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
                require(it.size <= MAX_ARCHIVE_BYTES) { "Portable archive is too large" }
            }
    }

    fun decode(archive: ByteArray, passphrase: CharArray): PortableWorkspaceSnapshot {
        requireValidPassphrase(passphrase)
        if (archive.isEmpty() || archive.size > MAX_ARCHIVE_BYTES) {
            throw InvalidPortableArchiveException("Portable archive size is invalid")
        }
        val envelope = try {
            JSONObject(String(archive, StandardCharsets.UTF_8))
        } catch (failure: Exception) {
            throw InvalidPortableArchiveException("Portable archive is not valid JSON", failure)
        }

        val iterations = try {
            require(envelope.getString("format") == ENVELOPE_FORMAT)
            require(envelope.getInt("envelopeVersion") == ENVELOPE_VERSION)
            require(envelope.getString("kdf") == KDF_NAME)
            require(envelope.getString("cipher") == CIPHER_NAME)
            envelope.getInt("kdfIterations").also {
                require(it in MIN_KDF_ITERATIONS..MAX_KDF_ITERATIONS)
            }
        } catch (failure: Exception) {
            throw InvalidPortableArchiveException("Portable archive envelope is unsupported", failure)
        }

        val salt = decodeBoundedBase64(envelope, "salt", SALT_BYTES, SALT_BYTES)
        val nonce = decodeBoundedBase64(envelope, "nonce", GCM_NONCE_BYTES, GCM_NONCE_BYTES)
        val ciphertext = decodeBoundedBase64(
            envelope,
            "ciphertext",
            GCM_TAG_BITS / 8,
            MAX_ARCHIVE_BYTES,
        )
        val key = deriveKey(passphrase, salt, iterations)
        val plaintext = try {
            Cipher.getInstance(CIPHER_ALGORITHM).run {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, nonce))
                updateAAD(aad(iterations))
                doFinal(ciphertext)
            }
        } catch (failure: AEADBadTagException) {
            throw InvalidPortableArchiveException(
                "Passphrase is incorrect or the archive was damaged",
                failure,
            )
        } catch (failure: Exception) {
            throw InvalidPortableArchiveException("Portable archive could not be decrypted", failure)
        } finally {
            key.encoded.fill(0)
        }

        try {
            val payload = JSONObject(String(plaintext, StandardCharsets.UTF_8))
            val snapshotJson = payload.getString("snapshotJson")
            val expectedChecksum = payload.getString("checksumSha256")
            val actualChecksum = sha256(snapshotJson.toByteArray(StandardCharsets.UTF_8)).toHex()
            if (!MessageDigest.isEqual(
                    expectedChecksum.toByteArray(StandardCharsets.US_ASCII),
                    actualChecksum.toByteArray(StandardCharsets.US_ASCII),
                )
            ) {
                throw InvalidPortableArchiveException("Portable archive checksum is invalid")
            }
            return validator.validate(json.decode(snapshotJson))
        } catch (failure: InvalidPortableArchiveException) {
            throw failure
        } catch (failure: Exception) {
            throw InvalidPortableArchiveException("Portable archive payload is invalid", failure)
        } finally {
            plaintext.fill(0)
        }
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BITS)
        return try {
            val bytes = SecretKeyFactory.getInstance(KDF_ALGORITHM).generateSecret(spec).encoded
            SecretKeySpec(bytes, "AES").also { bytes.fill(0) }
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
        throw InvalidPortableArchiveException("Portable archive $field is invalid", failure)
    }

    private fun requireValidPassphrase(passphrase: CharArray) {
        require(passphrase.size in MIN_PASSPHRASE_LENGTH..MAX_PASSPHRASE_LENGTH) {
            "Passphrase must contain at least $MIN_PASSPHRASE_LENGTH characters"
        }
    }

    private fun aad(iterations: Int): ByteArray =
        "$ENVELOPE_FORMAT|$ENVELOPE_VERSION|$KDF_NAME|$iterations|$CIPHER_NAME"
            .toByteArray(StandardCharsets.UTF_8)

    private fun sha256(value: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(value)

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private companion object {
        const val ENVELOPE_FORMAT = "minkboard-encrypted-archive"
        const val ENVELOPE_VERSION = 1
        const val KDF_NAME = "PBKDF2-HMAC-SHA256"
        const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
        const val CIPHER_NAME = "AES-256-GCM"
        const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
        const val DEFAULT_KDF_ITERATIONS = 210_000
        const val MIN_KDF_ITERATIONS = 10_000
        const val MAX_KDF_ITERATIONS = 5_000_000
        const val MIN_PASSPHRASE_LENGTH = 8
        const val MAX_PASSPHRASE_LENGTH = 1_024
        const val SALT_BYTES = 16
        const val GCM_NONCE_BYTES = 12
        const val GCM_TAG_BITS = 128
        const val KEY_BITS = 256
        const val MAX_ARCHIVE_BYTES = 32 * 1024 * 1024
    }
}
