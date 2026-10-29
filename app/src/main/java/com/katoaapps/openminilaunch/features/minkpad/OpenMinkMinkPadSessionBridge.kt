package com.katoaapps.openminilaunch.features.minkpad

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.katoaapps.minkpad.core.MinkPadRepository
import com.katoaapps.minkpad.core.MinkPadSessionBridge

/**
 * Keeps launcher-owned Android service wiring outside the reusable MinkPad controller module.
 */
internal object OpenMinkMinkPadSessionBridge : MinkPadSessionBridge {
    override fun startForegroundSession(context: Context): Boolean = runCatching {
        ContextCompat.startForegroundService(
            context,
            Intent(context, MinkPadSessionService::class.java),
        )
        true
    }.getOrDefault(false)

    override fun stopSession(context: Context) {
        context.stopService(Intent(context, MinkPadSessionService::class.java))
    }

    override fun publishState(context: Context) {
        context.sendBroadcast(
            Intent(MinkPadSessionService.ACTION_UPDATE).setPackage(context.packageName),
        )
    }
}

/** Configures the app boundary before returning the process-wide controller instance. */
internal fun minkPadRepository(context: Context): MinkPadRepository {
    MinkPadRepository.configureSessionBridge(OpenMinkMinkPadSessionBridge)
    return MinkPadRepository.get(context)
}
