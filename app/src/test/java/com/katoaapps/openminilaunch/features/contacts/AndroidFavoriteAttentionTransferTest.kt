package com.katoaapps.openminilaunch.features.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidFavoriteAttentionTransferTest {
    @Test
    fun exactProviderIdentityReconcilesToTheDestinationLookupKey() {
        val transfer = createTransfer(
            colors = mapOf("source-lookup" to BLUE),
            candidates = mapOf("source-lookup" to listOf(candidate("opaque-source-01"))),
        )

        val resolution = AndroidFavoriteAttentionTransfers.resolve(
            checkNotNull(transfer),
            mapOf("destination-lookup" to listOf(candidate("opaque-source-01"))),
        )

        assertEquals(1, resolution.matches.size)
        assertEquals("destination-lookup", resolution.matches.single().lookupKey)
        assertEquals(BLUE, resolution.matches.single().entry.colorArgb)
        assertTrue(resolution.unresolvedEntries.isEmpty())
    }

    @Test
    fun tokensAreExportScopedAndDoNotContainProviderMetadata() {
        val first = createTransfer(
            colors = mapOf("lookup" to BLUE),
            candidates = mapOf("lookup" to listOf(candidate("opaque-source-01"))),
            scopeByte = 1,
        )
        val second = createTransfer(
            colors = mapOf("lookup" to BLUE),
            candidates = mapOf("lookup" to listOf(candidate("opaque-source-01"))),
            scopeByte = 2,
        )
        val firstToken = checkNotNull(first).entries.single().tokens.single()
        val secondToken = checkNotNull(second).entries.single().tokens.single()

        assertNotEquals(first.scopeKey, second.scopeKey)
        assertNotEquals(firstToken, secondToken)
        assertFalse(firstToken.contains("opaque-source-01"))
        assertFalse(firstToken.contains("com.example.contacts"))
    }

    @Test
    fun ambiguousDestinationDoesNotReceiveAColor() {
        val transfer = createTransfer(
            colors = mapOf("source-lookup" to BLUE),
            candidates = mapOf("source-lookup" to listOf(candidate("opaque-source-01"))),
        )

        val resolution = AndroidFavoriteAttentionTransfers.resolve(
            checkNotNull(transfer),
            mapOf(
                "destination-a" to listOf(candidate("opaque-source-01")),
                "destination-b" to listOf(candidate("opaque-source-01")),
            ),
        )

        assertTrue(resolution.matches.isEmpty())
        assertEquals(transfer.entries, resolution.unresolvedEntries)
    }

    @Test
    fun ambiguousProviderIdentityIsNotWrittenToTheBackup() {
        val duplicate = candidate("opaque-source-01")

        assertNull(
            createTransfer(
                colors = mapOf("source-a" to BLUE, "source-b" to ORANGE),
                candidates = mapOf(
                    "source-a" to listOf(duplicate),
                    "source-b" to listOf(duplicate),
                ),
            ),
        )
    }

    @Test
    fun twoImportedEntriesCannotClaimTheSameDestination() {
        val original = createTransfer(
            colors = mapOf("source-lookup" to BLUE),
            candidates = mapOf("source-lookup" to listOf(candidate("opaque-source-01"))),
        )
        val entry = checkNotNull(original).entries.single()
        val duplicated = original.copy(entries = listOf(entry, entry.copy(colorArgb = ORANGE)))

        val resolution = AndroidFavoriteAttentionTransfers.resolve(
            duplicated,
            mapOf("destination" to listOf(candidate("opaque-source-01"))),
        )

        assertTrue(resolution.matches.isEmpty())
        assertEquals(duplicated.entries, resolution.unresolvedEntries)
    }

    @Test
    fun directContactInformationIsNotTokenized() {
        val rejectedValues = listOf(
            "person@example.com",
            "+1 (555) 555-1212",
            "https://example.com/person",
        )

        rejectedValues.forEach { value ->
            assertNull(
                createTransfer(
                    colors = mapOf("lookup" to BLUE),
                    candidates = mapOf("lookup" to listOf(candidate(value))),
                ),
            )
        }
    }

    @Test
    fun providerMetadataMustBeBoundedAndFreeOfControlCharacters() {
        assertFalse(
            isSafePortableContactCandidate(
                candidate("opaque-source-01").copy(accountType = "bad\u0000type"),
            ),
        )
        assertFalse(
            isSafePortableContactCandidate(
                candidate("opaque-source-01").copy(dataSet = "x".repeat(513)),
            ),
        )
        assertTrue(isSafePortableContactCandidate(candidate("opaque-source-01")))
    }

    private fun createTransfer(
        colors: Map<String, Int>,
        candidates: Map<String, List<AndroidFavoriteIdentityCandidate>>,
        scopeByte: Byte = 7,
    ): AndroidFavoriteAttentionTransfer? = AndroidFavoriteAttentionTransfers.create(
        colorsByLookupKey = colors,
        candidatesByLookupKey = candidates,
        scopeKeyBytes = ByteArray(32) { scopeByte },
    )

    private fun candidate(value: String) = AndroidFavoriteIdentityCandidate(
        kind = AndroidFavoriteIdentityKind.SOURCE_ID,
        accountType = "com.example.contacts",
        dataSet = null,
        value = value,
    )

    private companion object {
        val BLUE = 0xFF005BBB.toInt()
        val ORANGE = 0xFFCC5500.toInt()
    }
}
