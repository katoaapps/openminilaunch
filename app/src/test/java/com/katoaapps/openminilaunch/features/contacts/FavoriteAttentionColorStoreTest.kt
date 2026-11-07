package com.katoaapps.openminilaunch.features.contacts

import com.katoaapps.openminilaunch.model.FavoriteContactId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteAttentionColorStoreTest {
    @Test
    fun codecRoundTripsOpaqueColorsWithoutExposingFieldsOutsideEncryption() {
        val colors = mapOf(
            "android-contact:amy" to 0xFF005BBB.toInt(),
            "integrated-contact:gary" to 0xFFCC5500.toInt(),
        )

        assertEquals(
            colors,
            FavoriteAttentionColorCodec.decode(FavoriteAttentionColorCodec.encode(colors)),
        )
    }

    @Test
    fun backupMergeRestoresColorsWithoutClearingExistingChoices() {
        val store = FavoriteAttentionColorStore(
            EncryptedFavoriteAttentionColorPersistence(
                storage = MemoryAttentionColorStorage(),
                cipher = CopyingAttentionColorCipher,
            ),
        )
        val amy = FavoriteContactId.Android("amy")
        val may = FavoriteContactId.Integrated("may")
        store.update(amy, 0xFF005BBB.toInt())

        assertTrue(store.mergeFromBackup(mapOf(may.storageKey to 0xFFCC5500.toInt())))
        assertEquals(0xFF005BBB.toInt(), store.color(amy))
        assertEquals(0xFFCC5500.toInt(), store.color(may))
        assertEquals(
            mapOf(
                amy.storageKey to 0xFF005BBB.toInt(),
                may.storageKey to 0xFFCC5500.toInt(),
            ),
            store.colorsForBackup(),
        )
    }
}

private class MemoryAttentionColorStorage : ContactDataBlobStorage {
    private var bytes: ByteArray? = null

    override fun read(): ByteArray? = bytes?.copyOf()

    override fun write(bytes: ByteArray) {
        this.bytes = bytes.copyOf()
    }

    override fun clear() {
        bytes = null
    }
}

private object CopyingAttentionColorCipher : ContactDataCipher {
    override fun encrypt(plaintext: ByteArray): ByteArray = plaintext.copyOf()
    override fun decrypt(envelope: ByteArray): ByteArray = envelope.copyOf()
    override fun resetKey() = Unit
}
