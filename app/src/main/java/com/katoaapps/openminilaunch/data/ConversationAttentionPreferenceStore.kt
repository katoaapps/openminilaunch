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
    private val acknowledgements = mutableStateOf(loadAcknowledgements())

    fun acknowledgedThrough(conversationId: String): Long =
        acknowledgements.value[conversationIdHash(conversationId)] ?: 0L

    fun acknowledge(conversationId: String, activityAt: Long?) {
        val next = activityAt?.coerceAtLeast(0L) ?: return
        val key = conversationIdHash(conversationId)
        val current = acknowledgements.value
        if (next <= (current[key] ?: 0L)) return
        val updated = (current + (key to next)).entries
            .sortedByDescending(Map.Entry<String, Long>::value)
            .take(MAX_ACKNOWLEDGEMENTS)
            .associate { it.toPair() }
        acknowledgements.value = updated
        prefs.edit().putStringSet(
            ACKNOWLEDGEMENTS_KEY,
            updated.mapTo(linkedSetOf()) { (id, timestamp) -> "$id|$timestamp" },
        ).apply()
    }

    private fun loadAcknowledgements(): Map<String, Long> = prefs
        .getStringSet(ACKNOWLEDGEMENTS_KEY, emptySet())
        .orEmpty()
        .mapNotNull { entry ->
            val separator = entry.lastIndexOf('|')
            if (separator <= 0) return@mapNotNull null
            val timestamp = entry.substring(separator + 1).toLongOrNull() ?: return@mapNotNull null
            entry.substring(0, separator) to timestamp
        }
        .toMap()

    private fun conversationIdHash(conversationId: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(conversationId.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private companion object {
        const val ACKNOWLEDGEMENTS_KEY = "conversation_attention_acknowledgements"
        const val MAX_ACKNOWLEDGEMENTS = 512
    }
}
