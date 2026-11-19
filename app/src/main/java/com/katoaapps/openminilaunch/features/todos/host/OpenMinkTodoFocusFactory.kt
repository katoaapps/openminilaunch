package com.katoaapps.openminilaunch.features.todos.host

import android.content.Context
import android.content.Intent
import com.katoaapps.minkboard.core.MinkBoardFocus
import com.katoaapps.minkboard.focus.android.AndroidMinkBoardFocusRuntime
import com.katoaapps.minkboard.focus.android.MinkBoardFocusAndroidConfig
import com.katoaapps.minkboard.focus.android.MinkBoardFocusNotificationChannels
import com.katoaapps.minkboard.focus.android.MinkBoardFocusNotificationIds
import com.katoaapps.minkboard.focus.android.MinkBoardFocusNotificationText
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.launcher.MainActivity

/** OpenMink resources and navigation hook for the reusable MinkBoard focus SDK. */
internal fun createOpenMinkTodoFocusRuntime(
    context: Context,
    store: MinkBoardFocus,
): AndroidMinkBoardFocusRuntime {
    val applicationContext = context.applicationContext
    return AndroidMinkBoardFocusRuntime.create(
        context = applicationContext,
        store = store,
        config = MinkBoardFocusAndroidConfig(
            smallIconResId = R.drawable.ic_minkboard_notification,
            text = applicationContext.focusNotificationText(),
            channels = MinkBoardFocusNotificationChannels(
                running = "openmink_minkboard_focus_running_v1",
                finishedAlerting = "openmink_minkboard_focus_finished_silent_v1",
                finishedQuiet = "openmink_minkboard_focus_finished_quiet_v1",
                retired = setOf("openmink_minkboard_focus_finished_v1"),
            ),
            ids = MinkBoardFocusNotificationIds(
                running = 43_001,
                finished = 43_002,
                openRequest = 43_101,
                stopRequest = 43_102,
                dismissRequest = 43_103,
                alarmRequest = 41_001,
            ),
            createOpenIntent = { host -> Intent(host, MainActivity::class.java) },
        ),
    )
}

private fun Context.focusNotificationText() = MinkBoardFocusNotificationText(
    runningChannelName = getString(R.string.minkboard_focus_running_channel_name),
    runningChannelDescription = getString(R.string.minkboard_focus_running_channel_description),
    finishedChannelName = getString(R.string.minkboard_focus_finished_channel_name),
    finishedChannelDescription = getString(R.string.minkboard_focus_finished_channel_description),
    quietFinishedChannelName = getString(R.string.minkboard_focus_finished_quiet_channel_name),
    quietFinishedChannelDescription = getString(
        R.string.minkboard_focus_finished_quiet_channel_description,
    ),
    genericRunningTitle = getString(R.string.minkboard_focus_generic_running_title),
    genericFinishedTitle = getString(R.string.minkboard_focus_generic_finished_title),
    focusingTitle = { getString(R.string.minkboard_focus_running_title, it) },
    finishedTitle = { getString(R.string.minkboard_focus_finished_title, it) },
    runningBody = getString(R.string.minkboard_focus_running_body),
    finishedBody = getString(R.string.minkboard_focus_finished_body),
    stopAction = getString(R.string.minkboard_focus_stop_action),
    dismissAction = getString(R.string.minkboard_focus_dismiss_action),
)
