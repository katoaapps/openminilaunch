package com.katoaapps.openminilaunch.features.minkpad

import android.Manifest
import android.annotation.SuppressLint
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
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.katoaapps.openminilaunch.features.minkpad.connection.MinkPadBluetoothMonitor
import com.katoaapps.openminilaunch.features.minkpad.connection.MinkPadPairingGate
import com.katoaapps.openminilaunch.features.minkpad.connection.classic.MinkPadClassicProfile
import com.katoaapps.openminilaunch.features.minkpad.persistence.MinkPadPreferenceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Application-scoped controller for Android's native Bluetooth HID Device profile. */
@SuppressLint("NewApi")
internal class MinkPadRepository private constructor(private val context: Context) {
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
            fail("MinkPad requires Android 9+ and Bluetooth HID Device support.", MinkPadStatus.UNSUPPORTED)
            return
        }
        if (!permissionsGranted()) {
            closeProfile()
            fail("Allow Nearby devices to start MinkPad.")
            return
        }
        if (!bluetoothEnabled()) {
            closeProfile()
            fail("Turn on Bluetooth to start MinkPad.")
            return
        }
        if (!prepareAdapterName()) {
            fail("Android could not apply MinkPad's temporary Bluetooth name. Try again after restarting Bluetooth.")
            return
        }
        if (serviceRunning) {
            startProfile()
            return
        }
        runCatching {
            ContextCompat.startForegroundService(context, Intent(context, MinkPadSessionService::class.java))
        }.onFailure {
            restoreExperimentalAdapterName()
            fail("Android could not start the Bluetooth session. Open MinkPad and try again.")
        }
    }

    /** Repairs a temporary compatibility name after an abrupt process stop. */
    fun restoreAdapterNameIfIdle() {
        if (!serviceRunning) restoreExperimentalAdapterName()
    }

    /** Reapplies the compatibility identity before Android opens its pairing surface. */
    fun preparePairingIdentity(): Boolean {
        if (serviceRunning && prepareAdapterName()) return true
        if (!serviceRunning) {
            fail("Start MinkPad before pairing a new receiving device.")
        } else {
            fail("Android could not apply MinkPad's temporary Bluetooth name. Pause and try again after restarting Bluetooth.")
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
                            message = previous?.let { "Native Bluetooth input is ready. Reconnecting to ${host(it).name}…" },
                        ))
                    } else fail("Android ended the HID session. Another Bluetooth input app may have taken over.")
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
            fail("This phone does not expose Android's native Bluetooth HID Device profile.", MinkPadStatus.UNSUPPORTED)
            return
        }
        main.postDelayed({
            if (generation == epoch && serviceRunning && mutable.value.status == MinkPadStatus.REGISTERING) {
                fail("Native Bluetooth HID registration did not respond. Stop and retry.")
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
            message = "This phone is discoverable. Select it from the receiving device's Bluetooth settings.",
            advertisedName = advertisedName(),
            pairingDeadlineElapsedRealtime = SystemClock.elapsedRealtime() + duration * 1_000L,
        ))
        main.postDelayed({
            if (pairingGeneration == pairing && mutable.value.status == MinkPadStatus.WAITING_FOR_HOST &&
                mutable.value.host == null) {
                finishPairing("The system pairing window ended. Start pairing again when the receiving device is ready.")
            }
        }, duration * 1_000L)
    }

    fun pairingDeclined() {
        if (!registered) return
        update(mutable.value.copy(
            message = "Pairing was not started.",
            advertisedName = null,
            pairingDeadlineElapsedRealtime = null,
        ))
    }

    fun cancelPairing() {
        if (mutable.value.status != MinkPadStatus.WAITING_FOR_HOST) return
        pairingGeneration++
        finishPairing("Pairing instructions closed. Android may keep the phone discoverable until its system timer ends.")
    }

    private fun finishPairing(message: String) {
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
    private fun advertisedName(): String = if (!permissionsGranted()) "this phone" else
        runCatching { adapter?.name }.getOrNull()?.takeIf(String::isNotBlank) ?: "this phone"

    fun classicLatencyExperimentEnabled(): Boolean = preferences.classicLatencyExperimentEnabled()

    /**
     * Diagnostic-only host compatibility experiment. Android's HID host gives device names that
     * begin with "Pro Controller" an 11.25 ms sniff limit through its interop database. Nothing
     * else about MinkPad's descriptor, reports, or connection flow changes.
     */
    @SuppressLint("MissingPermission")
    fun setClassicLatencyExperiment(enabled: Boolean): String {
        if (!permissionsGranted()) return "Allow Nearby devices before changing the Bluetooth test identity."
        val bluetooth = adapter ?: return "Bluetooth is unavailable on this phone."
        val current = runCatching { bluetooth.name }.getOrNull()?.trim().orEmpty()

        if (enabled) {
            val original = current.takeUnless(::isExperimentalAdapterName)
                ?: configuredDeviceName()
                ?: Build.MODEL.trim().takeIf(String::isNotEmpty)
                ?: return "MinkPad could not determine the Bluetooth name to restore after the test."
            if (!preferences.classicLatencyExperimentEnabled()) {
                preferences.enableClassicLatencyExperiment(original)
            }
            return "Compatibility identity enabled. Start MinkPad to use $CLASSIC_LATENCY_EXPERIMENT_NAME; Pause restores $original."
        }

        stop()
        if (restoreExperimentalAdapterName()) {
            preferences.disableClassicLatencyExperiment()
            return "Compatibility identity disabled. MinkPad will use the phone's normal Bluetooth name."
        }
        if (!isExperimentalAdapterName(current)) {
            preferences.disableClassicLatencyExperiment()
            return "Compatibility identity disabled."
        }
        return "Android did not restore the Bluetooth name. Pause MinkPad and try again."
    }

    @SuppressLint("MissingPermission")
    private fun prepareAdapterName(): Boolean {
        if (preferences.classicLatencyExperimentEnabled()) {
            val bluetooth = adapter ?: return false
            if (runCatching { bluetooth.name }.getOrNull() == CLASSIC_LATENCY_EXPERIMENT_NAME) return true
            return runCatching { bluetooth.setName(CLASSIC_LATENCY_EXPERIMENT_NAME) }.getOrDefault(false)
        }
        return restoreExperimentalAdapterName()
    }

    /** Repairs an adapter name left behind by an interrupted MinkPad experiment. */
    @SuppressLint("MissingPermission")
    private fun restoreExperimentalAdapterName(): Boolean {
        if (!permissionsGranted()) return false
        val bluetooth = adapter ?: return false
        val current = runCatching { bluetooth.name }.getOrNull() ?: return false
        if (!isExperimentalAdapterName(current)) return true
        val deviceName = preferences.originalBluetoothName()
            ?.trim()
            ?.takeIf { it.isNotEmpty() && !isExperimentalAdapterName(it) }
            ?: configuredDeviceName()
            ?: Build.MODEL.trim().takeIf(String::isNotEmpty)
            ?: return false
        return runCatching { bluetooth.setName(deviceName) }.getOrDefault(false)
    }

    private fun configuredDeviceName(): String? =
        Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
            ?.trim()
            ?.takeIf { it.isNotEmpty() && !isExperimentalAdapterName(it) }

    private fun isExperimentalAdapterName(value: String): Boolean =
        value == CLASSIC_LATENCY_EXPERIMENT_NAME ||
            value == LEGACY_CLASSIC_LATENCY_EXPERIMENT_NAME ||
            LEGACY_EXPERIMENTAL_NAME.matches(value)

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
                            message = "That connection was cancelled. Choose a saved device or start pairing.",
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
                        message = "Releasing the previous receiving device…",
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
                    message = "Native Bluetooth keyboard, mouse, and media controls are connected.",
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
                if (!prepareAdapterName()) {
                    profile?.disconnect(device)
                    fail("Android could not apply MinkPad's temporary Bluetooth name. Pause and try again after restarting Bluetooth.")
                    return
                }
                update(mutable.value.copy(
                    status = MinkPadStatus.CONNECTING,
                    host = host(device),
                    message = "Connecting the native Bluetooth input profile…",
                ))
                armConnectionTimeout(device)
            }
            BluetoothProfile.STATE_DISCONNECTING -> {
                if (mutable.value.status != MinkPadStatus.WAITING_FOR_HOST) {
                    update(mutable.value.copy(
                        status = MinkPadStatus.DISCONNECTING,
                        host = host(device),
                        message = "Disconnecting Bluetooth input…",
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
                    message = if (next == null) "Not connected. Reconnect a saved device or start pairing."
                        else "Switching receiving devices…",
                ))
                if (next != null) {
                    schedulePendingConnect(next)
                } else restoreNameAfterHostLoss()
            }
        }
    }

    private fun restoreNameAfterHostLoss() {
        if (!restoreExperimentalAdapterName()) {
            update(mutable.value.copy(
                message = "Not connected. Android did not restore the phone's Bluetooth name; Pause and try again after restarting Bluetooth.",
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
                fail("The selected receiving device is no longer paired.")
                return@postDelayed
            }
            pendingConnectAttempts++
            if (profile?.connect(device) == true) {
                handleConnection(device, BluetoothProfile.STATE_CONNECTING)
                update(mutable.value.copy(message = "Finishing connection to ${host(device).name}…"))
            } else if (pendingConnectAttempts < HOST_SWITCH_ATTEMPTS) {
                schedulePendingConnect(address)
            } else {
                pendingConnectAddress = null
                fail("Android could not move Bluetooth input to ${host(device).name}. Try Reconnect again.")
            }
        }, HOST_SWITCH_SETTLE_MS)
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        if (Build.VERSION.SDK_INT < 28 || !registered || !permissionsGranted()) return
        val device = runCatching { adapter?.getRemoteDevice(address) }.getOrNull() ?: return
        if (!mutable.value.canChooseHost || device.bondState != BluetoothDevice.BOND_BONDED) return
        if (!prepareAdapterName()) {
            fail("Android could not apply MinkPad's temporary Bluetooth name. Pause and try again after restarting Bluetooth.")
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
                message = "Switching connection to ${host(device).name}…",
            ))
            schedulePendingConnect(address)
            return
        }
        if (mutable.value.status == MinkPadStatus.DISCONNECTING) {
            clearPendingConnect()
            pendingConnectAddress = address
            update(mutable.value.copy(
                host = host(device),
                message = "Switching connection to ${host(device).name}…",
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
                    message = "Switching receiving devices…",
                ))
            } else {
                pendingConnectAddress = null
                fail("Android could not release the current receiving device.")
            }
            return
        }
        invalidateConnectionAttempt()
        clearPendingConnect()
        if (profile?.connect(device) == true) handleConnection(device, BluetoothProfile.STATE_CONNECTING)
        else fail("Android rejected the reconnect request. Confirm the device is still paired and try again.")
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
                        message = "Pairing with ${host(device).name}…",
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
                            message = "Paired with ${host(device).name}. Finishing keyboard and mouse connection…",
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
                        message = "Pairing failed. If the device was paired before, forget it on both devices and try again.",
                    ))
                    return
                }
                if (mutable.value.host?.address == device.address) {
                    update(MinkPadSessionState(
                        status = if (registered) MinkPadStatus.READY else MinkPadStatus.IDLE,
                        hosts = hosts(),
                        message = "The saved Bluetooth pairing was removed.",
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

    fun forget(address: String) {
        preferences.forgetHost(address)
        suppressedHosts.add(address)
        val cancelling = pendingConnectAddress == address ||
            (mutable.value.host?.address == address && mutable.value.status in setOf(
                MinkPadStatus.CONNECTING,
                MinkPadStatus.DISCONNECTING,
            ))
        if (cancelling) {
            invalidateConnectionAttempt()
            clearPendingConnect()
            runCatching { adapter?.getRemoteDevice(address) }.getOrNull()
                ?.let { profile?.disconnect(it) }
            update(MinkPadSessionState(
                status = if (registered) MinkPadStatus.READY else MinkPadStatus.IDLE,
                hosts = hosts(),
                message = "Forgotten. MinkPad will not reconnect to that device automatically.",
            ))
        } else {
            refreshHosts()
        }
    }

    @SuppressLint("MissingPermission")
    private fun host(device: BluetoothDevice) = MinkPadHost(
        device.address,
        device.name ?: "Bluetooth device",
        preferences.lastUsed(device.address) > 0,
    )

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
            fail("Android did not accept the disconnect request.")
            return
        }
        update(mutable.value.copy(status = MinkPadStatus.DISCONNECTING, message = "Disconnecting Bluetooth input…"))
        val attempt = disconnectAttempt
        main.postDelayed({
            if (disconnectAttempt == attempt && mutable.value.status == MinkPadStatus.DISCONNECTING) {
                fail("The receiving device did not finish disconnecting. Stop MinkPad and try again.")
            }
        }, DISCONNECT_TIMEOUT_MS)
    }

    fun onServiceStartFailed() {
        fail("Android could not start the Bluetooth session service. Check Nearby devices access and try again.")
    }

    fun releaseInput() {
        inputResetState.value++
        if (Build.VERSION.SDK_INT >= 28) profile?.releaseInput()
    }

    /** Cancels setup without pausing the entire MinkPad session. */
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
            message = "Connection attempt cancelled. Choose another saved device or start pairing.",
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
        releaseInput()
        pairingGeneration++
        if (serviceRunning) {
            update(mutable.value.copy(status = MinkPadStatus.STOPPING, message = "Stopping Bluetooth input…"))
            context.stopService(Intent(context, MinkPadSessionService::class.java))
        } else {
            if (mutable.value.status != MinkPadStatus.IDLE) closeProfile()
            if (!restoreExperimentalAdapterName()) {
                update(mutable.value.copy(message = "MinkPad stopped, but Android did not restore the phone's Bluetooth name. Start and Pause again after restarting Bluetooth."))
            }
        }
    }

    fun onServiceDestroyed() {
        serviceRunning = false
        closeProfile()
        if (!restoreExperimentalAdapterName()) {
            update(mutable.value.copy(message = "MinkPad stopped, but Android did not restore the phone's Bluetooth name. Start and Pause again after restarting Bluetooth."))
        }
    }

    private fun closeProfile() {
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
                message = "${host(device).name} did not respond. Choose another saved device or retry.",
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
        private const val CLASSIC_LATENCY_EXPERIMENT_NAME = "Pro Controller - MinkPad"
        private const val LEGACY_CLASSIC_LATENCY_EXPERIMENT_NAME = "Pro Controller"
        private val LEGACY_EXPERIMENTAL_NAME = Regex("^MinkPad [0-9A-F]{8}$", RegexOption.IGNORE_CASE)
        @Volatile private var instance: MinkPadRepository? = null
        fun get(context: Context): MinkPadRepository = instance ?: synchronized(this) {
            instance ?: MinkPadRepository(context.applicationContext).also { instance = it }
        }
    }
}
