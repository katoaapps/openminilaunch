package com.katoaapps.openminilaunch.features.minkpad.connection

/**
 * Keeps an explicit pairing window isolated from saved-host auto-reconnects.
 *
 * Android can connect the HID profile before it reports [android.bluetooth.BluetoothDevice.BOND_BONDED],
 * so the address is admitted as soon as bonding starts rather than after bonding finishes.
 */
internal class MinkPadPairingGate {
    var targetAddress: String? = null
        private set

    var active: Boolean = false
        private set

    fun begin() {
        active = true
        targetAddress = null
    }

    fun admit(address: String) {
        if (active) targetAddress = address
    }

    fun blocks(address: String): Boolean = active && address != targetAddress

    fun finish() {
        active = false
        targetAddress = null
    }
}
