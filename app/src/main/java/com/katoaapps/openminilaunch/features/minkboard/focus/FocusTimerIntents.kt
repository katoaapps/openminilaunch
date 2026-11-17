package com.katoaapps.openminilaunch.features.minkboard.focus

import android.content.Intent
import android.net.Uri
import com.katoaapps.minkboard.model.FocusTimerId

internal object FocusTimerIntents {
    const val ACTION_ALARM = "com.katoaapps.openminilaunch.minkboard.focus.ALARM"
    const val ACTION_STOP = "com.katoaapps.openminilaunch.minkboard.focus.STOP"
    const val ACTION_DISMISS = "com.katoaapps.openminilaunch.minkboard.focus.DISMISS"
    const val ACTION_OPEN = "com.katoaapps.openminilaunch.minkboard.focus.OPEN"

    const val EXTRA_TIMER_ID = "focus_timer_id"
    const val EXTRA_BOARD_ID = "focus_board_id"
    const val EXTRA_CARD_ID = "focus_card_id"

    fun timerId(intent: Intent): FocusTimerId? = intent.getStringExtra(EXTRA_TIMER_ID)
        ?.let { value -> runCatching { FocusTimerId(value) }.getOrNull() }

    fun data(action: String, timerId: FocusTimerId): Uri = Uri.Builder()
        .scheme("openmink")
        .authority("minkboard-focus")
        .appendPath(action.substringAfterLast('.').lowercase())
        .appendPath(timerId.value)
        .build()
}
