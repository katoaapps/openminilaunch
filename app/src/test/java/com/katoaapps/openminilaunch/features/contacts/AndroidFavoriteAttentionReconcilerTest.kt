package com.katoaapps.openminilaunch.features.contacts

import com.katoaapps.openminilaunch.model.FavoriteContactId
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidFavoriteAttentionReconcilerTest {
    @Test
    fun unresolvedTransferWaitsForContactsThenAppliesExactlyOnce() {
        val pendingStore = pendingStore()
        val colorStore = colorStore(ReconcilerMemoryStorage())
        var liveCandidates = emptyMap<String, List<AndroidFavoriteIdentityCandidate>>()
        val reconciler = AndroidFavoriteAttentionReconciler(
            colorStore = colorStore,
            pendingStore = pendingStore,
            candidates = { liveCandidates },
        )
        val transfer = sourceTransfer()

        reconciler.restore(transfer)
        assertEquals(0, reconciler.reconcilePending())
        assertEquals(listOf(transfer), pendingStore.load())

        liveCandidates = mapOf("destination-lookup" to listOf(candidate()))
        assertEquals(1, reconciler.reconcilePending())
        assertEquals(
            BLUE,
            colorStore.color(FavoriteContactId.Android("destination-lookup")),
        )
        assertTrue(pendingStore.load().isEmpty())
        assertEquals(0, reconciler.reconcilePending())
    }

    @Test
    fun failedColorWriteKeepsTheTransferPending() {
        val pendingStore = pendingStore()
        val colorStore = colorStore(FailingReconcilerStorage())
        val transfer = sourceTransfer()
        val reconciler = AndroidFavoriteAttentionReconciler(
            colorStore = colorStore,
            pendingStore = pendingStore,
            candidates = { mapOf("destination-lookup" to listOf(candidate())) },
        )

        reconciler.restore(transfer)

        assertEquals(0, reconciler.reconcilePending())
        assertEquals(listOf(transfer), pendingStore.load())
        assertNull(colorStore.color(FavoriteContactId.Android("destination-lookup")))
    }

    @Test
    fun backupCreationDoesNotQueryContactsWithoutAndroidColors() {
        val colorStore = colorStore(ReconcilerMemoryStorage())
        colorStore.update(FavoriteContactId.Integrated("mink-contact"), BLUE)
        var candidateQueries = 0
        val reconciler = AndroidFavoriteAttentionReconciler(
            colorStore = colorStore,
            pendingStore = pendingStore(),
            candidates = {
                candidateQueries++
                emptyMap()
            },
        )

        assertNull(reconciler.createTransfer())
        assertEquals(0, candidateQueries)
    }

    private fun sourceTransfer(): AndroidFavoriteAttentionTransfer = checkNotNull(
        AndroidFavoriteAttentionTransfers.create(
            colorsByLookupKey = mapOf("source-lookup" to BLUE),
            candidatesByLookupKey = mapOf("source-lookup" to listOf(candidate())),
            scopeKeyBytes = ByteArray(32) { 9 },
        ),
    )

    private fun candidate() = AndroidFavoriteIdentityCandidate(
        kind = AndroidFavoriteIdentityKind.SOURCE_ID,
        accountType = "com.example.contacts",
        dataSet = null,
        value = "opaque-source-01",
    )

    private fun pendingStore() = AndroidFavoriteAttentionPendingStore(
        ReconcilerMemoryStorage(),
        ReconcilerCopyingCipher,
    )

    private fun colorStore(storage: ContactDataBlobStorage) = FavoriteAttentionColorStore(
        EncryptedFavoriteAttentionColorPersistence(storage, ReconcilerCopyingCipher),
    )

    private companion object {
        val BLUE = 0xFF005BBB.toInt()
    }
}

private open class ReconcilerMemoryStorage : ContactDataBlobStorage {
    private var bytes: ByteArray? = null

    override fun read(): ByteArray? = bytes?.copyOf()

    override fun write(bytes: ByteArray) {
        this.bytes = bytes.copyOf()
    }

    override fun clear() {
        bytes = null
    }
}

private class FailingReconcilerStorage : ReconcilerMemoryStorage() {
    override fun write(bytes: ByteArray) {
        throw IOException("simulated write failure")
    }
}

private object ReconcilerCopyingCipher : ContactDataCipher {
    override fun encrypt(plaintext: ByteArray): ByteArray = plaintext.copyOf()
    override fun decrypt(envelope: ByteArray): ByteArray = envelope.copyOf()
    override fun resetKey() = Unit
}
