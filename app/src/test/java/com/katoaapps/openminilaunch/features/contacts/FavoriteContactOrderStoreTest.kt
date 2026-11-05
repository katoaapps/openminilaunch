package com.katoaapps.openminilaunch.features.contacts

import com.katoaapps.openminilaunch.model.FavoriteAndroidContact
import com.katoaapps.openminilaunch.model.FavoriteContactId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteContactOrderStoreTest {
    @Test fun codecRoundTripsStableKeys() {
        val order = listOf("android-contact:one", "integrated-contact:two")

        assertEquals(order, FavoriteContactOrderCodec.decode(FavoriteContactOrderCodec.encode(order)))
    }

    @Test fun savedOrderAppliesBeforeNewContacts() {
        val store = store()
        val amy = contact("amy", "Amy")
        val may = contact("may", "May")
        val zoe = contact("zoe", "Zoe")

        assertTrue(store.updateVisibleOrder(listOf(zoe.id.storageKey, amy.id.storageKey)))

        assertEquals(listOf(zoe, amy, may), store.applyTo(listOf(amy, may, zoe)))
    }

    @Test fun reorderingVisibleContactsPreservesTemporarilyHiddenContacts() {
        val store = store()
        val amy = contact("amy", "Amy")
        val may = contact("may", "May")
        val zoe = contact("zoe", "Zoe")
        assertTrue(
            store.updateVisibleOrder(
                listOf(amy.id.storageKey, may.id.storageKey, zoe.id.storageKey),
            ),
        )

        assertTrue(store.updateVisibleOrder(listOf(zoe.id.storageKey, amy.id.storageKey)))

        assertEquals(
            listOf(zoe.id.storageKey, may.id.storageKey, amy.id.storageKey),
            store.order(),
        )
    }

    @Test fun portableOrderContainsOnlyAvailableIntegratedContacts() {
        val store = store()
        assertTrue(
            store.updateVisibleOrder(
                listOf(
                    "android-contact:amy",
                    "integrated-contact:gary",
                    "integrated-contact:may",
                ),
            ),
        )

        assertEquals(
            listOf("may"),
            store.portableIntegratedOrder(setOf("may")),
        )
    }

    private fun contact(key: String, name: String) = FavoriteAndroidContact(
        id = FavoriteContactId.Android(key),
        contactUri = "content://contacts/$key",
        displayName = name,
        photoUri = null,
        organization = null,
        phones = emptyList(),
    )

    private fun store(): FavoriteContactOrderStore {
        val persistence = EncryptedFavoriteContactOrderPersistence(
            storage = MemoryOrderStorage(),
            cipher = CopyingCipher,
        )
        return FavoriteContactOrderStore(persistence)
    }
}

private class MemoryOrderStorage : ContactDataBlobStorage {
    private var bytes: ByteArray? = null

    override fun read(): ByteArray? = bytes?.copyOf()

    override fun write(bytes: ByteArray) {
        this.bytes = bytes.copyOf()
    }

    override fun clear() {
        bytes = null
    }
}

private object CopyingCipher : ContactDataCipher {
    override fun encrypt(plaintext: ByteArray): ByteArray = plaintext.copyOf()
    override fun decrypt(envelope: ByteArray): ByteArray = envelope.copyOf()
    override fun resetKey() = Unit
}
