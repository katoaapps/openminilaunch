package com.katoaapps.openminilaunch.features.bubbles

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.LauncherAppTarget

/**
 * Confirms that Android honored a posted bubble request without inspecting or manipulating tasks.
 *
 * Some devices accept the notification but decline to expose it as a bubble. Android marks an
 * honored request with [Notification.FLAG_BUBBLE]. A few short retries avoid treating ordinary
 * System UI publication latency as a failure.
 */
internal class AppBubblePublicationVerifier(context: Context) {
    private val appContext = context.applicationContext
    private val notificationManager = appContext.getSystemService(NotificationManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingChecks = mutableMapOf<String, Runnable>()

    fun verify(
        target: LauncherAppTarget,
        notificationTag: String,
        notificationId: Int,
        onRejected: () -> Unit,
    ) {
        cancelPendingCheck(target.selectionKey)

        var attempt = 0
        val check = object : Runnable {
            override fun run() {
                attempt += 1
                if (isActiveBubble(notificationTag, notificationId)) {
                    pendingChecks.remove(target.selectionKey)
                    Log.d(APP_BUBBLE_LOG_TAG, "Android honored bubble request for ${target.packageName}")
                    return
                }
                if (attempt < VERIFICATION_DELAYS_MS.size) {
                    mainHandler.postDelayed(this, VERIFICATION_DELAYS_MS[attempt])
                    return
                }

                pendingChecks.remove(target.selectionKey)
                Log.w(
                    APP_BUBBLE_LOG_TAG,
                    "Android did not honor bubble request for ${target.packageName}; " +
                        "leaving the launched app and other bubbles untouched",
                )
                onRejected()
                Toast.makeText(
                    appContext,
                    R.string.app_bubble_launch_failed,
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
        pendingChecks[target.selectionKey] = check
        mainHandler.postDelayed(check, VERIFICATION_DELAYS_MS.first())
    }

    fun cancel(target: LauncherAppTarget) {
        cancelPendingCheck(target.selectionKey)
    }

    private fun cancelPendingCheck(selectionKey: String) {
        pendingChecks.remove(selectionKey)?.let(mainHandler::removeCallbacks)
    }

    private fun isActiveBubble(notificationTag: String, notificationId: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            notificationManager.activeNotifications.any { statusBarNotification ->
                statusBarNotification.tag == notificationTag &&
                    statusBarNotification.id == notificationId &&
                    statusBarNotification.notification.flags and Notification.FLAG_BUBBLE != 0
            }
        }.onFailure { error ->
            Log.w(APP_BUBBLE_LOG_TAG, "Unable to verify posted bubble notification", error)
        }.getOrDefault(false)
    }

    private companion object {
        val VERIFICATION_DELAYS_MS = longArrayOf(350L, 700L, 1_400L)
    }
}
