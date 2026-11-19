package com.katoaapps.minkboard.focus.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MinkBoardFocusAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != MinkBoardFocusIntents.ACTION_ALARM) return
        val timerId = MinkBoardFocusIntents.timerId(intent) ?: return
        launchAsync(context) { complete(timerId) }
    }
}

class MinkBoardFocusActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timerId = MinkBoardFocusIntents.timerId(intent) ?: return
        when (intent.action) {
            MinkBoardFocusIntents.ACTION_STOP -> launchAsync(context) { stop(timerId) }
            MinkBoardFocusIntents.ACTION_DISMISS -> launchAsync(context) { dismiss(timerId) }
        }
    }
}

class MinkBoardFocusBootReceiver : BroadcastReceiver() {
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
    block: suspend MinkBoardFocusTimerCoordinator.() -> Unit,
) {
    val pendingResult = goAsync()
    val owner = context.applicationContext as? MinkBoardFocusRuntimeOwner
    if (owner == null) {
        pendingResult.finish()
        return
    }
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            owner.minkBoardFocusRuntime.coordinator.block()
        } finally {
            pendingResult.finish()
        }
    }
}
