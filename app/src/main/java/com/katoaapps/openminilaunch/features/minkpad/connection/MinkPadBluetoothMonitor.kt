package com.katoaapps.openminilaunch.features.minkpad.connection

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

/** Observes only adapter availability and Android-owned bond records. */
internal class MinkPadBluetoothMonitor(
    private val context: Context,
    private val available: () -> Boolean,
    private val invalidate: () -> Unit,
    private val onBond: (BluetoothDevice) -> Unit,
) {
    private var registered = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!available()) {
                invalidate()
                return
            }
            if (intent.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) device(intent)?.let(onBond)
        }
    }

    @Suppress("DEPRECATION")
    private fun device(intent: Intent): BluetoothDevice? =
        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

    fun start() {
        if (registered) return
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED).apply {
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
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
}
