package com.katoaapps.openminilaunch.features.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidFavoriteAttentionPendingStoreTest {
    @Test
    fun pendingTransfersRoundTripThroughPersistence() {
        val storage = MemoryPendingStorage()
        val store = AndroidFavoriteAttentionPendingStore(storage, CopyingPendingCipher)
        val transfer = validTransfer('A', 'B')

        assertTrue(store.add(transfer))
        assertEquals(listOf(transfer), AndroidFavoriteAttentionPendingStore(storage, CopyingPendingCipher).load())
    }

    @Test
    fun addingTheSameExportReplacesItsOlderPendingCopy() {
        val store = AndroidFavoriteAttentionPendingStore(
            MemoryPendingStorage(),
            CopyingPendingCipher,
        )
        val first = validTransfer('A', 'B')
        val revised = first.copy(
            entries = listOf(first.entries.single().copy(colorArgb = 0xFFCC5500.toInt())),
        )

        assertTrue(store.add(first))
        assertTrue(store.add(revised))
        assertEquals(listOf(revised), store.load())
    }

    @Test
    fun malformedTransfersAreRejectedBeforePersistence() {
        val storage = MemoryPendingStorage()
        val store = AndroidFavoriteAttentionPendingStore(storage, CopyingPendingCipher)
        val invalid = validTransfer('A', 'B').copy(scopeKey = "not-base64")

        assertFalse(store.add(invalid))
        assertTrue(store.load().isEmpty())
    }

    private fun validTransfer(scope: Char, token: Char) = AndroidFavoriteAttentionTransfer(
        scopeKey = scope.toString().repeat(43),
        entries = listOf(
            AndroidFavoriteAttentionTransferEntry(
                tokens = listOf(token.toString().repeat(43)),
                colorArgb = 0xFF005BBB.toInt(),
            ),
        ),
    )
}

private class MemoryPendingStorage : ContactDataBlobStorage {
    private var bytes: ByteArray? = null

    override fun read(): ByteArray? = bytes?.copyOf()

    override fun write(bytes: ByteArray) {
        this.bytes = bytes.copyOf()
    }

    override fun clear() {
        bytes = null
    }
}

private object CopyingPendingCipher : ContactDataCipher {
    override fun encrypt(plaintext: ByteArray): ByteArray = plaintext.copyOf()
    override fun decrypt(envelope: ByteArray): ByteArray = envelope.copyOf()
    override fun resetKey() = Unit
}
