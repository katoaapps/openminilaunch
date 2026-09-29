package com.katoaapps.openminilaunch.features.bubbles

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.apps.LauncherAppRepository
import com.katoaapps.openminilaunch.model.LauncherAppTarget

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap

internal enum class AppBubbleLaunchResult {
    OPENED,
    UNSUPPORTED,
    NOTIFICATION_PERMISSION_REQUIRED,
    BUBBLES_DISABLED,
    WORK_PROFILE_UNSUPPORTED,
    APP_UNAVAILABLE,
    FAILED,
}

/**
 * Publishes an app as a real Android notification bubble.
 *
 * Android owns the collapsed bubble stack. The bubble opens [AppBubbleHostActivity], which starts
 * the selected launcher activity from inside the bubble task so compatible apps inherit that
 * window. No overlay or accessibility permission is involved.
 */
internal class AppBubbleManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val notificationManager = appContext.getSystemService(NotificationManager::class.java)
    private val shortcutManager = appContext.getSystemService(ShortcutManager::class.java)
    private val appRepository = LauncherAppRepository.get(appContext)

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun notificationsGranted(): Boolean {
        if (!notificationManager.areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun bubblesAllowed(): Boolean {
        if (!isSupported() || !notificationsGranted()) return false
        return bubblesAllowedOnAndroid11()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @Suppress("DEPRECATION")
    private fun bubblesAllowedOnAndroid11(): Boolean {
        ensureChannel()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!notificationManager.areBubblesEnabled()) return false
            when (notificationManager.bubblePreference) {
                NotificationManager.BUBBLE_PREFERENCE_ALL -> true
                NotificationManager.BUBBLE_PREFERENCE_SELECTED ->
                    notificationManager.getNotificationChannel(CHANNEL_ID)?.canBubble() == true
                else -> false
            }
        } else {
            notificationManager.areBubblesAllowed()
        }
    }

    fun launch(target: LauncherAppTarget): AppBubbleLaunchResult {
        if (!isSupported()) return AppBubbleLaunchResult.UNSUPPORTED
        if (!target.isAvailable) return AppBubbleLaunchResult.APP_UNAVAILABLE
        if (target.isWorkProfile) return AppBubbleLaunchResult.WORK_PROFILE_UNSUPPORTED
        if (!notificationsGranted()) return AppBubbleLaunchResult.NOTIFICATION_PERMISSION_REQUIRED

        return launchOnAndroid11(target)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun launchOnAndroid11(target: LauncherAppTarget): AppBubbleLaunchResult {
        ensureChannel()
        if (!bubblesAllowedOnAndroid11()) return AppBubbleLaunchResult.BUBBLES_DISABLED

        return runCatching {
            val shortcutId = shortcutId(target)
            val bubbleIcon = targetIcon(target)
            val hostIntent = AppBubbleHostActivity.intentFor(appContext, target)
            val targetIntent = Intent.makeMainActivity(target.componentName)
            val bubbleIntent = PendingIntent.getActivity(
                appContext,
                target.selectionKey.hashCode(),
                targetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or bubblePendingIntentMutabilityFlag(),
            )
            if (!publishShortcut(target, shortcutId, bubbleIcon, hostIntent)) {
                return@runCatching AppBubbleLaunchResult.FAILED
            }

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
            val notification = Notification.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_bubble_notification)
                .setContentTitle(target.label)
                .setContentText(appContext.getString(R.string.app_bubble_notification_text, target.label))
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setShortcutId(shortcutId)
                .setOnlyAlertOnce(true)
                .setContentIntent(bubbleIntent)
                .setBubbleMetadata(bubble)
                .setStyle(
                    Notification.MessagingStyle(minkPerson)
                        .setConversationTitle(target.label)
                        .addMessage(
                            appContext.getString(R.string.app_bubble_notification_text, target.label),
                            System.currentTimeMillis(),
                            appPerson,
                        ),
                )
                .build()

            notificationManager.notify(notificationTag(target), NOTIFICATION_ID, notification)
            AppBubbleLaunchResult.OPENED
        }.getOrElse { error ->
            Log.w(TAG, "Unable to publish requested app bubble", error)
            AppBubbleLaunchResult.FAILED
        }
    }

    fun notificationSettingsIntent(): Intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
    }

    fun bubbleSettingsIntent(): Intent {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return notificationSettingsIntent()
        val bubbleSettings = Intent(Settings.ACTION_APP_NOTIFICATION_BUBBLE_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
        }
        return bubbleSettings.takeIf { it.resolveActivity(appContext.packageManager) != null }
            ?: notificationSettingsIntent()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun ensureChannel() {
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
    private fun publishShortcut(
        target: LauncherAppTarget,
        shortcutId: String,
        icon: Icon,
        hostIntent: Intent,
    ): Boolean {
        val existingShortcutIsUsable = runCatching {
            shortcutManager.dynamicShortcuts.any { shortcut ->
                shortcut.id == shortcutId && shortcut.isEnabled
            }
        }.onFailure { error ->
            Log.w(TAG, "Unable to inspect existing app-bubble shortcuts", error)
        }.getOrDefault(false)
        if (existingShortcutIsUsable) return true

        val shortcut = ShortcutInfo.Builder(appContext, shortcutId)
            .setShortLabel(target.label.take(40))
            .setLongLabel(target.label.take(80))
            .setIcon(icon)
            .setIntent(hostIntent)
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
            Log.w(TAG, "Unable to push app-bubble shortcut; trying addDynamicShortcuts", error)
        }
            .getOrDefault(false)
        return pushed || runCatching { shortcutManager.addDynamicShortcuts(listOf(shortcut)) }
            .onFailure { error -> Log.w(TAG, "Unable to add app-bubble shortcut", error) }
            .getOrDefault(false)
    }

    private fun targetIcon(target: LauncherAppTarget): Icon {
        val drawable = appRepository.icon(target)
        return drawable?.toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX)
            ?.let(Icon::createWithBitmap)
            ?: Icon.createWithResource(appContext, R.mipmap.ic_launcher)
    }

    private fun shortcutId(target: LauncherAppTarget): String =
        "mink-app-bubble-${target.selectionKey.hashCode().toUInt().toString(16)}"

    private fun notificationTag(target: LauncherAppTarget): String =
        "mink-app-bubble:${target.selectionKey}"

    private fun bubblePendingIntentMutabilityFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

    companion object {
        private const val CHANNEL_ID = "mink_app_bubbles"
        private const val NOTIFICATION_ID = 0x4D42
        private const val ICON_SIZE_PX = 192
        private const val MIN_EXPANDED_BUBBLE_HEIGHT_DP = 720
        private const val MINK_PERSON_KEY = "mink-app-bubbles"
        private const val TAG = "MinkAppBubbles"

        @Volatile private var instance: AppBubbleManager? = null

        fun get(context: Context): AppBubbleManager = instance ?: synchronized(this) {
            instance ?: AppBubbleManager(context).also { instance = it }
        }
    }
}
