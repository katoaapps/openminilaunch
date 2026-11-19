package com.katoaapps.openminilaunch.features.minkboard

import android.content.Context
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkboard.focus.AlarmManagerFocusTimerScheduler
import com.katoaapps.openminilaunch.features.minkboard.focus.AndroidFocusTimerNotifier
import com.katoaapps.openminilaunch.features.minkboard.focus.FocusNotificationText
import com.katoaapps.openminilaunch.features.minkboard.focus.FocusTimerCoordinator
import com.katoaapps.minkboard.core.FocusTimerStore

/** Android focus-timer services and localized notification copy. */
internal class OpenMinkFocusTimerGraph(
    context: Context,
    store: FocusTimerStore,
) {
    private val applicationContext = context.applicationContext
    private val scheduler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AlarmManagerFocusTimerScheduler(applicationContext)
    }

    val notifier by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AndroidFocusTimerNotifier(
            applicationContext,
            applicationContext.focusNotificationText(),
        )
    }

    val coordinator by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        FocusTimerCoordinator(store, scheduler, notifier)
    }
}

private fun Context.focusNotificationText() = FocusNotificationText(
    runningChannelName = getString(R.string.minkboard_focus_running_channel_name),
    runningChannelDescription = getString(R.string.minkboard_focus_running_channel_description),
    finishedChannelName = getString(R.string.minkboard_focus_finished_channel_name),
    finishedChannelDescription = getString(R.string.minkboard_focus_finished_channel_description),
    quietFinishedChannelName = getString(R.string.minkboard_focus_finished_quiet_channel_name),
    quietFinishedChannelDescription = getString(R.string.minkboard_focus_finished_quiet_channel_description),
    genericRunningTitle = getString(R.string.minkboard_focus_generic_running_title),
    genericFinishedTitle = getString(R.string.minkboard_focus_generic_finished_title),
    focusingTitle = { getString(R.string.minkboard_focus_running_title, it) },
    finishedTitle = { getString(R.string.minkboard_focus_finished_title, it) },
    runningBody = getString(R.string.minkboard_focus_running_body),
    finishedBody = getString(R.string.minkboard_focus_finished_body),
    stopAction = getString(R.string.minkboard_focus_stop_action),
    dismissAction = getString(R.string.minkboard_focus_dismiss_action),
)
