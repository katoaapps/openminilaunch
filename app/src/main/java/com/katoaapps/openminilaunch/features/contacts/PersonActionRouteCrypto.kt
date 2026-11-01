package com.katoaapps.openminilaunch.features.contacts

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal fun interface PersonActionRouteSecretKeyProvider {
    fun secretKey(): SecretKey

    fun reset() = Unit
}

/** Supplies a non-exportable per-install key. Hardware backing is used when the device supports it. */
internal class AndroidKeystoreSecretKeyProvider(
    private val alias: String,
) : PersonActionRouteSecretKeyProvider {
    override fun secretKey(): SecretKey = synchronized(KEYSTORE_LOCK) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = runCatching { keyStore.getKey(alias, null) }.getOrNull()
        if (existing is SecretKey) return@synchronized existing
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
        generateKey()
    }

    override fun reset() = synchronized(KEYSTORE_LOCK) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
    }

    private fun generateKey(): SecretKey {
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(AES_KEY_BITS)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val AES_KEY_BITS = 256
        val KEYSTORE_LOCK = Any()
    }
}

internal interface PersonActionRouteCipher {
    fun encrypt(plaintext: ByteArray): ByteArray
    fun decrypt(envelope: ByteArray): ByteArray
    fun resetKey()
}

/** Versioned AES-GCM envelope with authenticated context binding and a fresh IV per write. */
internal class AesGcmPersonActionRouteCipher(
    private val keyProvider: PersonActionRouteSecretKeyProvider,
) : PersonActionRouteCipher {
    override fun resetKey() {
        keyProvider.reset()
    }

    override fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider.secretKey())
        cipher.updateAAD(AUTHENTICATED_CONTEXT)
        val ciphertext = cipher.doFinal(plaintext)
        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(ENVELOPE_MAGIC)
                output.writeInt(ENVELOPE_VERSION)
                output.writeInt(cipher.iv.size)
                output.write(cipher.iv)
                output.writeInt(ciphertext.size)
                output.write(ciphertext)
            }
            bytes.toByteArray()
        }.also { ciphertext.fill(0) }
    }

    override fun decrypt(envelope: ByteArray): ByteArray {
        val parsed = DataInputStream(ByteArrayInputStream(envelope)).use { input ->
            if (input.readInt() != ENVELOPE_MAGIC) throw IOException("Invalid route envelope")
            if (input.readInt() != ENVELOPE_VERSION) throw IOException("Unsupported route envelope")
            val iv = input.readSizedBytes(MAX_IV_BYTES, "IV")
            val ciphertext = input.readSizedBytes(MAX_CIPHERTEXT_BYTES, "ciphertext")
            if (input.read() != -1) throw IOException("Trailing route envelope data")
            ParsedEnvelope(iv, ciphertext)
        }

        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                keyProvider.secretKey(),
                GCMParameterSpec(GCM_TAG_BITS, parsed.iv),
            )
            cipher.updateAAD(AUTHENTICATED_CONTEXT)
            cipher.doFinal(parsed.ciphertext)
        } finally {
            parsed.iv.fill(0)
            parsed.ciphertext.fill(0)
        }
    }

    private data class ParsedEnvelope(val iv: ByteArray, val ciphertext: ByteArray)

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val ENVELOPE_MAGIC = 0x4D_52_54_45 // MRTE
        const val ENVELOPE_VERSION = 1
        const val MAX_IV_BYTES = 32
        const val MAX_CIPHERTEXT_BYTES = 2 * 1024 * 1024
        val AUTHENTICATED_CONTEXT =
            "openmink:favorite-person-action-routes:v1".toByteArray(Charsets.UTF_8)
    }
}

private fun DataInputStream.readSizedBytes(maximumSize: Int, label: String): ByteArray {
    val size = readInt()
    if (size <= 0 || size > maximumSize) throw IOException("Invalid $label size")
    return ByteArray(size).also(::readFully)
}
