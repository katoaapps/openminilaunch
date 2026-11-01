package com.katoaapps.openminilaunch.features.contacts

import com.katoaapps.openminilaunch.model.FavoriteContactId
import com.katoaapps.openminilaunch.model.FavoriteAndroidContact
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import com.katoaapps.openminilaunch.model.IntegratedContactEndpoint
import javax.crypto.KeyGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class FavoriteIntegratedContactStoreTest {
    private val may = FavoriteIntegratedContact(
        id = FavoriteContactId.Integrated("local-may"),
        displayName = "May Wong",
        endpoints = listOf(
            IntegratedContactEndpoint(
                packageName = "com.example.chat",
                shortcutId = "conversation-42",
                userSerial = 0L,
                label = "May",
            ),
        ),
    )

    @Test fun codecRoundTripPreservesOwnedContactData() {
        assertEquals(listOf(may), FavoriteIntegratedContactCodec.decode(
            FavoriteIntegratedContactCodec.encode(listOf(may)),
        ))
    }

    @Test fun encryptedStoreDoesNotExposeNamesOrProviderIdentity() {
        val storage = MemoryContactDataStorage()
        val persistence = EncryptedFavoriteIntegratedContactPersistence(storage, testCipher())

        assertTrue(persistence.save(listOf(may)))

        val encrypted = checkNotNull(storage.bytes)
        assertFalse(encrypted.containsBytes("May Wong".toByteArray()))
        assertFalse(encrypted.containsBytes("com.example.chat".toByteArray()))
        assertFalse(encrypted.containsBytes("conversation-42".toByteArray()))
        assertEquals(listOf(may), persistence.load())
    }

    @Test fun duplicateEndpointIdentityIsRejected() {
        val duplicate = may.copy(
            id = FavoriteContactId.Integrated("different-local-id"),
            displayName = "Duplicate May",
        )
        assertThrows(IllegalArgumentException::class.java) {
            FavoriteIntegratedContactCodec.encode(listOf(may, duplicate))
        }
    }

    @Test fun tamperedStoreIsClearedWithoutLeakingOrCrashing() {
        val storage = MemoryContactDataStorage()
        val persistence = EncryptedFavoriteIntegratedContactPersistence(storage, testCipher())
        assertTrue(persistence.save(listOf(may)))
        storage.bytes!![storage.bytes!!.lastIndex] =
            (storage.bytes!!.last().toInt() xor 1).toByte()

        assertEquals(emptyList<FavoriteIntegratedContact>(), persistence.load())
        assertNull(storage.bytes)
    }

    @Test fun catalogNeverMergesMatchingDisplayNames() {
        val androidMay = FavoriteAndroidContact(
            id = FavoriteContactId.Android("android-may"),
            contactUri = "content://contacts/may",
            displayName = "May Wong",
            photoUri = null,
            organization = null,
            phones = emptyList(),
        )

        val combined = FavoriteContactCatalog.combine(
            androidContacts = listOf(androidMay),
            integratedContacts = listOf(may),
            locale = Locale.US,
        )

        assertEquals(2, combined.size)
        assertEquals(setOf(androidMay.id, may.id), combined.map { it.id }.toSet())
    }

    private fun testCipher(): AesGcmContactDataCipher {
        val generator = KeyGenerator.getInstance("AES")
        generator.init(256)
        val key = generator.generateKey()
        return AesGcmContactDataCipher(
            keyProvider = { key },
            envelopeMagic = 0x4D_49_43_45,
            authenticatedContext =
                "openmink:favorite-integrated-contacts:v1".toByteArray(Charsets.UTF_8),
        )
    }
}

private class MemoryContactDataStorage : ContactDataBlobStorage {
    var bytes: ByteArray? = null

    override fun read(): ByteArray? = bytes?.copyOf()

    override fun write(bytes: ByteArray) {
        this.bytes = bytes.copyOf()
    }

    override fun clear() {
        bytes = null
    }
}

private fun ByteArray.containsBytes(candidate: ByteArray): Boolean {
    if (candidate.isEmpty() || candidate.size > size) return false
    return indices.any { start ->
        start + candidate.size <= size && candidate.indices.all { offset ->
            this[start + offset] == candidate[offset]
        }
    }
}
