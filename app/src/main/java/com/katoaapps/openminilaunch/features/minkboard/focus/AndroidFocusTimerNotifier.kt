package com.katoaapps.openminilaunch.features.minkboard.focus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.katoaapps.minkboard.model.FocusTimerSnapshot
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.launcher.MainActivity

internal data class FocusNotificationText(
    val runningChannelName: String,
    val runningChannelDescription: String,
    val finishedChannelName: String,
    val finishedChannelDescription: String,
    val genericRunningTitle: String,
    val genericFinishedTitle: String,
    val focusingTitle: (String) -> String,
    val finishedTitle: (String) -> String,
    val runningBody: String,
    val finishedBody: String,
    val stopAction: String,
    val dismissAction: String,
)

internal class AndroidFocusTimerNotifier(
    context: Context,
    private val text: FocusNotificationText,
) : FocusTimerNotifier {
    private val applicationContext = context.applicationContext
    private val notificationManager = NotificationManagerCompat.from(applicationContext)

    init {
        createChannels()
    }

    override fun showRunning(snapshot: FocusTimerSnapshot) {
        FocusTimerService.start(applicationContext)
    }

    fun buildRunning(snapshot: FocusTimerSnapshot, nowMillis: Long): Notification {
        val session = snapshot.session
        val title = if (snapshot.boardIsLocked) {
            text.genericRunningTitle
        } else {
            text.focusingTitle(snapshot.cardTitle)
        }
        return NotificationCompat.Builder(applicationContext, CHANNEL_RUNNING)
            .setSmallIcon(R.drawable.ic_minkboard_notification)
            .setContentTitle(title)
            .setContentText(text.runningBody)
            .setContentIntent(openPendingIntent(snapshot))
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification(text.genericRunningTitle, text.runningBody))
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setOngoing(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(session.endsAtEpochMillis)
            .setProgress(
                durationSeconds(session.startedAtEpochMillis, session.endsAtEpochMillis),
                elapsedSeconds(
                    session.startedAtEpochMillis,
                    session.endsAtEpochMillis,
                    nowMillis,
                ),
                false,
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                text.stopAction,
                actionPendingIntent(snapshot, FocusTimerIntents.ACTION_STOP, REQUEST_STOP),
            )
            .build()
    }

    fun buildStarting(): Notification = NotificationCompat.Builder(
        applicationContext,
        CHANNEL_RUNNING,
    )
        .setSmallIcon(R.drawable.ic_minkboard_notification)
        .setContentTitle(text.genericRunningTitle)
        .setContentText(text.runningBody)
        .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setOngoing(true)
        .setProgress(0, 0, true)
        .build()

    override fun showFinished(snapshot: FocusTimerSnapshot, alert: Boolean) {
        FocusTimerService.start(applicationContext)
    }

    fun buildFinished(snapshot: FocusTimerSnapshot): Notification {
        val title = if (snapshot.boardIsLocked) {
            text.genericFinishedTitle
        } else {
            text.finishedTitle(snapshot.cardTitle)
        }
        return NotificationCompat.Builder(applicationContext, CHANNEL_FINISHED)
            .setSmallIcon(R.drawable.ic_minkboard_notification)
            .setContentTitle(title)
            .setContentText(text.finishedBody)
            .setContentIntent(openPendingIntent(snapshot))
            .setDeleteIntent(
                actionPendingIntent(snapshot, FocusTimerIntents.ACTION_DISMISS, REQUEST_DISMISS),
            )
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification(text.genericFinishedTitle, text.finishedBody))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                text.dismissAction,
                actionPendingIntent(snapshot, FocusTimerIntents.ACTION_DISMISS, REQUEST_DISMISS),
            )
            .build()
            .apply { flags = flags or Notification.FLAG_INSISTENT }
    }

    override fun cancelAll() {
        stopFinishedEffects()
        FocusTimerService.stop(applicationContext)
        notificationManager.cancel(NOTIFICATION_RUNNING)
        notificationManager.cancel(NOTIFICATION_FINISHED)
    }

    private fun openPendingIntent(snapshot: FocusTimerSnapshot): PendingIntent =
        PendingIntent.getActivity(
            applicationContext,
            REQUEST_OPEN,
            Intent(applicationContext, MainActivity::class.java).apply {
                action = FocusTimerIntents.ACTION_OPEN
                data = FocusTimerIntents.data(FocusTimerIntents.ACTION_OPEN, snapshot.session.id)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(FocusTimerIntents.EXTRA_TIMER_ID, snapshot.session.id.value)
                putExtra(FocusTimerIntents.EXTRA_BOARD_ID, snapshot.session.boardId.value)
                putExtra(FocusTimerIntents.EXTRA_CARD_ID, snapshot.session.cardId.value)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun actionPendingIntent(
        snapshot: FocusTimerSnapshot,
        timerAction: String,
        requestCode: Int,
    ): PendingIntent = PendingIntent.getBroadcast(
        applicationContext,
        requestCode,
        Intent(applicationContext, FocusTimerActionReceiver::class.java).apply {
            action = timerAction
            data = FocusTimerIntents.data(timerAction, snapshot.session.id)
            putExtra(FocusTimerIntents.EXTRA_TIMER_ID, snapshot.session.id.value)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun publicNotification(title: String, body: String): Notification =
        NotificationCompat.Builder(applicationContext, CHANNEL_RUNNING)
            .setSmallIcon(R.drawable.ic_minkboard_notification)
            .setContentTitle(title)
            .setContentText(body)
            .build()

    private fun createChannels() {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        val running = NotificationChannel(
            CHANNEL_RUNNING,
            text.runningChannelName,
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = text.runningChannelDescription }
        val finished = NotificationChannel(
            CHANNEL_FINISHED,
            text.finishedChannelName,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = text.finishedChannelDescription
            setSound(
                Settings.System.DEFAULT_ALARM_ALERT_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(false)
        }
        manager.createNotificationChannels(listOf(running, finished))
    }

    fun startFinishedEffects() {
        wakeSleepingDisplay()
        vibrateAsAlarm()
    }

    fun stopFinishedEffects() {
        runCatching { systemVibrator().cancel() }
    }

    @Suppress("DEPRECATION")
    private fun wakeSleepingDisplay() {
        val powerManager = applicationContext.getSystemService(PowerManager::class.java)
        if (powerManager.isInteractive) return
        runCatching {
            powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "${applicationContext.packageName}:minkboard-focus-finished",
            ).acquire(SCREEN_WAKE_DURATION_MILLIS)
        }
    }

    private fun vibrateAsAlarm() {
        runCatching {
            val vibrator = systemVibrator()
            if (!vibrator.hasVibrator()) return
            val effect = VibrationEffect.createWaveform(SPENCER_VIBRATION_PATTERN, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(
                    effect,
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(
                    effect,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            }
        }
    }

    private fun systemVibrator(): Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        applicationContext.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        applicationContext.getSystemService(Vibrator::class.java)
    }

    companion object {
        const val NOTIFICATION_RUNNING = 43_001
        private const val CHANNEL_RUNNING = "openmink_minkboard_focus_running_v1"
        private const val CHANNEL_FINISHED = "openmink_minkboard_focus_finished_v1"
        private const val NOTIFICATION_FINISHED = 43_002
        private const val REQUEST_OPEN = 43_101
        private const val REQUEST_STOP = 43_102
        private const val REQUEST_DISMISS = 43_103
        private const val SCREEN_WAKE_DURATION_MILLIS = 5_000L
        private val SPENCER_VIBRATION_PATTERN = longArrayOf(0L, 1_000L, 500L, 1_000L)
    }
}

private fun durationSeconds(startedAt: Long, endsAt: Long): Int =
    ((endsAt - startedAt).coerceAtLeast(1L) / 1_000L)
        .coerceIn(1L, Int.MAX_VALUE.toLong())
        .toInt()

private fun elapsedSeconds(startedAt: Long, endsAt: Long, now: Long): Int =
    ((now.coerceIn(startedAt, endsAt) - startedAt) / 1_000L)
        .coerceIn(0L, durationSeconds(startedAt, endsAt).toLong())
        .toInt()
