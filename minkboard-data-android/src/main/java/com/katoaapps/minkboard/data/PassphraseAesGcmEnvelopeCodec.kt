package com.katoaapps.minkboard.data

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

data class PassphraseEnvelopeConfig(
    val format: String,
    val envelopeVersion: Int = 1,
    val defaultKdfIterations: Int = 210_000,
    val minKdfIterations: Int = 10_000,
    val maxKdfIterations: Int = 1_000_000,
    val minPassphraseLength: Int = 8,
    val maxPassphraseLength: Int = 1_024,
    val maxEnvelopeBytes: Int,
    val maxPlaintextBytes: Int = maxEnvelopeBytes,
)

sealed interface PassphraseEnvelopeDecodeResult {
    data class Decoded(val plaintext: ByteArray) : PassphraseEnvelopeDecodeResult
    data object Unsupported : PassphraseEnvelopeDecodeResult
    data object Corrupt : PassphraseEnvelopeDecodeResult
}

/** Shared low-level passphrase envelope used by product-specific JSON codecs. */
class PassphraseAesGcmEnvelopeCodec(
    private val config: PassphraseEnvelopeConfig,
    private val random: SecureRandom = SecureRandom(),
    private val writeIterations: Int = config.defaultKdfIterations,
) {
    init {
        require(config.format.isNotBlank())
        require(config.maxPlaintextBytes in 1..config.maxEnvelopeBytes)
        require(writeIterations in config.minKdfIterations..config.maxKdfIterations)
    }

    fun encode(plaintext: ByteArray, passphrase: CharArray): ByteArray {
        requireValidPassphrase(passphrase)
        require(plaintext.size in 1..config.maxPlaintextBytes)
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(GCM_NONCE_BYTES).also(random::nextBytes)
        val keyBytes = deriveKeyBytes(passphrase, salt, writeIterations)
        val ciphertext = try {
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
        }
        return JSONObject()
            .put("format", config.format)
            .put("envelopeVersion", config.envelopeVersion)
            .put("kdf", KDF_NAME)
            .put("kdfIterations", writeIterations)
            .put("salt", Base64.getEncoder().encodeToString(salt))
            .put("cipher", CIPHER_NAME)
            .put("nonce", Base64.getEncoder().encodeToString(nonce))
            .put("ciphertext", Base64.getEncoder().encodeToString(ciphertext))
            .toString()
            .toByteArray(StandardCharsets.UTF_8)
            .also { require(it.size in 1..config.maxEnvelopeBytes) }
    }

    fun decode(envelopeBytes: ByteArray, passphrase: CharArray): PassphraseEnvelopeDecodeResult {
        if (envelopeBytes.isEmpty() || envelopeBytes.size > config.maxEnvelopeBytes) {
            return PassphraseEnvelopeDecodeResult.Corrupt
        }
        return try {
            requireValidPassphrase(passphrase)
            val envelope = JSONObject(String(envelopeBytes, StandardCharsets.UTF_8))
            if (envelope.optString("format") != config.format) {
                return PassphraseEnvelopeDecodeResult.Unsupported
            }
            if (envelope.optInt("envelopeVersion", -1) != config.envelopeVersion ||
                envelope.optString("kdf") != KDF_NAME ||
                envelope.optString("cipher") != CIPHER_NAME
            ) {
                return PassphraseEnvelopeDecodeResult.Unsupported
            }
            val iterations = envelope.getInt("kdfIterations")
            if (iterations !in config.minKdfIterations..config.maxKdfIterations) {
                return PassphraseEnvelopeDecodeResult.Unsupported
            }
            val salt = decodeFixed(envelope, "salt", SALT_BYTES)
            val nonce = decodeFixed(envelope, "nonce", GCM_NONCE_BYTES)
            val ciphertext = Base64.getDecoder().decode(envelope.getString("ciphertext"))
            require(ciphertext.size in (GCM_TAG_BITS / 8)..config.maxEnvelopeBytes)
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
            } finally {
                keyBytes.fill(0)
            }
            if (plaintext.isEmpty() || plaintext.size > config.maxPlaintextBytes) {
                plaintext.fill(0)
                PassphraseEnvelopeDecodeResult.Corrupt
            } else {
                PassphraseEnvelopeDecodeResult.Decoded(plaintext)
            }
        } catch (_: AEADBadTagException) {
            PassphraseEnvelopeDecodeResult.Corrupt
        } catch (_: Exception) {
            PassphraseEnvelopeDecodeResult.Corrupt
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

    private fun decodeFixed(root: JSONObject, name: String, expectedSize: Int): ByteArray =
        Base64.getDecoder().decode(root.getString(name)).also { require(it.size == expectedSize) }

    private fun requireValidPassphrase(passphrase: CharArray) {
        require(passphrase.size in config.minPassphraseLength..config.maxPassphraseLength)
    }

    private fun aad(iterations: Int): ByteArray =
        "${config.format}|${config.envelopeVersion}|$KDF_NAME|$iterations|$CIPHER_NAME"
            .toByteArray(StandardCharsets.UTF_8)

    private companion object {
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
