package com.katoaapps.openminilaunch.features.conversations

import java.security.MessageDigest
import java.util.LinkedHashMap
import java.util.LinkedHashSet

/**
 * Protects the live conversation feed from notification payloads that Android or a source app
 * silently republishes after the user already resolved them.
 *
 * Its bounded, process-local state retains only timestamps and one-way hashes. Message text,
 * sender names, notification keys, and raw conversation identities are never persisted or copied
 * into that state.
 */
internal class ConversationNotificationReconciler(
    private val maxRememberedStates: Int = MAX_REMEMBERED_STATES,
) {
    init {
        require(maxRememberedStates > 0)
    }

    private val latestActivityByNotificationKey = LinkedHashMap<String, Long>()
    private val suppressedContentStates = LinkedHashSet<String>()

    fun reconcile(items: List<ConversationNotification>): List<ConversationNotification> =
        items.filter(::shouldDisplay)

    fun shouldDisplay(item: ConversationNotification): Boolean {
        if (conversationNotificationContentHash(item) in suppressedContentStates) return false

        val notificationKeyHash = oneWayHash(item.key)
        val activityAt = notificationContentActivityAt(item)
        val latestKnownActivity = latestActivityByNotificationKey[notificationKeyHash] ?: 0L
        if (activityAt < latestKnownActivity) return false

        if (activityAt > latestKnownActivity) {
            rememberLatestActivity(notificationKeyHash, activityAt)
        }
        return true
    }

    fun suppress(item: ConversationNotification) {
        val contentHash = conversationNotificationContentHash(item)
        suppressedContentStates.remove(contentHash)
        suppressedContentStates += contentHash
        val notificationKeyHash = oneWayHash(item.key)
        rememberLatestActivity(
            notificationKeyHash,
            maxOf(
                latestActivityByNotificationKey[notificationKeyHash] ?: 0L,
                notificationContentActivityAt(item),
            ),
        )
        trimToCapacity(suppressedContentStates)
    }

    private fun rememberLatestActivity(notificationKeyHash: String, activityAt: Long) {
        latestActivityByNotificationKey.remove(notificationKeyHash)
        latestActivityByNotificationKey[notificationKeyHash] = activityAt
        while (latestActivityByNotificationKey.size > maxRememberedStates) {
            val oldest = latestActivityByNotificationKey.keys.firstOrNull() ?: break
            latestActivityByNotificationKey.remove(oldest)
        }
    }

    private fun trimToCapacity(states: LinkedHashSet<String>) {
        while (states.size > maxRememberedStates) {
            val oldest = states.firstOrNull() ?: break
            states.remove(oldest)
        }
    }
}

/** Source-message activity is authoritative; a fresh notification wrapper is not a new message. */
internal fun notificationContentActivityAt(item: ConversationNotification): Long =
    item.messages.asSequence()
        .filterNot(ConversationMessage::isOutgoing)
        .maxOfOrNull(ConversationMessage::timestamp)
        ?: 0L

internal fun conversationNotificationContentHash(item: ConversationNotification): String {
    val incomingMessages = item.messages.filterNot(ConversationMessage::isOutgoing)
    val relevantMessages = incomingMessages.ifEmpty { item.messages }
    val latestMessage = relevantMessages.maxWithOrNull(
        compareBy(ConversationMessage::timestamp, ConversationMessage::id),
    )
    val contentIdentity = buildString {
        append(item.packageName)
        append('\u0000')
        append(item.conversationId)
        append('\u0000')
        append(latestMessage?.id.orEmpty())
    }
    return oneWayHash(contentIdentity)
}

private fun oneWayHash(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8))
    .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

private const val MAX_REMEMBERED_STATES = 256
