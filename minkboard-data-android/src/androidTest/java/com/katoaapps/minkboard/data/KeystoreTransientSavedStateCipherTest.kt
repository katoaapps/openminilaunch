package com.katoaapps.minkboard.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.KeyStore
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeystoreTransientSavedStateCipherTest {
    private val alias = "com.katoaapps.minkboard.transient.test.${UUID.randomUUID()}"

    @After
    fun deleteTestKey() {
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
            if (containsAlias(alias)) deleteEntry(alias)
        }
    }

    @Test
    fun roundTripUsesFreshAuthenticatedCiphertext() {
        val cipher = KeystoreTransientSavedStateCipher(alias)
        val plaintext = "Private card details 🦦".toByteArray()

        val first = cipher.encrypt(plaintext)
        val second = cipher.encrypt(plaintext)

        assertFalse(first.contentEquals(second))
        assertArrayEquals(plaintext, cipher.decrypt(first))
        assertArrayEquals(plaintext, cipher.decrypt(second))

        val tampered = first.copyOf().apply { this[lastIndex] = (last().toInt() xor 0x01).toByte() }
        assertTrue(runCatching { cipher.decrypt(tampered) }.isFailure)
    }
}
