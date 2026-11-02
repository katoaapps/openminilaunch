package com.katoaapps.openminilaunch.features.conversations

import android.app.PendingIntent

/** A no-input action supplied by the app that posted a conversation notification. */
data class ConversationNotificationAction(
    val id: String,
    val title: String,
    val packageName: String,
    val appName: String,
    internal val actionIntent: PendingIntent,
)

internal fun shouldExposeConversationAction(
    title: String,
    requiresRemoteInput: Boolean,
): Boolean = title.isNotBlank() && !requiresRemoteInput
