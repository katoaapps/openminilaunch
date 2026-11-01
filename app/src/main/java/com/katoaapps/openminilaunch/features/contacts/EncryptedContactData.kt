package com.katoaapps.openminilaunch.features.contacts

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal fun interface ContactDataSecretKeyProvider {
    fun secretKey(): SecretKey

    fun reset() = Unit
}

/** Supplies a non-exportable per-install key. Hardware backing is used when available. */
internal class AndroidKeystoreContactDataKeyProvider(
    private val alias: String,
) : ContactDataSecretKeyProvider {
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

internal interface ContactDataCipher {
    fun encrypt(plaintext: ByteArray): ByteArray
    fun decrypt(envelope: ByteArray): ByteArray
    fun resetKey()
}

/** AES-GCM envelope whose magic and AAD keep each contact store cryptographically separate. */
internal class AesGcmContactDataCipher(
    private val keyProvider: ContactDataSecretKeyProvider,
    private val envelopeMagic: Int,
    private val authenticatedContext: ByteArray,
) : ContactDataCipher {
    override fun resetKey() = keyProvider.reset()

    override fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider.secretKey())
        cipher.updateAAD(authenticatedContext)
        val ciphertext = cipher.doFinal(plaintext)
        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(envelopeMagic)
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
            if (input.readInt() != envelopeMagic) throw IOException("Invalid encrypted contact data")
            if (input.readInt() != ENVELOPE_VERSION) throw IOException("Unsupported encrypted contact data")
            val iv = input.readSizedBytes(MAX_IV_BYTES, "IV")
            val ciphertext = input.readSizedBytes(MAX_CIPHERTEXT_BYTES, "ciphertext")
            if (input.read() != -1) throw IOException("Trailing encrypted contact data")
            ParsedEnvelope(iv, ciphertext)
        }
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                keyProvider.secretKey(),
                GCMParameterSpec(GCM_TAG_BITS, parsed.iv),
            )
            cipher.updateAAD(authenticatedContext)
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
        const val ENVELOPE_VERSION = 1
        const val MAX_IV_BYTES = 32
        const val MAX_CIPHERTEXT_BYTES = 2 * 1024 * 1024
    }
}

internal interface ContactDataBlobStorage {
    fun read(): ByteArray?
    fun write(bytes: ByteArray)
    fun clear()
}

/** Atomic encrypted-blob storage under the caller-supplied no-backup path. */
internal class AtomicContactDataBlobStorage(file: File) : ContactDataBlobStorage {
    private val atomicFile = AtomicFile(file)

    override fun read(): ByteArray? {
        if (!atomicFile.baseFile.exists()) return null
        return atomicFile.openRead().use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                total += read
                if (total > MAX_ENCRYPTED_FILE_BYTES) throw IOException("Contact store is too large")
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        }
    }

    override fun write(bytes: ByteArray) {
        if (bytes.size > MAX_ENCRYPTED_FILE_BYTES) throw IOException("Contact store is too large")
        atomicFile.baseFile.parentFile?.mkdirs()
        val output = atomicFile.startWrite()
        try {
            output.write(bytes)
            output.fd.sync()
            atomicFile.finishWrite(output)
        } catch (failure: Exception) {
            atomicFile.failWrite(output)
            throw failure
        }
    }

    override fun clear() = atomicFile.delete()

    private companion object {
        const val MAX_ENCRYPTED_FILE_BYTES = 2 * 1024 * 1024
    }
}

private fun DataInputStream.readSizedBytes(maximumSize: Int, label: String): ByteArray {
    val size = readInt()
    if (size <= 0 || size > maximumSize) throw IOException("Invalid $label size")
    return ByteArray(size).also(::readFully)
}
