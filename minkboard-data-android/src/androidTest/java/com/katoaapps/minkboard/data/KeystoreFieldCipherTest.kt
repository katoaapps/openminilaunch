package com.katoaapps.minkboard.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.KeyStore
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeystoreFieldCipherTest {
    private val alias = "com.katoaapps.minkboard.test.${UUID.randomUUID()}"

    @After
    fun deleteTestKey() {
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
            if (containsAlias(alias)) deleteEntry(alias)
        }
    }

    @Test
    fun ciphertextIsBoundToItsEntityAndField() {
        val cipher = KeystoreFieldCipher(alias)
        val encrypted = cipher.encrypt("Private task", "card:one", "title")

        assertEquals("Private task", cipher.decrypt(encrypted, "card:one", "title"))
        val failure = runCatching {
            cipher.decrypt(encrypted, "card:two", "title")
        }.exceptionOrNull()
        assertTrue(failure is MinkBoardDecryptionException)
    }
}
