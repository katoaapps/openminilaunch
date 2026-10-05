package com.katoaapps.openminilaunch.features.minkpad

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.ui.minkpad.MinkPadActivity

/** Internal destination: selectable without exposing another icon in other launchers. */
internal object MinkPadLauncherEntry {
    const val KEY = "internal:minkpad"
    fun target(context: Context, userSerial: Long) = LauncherAppTarget(
        label = "MinkPad", packageName = context.packageName,
        componentName = ComponentName(context, MinkPadActivity::class.java),
        userSerial = userSerial, isWorkProfile = false, selectionKey = KEY,
    )
    /** All entry points share one Recents task, separate from the launcher's hidden Home task. */
    fun intent(context: Context): Intent = Intent(context, MinkPadActivity::class.java).addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
    )

    fun open(context: Context): Boolean = runCatching {
        context.startActivity(intent(context))
        true
    }.getOrDefault(false)
}
