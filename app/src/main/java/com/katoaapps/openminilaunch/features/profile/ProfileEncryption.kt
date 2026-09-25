package com.katoaapps.openminilaunch.features.profile

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class ProfileEncryption {
    private val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    fun encrypt(plainText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val cipherText = cipher.doFinal(plainText)
        return byteArrayOf(FILE_VERSION, cipher.iv.size.toByte()) + cipher.iv + cipherText
    }

    fun decrypt(payload: ByteArray): ByteArray {
        require(payload.size > HEADER_SIZE && payload[0] == FILE_VERSION) { "Invalid encrypted Profile file" }
        val ivSize = payload[1].toInt() and 0xff
        require(ivSize in 12..16 && payload.size > HEADER_SIZE + ivSize) { "Invalid Profile IV" }
        val iv = payload.copyOfRange(HEADER_SIZE, HEADER_SIZE + ivSize)
        val cipherText = payload.copyOfRange(HEADER_SIZE + ivSize, payload.size)
        return Cipher.getInstance(TRANSFORMATION).run {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            doFinal(cipherText)
        }
    }

    fun resetKey() {
        if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
    }

    private fun key(): SecretKey {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "openmink_profile_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val HEADER_SIZE = 2
        const val FILE_VERSION: Byte = 1
    }
}
