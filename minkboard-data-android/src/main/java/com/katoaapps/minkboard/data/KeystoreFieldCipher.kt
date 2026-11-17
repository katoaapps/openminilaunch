package com.katoaapps.minkboard.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class KeystoreFieldCipher(
    private val keyAlias: String = "com.katoaapps.minkboard.local.v1",
) : FieldCipher {
    private val secretKey: SecretKey by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        loadOrCreateKey()
    }

    override fun encrypt(value: String, subject: String, field: String): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        cipher.updateAAD(aad(subject, field))
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        return ByteBuffer.allocate(2 + iv.size + encrypted.size)
            .put(FORMAT_VERSION)
            .put(iv.size.toByte())
            .put(iv)
            .put(encrypted)
            .array()
    }

    override fun decrypt(value: ByteArray, subject: String, field: String): String {
        try {
            val buffer = ByteBuffer.wrap(value)
            require(buffer.get() == FORMAT_VERSION) { "Unsupported encrypted field version" }
            val ivLength = buffer.get().toInt() and 0xFF
            require(ivLength in 12..32 && buffer.remaining() > ivLength) { "Malformed encrypted field" }
            val iv = ByteArray(ivLength).also(buffer::get)
            val encrypted = ByteArray(buffer.remaining()).also(buffer::get)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            cipher.updateAAD(aad(subject, field))
            return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
        } catch (error: Exception) {
            throw MinkBoardDecryptionException(subject, field, error)
        }
    }

    private fun loadOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(keyAlias)) {
            return requireNotNull(keyStore.getKey(keyAlias, null) as? SecretKey) {
                "Existing MinkBoard key is unavailable or has the wrong type"
            }
        }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private fun aad(subject: String, field: String): ByteArray =
        "minkboard|local-v1|$subject|$field".toByteArray(Charsets.UTF_8)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val FORMAT_VERSION: Byte = 1
    }
}

class MinkBoardDecryptionException(
    subject: String,
    field: String,
    cause: Throwable,
) : IllegalStateException("MinkBoard could not decrypt $field for $subject", cause)
