package com.katoaapps.openminilaunch.ui.launcher

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Routes app-icon and development launches through Android's HOME intent.
 *
 * MainActivity must always belong to a home task. Launching that same activity directly from
 * both LAUNCHER and HOME intents lets the window manager reuse a standard task as a home task,
 * which can crash when Android applies an app-specific locale configuration.
 */
class LauncherEntryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setPackage(packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }
}
