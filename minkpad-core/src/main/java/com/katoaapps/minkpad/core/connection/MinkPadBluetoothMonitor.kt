package com.katoaapps.minkpad.core.connection

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat

/** Observes only adapter availability and Android-owned bond records. */
class MinkPadBluetoothMonitor(
    private val context: Context,
    private val available: () -> Boolean,
    private val invalidate: () -> Unit,
    private val onBond: (BluetoothDevice) -> Unit,
    private val onKeyMissing: (BluetoothDevice) -> Unit,
    private val onEncryptionRestored: (BluetoothDevice) -> Unit,
) {
    private var registered = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!available()) {
                invalidate()
                return
            }
            when (intent.action) {
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> device(intent)?.let(onBond)
                ACTION_KEY_MISSING -> device(intent)?.let(onKeyMissing)
                ACTION_ENCRYPTION_CHANGE -> {
                    if (intent.getBooleanExtra(EXTRA_ENCRYPTION_ENABLED, false)) {
                        device(intent)?.let(onEncryptionRestored)
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun device(intent: Intent): BluetoothDevice? =
        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

    fun start() {
        if (registered) return
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED).apply {
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            if (Build.VERSION.SDK_INT >= 36) {
                addAction(ACTION_KEY_MISSING)
                addAction(ACTION_ENCRYPTION_CHANGE)
            }
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        registered = true
        if (!available()) invalidate()
    }

    fun stop() {
        if (!registered) return
        context.unregisterReceiver(receiver)
        registered = false
    }

    private companion object {
        // Public Android 16 actions kept as literals so the shared core can retain
        // OpenMink's API 35 compile baseline while still handling them at runtime.
        const val ACTION_KEY_MISSING = "android.bluetooth.device.action.KEY_MISSING"
        const val ACTION_ENCRYPTION_CHANGE = "android.bluetooth.device.action.ENCRYPTION_CHANGE"
        const val EXTRA_ENCRYPTION_ENABLED = "android.bluetooth.device.extra.ENCRYPTION_ENABLED"
    }
}
