package com.katoaapps.openminilaunch.ui.minkpad

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings

internal fun Context.openMinkPadBluetoothSettings() {
    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
    if (this !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}
