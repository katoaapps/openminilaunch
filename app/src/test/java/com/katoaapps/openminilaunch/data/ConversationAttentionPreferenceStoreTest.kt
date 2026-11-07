package com.katoaapps.openminilaunch.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationAttentionPreferenceStoreTest {
    @Test
    fun codecRoundTripsValidAcknowledgementsAndDropsMalformedEntries() {
        val decoded = ConversationAttentionPreferenceCodec.decode(
            setOf(
                "first|10",
                "first|5",
                "second|20",
                "missing-timestamp|",
                "negative|-1",
                "not-a-timestamp|later",
                "|30",
            ),
        )

        assertEquals(mapOf("second" to 20L, "first" to 10L), decoded)
        assertEquals(decoded, ConversationAttentionPreferenceCodec.decode(
            ConversationAttentionPreferenceCodec.encode(decoded),
        ))
    }

    @Test
    fun newerAcknowledgementReplacesOlderTimestamp() {
        val current = mapOf("conversation" to 10L)

        val updated = updateConversationAcknowledgements(current, "conversation", 20L)

        assertEquals(20L, updated["conversation"])
        assertSame(updated, updateConversationAcknowledgements(updated, "conversation", 20L))
        assertSame(updated, updateConversationAcknowledgements(updated, "conversation", 5L))
    }

    @Test
    fun acknowledgementHistoryKeepsTheNewest512Entries() {
        val current = (0 until 512).associate { index -> "conversation-$index" to index.toLong() }

        val updated = updateConversationAcknowledgements(current, "newest", 10_000L)

        assertEquals(512, updated.size)
        assertTrue("newest" in updated)
        assertFalse("conversation-0" in updated)
    }

    @Test
    fun persistedConversationIdentityIsOpaqueAndDeterministic() {
        val rawIdentity = "com.example.messages:direct:May"

        val first = conversationIdHash(rawIdentity)

        assertEquals(first, conversationIdHash(rawIdentity))
        assertFalse(first.contains("May", ignoreCase = true))
        assertEquals(64, first.length)
    }
}
