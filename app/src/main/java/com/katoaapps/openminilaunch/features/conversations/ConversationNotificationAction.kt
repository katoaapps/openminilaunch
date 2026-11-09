package com.katoaapps.openminilaunch.features.conversations

import android.app.PendingIntent

/** A no-input action supplied by the app that posted a conversation notification. */
data class ConversationNotificationAction(
    val id: String,
    val title: String,
    val packageName: String,
    val appName: String,
    internal val notificationKey: String,
    internal val resolvesNotification: Boolean,
    internal val actionIntent: PendingIntent,
)

internal fun shouldExposeConversationAction(
    title: String,
    requiresRemoteInput: Boolean,
): Boolean = title.isNotBlank() && !requiresRemoteInput

internal fun resolvesConversationNotification(semanticAction: Int): Boolean = when (semanticAction) {
    android.app.Notification.Action.SEMANTIC_ACTION_MARK_AS_READ,
    android.app.Notification.Action.SEMANTIC_ACTION_DELETE,
    android.app.Notification.Action.SEMANTIC_ACTION_ARCHIVE,
    -> true
    else -> false
}
