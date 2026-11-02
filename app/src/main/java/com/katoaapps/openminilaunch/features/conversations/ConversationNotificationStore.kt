package com.katoaapps.openminilaunch.features.conversations

import android.app.Activity
import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.NotificationManagerCompat
import java.util.Locale

data class ConversationMessage(
    val id: String,
    val conversationId: String,
    val notificationKey: String,
    val packageName: String,
    val appName: String,
    val text: String,
    val timestamp: Long,
    val senderName: String?,
    val isOutgoing: Boolean,
)

internal fun isConversationNotification(category: String?, hasStructuredMessages: Boolean): Boolean =
    hasStructuredMessages ||
        category == Notification.CATEGORY_MESSAGE ||
        category == Notification.CATEGORY_EMAIL

data class ConversationNotification(
    val key: String,
    val conversationId: String,
    val conversationName: String,
    val packageName: String,
    val appName: String,
    val postedAt: Long,
    val isDismissible: Boolean,
    val messages: List<ConversationMessage>,
    internal val contentIntent: PendingIntent?,
    internal val replyAction: Notification.Action?,
    val actions: List<ConversationNotificationAction> = emptyList(),
    val isDemo: Boolean = false,
) {
    val canReply: Boolean get() = isDemo || replyAction != null
}

data class NotificationConversation(
    val id: String,
    val name: String,
    val notifications: List<ConversationNotification>,
    val messages: List<ConversationMessage>,
) {
    val latestNotification: ConversationNotification get() =
        notifications.maxBy(ConversationNotification::postedAt)
    val openTarget: ConversationNotification get() =
        notifications.firstOrNull { it.contentIntent != null } ?: latestNotification
    val latestMessage: ConversationMessage? get() = messages.maxByOrNull(ConversationMessage::timestamp)
    val replyTarget: ConversationNotification? get() = notifications
        .filter(ConversationNotification::canReply)
        .maxByOrNull(ConversationNotification::postedAt)
    val sourcePackages: List<String> get() = notifications
        .sortedByDescending(ConversationNotification::postedAt)
        .map(ConversationNotification::packageName)
        .distinct()
    val actions: List<ConversationNotificationAction> get() = notifications
        .sortedByDescending(ConversationNotification::postedAt)
        .flatMap(ConversationNotification::actions)
        .distinctBy { action ->
            action.packageName to action.title.lowercase(Locale.ROOT)
        }
    val canDismiss: Boolean get() = notifications.any(ConversationNotification::isDismissible)
}

object ConversationNotificationStore {
    val activeNotifications = mutableStateListOf<ConversationNotification>()
    private val sentReplies = mutableStateListOf<ConversationMessage>()
    private val demoReplies = mutableStateListOf<ConversationMessage>()

    fun hasNotificationAccess(context: Context): Boolean =
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    fun notificationAccessSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    fun currentConversations(useDemoData: Boolean = false): List<NotificationConversation> =
        groupConversations(
            sourceNotifications = if (useDemoData) {
                DemoConversationData.notifications()
            } else {
                activeNotifications
            },
            replies = if (useDemoData) demoReplies else sentReplies,
        )

    internal fun groupConversations(
        sourceNotifications: List<ConversationNotification>,
        replies: List<ConversationMessage>,
    ): List<NotificationConversation> = sourceNotifications
        .groupBy(ConversationNotification::conversationId)
        .map { (id, grouped) ->
            val orderedNotifications = grouped.sortedByDescending(ConversationNotification::postedAt)
            val providerMessages = grouped.flatMap(ConversationNotification::messages)
            val localReplies = replies.filter { it.conversationId == id }.filterNot { local ->
                providerMessages.any { provider ->
                    provider.isOutgoing && provider.text == local.text &&
                        kotlin.math.abs(provider.timestamp - local.timestamp) < 120_000L
                }
            }
            val messages = (providerMessages + localReplies)
                .distinctBy(ConversationMessage::id)
                .sortedBy(ConversationMessage::timestamp)
            NotificationConversation(
                id = id,
                name = orderedNotifications.first().conversationName,
                notifications = orderedNotifications,
                messages = messages,
            )
        }
        .sortedByDescending { it.latestMessage?.timestamp ?: it.latestNotification.postedAt }

    internal fun replaceNotifications(items: List<ConversationNotification>) {
        activeNotifications.clear()
        activeNotifications.addAll(items.sortedByDescending(ConversationNotification::postedAt))
        val activeConversationIds = items.map(ConversationNotification::conversationId).toSet()
        sentReplies.removeAll { it.conversationId !in activeConversationIds }
    }

    internal fun upsertNotification(item: ConversationNotification) {
        activeNotifications.removeAll { it.key == item.key }
        activeNotifications += item
        activeNotifications.sortByDescending(ConversationNotification::postedAt)
    }

    internal fun removeNotification(key: String) {
        val conversationId = activeNotifications.firstOrNull { it.key == key }?.conversationId
        activeNotifications.removeAll { it.key == key }
        if (conversationId != null && activeNotifications.none { it.conversationId == conversationId }) {
            sentReplies.removeAll { it.conversationId == conversationId }
        }
    }

    fun openConversation(context: Context, item: ConversationNotification): Boolean {
        val pendingIntent = item.contentIntent ?: return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                    .toBundle()
                pendingIntent.send(context, 0, null, null, null, null, options)
            } else if (context is Activity) {
                context.startIntentSender(pendingIntent.intentSender, null, 0, 0, 0)
            } else {
                pendingIntent.send()
            }
            true
        }.getOrDefault(false)
    }

    fun sendReply(item: ConversationNotification, reply: String): Boolean {
        val clean = reply.trim()
        if (clean.isEmpty()) return false
        if (item.isDemo) {
            val now = System.currentTimeMillis()
            demoReplies += ConversationMessage(
                id = "demo-local|${item.conversationId}|$now|${System.nanoTime()}",
                conversationId = item.conversationId,
                notificationKey = item.key,
                packageName = item.packageName,
                appName = item.appName,
                text = clean,
                timestamp = now,
                senderName = null,
                isOutgoing = true,
            )
            return true
        }
        val action = item.replyAction ?: return false
        val inputs = action.remoteInputs
            ?.filter(RemoteInput::getAllowFreeFormInput)
            ?.toTypedArray()
            .orEmpty()
        if (inputs.isEmpty()) return false
        return runCatching {
            val results = Bundle().apply { inputs.forEach { putCharSequence(it.resultKey, clean) } }
            val fillIn = Intent()
            RemoteInput.addResultsToIntent(inputs, fillIn, results)
            action.actionIntent.send(MinkNotificationListenerService.connectedService, 0, fillIn)
            val now = System.currentTimeMillis()
            sentReplies += ConversationMessage(
                id = "local|${item.conversationId}|$now|${System.nanoTime()}",
                conversationId = item.conversationId,
                notificationKey = item.key,
                packageName = item.packageName,
                appName = item.appName,
                text = clean,
                timestamp = now,
                senderName = null,
                isOutgoing = true,
            )
            true
        }.getOrDefault(false)
    }

    fun performAction(context: Context, action: ConversationNotificationAction): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val options = ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                .toBundle()
            action.actionIntent.send(context, 0, null, null, null, null, options)
        } else {
            action.actionIntent.send(context, 0, null)
        }
        true
    }.getOrDefault(false)

    fun dismissConversation(conversation: NotificationConversation): Boolean {
        val service = MinkNotificationListenerService.connectedService ?: return false
        var dismissedAny = false
        conversation.notifications
            .filter(ConversationNotification::isDismissible)
            .forEach { notification ->
                val requested = runCatching {
                    service.cancelNotification(notification.key)
                    true
                }.getOrDefault(false)
                if (requested) {
                    removeNotification(notification.key)
                    dismissedAny = true
                }
            }
        return dismissedAny
    }

    fun clearDemoReplyHistory() {
        demoReplies.clear()
    }

    fun requestListenerReconnect(context: Context) {
        runCatching {
            NotificationListenerService.requestRebind(
                ComponentName(context, MinkNotificationListenerService::class.java),
            )
        }
    }
}
