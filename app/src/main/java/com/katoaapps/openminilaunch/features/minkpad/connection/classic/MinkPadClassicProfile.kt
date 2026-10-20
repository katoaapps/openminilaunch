package com.katoaapps.openminilaunch.features.minkpad.connection.classic

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkpad.hid.MinkPadReportId
import com.katoaapps.openminilaunch.features.minkpad.hid.MinkPadReportSender
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Android's native BR/EDR HID Device profile. All state callbacks are returned on the main thread. */
@SuppressLint("MissingPermission")
@RequiresApi(28)
internal class MinkPadClassicProfile(
    context: Context,
    private val initialHost: BluetoothDevice?,
    private val shouldReconnect: (BluetoothDevice) -> Boolean,
    private val onRegistered: (Boolean) -> Unit,
    private val onConnection: (BluetoothDevice, Int) -> Unit,
    private val onFailure: (String) -> Unit,
    private val onCapsLock: (Boolean) -> Unit,
    private val onInputAvailability: () -> Unit,
) {
    private val appContext = context.applicationContext
    private val adapter = appContext.getSystemService(BluetoothManager::class.java)?.adapter
    private val main = Handler(Looper.getMainLooper())
    private val callbackExecutor: ExecutorService = Executors.newSingleThreadExecutor { task ->
        Thread(task, "MinkPadClassicCallbacks")
    }
    val sender = MinkPadReportSender(::sendReport)

    @Volatile private var hid: BluetoothHidDevice? = null
    @Volatile private var connectedHost: BluetoothDevice? = null
    @Volatile private var appRegistered = false
    @Volatile private var closing = false

    val keyboardReady get() = appRegistered && connectedHost != null
    val mouseReady get() = appRegistered && connectedHost != null
    val consumerReady get() = appRegistered && connectedHost != null
    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE || closing) return
            val device = proxy as? BluetoothHidDevice ?: run {
                fail(appContext.getString(R.string.minkpad_error_incompatible_hid_profile))
                return
            }
            hid = device
            val accepted = runCatching {
                device.registerApp(SDP, null, OUTGOING_QOS, callbackExecutor, callback)
            }.getOrDefault(false)
            if (!accepted) fail(appContext.getString(R.string.minkpad_error_hid_registration_rejected))
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hid = null
            connectedHost = null
            appRegistered = false
            if (!closing) fail(appContext.getString(R.string.minkpad_error_hid_service_ended))
        }
    }

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            appRegistered = registered
            main.post {
                if (closing) return@post
                onRegistered(registered)
                onInputAvailability()
                if (!registered) return@post
                reconnect(listOfNotNull(pluggedDevice, initialHost).firstOrNull(shouldReconnect))
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            if (state == BluetoothProfile.STATE_CONNECTED) connectedHost = device
            else if (connectedHost?.address == device.address) connectedHost = null
            main.post {
                if (!closing) {
                    onConnection(device, state)
                    onInputAvailability()
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            val report = when (id.toInt() and 0xff) {
                MinkPadReportId.KEYBOARD -> sender.keyboardReport
                MinkPadReportId.POINTER -> sender.pointerReport
                MinkPadReportId.CONSUMER -> ByteArray(2)
                else -> null
            }
            val service = hid ?: return
            if (type != BluetoothHidDevice.REPORT_TYPE_INPUT || report == null) {
                service.reportError(device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID)
                return
            }
            val reply = if (bufferSize > 0 && report.size > bufferSize) report.copyOf(bufferSize) else report
            service.replyReport(device, type, id, reply)
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            if (type == BluetoothHidDevice.REPORT_TYPE_OUTPUT && (id.toInt() and 0xff) == MinkPadReportId.KEYBOARD) {
                acceptKeyboardLeds(data)
            } else hid?.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
        }

        override fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray) {
            if ((reportId.toInt() and 0xff) == MinkPadReportId.KEYBOARD) acceptKeyboardLeds(data)
        }

        override fun onSetProtocol(device: BluetoothDevice, protocol: Byte) {
            // The same report descriptor supports both boot-capable hosts and report protocol.
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice) {
            connectedHost = null
            main.post {
                if (!closing) {
                    onConnection(device, BluetoothProfile.STATE_DISCONNECTED)
                    onInputAvailability()
                }
            }
        }
    }

    fun start(): Boolean = adapter?.getProfileProxy(appContext, serviceListener, BluetoothProfile.HID_DEVICE) == true

    fun connect(device: BluetoothDevice): Boolean {
        if (!appRegistered) return false
        val service = hid ?: return false
        val accepted = runCatching { service.connect(device) }.getOrDefault(false)
        return accepted
    }

    fun disconnect(): Boolean {
        val device = connectedHost ?: return false
        return disconnect(device)
    }

    /** Also cancels a connection that Android has accepted but has not completed yet. */
    fun disconnect(device: BluetoothDevice): Boolean =
        runCatching { hid?.disconnect(device) == true }.getOrDefault(false)

    fun releaseInput() = sender.releaseAll()

    fun close() {
        if (closing) return
        closing = true
        sender.close {
            runCatching { hid?.unregisterApp() }
            hid?.let { proxy -> runCatching { adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, proxy) } }
            connectedHost = null
            appRegistered = false
            hid = null
            callbackExecutor.shutdown()
        }
    }

    private fun reconnect(device: BluetoothDevice?) {
        if (device == null) return
        val state = runCatching { hid?.getConnectionState(device) }.getOrNull()
        when (state) {
            BluetoothProfile.STATE_CONNECTED -> onConnection(device, BluetoothProfile.STATE_CONNECTED)
            BluetoothProfile.STATE_CONNECTING -> onConnection(device, BluetoothProfile.STATE_CONNECTING)
            else -> if (connect(device)) onConnection(device, BluetoothProfile.STATE_CONNECTING)
        }
    }

    private fun acceptKeyboardLeds(data: ByteArray) {
        val capsLock = data.firstOrNull()?.toInt()?.and(0x02) != 0
        main.post { if (!closing) onCapsLock(capsLock) }
    }

    private fun sendReport(id: Int, report: ByteArray): Boolean {
        val service = hid
        val device = connectedHost
        val accepted = service != null && device != null && appRegistered &&
            runCatching { service.sendReport(device, id, report) }.getOrDefault(false)
        return accepted
    }

    private fun fail(message: String) = main.post { if (!closing) onFailure(message) }

    private companion object {
        val SDP = BluetoothHidDeviceAppSdpSettings(
            "MinkPad",
            "MinkPad keyboard and trackpad",
            "MinkLauncher Open",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            MinkPadClassicReportMap.bytes,
        )

        // Android's CTS HID-device test values: 9-byte reports at an 11.25 ms latency target.
        val OUTGOING_QOS = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,
            9,
            0,
            11_250,
            BluetoothHidDeviceAppQosSettings.MAX,
        )
    }
}
