package com.katoaapps.minkboard.focus.android

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.katoaapps.minkboard.model.FocusSessionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

/** Process-independent timer presentation supplied by the reusable MinkBoard Android SDK. */
class MinkBoardFocusTimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var reconcileJob: Job? = null
    private var finishedEffectsActive = false

    private val runtime: AndroidMinkBoardFocusRuntime
        get() = (application as MinkBoardFocusRuntimeOwner).minkBoardFocusRuntime

    override fun onCreate() {
        super.onCreate()
        startInForeground(runtime.notifier.buildStarting())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (reconcileJob?.isActive != true) {
            reconcileJob = scope.launch { runTimerUntilStopped() }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runtime.notifier.stopFinishedEffects()
        finishedEffectsActive = false
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun runTimerUntilStopped() {
        try {
            while (scope.isActive) {
                val snapshot = runtime.store.currentFocusTimer()
                if (snapshot == null) {
                    stopForegroundAndSelf()
                    return
                }

                when (snapshot.session.state) {
                    FocusSessionState.Running -> {
                        if (finishedEffectsActive) {
                            runtime.notifier.stopFinishedEffects()
                            finishedEffectsActive = false
                        }
                        val now = System.currentTimeMillis()
                        val remaining = snapshot.session.endsAtEpochMillis - now
                        if (remaining <= 0L) {
                            runtime.coordinator.complete(snapshot.session.id)
                            continue
                        }
                        startInForeground(runtime.notifier.buildRunning(snapshot, now))
                        delay(min(remaining, NOTIFICATION_TICK_MILLIS))
                    }

                    FocusSessionState.Finished -> {
                        val alertRemaining = finishedFocusAlertRemainingMillis(
                            endsAtEpochMillis = snapshot.session.endsAtEpochMillis,
                            nowEpochMillis = System.currentTimeMillis(),
                        )
                        if (alertRemaining > 0L && !finishedEffectsActive) {
                            startInForeground(
                                runtime.notifier.buildFinished(snapshot, alerting = true),
                                runtime.notifier.notificationFinishedId(),
                            )
                            runtime.notifier.cancelRunningPresentation()
                            runtime.notifier.startFinishedEffects()
                            finishedEffectsActive = true
                        }
                        if (alertRemaining > 0L) {
                            delay(min(alertRemaining, ALARM_STATE_POLL_MILLIS))
                        } else {
                            if (finishedEffectsActive) {
                                runtime.notifier.stopFinishedEffects()
                                finishedEffectsActive = false
                            }
                            startInForeground(
                                runtime.notifier.buildFinished(snapshot, alerting = false),
                                runtime.notifier.notificationFinishedId(),
                            )
                            runtime.notifier.cancelRunningPresentation()
                            detachForegroundAndSelf()
                            return
                        }
                    }
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            stopForegroundAndSelf()
        }
    }

    private fun startInForeground(
        notification: Notification,
        notificationId: Int = runtime.notifier.notificationRunningId(),
    ) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, notificationId, notification, type)
    }

    private fun stopForegroundAndSelf() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun detachForegroundAndSelf() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    companion object {
        private const val NOTIFICATION_TICK_MILLIS = 1_000L
        private const val ALARM_STATE_POLL_MILLIS = 1_000L

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context.applicationContext,
                    Intent(context.applicationContext, MinkBoardFocusTimerService::class.java),
                )
            }
        }

        fun stop(context: Context) {
            context.applicationContext.stopService(
                Intent(context.applicationContext, MinkBoardFocusTimerService::class.java),
            )
        }
    }
}

internal const val FINISHED_FOCUS_ALERT_DURATION_MILLIS = 2 * 60 * 1_000L

internal fun finishedFocusAlertRemainingMillis(
    endsAtEpochMillis: Long,
    nowEpochMillis: Long,
): Long {
    val alertDeadline = if (
        endsAtEpochMillis > Long.MAX_VALUE - FINISHED_FOCUS_ALERT_DURATION_MILLIS
    ) {
        Long.MAX_VALUE
    } else {
        endsAtEpochMillis + FINISHED_FOCUS_ALERT_DURATION_MILLIS
    }
    return (alertDeadline - nowEpochMillis).coerceAtLeast(0L)
}
