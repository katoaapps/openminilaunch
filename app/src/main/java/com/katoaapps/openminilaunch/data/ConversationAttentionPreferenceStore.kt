package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import java.security.MessageDigest

/**
 * Remembers acknowledgement independently for each conversation.
 *
 * Only one-way hashes and timestamps are persisted. Message contents, people, notification keys,
 * source apps, and raw conversation identities never enter preferences here.
 */
internal class ConversationAttentionPreferenceStore(private val prefs: SharedPreferences) {
    private val acknowledgements = mutableStateOf(
        ConversationAttentionPreferenceCodec.decode(
            prefs.getStringSet(ACKNOWLEDGEMENTS_KEY, emptySet()),
        ),
    )

    fun acknowledgedThrough(conversationId: String): Long =
        acknowledgements.value[conversationIdHash(conversationId)] ?: 0L

    fun acknowledge(conversationId: String, activityAt: Long?) {
        val next = activityAt?.coerceAtLeast(0L) ?: return
        val key = conversationIdHash(conversationId)
        val current = acknowledgements.value
        if (next <= (current[key] ?: 0L)) return
        val updated = updateConversationAcknowledgements(current, key, next)
        if (updated === current) return
        acknowledgements.value = updated
        prefs.edit().putStringSet(
            ACKNOWLEDGEMENTS_KEY,
            ConversationAttentionPreferenceCodec.encode(updated),
        ).apply()
    }

    private companion object {
        const val ACKNOWLEDGEMENTS_KEY = "conversation_attention_acknowledgements"
    }
}

internal fun conversationIdHash(conversationId: String): String = MessageDigest
    .getInstance("SHA-256")
    .digest(conversationId.toByteArray(Charsets.UTF_8))
    .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

internal fun updateConversationAcknowledgements(
    current: Map<String, Long>,
    conversationHash: String,
    acknowledgedThrough: Long,
): Map<String, Long> {
    if (conversationHash.isBlank() || acknowledgedThrough <= (current[conversationHash] ?: 0L)) {
        return current
    }
    return (current + (conversationHash to acknowledgedThrough)).entries
        .sortedByDescending(Map.Entry<String, Long>::value)
        .take(MAX_ACKNOWLEDGEMENTS)
        .associate { it.toPair() }
}

internal object ConversationAttentionPreferenceCodec {
    fun encode(acknowledgements: Map<String, Long>): Set<String> = acknowledgements
        .filter { (id, timestamp) -> id.isNotBlank() && timestamp >= 0L }
        .mapTo(linkedSetOf()) { (id, timestamp) -> "$id|$timestamp" }

    fun decode(entries: Set<String>?): Map<String, Long> {
        val parsed = entries.orEmpty().mapNotNull { entry ->
            val separator = entry.lastIndexOf('|')
            if (separator <= 0) return@mapNotNull null
            val timestamp = entry.substring(separator + 1).toLongOrNull()
                ?.takeIf { it >= 0L }
                ?: return@mapNotNull null
            entry.substring(0, separator)
                .takeIf(String::isNotBlank)
                ?.let { it to timestamp }
        }
        return buildMap {
            for ((id, timestamp) in parsed.sortedByDescending(Pair<String, Long>::second)) {
                if (size >= MAX_ACKNOWLEDGEMENTS) break
                putIfAbsent(id, timestamp)
            }
        }
    }
}

private const val MAX_ACKNOWLEDGEMENTS = 512
