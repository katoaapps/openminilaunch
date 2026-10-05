package com.katoaapps.openminilaunch.features.minkpad

internal enum class MinkPadStatus {
    IDLE,
    REGISTERING,
    READY,
    WAITING_FOR_HOST,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    DISCONNECTED,
    STOPPING,
    UNSUPPORTED,
    ERROR,
}
internal data class MinkPadHost(val address: String, val name: String, val remembered: Boolean)
internal data class MinkPadSessionState(
    val status: MinkPadStatus = MinkPadStatus.IDLE,
    val host: MinkPadHost? = null,
    val hosts: List<MinkPadHost> = emptyList(),
    val message: String? = null,
    val advertisedName: String? = null,
    val pairingDeadlineElapsedRealtime: Long? = null,
    val capsLock: Boolean = false,
    // Never capture keys until the native HID profile confirms a connected host.
    val keyboardReady: Boolean = false,
    val mouseReady: Boolean = false,
    val consumerReady: Boolean = false,
)

internal val MinkPadSessionState.canChooseHost: Boolean
    get() = status == MinkPadStatus.READY || status == MinkPadStatus.WAITING_FOR_HOST ||
        status == MinkPadStatus.DISCONNECTED || status == MinkPadStatus.CONNECTED

internal data class MinkPadPreferences(
    val keepAwake: Boolean = true,
    val pointerSensitivity: Float = 1f,
    val scrollSensitivity: Float = 1f,
    val naturalScroll: Boolean = true,
    val tapToClick: Boolean = true,
    val haptics: Boolean = true,
)
