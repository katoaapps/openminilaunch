package com.katoaapps.openminilaunch.features.minkboard.focus

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.katoaapps.minkboard.model.FocusSessionState
import com.katoaapps.openminilaunch.OpenMinkApplication
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

/** Keeps a user-started timer alive independently of launcher activity and Recents. */
internal class FocusTimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var reconcileJob: Job? = null
    private var finishedEffectsActive = false

    private val runtime
        get() = (application as OpenMinkApplication).minkBoardRuntime

    override fun onCreate() {
        super.onCreate()
        startInForeground(runtime.focusTimerNotifier.buildStarting())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (reconcileJob?.isActive != true) {
            reconcileJob = scope.launch { runTimerUntilStopped() }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runtime.focusTimerNotifier.stopFinishedEffects()
        finishedEffectsActive = false
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun runTimerUntilStopped() {
        try {
            while (scope.isActive) {
                val snapshot = runtime.repository.currentFocusTimer()
                if (snapshot == null) {
                    stopForegroundAndSelf()
                    return
                }
                when (snapshot.session.state) {
                    FocusSessionState.Running -> {
                        if (finishedEffectsActive) {
                            runtime.focusTimerNotifier.stopFinishedEffects()
                            finishedEffectsActive = false
                        }
                        val now = System.currentTimeMillis()
                        val remaining = snapshot.session.endsAtEpochMillis - now
                        if (remaining <= 0L) {
                            runtime.focusTimerCoordinator.complete(snapshot.session.id)
                            continue
                        }
                        startInForeground(runtime.focusTimerNotifier.buildRunning(snapshot, now))
                        delay(min(remaining, NOTIFICATION_TICK_MILLIS))
                    }

                    FocusSessionState.Finished -> {
                        if (!finishedEffectsActive) {
                            startInForeground(runtime.focusTimerNotifier.buildFinished(snapshot))
                            runtime.focusTimerNotifier.startFinishedEffects()
                            finishedEffectsActive = true
                        }
                        delay(ALARM_STATE_POLL_MILLIS)
                    }
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // AlarmManager and Room remain independent recovery points.
            stopForegroundAndSelf()
        }
    }

    private fun startInForeground(notification: android.app.Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            AndroidFocusTimerNotifier.NOTIFICATION_RUNNING,
            notification,
            type,
        )
    }

    private fun stopForegroundAndSelf() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val NOTIFICATION_TICK_MILLIS = 1_000L
        private const val ALARM_STATE_POLL_MILLIS = 1_000L

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context.applicationContext,
                    Intent(context.applicationContext, FocusTimerService::class.java),
                )
            }
        }

        fun stop(context: Context) {
            context.applicationContext.stopService(
                Intent(context.applicationContext, FocusTimerService::class.java),
            )
        }
    }
}
