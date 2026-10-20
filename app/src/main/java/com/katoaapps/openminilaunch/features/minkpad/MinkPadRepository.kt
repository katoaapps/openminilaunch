package com.katoaapps.openminilaunch.features.minkpad

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkpad.connection.MinkPadBluetoothMonitor
import com.katoaapps.openminilaunch.features.minkpad.connection.MinkPadNearbyDiscovery
import com.katoaapps.openminilaunch.features.minkpad.connection.MinkPadPairingGate
import com.katoaapps.openminilaunch.features.minkpad.connection.classic.MinkPadClassicProfile
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadHostPlatform
import com.katoaapps.openminilaunch.features.minkpad.input.minkPadHostPlatformHint
import com.katoaapps.openminilaunch.features.minkpad.input.minkPadRemoteControlsHint
import com.katoaapps.openminilaunch.features.minkpad.persistence.MinkPadPreferenceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Application-scoped controller for Android's native Bluetooth HID Device profile. */
@SuppressLint("NewApi")
internal class MinkPadRepository private constructor(private val context: Context) {
    private fun text(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)

    val preferences = MinkPadPreferenceStore(context)
    private val mutable = MutableStateFlow(MinkPadSessionState())
    val state = mutable.asStateFlow()
    private val inputResetState = MutableStateFlow(0L)
    val inputReset = inputResetState.asStateFlow()
    private val main = Handler(Looper.getMainLooper())
    private var profile: MinkPadClassicProfile? = null
    private var registered = false
    private var generation = 0
    private var pairingGeneration = 0
    private var pendingConnectAddress: String? = null
    private var pendingConnectAttempts = 0
    private var pendingConnectScheduled = false
    private var pendingConnectSequence = 0L
    private var connectionAttempt = 0L
    private val suppressedHosts = mutableSetOf<String>()
    private val pairingGate = MinkPadPairingGate()
    private val nearbyDiscovery = MinkPadNearbyDiscovery(context) { adapter }
    val nearbyDevices = nearbyDiscovery.state
    private val bluetoothName = MinkPadBluetoothNameManager(
        context = context,
        preferences = preferences,
        adapter = { adapter },
        permissionsGranted = ::permissionsGranted,
    )
    private var serviceRunning = false
    private var disconnectAttempt = 0L
    private val monitor = MinkPadBluetoothMonitor(
        context = context,
        available = ::bluetoothEnabled,
        invalidate = { if (serviceRunning) stop() else closeProfile() },
        onBond = ::onBondStateChanged,
    )
    private val adapter get() = context.getSystemService(BluetoothManager::class.java)?.adapter

    fun supported(): Boolean = Build.VERSION.SDK_INT >= 28 && adapter != null
    fun supportsNearbyDiscovery(): Boolean = Build.VERSION.SDK_INT >= 31
    fun nearbyDiscoveryPermissionGranted(): Boolean = Build.VERSION.SDK_INT >= 31 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
    fun permissionsGranted(): Boolean = Build.VERSION.SDK_INT < 31 ||
        listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE).all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    @SuppressLint("MissingPermission")
    fun bluetoothEnabled(): Boolean = permissionsGranted() &&
        runCatching { adapter?.isEnabled == true }.getOrDefault(false)

    fun start() {
        if (mutable.value.status == MinkPadStatus.STOPPING) return
        if (!supported()) {
            fail(text(R.string.minkpad_error_requires_android), MinkPadStatus.UNSUPPORTED)
            return
        }
        if (!permissionsGranted()) {
            closeProfile()
            fail(text(R.string.minkpad_error_nearby_permission))
            return
        }
        if (!bluetoothEnabled()) {
            closeProfile()
            fail(text(R.string.minkpad_error_enable_bluetooth))
            return
        }
        if (!bluetoothName.prepare()) {
            fail(text(R.string.minkpad_error_temporary_name))
            return
        }
        if (serviceRunning) {
            startProfile()
            return
        }
        runCatching {
            ContextCompat.startForegroundService(context, Intent(context, MinkPadSessionService::class.java))
        }.onFailure {
            bluetoothName.restore()
            fail(text(R.string.minkpad_error_start_session))
        }
    }

    /** Repairs a temporary compatibility name after an abrupt process stop. */
    fun restoreAdapterNameIfIdle() {
        if (!serviceRunning) bluetoothName.restore()
    }

    /** Reapplies the compatibility identity before Android opens its pairing surface. */
    fun preparePairingIdentity(): Boolean {
        if (serviceRunning && bluetoothName.prepare()) return true
        if (!serviceRunning) {
            fail(text(R.string.minkpad_error_start_before_pairing))
        } else {
            fail(text(R.string.minkpad_error_temporary_name_stop))
        }
        return false
    }

    fun onServiceStarted() {
        serviceRunning = true
        if (Build.VERSION.SDK_INT < 28 || !permissionsGranted() || !bluetoothEnabled()) {
            stop()
            return
        }
        startProfile()
    }

    private fun startProfile() {
        profile?.close()
        profile = null
        registered = false
        monitor.start()
        val epoch = ++generation
        update(MinkPadSessionState(MinkPadStatus.REGISTERING, hosts = hosts()))

        val controller = MinkPadClassicProfile(
            context = context,
            initialHost = rememberedHost(),
            shouldReconnect = { device ->
                preferences.lastUsed(device.address) > 0 &&
                    device.address !in suppressedHosts &&
                    !pairingGate.active
            },
            onRegistered = { ready ->
                if (generation == epoch && serviceRunning) {
                    registered = ready
                    if (ready) {
                        val previous = rememberedHost()
                        update(mutable.value.copy(
                            status = MinkPadStatus.READY,
                            hosts = hosts(),
                            message = previous?.let { text(R.string.minkpad_ready_reconnecting, host(it).name) },
                        ))
                    } else fail(text(R.string.minkpad_error_session_ended))
                }
            },
            onConnection = { device, status ->
                if (generation == epoch && serviceRunning) handleConnection(device, status)
            },
            onFailure = { message ->
                if (generation == epoch && serviceRunning) fail(message)
            },
            onCapsLock = { enabled ->
                if (generation == epoch && serviceRunning) mutable.value = mutable.value.copy(capsLock = enabled)
            },
            onInputAvailability = {
                if (generation == epoch && serviceRunning) update(mutable.value)
            },
        )
        profile = controller
        if (!controller.start()) {
            fail(text(R.string.minkpad_error_no_hid_profile), MinkPadStatus.UNSUPPORTED)
            return
        }
        main.postDelayed({
            if (generation == epoch && serviceRunning && mutable.value.status == MinkPadStatus.REGISTERING) {
                fail(text(R.string.minkpad_error_registration_timeout))
            }
        }, REGISTRATION_TIMEOUT_MS)
    }

    /** Called only after Android confirms that the phone is discoverable. */
    fun beginClassicPairing(durationSeconds: Int) {
        if (!registered || !mutable.value.canChooseHost) return
        clearPendingConnect()
        invalidateConnectionAttempt()
        pairingGate.begin()
        if (mutable.value.status in setOf(
                MinkPadStatus.CONNECTING,
                MinkPadStatus.CONNECTED,
                MinkPadStatus.DISCONNECTING,
            )) {
            val previousAddress = mutable.value.host?.address
            releaseInput()
            previousAddress
                ?.let { runCatching { adapter?.getRemoteDevice(it) }.getOrNull() }
                ?.let { profile?.disconnect(it) }
            profile?.disconnect()
        }
        val duration = durationSeconds.coerceAtLeast(1)
        val pairing = ++pairingGeneration
        update(mutable.value.copy(
            status = MinkPadStatus.WAITING_FOR_HOST,
            host = null,
            message = text(R.string.minkpad_discoverable),
            advertisedName = bluetoothName.advertisedName(),
            pairingDeadlineElapsedRealtime = SystemClock.elapsedRealtime() + duration * 1_000L,
        ))
        main.postDelayed({
            if (pairingGeneration == pairing && mutable.value.status == MinkPadStatus.WAITING_FOR_HOST &&
                mutable.value.host == null) {
                finishPairing(text(R.string.minkpad_pairing_window_ended))
            }
        }, duration * 1_000L)
    }

    fun pairingDeclined() {
        if (!registered) return
        update(mutable.value.copy(
            message = text(R.string.minkpad_pairing_not_started),
            advertisedName = null,
            pairingDeadlineElapsedRealtime = null,
        ))
    }

    fun cancelPairing() {
        if (mutable.value.status != MinkPadStatus.WAITING_FOR_HOST) return
        stopNearbyDiscovery()
        pairingGeneration++
        finishPairing(text(R.string.minkpad_pairing_instructions_closed))
    }

    private fun finishPairing(message: String) {
        stopNearbyDiscovery()
        pairingGate.finish()
        update(mutable.value.copy(
            status = MinkPadStatus.READY,
            host = null,
            message = message,
            advertisedName = null,
            pairingDeadlineElapsedRealtime = null,
        ))
        rememberedHost()?.let { previous -> main.post { connect(previous.address) } }
    }

    @SuppressLint("MissingPermission")
    private fun handleConnection(device: BluetoothDevice, status: Int) {
        when (status) {
            BluetoothProfile.STATE_CONNECTED -> {
                if (pairingGate.blocks(device.address)) {
                    profile?.disconnect(device)
                    if (mutable.value.status == MinkPadStatus.WAITING_FOR_HOST) {
                        update(mutable.value.copy(host = null, hosts = hosts()))
                    }
                    return
                }
                if (device.address in suppressedHosts) {
                    profile?.disconnect(device)
                    if (mutable.value.host?.address == device.address) {
                        update(MinkPadSessionState(
                            status = MinkPadStatus.READY,
                            hosts = hosts(),
                            message = text(R.string.minkpad_connection_cancelled),
                        ))
                    }
                    return
                }
                val switchingTo = pendingConnectAddress
                if (switchingTo != null && device.address != switchingTo) {
                    profile?.disconnect(device)
                    update(mutable.value.copy(
                        status = MinkPadStatus.DISCONNECTING,
                        host = host(device),
                        message = text(R.string.minkpad_releasing_previous),
                    ))
                    return
                }
                pairingGeneration++
                invalidateConnectionAttempt()
                clearPendingConnect()
                pairingGate.finish()
                suppressedHosts.remove(device.address)
                preferences.rememberHost(device.address)
                update(MinkPadSessionState(
                    status = MinkPadStatus.CONNECTED,
                    host = host(device),
                    hosts = hosts(),
                    message = text(R.string.minkpad_native_connected),
                ))
            }
            BluetoothProfile.STATE_CONNECTING -> {
                if (pairingGate.blocks(device.address)) {
                    profile?.disconnect(device)
                    return
                }
                if (device.address in suppressedHosts) {
                    profile?.disconnect(device)
                    return
                }
                if (!bluetoothName.prepare()) {
                    profile?.disconnect(device)
                    fail(text(R.string.minkpad_error_temporary_name_stop))
                    return
                }
                update(mutable.value.copy(
                    status = MinkPadStatus.CONNECTING,
                    host = host(device),
                    message = text(R.string.minkpad_connecting_profile),
                ))
                armConnectionTimeout(device)
            }
            BluetoothProfile.STATE_DISCONNECTING -> {
                if (mutable.value.status != MinkPadStatus.WAITING_FOR_HOST) {
                    update(mutable.value.copy(
                        status = MinkPadStatus.DISCONNECTING,
                        host = host(device),
                        message = text(R.string.minkpad_disconnecting_input),
                    ))
                }
            }
            BluetoothProfile.STATE_DISCONNECTED -> {
                if (pairingGate.blocks(device.address)) {
                    if (mutable.value.status == MinkPadStatus.WAITING_FOR_HOST) {
                        update(mutable.value.copy(host = null, hosts = hosts()))
                    }
                    return
                }
                val next = pendingConnectAddress
                if (next != null && device.address != next &&
                    mutable.value.status == MinkPadStatus.CONNECTING &&
                    mutable.value.host?.address == next) {
                    // A cancelled host can report DISCONNECTED after the replacement request has begun.
                    return
                }
                invalidateConnectionAttempt()
                if (device.address in suppressedHosts && pendingConnectAddress == null) {
                    update(MinkPadSessionState(
                        status = if (registered) MinkPadStatus.READY else MinkPadStatus.IDLE,
                        hosts = hosts(),
                        message = mutable.value.message,
                    ))
                    restoreNameAfterHostLoss()
                    return
                }
                if (mutable.value.status == MinkPadStatus.CONNECTED &&
                    mutable.value.host?.address != device.address) {
                    refreshHosts()
                    return
                }
                if (mutable.value.status == MinkPadStatus.WAITING_FOR_HOST &&
                    mutable.value.pairingDeadlineElapsedRealtime != null) {
                    update(mutable.value.copy(host = null, hosts = hosts()))
                    if (next != null) schedulePendingConnect(next)
                    return
                }
                update(MinkPadSessionState(
                    status = MinkPadStatus.DISCONNECTED,
                    host = host(device),
                    hosts = hosts(),
                    message = if (next == null) text(R.string.minkpad_not_connected)
                        else text(R.string.minkpad_switching_devices),
                ))
                if (next != null) {
                    schedulePendingConnect(next)
                } else restoreNameAfterHostLoss()
            }
        }
    }

    private fun restoreNameAfterHostLoss() {
        if (!bluetoothName.restore()) {
            update(mutable.value.copy(
                message = text(R.string.minkpad_name_restore_failed),
            ))
        }
    }

    @SuppressLint("MissingPermission")
    private fun schedulePendingConnect(address: String) {
        if (pendingConnectScheduled || pendingConnectAddress != address) return
        pendingConnectScheduled = true
        val sequence = ++pendingConnectSequence
        main.postDelayed({
            if (sequence != pendingConnectSequence) return@postDelayed
            pendingConnectScheduled = false
            if (!serviceRunning || !registered || pendingConnectAddress != address) return@postDelayed
            val device = runCatching { adapter?.getRemoteDevice(address) }.getOrNull()
            if (device == null || device.bondState != BluetoothDevice.BOND_BONDED) {
                pendingConnectAddress = null
                fail(text(R.string.minkpad_error_host_unpaired))
                return@postDelayed
            }
            pendingConnectAttempts++
            if (profile?.connect(device) == true) {
                handleConnection(device, BluetoothProfile.STATE_CONNECTING)
                update(mutable.value.copy(message = text(R.string.minkpad_finishing_connection, host(device).name)))
            } else if (pendingConnectAttempts < HOST_SWITCH_ATTEMPTS) {
                schedulePendingConnect(address)
            } else {
                pendingConnectAddress = null
                fail(text(R.string.minkpad_error_move_input, host(device).name))
            }
        }, HOST_SWITCH_SETTLE_MS)
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        if (Build.VERSION.SDK_INT < 28 || !registered || !permissionsGranted()) return
        val device = runCatching { adapter?.getRemoteDevice(address) }.getOrNull() ?: return
        if (!mutable.value.canChooseHost || device.bondState != BluetoothDevice.BOND_BONDED) return
        if (!bluetoothName.prepare()) {
            fail(text(R.string.minkpad_error_temporary_name_stop))
            return
        }
        if (pairingGate.active) {
            pairingGeneration++
            pairingGate.finish()
        }
        suppressedHosts.remove(address)
        val currentAddress = mutable.value.host?.address
        if (mutable.value.status == MinkPadStatus.CONNECTING) {
            if (currentAddress == address) return
            currentAddress?.let { oldAddress ->
                suppressedHosts.add(oldAddress)
                runCatching { adapter?.getRemoteDevice(oldAddress) }.getOrNull()
                    ?.let { profile?.disconnect(it) }
            }
            invalidateConnectionAttempt()
            clearPendingConnect()
            pendingConnectAddress = address
            update(mutable.value.copy(
                status = MinkPadStatus.CONNECTING,
                host = host(device),
                message = text(R.string.minkpad_switching_connection, host(device).name),
            ))
            schedulePendingConnect(address)
            return
        }
        if (mutable.value.status == MinkPadStatus.DISCONNECTING) {
            clearPendingConnect()
            pendingConnectAddress = address
            update(mutable.value.copy(
                host = host(device),
                message = text(R.string.minkpad_switching_connection, host(device).name),
            ))
            return
        }
        if (mutable.value.status == MinkPadStatus.CONNECTED) {
            if (mutable.value.host?.address == address) return
            clearPendingConnect()
            pendingConnectAddress = address
            releaseInput()
            if (profile?.disconnect() == true) {
                update(mutable.value.copy(
                    status = MinkPadStatus.DISCONNECTING,
                    message = text(R.string.minkpad_switching_devices),
                ))
            } else {
                pendingConnectAddress = null
                fail(text(R.string.minkpad_error_release_host))
            }
            return
        }
        invalidateConnectionAttempt()
        clearPendingConnect()
        if (profile?.connect(device) == true) handleConnection(device, BluetoothProfile.STATE_CONNECTING)
        else fail(text(R.string.minkpad_error_reconnect_rejected))
    }

    @SuppressLint("MissingPermission")
    fun onBondStateChanged(device: BluetoothDevice) {
        when (device.bondState) {
            BluetoothDevice.BOND_BONDING -> {
                if (pairingGate.active) {
                    pairingGate.admit(device.address)
                    suppressedHosts.remove(device.address)
                    update(mutable.value.copy(
                        host = host(device),
                        message = text(R.string.minkpad_pairing_with_ellipsis, host(device).name),
                    ))
                }
            }
            BluetoothDevice.BOND_BONDED -> {
                if (pairingGate.active) {
                    pairingGate.admit(device.address)
                    suppressedHosts.remove(device.address)
                    preferences.rememberHost(device.address)
                    if (mutable.value.status == MinkPadStatus.WAITING_FOR_HOST) {
                        pendingConnectAddress = device.address
                        pendingConnectAttempts = 0
                        update(mutable.value.copy(
                            host = host(device),
                            hosts = hosts(),
                            message = text(R.string.minkpad_paired_finishing, host(device).name),
                        ))
                        schedulePendingConnect(device.address)
                    }
                }
            }
            BluetoothDevice.BOND_NONE -> {
                preferences.forgetHost(device.address)
                if (pairingGate.active && pairingGate.targetAddress == device.address) {
                    invalidateConnectionAttempt()
                    clearPendingConnect()
                    pairingGate.begin()
                    suppressedHosts.remove(device.address)
                    update(mutable.value.copy(
                        status = MinkPadStatus.WAITING_FOR_HOST,
                        host = null,
                        hosts = hosts(),
                        message = text(R.string.minkpad_pairing_failed),
                    ))
                    return
                }
                if (mutable.value.host?.address == device.address) {
                    update(MinkPadSessionState(
                        status = if (registered) MinkPadStatus.READY else MinkPadStatus.IDLE,
                        hosts = hosts(),
                        message = text(R.string.minkpad_pairing_removed),
                    ))
                }
            }
        }
        refreshHosts()
    }

    fun refreshHosts() {
        if (!permissionsGranted()) {
            if (serviceRunning) stop() else closeProfile()
            return
        }
        mutable.value = mutable.value.copy(hosts = hosts())
    }

    fun startNearbyDiscovery() {
        if (!supportsNearbyDiscovery()) return
        if (!nearbyDiscoveryPermissionGranted()) {
            nearbyDiscovery.fail(text(R.string.minkpad_scan_permission_required))
            return
        }
        if (mutable.value.status != MinkPadStatus.WAITING_FOR_HOST || mutable.value.host != null) {
            nearbyDiscovery.fail(text(R.string.minkpad_scan_pairing_required))
            return
        }
        nearbyDiscovery.start()
    }

    fun nearbyDiscoveryPermissionDenied() {
        nearbyDiscovery.fail(text(R.string.minkpad_scan_permission_denied))
    }

    fun stopNearbyDiscovery(clear: Boolean = true) {
        if (supportsNearbyDiscovery()) nearbyDiscovery.stop(clear)
    }

    @SuppressLint("MissingPermission")
    fun pairNearbyDevice(address: String): Boolean {
        if (!nearbyDiscoveryPermissionGranted() || mutable.value.status != MinkPadStatus.WAITING_FOR_HOST) return false
        val device = runCatching { adapter?.getRemoteDevice(address) }.getOrNull() ?: return false
        val selectedHost = host(device)
        stopNearbyDiscovery(clear = false)
        pairingGate.admit(address)
        update(mutable.value.copy(
            host = selectedHost,
            message = text(R.string.minkpad_pairing_with_ellipsis, selectedHost.name),
        ))
        return runCatching { device.createBond() }.getOrDefault(false).also { started ->
            if (!started) {
                pairingGate.begin()
                val message = text(R.string.minkpad_error_pairing_start)
                update(mutable.value.copy(host = null, message = message))
                nearbyDiscovery.fail(message)
            }
        }
    }

    fun forget(address: String) {
        preferences.forgetHost(address)
        suppressedHosts.add(address)
        val isActiveHost = mutable.value.host?.address == address
        val shouldDisconnect = pendingConnectAddress == address ||
            (isActiveHost && mutable.value.status in setOf(
                MinkPadStatus.CONNECTED,
                MinkPadStatus.CONNECTING,
                MinkPadStatus.DISCONNECTING,
            ))
        if (shouldDisconnect) {
            invalidateConnectionAttempt()
            clearPendingConnect()
            if (isActiveHost) releaseInput()
            runCatching { adapter?.getRemoteDevice(address) }.getOrNull()
                ?.let { profile?.disconnect(it) }
            update(MinkPadSessionState(
                status = if (registered) MinkPadStatus.READY else MinkPadStatus.IDLE,
                hosts = hosts(),
                message = text(R.string.minkpad_forgotten),
            ))
        } else {
            refreshHosts()
        }
    }

    @SuppressLint("MissingPermission")
    private fun host(device: BluetoothDevice): MinkPadHost {
        val name = device.name
        val bluetoothClass = runCatching { device.bluetoothClass }.getOrNull()
        return MinkPadHost(
            device.address,
            name ?: text(R.string.minkpad_bluetooth_device),
            preferences.lastUsed(device.address) > 0,
            platformHint = minkPadHostPlatformHint(name) ?: when (bluetoothClass?.majorDeviceClass) {
                BluetoothClass.Device.Major.PHONE -> MinkPadHostPlatform.ANDROID
                else -> null
            },
            prefersRemoteControls = minkPadRemoteControlsHint(name, bluetoothClass?.deviceClass),
        )
    }

    @SuppressLint("MissingPermission")
    private fun hosts(): List<MinkPadHost> = if (!permissionsGranted()) emptyList() else runCatching {
        adapter?.bondedDevices.orEmpty()
            .filter { preferences.lastUsed(it.address) > 0 }
            .sortedWith(compareByDescending<BluetoothDevice> { preferences.lastUsed(it.address) }
                .thenBy { it.name ?: "" })
            .map(::host)
    }.getOrDefault(emptyList())

    @SuppressLint("MissingPermission")
    private fun rememberedHost(): BluetoothDevice? = hosts().firstOrNull { it.remembered }?.let {
        runCatching { adapter?.getRemoteDevice(it.address) }.getOrNull()
    }

    fun disconnect() {
        disconnectAttempt++
        invalidateConnectionAttempt()
        clearPendingConnect()
        if (Build.VERSION.SDK_INT < 28 || profile?.disconnect() != true) {
            fail(text(R.string.minkpad_error_disconnect_rejected))
            return
        }
        update(mutable.value.copy(
            status = MinkPadStatus.DISCONNECTING,
            message = text(R.string.minkpad_disconnecting_input),
        ))
        val attempt = disconnectAttempt
        main.postDelayed({
            if (disconnectAttempt == attempt && mutable.value.status == MinkPadStatus.DISCONNECTING) {
                fail(text(R.string.minkpad_error_disconnect_timeout))
            }
        }, DISCONNECT_TIMEOUT_MS)
    }

    fun onServiceStartFailed() {
        fail(text(R.string.minkpad_error_service_start))
    }

    fun releaseInput() {
        inputResetState.value++
        if (Build.VERSION.SDK_INT >= 28) profile?.releaseInput()
    }

    /** Cancels setup without stopping the entire MinkPad session. */
    @SuppressLint("MissingPermission")
    fun cancelConnection() {
        if (mutable.value.status !in setOf(MinkPadStatus.CONNECTING, MinkPadStatus.DISCONNECTING)) return
        val addresses = listOfNotNull(mutable.value.host?.address, pendingConnectAddress).distinct()
        suppressedHosts.addAll(addresses)
        pairingGeneration++
        pairingGate.finish()
        invalidateConnectionAttempt()
        clearPendingConnect()
        addresses.mapNotNull { runCatching { adapter?.getRemoteDevice(it) }.getOrNull() }
            .forEach { profile?.disconnect(it) }
        profile?.disconnect()
        update(MinkPadSessionState(
            status = if (registered) MinkPadStatus.READY else MinkPadStatus.IDLE,
            hosts = hosts(),
            message = text(R.string.minkpad_connection_attempt_cancelled),
        ))
    }

    @androidx.annotation.ChecksSdkIntAtLeast(api = 28)
    private fun ready(): Boolean = Build.VERSION.SDK_INT >= 28 && permissionsGranted() &&
        mutable.value.status == MinkPadStatus.CONNECTED

    fun key(usage: Int, pressed: Boolean, modifiers: Int = 0) {
        if (ready() && mutable.value.keyboardReady) profile?.sender?.key(usage, pressed, modifiers)
    }
    fun shortcut(usage: Int, modifiers: Int): Boolean {
        if (!ready() || !mutable.value.keyboardReady) return false
        profile?.sender?.shortcut(usage, modifiers) ?: return false
        return true
    }
    fun consumer(usage: Int) {
        if (ready() && mutable.value.consumerReady) profile?.sender?.consumer(usage)
    }
    fun tapKey(usage: Int) {
        if (ready() && mutable.value.keyboardReady) profile?.sender?.tapKey(usage)
    }
    fun modifiers(value: Int) {
        if (ready() && mutable.value.keyboardReady) profile?.sender?.modifiers(value)
    }
    fun type(text: String): Boolean {
        val controller = profile ?: return false
        if (!ready() || !mutable.value.keyboardReady) return false
        return controller.sender.type(text)
    }
    fun pointer(x: Float, y: Float) {
        if (ready() && mutable.value.mouseReady) profile?.sender?.pointer(x, y)
    }
    fun scroll(vertical: Float, horizontal: Float) {
        if (ready() && mutable.value.mouseReady) profile?.sender?.pointer(
            vertical = vertical,
            horizontal = horizontal,
        )
    }
    fun click(button: Int) {
        if (ready() && mutable.value.mouseReady) profile?.sender?.click(button)
    }
    fun mouseButton(button: Int, pressed: Boolean) {
        if (ready() && mutable.value.mouseReady) profile?.sender?.mouseButton(button, pressed)
    }
    fun perform(action: com.katoaapps.openminilaunch.features.minkpad.input.MinkPadAction) = tapKey(action.usage)

    fun stop() {
        stopNearbyDiscovery()
        releaseInput()
        pairingGeneration++
        if (serviceRunning) {
            update(mutable.value.copy(
                status = MinkPadStatus.STOPPING,
                message = text(R.string.minkpad_stopping_input),
            ))
            context.stopService(Intent(context, MinkPadSessionService::class.java))
        } else {
            if (mutable.value.status != MinkPadStatus.IDLE) closeProfile()
            if (!bluetoothName.restore()) {
                update(mutable.value.copy(message = text(R.string.minkpad_stopped_name_restore_failed)))
            }
        }
    }

    fun onServiceDestroyed() {
        serviceRunning = false
        closeProfile()
        if (!bluetoothName.restore()) {
            update(mutable.value.copy(message = text(R.string.minkpad_stopped_name_restore_failed)))
        }
    }

    private fun closeProfile() {
        stopNearbyDiscovery()
        monitor.stop()
        disconnectAttempt++
        pairingGeneration++
        generation++
        registered = false
        invalidateConnectionAttempt()
        clearPendingConnect()
        pairingGate.finish()
        suppressedHosts.clear()
        if (Build.VERSION.SDK_INT >= 28) profile?.close()
        profile = null
        mutable.value = MinkPadSessionState(hosts = hosts())
    }

    private fun fail(message: String, status: MinkPadStatus = MinkPadStatus.ERROR) {
        if (Build.VERSION.SDK_INT >= 28) profile?.releaseInput()
        update(mutable.value.copy(status = status, message = message))
    }

    private fun update(value: MinkPadSessionState) {
        val connected = value.status == MinkPadStatus.CONNECTED
        mutable.value = value.copy(
            keyboardReady = connected && profile?.keyboardReady == true,
            mouseReady = connected && profile?.mouseReady == true,
            consumerReady = connected && profile?.consumerReady == true,
            capsLock = if (connected) mutable.value.capsLock else false,
        )
        if (serviceRunning) {
            context.sendBroadcast(Intent(MinkPadSessionService.ACTION_UPDATE).setPackage(context.packageName))
        }
    }

    @SuppressLint("MissingPermission")
    private fun armConnectionTimeout(device: BluetoothDevice) {
        val attempt = ++connectionAttempt
        main.postDelayed({
            if (attempt != connectionAttempt || !serviceRunning ||
                mutable.value.status != MinkPadStatus.CONNECTING ||
                mutable.value.host?.address != device.address) return@postDelayed
            pairingGeneration++
            pairingGate.finish()
            suppressedHosts.add(device.address)
            clearPendingConnect()
            profile?.disconnect(device)
            update(MinkPadSessionState(
                status = if (registered) MinkPadStatus.READY else MinkPadStatus.IDLE,
                hosts = hosts(),
                message = text(R.string.minkpad_host_no_response, host(device).name),
            ))
        }, CONNECTION_TIMEOUT_MS)
    }

    private fun invalidateConnectionAttempt() {
        connectionAttempt++
    }

    private fun clearPendingConnect() {
        pendingConnectAddress = null
        pendingConnectAttempts = 0
        pendingConnectScheduled = false
        pendingConnectSequence++
    }

    companion object {
        private const val REGISTRATION_TIMEOUT_MS = 12_000L
        private const val DISCONNECT_TIMEOUT_MS = 10_000L
        private const val CONNECTION_TIMEOUT_MS = 12_000L
        private const val HOST_SWITCH_SETTLE_MS = 800L
        private const val HOST_SWITCH_ATTEMPTS = 3
        @Volatile private var instance: MinkPadRepository? = null
        fun get(context: Context): MinkPadRepository = instance ?: synchronized(this) {
            instance ?: MinkPadRepository(context.applicationContext).also { instance = it }
        }
    }
}
