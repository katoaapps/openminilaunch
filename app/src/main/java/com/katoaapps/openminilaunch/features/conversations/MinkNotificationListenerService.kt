package com.katoaapps.openminilaunch.features.conversations

import android.Manifest
import android.app.Notification
import android.app.RemoteInput
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat

/** Converts supported notification payloads into the local conversation model. */
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
        if (!isConversationNotification(notification.category, styleMessages.isNotEmpty())) return null
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
        val contactIdentity = if (isGroup) {
            null
        } else {
            resolveContactIdentity(latestIncoming?.personUri, conversationName)
        }
        val providerIdentity = notification.shortcutId
            ?.takeIf(String::isNotBlank)
            ?: normalizeName(conversationName)
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

    @Suppress("DEPRECATION")
    private fun extractLegacyMessages(extras: Bundle, key: String): List<ExtractedMessage> =
        extras.getParcelableArray(key).orEmpty().mapNotNull { it as? Bundle }.mapNotNull { bundle ->
            val body = bundle.getCharSequence("text")?.toString()?.takeIf(String::isNotBlank)
                ?: return@mapNotNull null
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
                val lookupUri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(digits),
                )
                queryLookupKey(lookupUri, ContactsContract.PhoneLookup.LOOKUP_KEY)?.let { return it }
                return "tel:$digits"
            }
        }
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) return null
        val matches = linkedSetOf<String>()
        val uri = Uri.withAppendedPath(
            ContactsContract.Contacts.CONTENT_FILTER_URI,
            Uri.encode(displayName),
        )
        runCatching {
            contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.Contacts.LOOKUP_KEY,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                val keyIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
                val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                while (cursor.moveToNext()) {
                    if (normalizeName(cursor.getString(nameIndex)) == normalizeName(displayName)) {
                        matches += cursor.getString(keyIndex)
                    }
                }
            }
        }
        return matches.singleOrNull()
    }

    private fun queryLookupKey(uri: Uri, column: String): String? {
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) return null
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

private data class ExtractedMessage(
    val text: String,
    val timestamp: Long,
    val senderName: String?,
    val personUri: String?,
    val isOutgoing: Boolean,
)
