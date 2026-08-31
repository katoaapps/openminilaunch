package com.katoaapps.openminilaunch.features.conversations


import android.Manifest
import android.app.Activity
import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

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

data class HubNotification(
    val key: String,
    val conversationId: String,
    val conversationName: String,
    val packageName: String,
    val appName: String,
    val postedAt: Long,
    val isOngoing: Boolean,
    val messages: List<ConversationMessage>,
    internal val contentIntent: PendingIntent?,
    internal val replyAction: Notification.Action?,
    val isDemo: Boolean = false,
) {
    val canReply: Boolean get() = isDemo || replyAction != null
}

data class HubConversation(
    val id: String,
    val name: String,
    val notifications: List<HubNotification>,
    val messages: List<ConversationMessage>,
) {
    val latestNotification: HubNotification get() = notifications.maxBy(HubNotification::postedAt)
    val openTarget: HubNotification get() = notifications.firstOrNull { it.contentIntent != null } ?: latestNotification
    val latestMessage: ConversationMessage? get() = messages.maxByOrNull(ConversationMessage::timestamp)
    val replyTarget: HubNotification? get() = notifications.filter(HubNotification::canReply).maxByOrNull(HubNotification::postedAt)
    val sourcePackages: List<String> get() = notifications.sortedByDescending(HubNotification::postedAt).map(HubNotification::packageName).distinct()
}

object NotificationHub {
    val notifications = mutableStateListOf<HubNotification>()
    private val sentReplies = mutableStateListOf<ConversationMessage>()
    private val demoReplies = mutableStateListOf<ConversationMessage>()

    fun hasAccess(context: Context): Boolean =
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    fun accessSettingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    fun conversations(useDemoData: Boolean = false): List<HubConversation> = buildConversations(
        sourceNotifications = if (useDemoData) DemoConversationData.notifications() else notifications,
        replies = if (useDemoData) demoReplies else sentReplies,
    )

    internal fun buildConversations(
        sourceNotifications: List<HubNotification>,
        replies: List<ConversationMessage>,
    ): List<HubConversation> = sourceNotifications
        .groupBy(HubNotification::conversationId)
        .map { (id, grouped) ->
            val orderedNotifications = grouped.sortedByDescending(HubNotification::postedAt)
            val providerMessages = grouped.flatMap(HubNotification::messages)
            val localReplies = replies.filter { it.conversationId == id }.filterNot { local ->
                providerMessages.any { provider ->
                    provider.isOutgoing && provider.text == local.text &&
                        kotlin.math.abs(provider.timestamp - local.timestamp) < 120_000L
                }
            }
            val messages = (providerMessages + localReplies)
                .distinctBy(ConversationMessage::id)
                .sortedBy(ConversationMessage::timestamp)
            HubConversation(
                id = id,
                name = orderedNotifications.first().conversationName,
                notifications = orderedNotifications,
                messages = messages,
            )
        }
        .sortedByDescending { it.latestMessage?.timestamp ?: it.latestNotification.postedAt }

    internal fun replace(items: List<HubNotification>) {
        notifications.clear()
        notifications.addAll(items.sortedByDescending(HubNotification::postedAt))
        val activeConversationIds = items.map(HubNotification::conversationId).toSet()
        sentReplies.removeAll { it.conversationId !in activeConversationIds }
    }

    internal fun upsert(item: HubNotification) {
        notifications.removeAll { it.key == item.key }
        notifications += item
        notifications.sortByDescending(HubNotification::postedAt)
    }

    internal fun remove(key: String) {
        val conversationId = notifications.firstOrNull { it.key == key }?.conversationId
        notifications.removeAll { it.key == key }
        if (conversationId != null && notifications.none { it.conversationId == conversationId }) {
            sentReplies.removeAll { it.conversationId == conversationId }
        }
    }

    fun open(context: Context, item: HubNotification): Boolean {
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

    fun reply(item: HubNotification, reply: String): Boolean {
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
        val inputs = action.remoteInputs?.filter(RemoteInput::getAllowFreeFormInput)?.toTypedArray().orEmpty()
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

    fun clearDemoReplies() {
        demoReplies.clear()
    }

    fun requestReconnect(context: Context) {
        runCatching {
            NotificationListenerService.requestRebind(
                ComponentName(context, MinkNotificationListenerService::class.java),
            )
        }
    }
}
