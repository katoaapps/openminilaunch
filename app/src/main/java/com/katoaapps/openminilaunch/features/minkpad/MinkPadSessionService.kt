package com.katoaapps.openminilaunch.features.minkpad

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.katoaapps.openminilaunch.R

/** User-started, non-sticky native Bluetooth HID session. */
class MinkPadSessionService : Service() {
    private val repository by lazy { MinkPadRepository.get(this) }
    private var receiverRegistered = false
    private var sessionStarted = false
    private val updates = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) { notifyState() }
    }

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                getString(R.string.minkpad_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(),
                if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0)
        } catch (_: Exception) {
            repository.onServiceStartFailed()
            stopSelf()
            return
        }
        ContextCompat.registerReceiver(this, updates, IntentFilter(ACTION_UPDATE), ContextCompat.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
        sessionStarted = true
        repository.onServiceStarted()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> repository.stop()
            ACTION_RECONNECT -> repository.state.value.host?.let { repository.connect(it.address) }
        }
        return START_NOT_STICKY
    }

    private fun notification(): android.app.Notification {
        val state = repository.state.value
        val open = PendingIntent.getActivity(this, 0, MinkPadLauncherEntry.intent(this), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        fun command(action: String, id: Int) = PendingIntent.getService(this, id,
            Intent(this, MinkPadSessionService::class.java).setAction(action), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_minkpad_notification)
            .setContentTitle(getString(R.string.minkpad_name))
            .setContentText(state.host?.name?.let {
                getString(R.string.minkpad_notification_with_host, statusText(state.status), it)
            } ?: getString(R.string.minkpad_notification_without_host, statusText(state.status)))
            .setContentIntent(open).setOngoing(true).setSilent(true)
            .addAction(0, getString(R.string.minkpad_notification_open), open)
            .apply {
                if (state.status == MinkPadStatus.DISCONNECTED && state.host != null) {
                    addAction(0, getString(R.string.minkpad_reconnect), command(ACTION_RECONNECT, 1))
                }
            }
            .addAction(0, getString(R.string.minkpad_notification_stop), command(ACTION_STOP, 2)).build()
    }

    private fun statusText(status: MinkPadStatus): String = getString(when (status) {
        MinkPadStatus.IDLE -> R.string.minkpad_notification_status_idle
        MinkPadStatus.REGISTERING -> R.string.minkpad_notification_status_registering
        MinkPadStatus.READY -> R.string.minkpad_notification_status_ready
        MinkPadStatus.WAITING_FOR_HOST -> R.string.minkpad_notification_status_waiting
        MinkPadStatus.CONNECTING -> R.string.minkpad_notification_status_connecting
        MinkPadStatus.CONNECTED -> R.string.minkpad_notification_status_connected
        MinkPadStatus.DISCONNECTING -> R.string.minkpad_notification_status_disconnecting
        MinkPadStatus.DISCONNECTED -> R.string.minkpad_notification_status_disconnected
        MinkPadStatus.STOPPING -> R.string.minkpad_notification_status_stopping
        MinkPadStatus.UNSUPPORTED -> R.string.minkpad_notification_status_unsupported
        MinkPadStatus.ERROR -> R.string.minkpad_notification_status_error
    })

    @android.annotation.SuppressLint("MissingPermission")
    private fun notifyState() { getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification()) }

    override fun onTaskRemoved(rootIntent: Intent?) {
        repository.stop()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            unregisterReceiver(updates)
            receiverRegistered = false
        }
        if (sessionStarted) repository.onServiceDestroyed()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object {
        private const val CHANNEL = "minkpad-session"
        private const val NOTIFICATION_ID = 7104
        const val ACTION_STOP = "com.katoaapps.openminilaunch.minkpad.STOP"
        const val ACTION_RECONNECT = "com.katoaapps.openminilaunch.minkpad.RECONNECT"
        const val ACTION_UPDATE = "com.katoaapps.openminilaunch.minkpad.UPDATE"
    }
}
