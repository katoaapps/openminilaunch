package com.katoaapps.minkboard.data

import java.util.Base64

/**
 * Encrypts small, process-restorable state before it enters Android saved-instance state.
 *
 * This is intentionally separate from both database field encryption and portable archives. The
 * encrypted value is only meaningful to this installation's Android Keystore key.
 */
interface TransientSavedStateCipher {
    fun encrypt(plaintext: ByteArray): ByteArray
    fun decrypt(ciphertext: ByteArray): ByteArray
}

class KeystoreTransientSavedStateCipher private constructor(
    private val fieldCipher: FieldCipher,
) : TransientSavedStateCipher {
    constructor() : this(KeystoreFieldCipher(KEY_ALIAS))
    internal constructor(keyAlias: String) : this(KeystoreFieldCipher(keyAlias))

    override fun encrypt(plaintext: ByteArray): ByteArray {
        require(plaintext.size <= MAX_PLAINTEXT_BYTES) { "Saved editor state is too large" }
        val encoded = Base64.getEncoder().encodeToString(plaintext)
        return fieldCipher.encrypt(encoded, SUBJECT, FIELD)
    }

    override fun decrypt(ciphertext: ByteArray): ByteArray {
        require(ciphertext.size <= MAX_CIPHERTEXT_BYTES) { "Encrypted editor state is too large" }
        val encoded = fieldCipher.decrypt(ciphertext, SUBJECT, FIELD)
        val plaintext = Base64.getDecoder().decode(encoded)
        require(plaintext.size <= MAX_PLAINTEXT_BYTES) { "Saved editor state is too large" }
        return plaintext
    }

    private companion object {
        const val KEY_ALIAS = "com.katoaapps.minkboard.transient-saved-state.v1"
        const val SUBJECT = "transient-editor-state"
        const val FIELD = "saved-state-v1"
        const val MAX_PLAINTEXT_BYTES = 256 * 1024
        const val MAX_CIPHERTEXT_BYTES = 384 * 1024
    }
}
