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

private data class ExtractedMessage(
    val text: String,
    val timestamp: Long,
    val senderName: String?,
    val personUri: String?,
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

class MinkNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        connectedService = this
        NotificationHub.replace(activeNotifications.orEmpty().mapNotNull(::toConversationNotification))
    }

    override fun onListenerDisconnected() {
        if (connectedService === this) connectedService = null
        NotificationHub.replace(emptyList())
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        toConversationNotification(sbn)?.let(NotificationHub::upsert)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        NotificationHub.remove(sbn.key)
    }

    @Suppress("DEPRECATION")
    private fun toConversationNotification(sbn: StatusBarNotification): HubNotification? {
        if (sbn.packageName == packageName) return null
        val notification = sbn.notification ?: return null
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null
        val extras = notification.extras ?: Bundle.EMPTY
        val style = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                Notification.Builder.recoverBuilder(this, notification).style as? Notification.MessagingStyle
            }.getOrNull()
        } else {
            null
        }
        val replyAction = notification.actions?.firstOrNull { action ->
            action.remoteInputs?.any(RemoteInput::getAllowFreeFormInput) == true
        }
        val appName = runCatching {
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(sbn.packageName)
        val rawTitle = style?.conversationTitle
            ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
        val styleMessages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && style != null) {
            (style.historicMessages + style.messages).mapNotNull { message ->
                val body = message.text?.toString()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val person = message.senderPerson
                val sender = person?.name?.toString() ?: message.sender?.toString()
                ExtractedMessage(
                    text = body,
                    timestamp = message.timestamp,
                    senderName = sender,
                    personUri = person?.uri,
                    isOutgoing = sender == null,
                )
            }
        } else {
            extractLegacyMessages(extras, Notification.EXTRA_HISTORIC_MESSAGES) +
                extractLegacyMessages(extras, Notification.EXTRA_MESSAGES)
        }
        val isConversation = isConversationNotification(notification.category, styleMessages.isNotEmpty())
        if (!isConversation) return null
        val latestIncoming = styleMessages.lastOrNull { !it.isOutgoing }
        val senderName = latestIncoming?.senderName
        val isGroup = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            style?.isGroupConversation == true
        } else {
            extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE) != null
        }
        val conversationName = when {
            isGroup && !rawTitle.isNullOrBlank() -> rawTitle.toString()
            !senderName.isNullOrBlank() -> senderName
            !rawTitle.isNullOrBlank() -> rawTitle.toString()
            else -> appName
        }
        val contactIdentity = if (isGroup) null else resolveContactIdentity(latestIncoming?.personUri, conversationName)
        val providerIdentity = notification.shortcutId?.takeIf(String::isNotBlank) ?: normalizeName(conversationName)
        val conversationId = when {
            isGroup -> "${sbn.packageName}:group:$providerIdentity"
            contactIdentity != null -> "contact:$contactIdentity"
            else -> "${sbn.packageName}:direct:$providerIdentity"
        }
        val messages = styleMessages.mapIndexedNotNull { index, message ->
            val body = message.text.takeIf(String::isNotBlank) ?: return@mapIndexedNotNull null
            val sender = message.senderName
            val timestamp = message.timestamp.takeIf { it > 0 } ?: sbn.postTime
            ConversationMessage(
                id = "${sbn.packageName}|$timestamp|${sender.orEmpty()}|$body",
                conversationId = conversationId,
                notificationKey = sbn.key,
                packageName = sbn.packageName,
                appName = appName,
                text = body,
                timestamp = timestamp + index,
                senderName = sender,
                isOutgoing = message.isOutgoing,
            )
        }.ifEmpty {
            val body = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)
            listOfNotNull(body?.toString()?.takeIf(String::isNotBlank)?.let {
                ConversationMessage(
                    id = "${sbn.packageName}|${sbn.postTime}|$it",
                    conversationId = conversationId,
                    notificationKey = sbn.key,
                    packageName = sbn.packageName,
                    appName = appName,
                    text = it,
                    timestamp = sbn.postTime,
                    senderName = conversationName,
                    isOutgoing = false,
                )
            })
        }
        if (messages.isEmpty()) return null

        return HubNotification(
            key = sbn.key,
            conversationId = conversationId,
            conversationName = conversationName,
            packageName = sbn.packageName,
            appName = appName,
            postedAt = sbn.postTime,
            isOngoing = notification.flags and Notification.FLAG_ONGOING_EVENT != 0,
            messages = messages,
            contentIntent = notification.contentIntent,
            replyAction = replyAction,
        )
    }

    private fun extractLegacyMessages(extras: Bundle, key: String): List<ExtractedMessage> =
        extras.getParcelableArray(key).orEmpty().mapNotNull { it as? Bundle }.mapNotNull { bundle ->
            val body = bundle.getCharSequence("text")?.toString()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val sender = bundle.getCharSequence("sender")?.toString()
            ExtractedMessage(
                text = body,
                timestamp = bundle.getLong("time"),
                senderName = sender,
                personUri = null,
                isOutgoing = sender == null,
            )
        }

    private fun resolveContactIdentity(personUri: String?, displayName: String): String? {
        val parsed = personUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
        if (parsed?.scheme.equals("tel", ignoreCase = true)) {
            val digits = parsed?.schemeSpecificPart.orEmpty().filter(Char::isDigit)
            if (digits.isNotBlank()) {
                val lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(digits))
                queryLookupKey(lookupUri, ContactsContract.PhoneLookup.LOOKUP_KEY)?.let { return it }
                return "tel:$digits"
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null
        val matches = linkedSetOf<String>()
        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(displayName))
        runCatching {
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
                null,
                null,
                null,
            )?.use { cursor ->
                val keyIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
                val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                while (cursor.moveToNext()) {
                    if (normalizeName(cursor.getString(nameIndex)) == normalizeName(displayName)) matches += cursor.getString(keyIndex)
                }
            }
        }
        return matches.singleOrNull()
    }

    private fun queryLookupKey(uri: Uri, column: String): String? {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null
        return runCatching {
            contentResolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }

    private fun normalizeName(value: String): String = value.lowercase().filter(Char::isLetterOrDigit)

    companion object {
        @Volatile internal var connectedService: MinkNotificationListenerService? = null
    }
}
