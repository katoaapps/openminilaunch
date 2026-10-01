package com.katoaapps.openminilaunch.features.bubbles

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.apps.LauncherAppRepository
import com.katoaapps.openminilaunch.model.LauncherAppTarget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toBitmap

/** Creates the dynamic shortcut and notification required by Android's bubble API. */
internal class AppBubbleNotificationPublisher(context: Context) {
    private val appContext = context.applicationContext
    private val notificationManager = appContext.getSystemService(NotificationManager::class.java)
    private val shortcutManager = appContext.getSystemService(ShortcutManager::class.java)
    private val appRepository = LauncherAppRepository.get(appContext)
    private val publicationVerifier = AppBubblePublicationVerifier(appContext)

    @RequiresApi(Build.VERSION_CODES.R)
    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.app_bubbles_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = appContext.getString(R.string.app_bubbles_channel_description)
            setAllowBubbles(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun channelCanBubble(): Boolean =
        notificationManager.getNotificationChannel(CHANNEL_ID)?.canBubble() == true

    @RequiresApi(Build.VERSION_CODES.R)
    fun publish(target: LauncherAppTarget): Boolean {
        val shortcutId = shortcutId(target)
        val bubbleIcon = targetIcon(target)
        val hostIntent = AppBubbleHostActivity.intentFor(appContext, target)
        if (!publishShortcut(target, shortcutId, bubbleIcon, hostIntent)) return false

        val bubbleIntent = targetPendingIntent(target)
        val notification = buildNotification(target, shortcutId, bubbleIcon, bubbleIntent)
        val notificationTag = notificationTag(target)
        notificationManager.notify(notificationTag, NOTIFICATION_ID, notification)
        publicationVerifier.verify(
            target = target,
            notificationTag = notificationTag,
            notificationId = NOTIFICATION_ID,
            onRejected = {
                notificationManager.cancel(notificationTag, NOTIFICATION_ID)
            },
        )
        return true
    }

    fun dismiss(target: LauncherAppTarget) {
        publicationVerifier.cancel(target)
        notificationManager.cancel(notificationTag(target), NOTIFICATION_ID)
    }

    private fun targetPendingIntent(target: LauncherAppTarget): PendingIntent =
        PendingIntent.getActivity(
            appContext,
            target.selectionKey.hashCode(),
            Intent.makeMainActivity(target.componentName),
            PendingIntent.FLAG_UPDATE_CURRENT or bubblePendingIntentMutabilityFlag(),
        )

    @RequiresApi(Build.VERSION_CODES.R)
    private fun buildNotification(
        target: LauncherAppTarget,
        shortcutId: String,
        bubbleIcon: Icon,
        bubbleIntent: PendingIntent,
    ): Notification {
        val appPerson = Person.Builder()
            .setName(target.label)
            .setKey(target.selectionKey)
            .setIcon(bubbleIcon)
            .build()
        val minkPerson = Person.Builder()
            .setName(appContext.getString(R.string.app_name))
            .setKey(MINK_PERSON_KEY)
            .build()
        val bubble = Notification.BubbleMetadata.Builder(bubbleIntent, bubbleIcon)
            .setDesiredHeight(
                maxOf(
                    MIN_EXPANDED_BUBBLE_HEIGHT_DP,
                    appContext.resources.configuration.screenHeightDp,
                ),
            )
            .setAutoExpandBubble(true)
            .setSuppressNotification(true)
            .build()
        val notificationText = appContext.getString(
            R.string.app_bubble_notification_text,
            target.label,
        )

        return Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_bubble_notification)
            .setContentTitle(target.label)
            .setContentText(notificationText)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setShortcutId(shortcutId)
            .setOnlyAlertOnce(true)
            .setContentIntent(bubbleIntent)
            .setBubbleMetadata(bubble)
            .setStyle(
                Notification.MessagingStyle(minkPerson)
                    .setConversationTitle(target.label)
                    .addMessage(notificationText, System.currentTimeMillis(), appPerson),
            )
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun publishShortcut(
        target: LauncherAppTarget,
        shortcutId: String,
        icon: Icon,
        shortcutIntent: Intent,
    ): Boolean {
        if (hasUsableShortcut(shortcutId)) return true

        val shortcut = ShortcutInfo.Builder(appContext, shortcutId)
            .setShortLabel(target.label.take(SHORT_LABEL_LENGTH))
            .setLongLabel(target.label.take(LONG_LABEL_LENGTH))
            .setIcon(icon)
            .setIntent(shortcutIntent)
            .setPerson(
                Person.Builder()
                    .setName(target.label)
                    .setKey(target.selectionKey)
                    .setIcon(icon)
                    .build(),
            )
            .setLongLived(true)
            .build()
        val pushed = runCatching {
            shortcutManager.pushDynamicShortcut(shortcut)
            true
        }.onFailure { error ->
            Log.w(
                APP_BUBBLE_LOG_TAG,
                "Unable to push app-bubble shortcut; trying addDynamicShortcuts",
                error,
            )
        }.getOrDefault(false)
        return pushed || runCatching { shortcutManager.addDynamicShortcuts(listOf(shortcut)) }
            .onFailure { error ->
                Log.w(APP_BUBBLE_LOG_TAG, "Unable to add app-bubble shortcut", error)
            }
            .getOrDefault(false)
    }

    private fun hasUsableShortcut(shortcutId: String): Boolean = runCatching {
        shortcutManager.dynamicShortcuts.any { shortcut ->
            shortcut.id == shortcutId &&
                shortcut.isEnabled &&
                AppBubbleHostActivity.isCurrentIntent(shortcut.intent)
        }
    }.onFailure { error ->
        Log.w(APP_BUBBLE_LOG_TAG, "Unable to inspect existing app-bubble shortcuts", error)
    }.getOrDefault(false)

    private fun targetIcon(target: LauncherAppTarget): Icon =
        appRepository.icon(target)
            ?.toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX)
            ?.let(Icon::createWithBitmap)
            ?: Icon.createWithResource(appContext, R.mipmap.ic_launcher)

    private fun shortcutId(target: LauncherAppTarget): String =
        "mink-app-bubble-${target.selectionKey.hashCode().toUInt().toString(16)}"

    private fun notificationTag(target: LauncherAppTarget): String =
        "mink-app-bubble:${target.selectionKey}"

    private fun bubblePendingIntentMutabilityFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

    private companion object {
        const val CHANNEL_ID = "mink_app_bubbles"
        const val NOTIFICATION_ID = 0x4D42
        const val ICON_SIZE_PX = 192
        const val MIN_EXPANDED_BUBBLE_HEIGHT_DP = 720
        const val SHORT_LABEL_LENGTH = 40
        const val LONG_LABEL_LENGTH = 80
        const val MINK_PERSON_KEY = "mink-app-bubbles"
    }
}

internal const val APP_BUBBLE_LOG_TAG = "MinkAppBubbles"
