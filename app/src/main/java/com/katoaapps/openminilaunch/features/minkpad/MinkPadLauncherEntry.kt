package com.katoaapps.openminilaunch.features.minkpad

import com.katoaapps.minkpad.core.R as MinkPadR

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.ui.minkpad.MinkPadActivity

/** Internal destination: selectable without exposing another icon in other launchers. */
internal object MinkPadLauncherEntry {
    const val KEY = "internal:minkpad"
    fun target(context: Context, userSerial: Long) = LauncherAppTarget(
        label = context.getString(MinkPadR.string.minkpad_name), packageName = context.packageName,
        componentName = ComponentName(context, MinkPadActivity::class.java),
        userSerial = userSerial, isWorkProfile = false, selectionKey = KEY,
    )
    /** All entry points share one Recents document, separate from the hidden Home task. */
    fun intent(context: Context): Intent = Intent(context, MinkPadActivity::class.java)
        .setData(
            Uri.Builder()
                .scheme("mink")
                .authority(context.packageName)
                .appendPath("minkpad")
                .build(),
        )
        .addFlags(
            Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS,
        )

    fun open(context: Context): Boolean = runCatching {
        context.startActivity(intent(context))
        true
    }.getOrDefault(false)
}
