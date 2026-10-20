package com.katoaapps.openminilaunch.features.minkpad.connection

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.katoaapps.openminilaunch.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class MinkPadNearbyDevice(
    val address: String,
    val name: String,
)

internal data class MinkPadNearbyDiscoveryState(
    val scanning: Boolean = false,
    val devices: List<MinkPadNearbyDevice> = emptyList(),
    val message: String? = null,
)

/** Owns the short-lived Classic discovery receiver used only by the pairing dialog. */
internal class MinkPadNearbyDiscovery(
    private val context: Context,
    private val adapter: () -> BluetoothAdapter?,
) {
    private val mutable = MutableStateFlow(MinkPadNearbyDiscoveryState())
    val state = mutable.asStateFlow()
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED ->
                    mutable.value = mutable.value.copy(scanning = true, message = null)
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED ->
                    mutable.value = mutable.value.copy(scanning = false)
                BluetoothDevice.ACTION_FOUND -> device(intent)?.let { device ->
                    add(device, intent.getStringExtra(BluetoothDevice.EXTRA_NAME))
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        stop(clear = true)
        register()
        val bluetooth = adapter()
        if (bluetooth == null || !bluetooth.isEnabled) {
            fail(context.getString(R.string.minkpad_scan_enable_bluetooth))
            return false
        }
        mutable.value = MinkPadNearbyDiscoveryState(scanning = true)
        return runCatching { bluetooth.startDiscovery() }.getOrDefault(false).also { started ->
            if (!started) fail(context.getString(R.string.minkpad_scan_start_failed))
        }
    }

    @SuppressLint("MissingPermission")
    fun stop(clear: Boolean) {
        runCatching { adapter()?.cancelDiscovery() }
        if (registered) {
            runCatching { context.unregisterReceiver(receiver) }
            registered = false
        }
        mutable.value = if (clear) MinkPadNearbyDiscoveryState()
        else mutable.value.copy(scanning = false)
    }

    fun fail(message: String) {
        mutable.value = mutable.value.copy(scanning = false, message = message)
    }

    private fun register() {
        if (registered) return
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND).apply {
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        registered = true
    }

    @SuppressLint("MissingPermission")
    private fun add(device: BluetoothDevice, discoveredName: String?) {
        if (device.bondState != BluetoothDevice.BOND_NONE) return
        val fallbackName = context.getString(R.string.minkpad_bluetooth_device)
        val result = MinkPadNearbyDevice(
            address = device.address,
            name = discoveredName?.trim().takeUnless { it.isNullOrEmpty() }
                ?: device.name?.trim().takeUnless { it.isNullOrEmpty() }
                ?: fallbackName,
        )
        val devices = (mutable.value.devices.filterNot { it.address == result.address } + result)
            .sortedWith(compareBy<MinkPadNearbyDevice> { it.name == fallbackName }.thenBy { it.name.lowercase() })
        mutable.value = mutable.value.copy(devices = devices)
    }

    @Suppress("DEPRECATION")
    private fun device(intent: Intent): BluetoothDevice? =
        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
}
