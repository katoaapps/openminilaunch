package com.katoaapps.openminilaunch.features.minkboard.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.katoaapps.openminilaunch.OpenMinkApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal class FocusAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != FocusTimerIntents.ACTION_ALARM) return
        val timerId = FocusTimerIntents.timerId(intent) ?: return
        launchAsync(context) { complete(timerId) }
    }
}

internal class FocusTimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timerId = FocusTimerIntents.timerId(intent) ?: return
        when (intent.action) {
            FocusTimerIntents.ACTION_STOP -> launchAsync(context) { stop(timerId) }
            FocusTimerIntents.ACTION_DISMISS -> launchAsync(context) { dismiss(timerId) }
        }
    }
}

internal class FocusBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        launchAsync(context) { reconcile() }
    }
}

private fun BroadcastReceiver.launchAsync(
    context: Context,
    block: suspend FocusTimerCoordinator.() -> Unit,
) {
    val pendingResult = goAsync()
    val application = context.applicationContext as? OpenMinkApplication
    if (application == null) {
        pendingResult.finish()
        return
    }
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            application.minkBoardRuntime.focusTimerCoordinator.block()
        } finally {
            pendingResult.finish()
        }
    }
}
