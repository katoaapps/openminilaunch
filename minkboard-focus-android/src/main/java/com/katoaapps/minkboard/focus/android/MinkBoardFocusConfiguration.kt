package com.katoaapps.minkboard.focus.android

import android.content.Context
import android.content.Intent
import com.katoaapps.minkboard.core.MinkBoardFocus

data class MinkBoardFocusNotificationText(
    val runningChannelName: String,
    val runningChannelDescription: String,
    val finishedChannelName: String,
    val finishedChannelDescription: String,
    val quietFinishedChannelName: String,
    val quietFinishedChannelDescription: String,
    val genericRunningTitle: String,
    val genericFinishedTitle: String,
    val focusingTitle: (String) -> String,
    val finishedTitle: (String) -> String,
    val runningBody: String,
    val finishedBody: String,
    val stopAction: String,
    val dismissAction: String,
)

data class MinkBoardFocusNotificationChannels(
    val running: String,
    val finishedAlerting: String,
    val finishedQuiet: String,
    val retired: Set<String> = emptySet(),
)

data class MinkBoardFocusNotificationIds(
    val running: Int,
    val finished: Int,
    val openRequest: Int,
    val stopRequest: Int,
    val dismissRequest: Int,
    val alarmRequest: Int,
)

/** Host-owned presentation hooks for the reusable Android focus runtime. */
data class MinkBoardFocusAndroidConfig(
    val smallIconResId: Int,
    val text: MinkBoardFocusNotificationText,
    val channels: MinkBoardFocusNotificationChannels,
    val ids: MinkBoardFocusNotificationIds,
    val createOpenIntent: (Context) -> Intent,
)

/**
 * Application-scoped SDK runtime. The library owns timer execution while the host supplies only
 * its presentation resources and destination activity.
 */
class AndroidMinkBoardFocusRuntime private constructor(
    internal val store: MinkBoardFocus,
    internal val notifier: AndroidMinkBoardFocusNotifier,
    val coordinator: MinkBoardFocusTimerCoordinator,
    private val scheduler: AlarmManagerMinkBoardFocusScheduler,
) {
    fun canScheduleExactTimers(): Boolean = scheduler.canScheduleExactTimers()

    companion object {
        fun create(
            context: Context,
            store: MinkBoardFocus,
            config: MinkBoardFocusAndroidConfig,
        ): AndroidMinkBoardFocusRuntime {
            val applicationContext = context.applicationContext
            val scheduler = AlarmManagerMinkBoardFocusScheduler(applicationContext, config.ids)
            val notifier = AndroidMinkBoardFocusNotifier(applicationContext, config)
            return AndroidMinkBoardFocusRuntime(
                store = store,
                notifier = notifier,
                coordinator = MinkBoardFocusTimerCoordinator(store, scheduler, notifier),
                scheduler = scheduler,
            )
        }
    }
}

/** Required by manifest-created SDK services and receivers. */
interface MinkBoardFocusRuntimeOwner {
    val minkBoardFocusRuntime: AndroidMinkBoardFocusRuntime
}
