package com.katoaapps.openminilaunch.features.minkpad

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkpad.persistence.MinkPadPreferenceStore

/** Owns MinkPad's temporary compatibility identity and restores the user's normal device name. */
internal class MinkPadBluetoothNameManager(
    private val context: Context,
    private val preferences: MinkPadPreferenceStore,
    private val adapter: () -> BluetoothAdapter?,
    private val permissionsGranted: () -> Boolean,
) {
    @SuppressLint("MissingPermission")
    fun advertisedName(): String = if (!permissionsGranted()) {
        context.getString(R.string.minkpad_this_phone)
    } else {
        runCatching { adapter()?.name }.getOrNull()?.takeIf(String::isNotBlank)
            ?: context.getString(R.string.minkpad_this_phone)
    }

    /** Applies the standard MinkPad HID identity for responsive Android-host pointer timing. */
    @SuppressLint("MissingPermission")
    fun prepare(): Boolean {
        val bluetooth = adapter() ?: return false
        val current = runCatching { bluetooth.name }.getOrNull()?.trim().orEmpty()
        val original = current.takeIf { it.isNotEmpty() && !isMinkPadBluetoothName(it) }
            ?: preferences.originalBluetoothName()
                ?.trim()
                ?.takeIf { it.isNotEmpty() && !isMinkPadBluetoothName(it) }
            ?: configuredDeviceName()
            ?: Build.MODEL.trim().takeIf(String::isNotEmpty)
            ?: return false
        preferences.rememberOriginalBluetoothName(original)
        val minkPadName = minkPadBluetoothName(original)
        if (current == minkPadName) return true
        return runCatching { bluetooth.setName(minkPadName) }.getOrDefault(false)
    }

    /** Restores the phone identity after MinkPad stops or loses its receiving host. */
    @SuppressLint("MissingPermission")
    fun restore(): Boolean {
        if (!permissionsGranted()) return false
        val bluetooth = adapter() ?: return false
        val current = runCatching { bluetooth.name }.getOrNull() ?: return false
        if (!isMinkPadBluetoothName(current)) return true
        val deviceName = preferences.originalBluetoothName()
            ?.trim()
            ?.takeIf { it.isNotEmpty() && !isMinkPadBluetoothName(it) }
            ?: configuredDeviceName()
            ?: Build.MODEL.trim().takeIf(String::isNotEmpty)
            ?: return false
        return runCatching { bluetooth.setName(deviceName) }.getOrDefault(false)
    }

    private fun configuredDeviceName(): String? =
        Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
            ?.trim()
            ?.takeIf { it.isNotEmpty() && !isMinkPadBluetoothName(it) }
}
