package com.katoaapps.openminilaunch.features.contacts

import com.katoaapps.openminilaunch.model.FavoriteContactId
import com.katoaapps.openminilaunch.model.FavoriteAndroidContact
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import com.katoaapps.openminilaunch.model.IntegratedContactEndpoint
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.model.ProviderAddressType
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import javax.crypto.KeyGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class FavoriteIntegratedContactStoreTest {
    private val may = FavoriteIntegratedContact(
        id = FavoriteContactId.Integrated("local-may"),
        displayName = "May Wong",
        endpoints = listOf(
            IntegratedContactEndpoint.ConversationShortcut(
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

    @Test fun codecRoundTripPreservesProviderAddress() {
        val telegram = FavoriteIntegratedContact(
            id = FavoriteContactId.Integrated("local-telegram"),
            displayName = "Mink Tester",
            endpoints = listOf(
                IntegratedContactEndpoint.ProviderAddress(
                    providerId = "telegram",
                    packageName = "org.telegram.messenger",
                    addressType = ProviderAddressType.USERNAME,
                    address = "minktester",
                ),
            ),
        )

        assertEquals(
            listOf(telegram),
            FavoriteIntegratedContactCodec.decode(
                FavoriteIntegratedContactCodec.encode(listOf(telegram)),
            ),
        )
    }

    @Test fun versionOneShortcutPayloadStillImports() {
        assertEquals(listOf(may), FavoriteIntegratedContactCodec.decode(versionOnePayload(may)))
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

    @Test fun tamperedStoreIsPreservedAndCannotBeOverwritten() {
        val storage = MemoryContactDataStorage()
        val persistence = EncryptedFavoriteIntegratedContactPersistence(storage, testCipher())
        assertTrue(persistence.save(listOf(may)))
        storage.bytes!![storage.bytes!!.lastIndex] =
            (storage.bytes!!.last().toInt() xor 1).toByte()

        assertEquals(emptyList<FavoriteIntegratedContact>(), persistence.load())
        assertTrue(storage.bytes != null)
        assertFalse(persistence.save(emptyList()))
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

    @Test fun reconnectReplacesEndpointWithoutChangingContactIdentityOrName() {
        val persistence = EncryptedFavoriteIntegratedContactPersistence(
            MemoryContactDataStorage(),
            testCipher(),
        )
        assertTrue(persistence.save(listOf(may)))
        val store = FavoriteIntegratedContactStore(persistence)

        assertTrue(store.replaceEndpoint(may.id, shortcut("new-conversation", "May W.")))

        val reconnected = FavoriteIntegratedContactStore(persistence).contacts().single()
        assertEquals(may.id, reconnected.id)
        assertEquals(may.displayName, reconnected.displayName)
        val endpoint = reconnected.endpoints.single() as IntegratedContactEndpoint.ConversationShortcut
        assertEquals("new-conversation", endpoint.shortcutId)
        assertEquals("May W.", endpoint.label)
    }

    @Test fun reconnectDoesNotStealAnEndpointFromAnotherFavorite() {
        val other = FavoriteIntegratedContact(
            id = FavoriteContactId.Integrated("local-gary"),
            displayName = "Gary",
            endpoints = listOf(
                IntegratedContactEndpoint.ConversationShortcut(
                    packageName = "com.example.chat",
                    shortcutId = "gary-conversation",
                    userSerial = 0L,
                    label = "Gary",
                ),
            ),
        )
        val persistence = EncryptedFavoriteIntegratedContactPersistence(
            MemoryContactDataStorage(),
            testCipher(),
        )
        assertTrue(persistence.save(listOf(may, other)))
        val store = FavoriteIntegratedContactStore(persistence)

        assertFalse(store.replaceEndpoint(may.id, shortcut("gary-conversation", "Gary")))
        assertEquals(setOf(may, other), store.contacts().toSet())
    }

    @Test fun reconnectCanReplaceShortcutWithProviderAddress() {
        val persistence = EncryptedFavoriteIntegratedContactPersistence(
            MemoryContactDataStorage(),
            testCipher(),
        )
        assertTrue(persistence.save(listOf(may)))
        val store = FavoriteIntegratedContactStore(persistence)

        assertTrue(
            store.replaceProviderAddress(
                id = may.id,
                providerId = "telegram",
                packageName = "org.telegram.messenger",
                addressType = ProviderAddressType.USERNAME,
                address = "@maywong",
            ),
        )

        val reconnected = store.contacts().single()
        assertEquals(may.id, reconnected.id)
        assertEquals(may.displayName, reconnected.displayName)
        assertEquals(
            IntegratedContactEndpoint.ProviderAddress(
                providerId = "telegram",
                packageName = "org.telegram.messenger",
                addressType = ProviderAddressType.USERNAME,
                address = "maywong",
            ),
            reconnected.endpoints.single(),
        )
    }

    @Test fun providerAddressRejectsEquivalentDuplicateUsername() {
        val persistence = EncryptedFavoriteIntegratedContactPersistence(
            MemoryContactDataStorage(),
            testCipher(),
        )
        val store = FavoriteIntegratedContactStore(persistence)

        assertEquals(
            FavoriteIntegratedContactAddResult.ADDED,
            store.addProviderAddress(
                providerId = "telegram",
                packageName = "org.telegram.messenger",
                addressType = ProviderAddressType.USERNAME,
                address = "MinkTester",
                displayName = "Mink Tester",
            ),
        )
        assertEquals(
            FavoriteIntegratedContactAddResult.DUPLICATE,
            store.addProviderAddress(
                providerId = "telegram",
                packageName = "org.telegram.messenger",
                addressType = ProviderAddressType.USERNAME,
                address = "@minktester",
                displayName = "Duplicate",
            ),
        )
    }

    @Test fun failedWriteIsReportedWithoutChangingCachedContacts() {
        val storage = MemoryContactDataStorage()
        val persistence = EncryptedFavoriteIntegratedContactPersistence(storage, testCipher())
        val store = FavoriteIntegratedContactStore(persistence)
        storage.failWrites = true

        assertEquals(
            FavoriteIntegratedContactAddResult.FAILED,
            store.add(shortcut("may", "May")),
        )
        assertEquals(emptyList<FavoriteIntegratedContact>(), store.contacts())
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

    private fun shortcut(id: String, label: String) = LauncherShortcutTarget(
        label = label,
        packageName = "com.example.chat",
        shortcutId = id,
        userSerial = 0L,
        isWorkProfile = false,
    )
}

private fun versionOnePayload(contact: FavoriteIntegratedContact): ByteArray =
    ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            val endpoint = contact.endpoints.single()
                as IntegratedContactEndpoint.ConversationShortcut
            output.writeInt(0x4D_49_43_50)
            output.writeInt(1)
            output.writeInt(1)
            output.writeTestString(contact.id.localId)
            output.writeTestString(contact.displayName)
            output.writeInt(1)
            output.writeTestString(endpoint.packageName)
            output.writeTestString(endpoint.shortcutId)
            output.writeLong(endpoint.userSerial)
            output.writeBoolean(endpoint.label != null)
            endpoint.label?.let(output::writeTestString)
        }
        bytes.toByteArray()
    }

private fun DataOutputStream.writeTestString(value: String) {
    val encoded = value.toByteArray(Charsets.UTF_8)
    writeInt(encoded.size)
    write(encoded)
}

private class MemoryContactDataStorage : ContactDataBlobStorage {
    var bytes: ByteArray? = null
    var failWrites: Boolean = false

    override fun read(): ByteArray? = bytes?.copyOf()

    override fun write(bytes: ByteArray) {
        if (failWrites) error("Test write failure")
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
