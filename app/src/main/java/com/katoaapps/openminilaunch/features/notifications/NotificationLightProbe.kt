package com.katoaapps.openminilaunch.features.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.katoaapps.openminilaunch.R

/**
 * Debug-only diagnostic for learning whether an OEM exposes its indicator LED to Android's
 * notification-channel API. A successful post only proves that Android accepted the request;
 * the user still has to confirm the physical light.
 */
internal class NotificationLightProbe(context: Context) {
    private val appContext = context.applicationContext
    private val notificationManager = appContext.getSystemService(NotificationManager::class.java)

    fun post(color: NotificationLightProbeColor): NotificationLightProbeResult {
        cancel()
        val channel = ensureChannel(color)
        val runtimePermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        val notificationsAllowed = notificationManager.areNotificationsEnabled() &&
            runtimePermissionGranted
        val channelAllowed = channel.importance != NotificationManager.IMPORTANCE_NONE

        if (notificationsAllowed && channelAllowed) {
            val notification = NotificationCompat.Builder(appContext, color.channelId)
                .setSmallIcon(R.drawable.ic_app_bubble_notification)
                .setContentTitle(appContext.getString(R.string.notification_light_probe_notification_title, color.label))
                .setContentText(appContext.getString(R.string.notification_light_probe_notification_text, color.label.lowercase()))
                .setCategory(Notification.CATEGORY_STATUS)
                .setColor(color.argb)
                .setLights(color.argb, LIGHT_ON_MS, LIGHT_OFF_MS)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .build()
            notificationManager.notify(color.notificationId, notification)
        }

        val registeredChannel = notificationManager.getNotificationChannel(color.channelId)
        return NotificationLightProbeResult(
            color = color,
            notificationsAllowed = notificationsAllowed,
            channelAllowed = channelAllowed,
            lightsRequested = registeredChannel?.shouldShowLights() == true,
            configuredColor = registeredChannel?.lightColor,
            notificationPosted = notificationsAllowed && channelAllowed,
        )
    }

    fun cancel() {
        NotificationLightProbeColor.entries.forEach { color ->
            notificationManager.cancel(color.notificationId)
        }
    }

    fun channelSettingsIntent(color: NotificationLightProbeColor): Intent =
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, color.channelId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun ensureChannel(color: NotificationLightProbeColor): NotificationChannel {
        val channel = NotificationChannel(
            color.channelId,
            appContext.getString(R.string.notification_light_probe_channel_name, color.label),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = appContext.getString(R.string.notification_light_probe_channel_description)
            enableLights(true)
            lightColor = color.argb
            enableVibration(false)
            setSound(null, null)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
        return notificationManager.getNotificationChannel(color.channelId) ?: channel
    }

    private companion object {
        const val LIGHT_ON_MS = 500
        const val LIGHT_OFF_MS = 1_500
    }
}

internal enum class NotificationLightProbeColor(
    val label: String,
    val argb: Int,
    val channelId: String,
    val notificationId: Int,
) {
    RED("Red", Color.RED, "debug_led_probe_red_v1", 31_101),
    GREEN("Green", Color.GREEN, "debug_led_probe_green_v1", 31_102),
    BLUE("Blue", Color.BLUE, "debug_led_probe_blue_v1", 31_103),
}

internal data class NotificationLightProbeResult(
    val color: NotificationLightProbeColor,
    val notificationsAllowed: Boolean,
    val channelAllowed: Boolean,
    val lightsRequested: Boolean,
    val configuredColor: Int?,
    val notificationPosted: Boolean,
)
