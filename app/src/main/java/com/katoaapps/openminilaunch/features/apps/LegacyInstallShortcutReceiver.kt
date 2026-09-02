package com.katoaapps.openminilaunch.features.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.katoaapps.openminilaunch.ui.shortcuts.PinShortcutActivity

/** Adapts the pre-O shortcut broadcast to Mink's normal placement screen. */
class LegacyInstallShortcutReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != LEGACY_INSTALL_SHORTCUT_ACTION) return
        val confirmation = Intent(context, PinShortcutActivity::class.java).apply {
            action = LEGACY_INSTALL_SHORTCUT_ACTION
            replaceExtras(intent.extras)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        runCatching { context.startActivity(confirmation) }
    }
}
