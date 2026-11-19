package com.katoaapps.minkboard.focus.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.katoaapps.minkboard.model.FocusTimerSnapshot

internal class AndroidMinkBoardFocusNotifier(
    context: Context,
    private val config: MinkBoardFocusAndroidConfig,
) : MinkBoardFocusNotifier {
    private val applicationContext = context.applicationContext
    private val notificationManager = NotificationManagerCompat.from(applicationContext)
    private val finishedEffects = FinishedMinkBoardFocusEffects(applicationContext)

    init {
        createChannels()
    }

    override fun showRunning(snapshot: FocusTimerSnapshot) {
        MinkBoardFocusTimerService.start(applicationContext)
    }

    fun buildRunning(snapshot: FocusTimerSnapshot, nowMillis: Long): Notification {
        val session = snapshot.session
        val title = if (snapshot.boardIsLocked) {
            config.text.genericRunningTitle
        } else {
            config.text.focusingTitle(snapshot.cardTitle)
        }
        val body = runningBody(snapshot)
        return NotificationCompat.Builder(applicationContext, config.channels.running)
            .setSmallIcon(config.smallIconResId)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openPendingIntent(snapshot))
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                publicNotification(config.text.genericRunningTitle, config.text.runningBody),
            )
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
                config.text.stopAction,
                actionPendingIntent(
                    snapshot,
                    MinkBoardFocusIntents.ACTION_STOP,
                    config.ids.stopRequest,
                ),
            )
            .build()
    }

    fun buildStarting(): Notification = NotificationCompat.Builder(
        applicationContext,
        config.channels.running,
    )
        .setSmallIcon(config.smallIconResId)
        .setContentTitle(config.text.genericRunningTitle)
        .setContentText(config.text.runningBody)
        .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setOngoing(true)
        .setProgress(0, 0, true)
        .build()

    override fun showFinished(snapshot: FocusTimerSnapshot, alert: Boolean) {
        MinkBoardFocusTimerService.start(applicationContext)
    }

    fun buildFinished(snapshot: FocusTimerSnapshot, alerting: Boolean): Notification {
        val title = if (snapshot.boardIsLocked) {
            config.text.genericFinishedTitle
        } else {
            config.text.finishedTitle(snapshot.cardTitle)
        }
        val channelId = if (alerting) {
            config.channels.finishedAlerting
        } else {
            config.channels.finishedQuiet
        }
        return NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(config.smallIconResId)
            .setContentTitle(title)
            .setContentText(config.text.finishedBody)
            .setContentIntent(openPendingIntent(snapshot))
            .setDeleteIntent(
                actionPendingIntent(
                    snapshot,
                    MinkBoardFocusIntents.ACTION_DISMISS,
                    config.ids.dismissRequest,
                ),
            )
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                publicNotification(config.text.genericFinishedTitle, config.text.finishedBody),
            )
            .setPriority(
                if (alerting) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_LOW,
            )
            .setAutoCancel(false)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                config.text.dismissAction,
                actionPendingIntent(
                    snapshot,
                    MinkBoardFocusIntents.ACTION_DISMISS,
                    config.ids.dismissRequest,
                ),
            )
            .build()
    }

    override fun cancelAll() {
        stopFinishedEffects()
        MinkBoardFocusTimerService.stop(applicationContext)
        notificationManager.cancel(config.ids.running)
        notificationManager.cancel(config.ids.finished)
    }

    fun cancelRunningPresentation() {
        notificationManager.cancel(config.ids.running)
    }

    fun startFinishedEffects() {
        finishedEffects.start()
    }

    fun stopFinishedEffects() {
        finishedEffects.stop()
    }

    fun notificationRunningId(): Int = config.ids.running

    fun notificationFinishedId(): Int = config.ids.finished

    private fun runningBody(snapshot: FocusTimerSnapshot): String {
        val details = snapshot.cardDetails.trim()
        if (details.isEmpty()) return config.text.runningBody
        if (details.length <= MAX_NOTIFICATION_DETAILS_LENGTH) return details
        return details.take(MAX_NOTIFICATION_DETAILS_LENGTH - 1).trimEnd() + "…"
    }

    private fun openPendingIntent(snapshot: FocusTimerSnapshot): PendingIntent =
        PendingIntent.getActivity(
            applicationContext,
            config.ids.openRequest,
            MinkBoardFocusIntents.decorateOpenIntent(
                config.createOpenIntent(applicationContext),
                snapshot,
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun actionPendingIntent(
        snapshot: FocusTimerSnapshot,
        timerAction: String,
        requestCode: Int,
    ): PendingIntent = PendingIntent.getBroadcast(
        applicationContext,
        requestCode,
        Intent(applicationContext, MinkBoardFocusActionReceiver::class.java).apply {
            action = timerAction
            data = MinkBoardFocusIntents.data(timerAction, snapshot.session.id)
            putExtra(MinkBoardFocusIntents.EXTRA_TIMER_ID, snapshot.session.id.value)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun publicNotification(title: String, body: String): Notification =
        NotificationCompat.Builder(applicationContext, config.channels.running)
            .setSmallIcon(config.smallIconResId)
            .setContentTitle(title)
            .setContentText(body)
            .build()

    private fun createChannels() {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        val running = NotificationChannel(
            config.channels.running,
            config.text.runningChannelName,
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = config.text.runningChannelDescription }
        val finishedAlerting = NotificationChannel(
            config.channels.finishedAlerting,
            config.text.finishedChannelName,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = config.text.finishedChannelDescription
            setSound(null, null)
            enableVibration(false)
        }
        val finishedQuiet = NotificationChannel(
            config.channels.finishedQuiet,
            config.text.quietFinishedChannelName,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = config.text.quietFinishedChannelDescription
            setSound(null, null)
            enableVibration(false)
        }
        config.channels.retired.forEach(manager::deleteNotificationChannel)
        manager.createNotificationChannels(listOf(running, finishedAlerting, finishedQuiet))
    }

    private companion object {
        const val MAX_NOTIFICATION_DETAILS_LENGTH = 1_000
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
