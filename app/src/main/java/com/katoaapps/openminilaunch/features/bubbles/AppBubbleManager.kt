package com.katoaapps.openminilaunch.features.bubbles

import com.katoaapps.openminilaunch.model.LauncherAppTarget

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

internal enum class AppBubbleLaunchResult {
    PUBLISHED,
    UNSUPPORTED,
    NOTIFICATION_PERMISSION_REQUIRED,
    BUBBLES_DISABLED,
    WORK_PROFILE_UNSUPPORTED,
    APP_UNAVAILABLE,
    FAILED,
}

/**
 * Validates Android's bubble requirements and delegates publication of the requested app.
 * Android owns the collapsed bubble stack; no overlay or accessibility permission is involved.
 */
internal class AppBubbleManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val notificationManager = appContext.getSystemService(NotificationManager::class.java)
    private val publisher by lazy { AppBubbleNotificationPublisher(appContext) }

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
        return bubblesAllowedBySystem()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    // Android 11 exposes only areBubblesAllowed(); Android 12 added the non-deprecated
    // global and per-channel preference APIs used by the newer branch.
    @Suppress("DEPRECATION")
    private fun bubblesAllowedBySystem(): Boolean {
        publisher.ensureChannel()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!notificationManager.areBubblesEnabled()) return false
            when (notificationManager.bubblePreference) {
                NotificationManager.BUBBLE_PREFERENCE_ALL -> true
                NotificationManager.BUBBLE_PREFERENCE_SELECTED ->
                    publisher.channelCanBubble()
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

        return publishBubble(target)
    }

    fun dismiss(target: LauncherAppTarget) {
        if (isSupported()) publisher.dismiss(target)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun publishBubble(target: LauncherAppTarget): AppBubbleLaunchResult {
        if (!bubblesAllowedBySystem()) return AppBubbleLaunchResult.BUBBLES_DISABLED

        return runCatching {
            if (publisher.publish(target)) {
                AppBubbleLaunchResult.PUBLISHED
            } else {
                AppBubbleLaunchResult.FAILED
            }
        }.getOrElse { error ->
            Log.w(APP_BUBBLE_LOG_TAG, "Unable to publish requested app bubble", error)
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

    companion object {
        @Volatile private var instance: AppBubbleManager? = null

        fun get(context: Context): AppBubbleManager = instance ?: synchronized(this) {
            instance ?: AppBubbleManager(context).also { instance = it }
        }
    }
}
