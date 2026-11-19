package com.katoaapps.minkboard.focus.android

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.katoaapps.minkboard.model.FocusSession

internal class AlarmManagerMinkBoardFocusScheduler(
    context: Context,
    private val ids: MinkBoardFocusNotificationIds,
) : MinkBoardFocusScheduler {
    private val applicationContext = context.applicationContext
    private val alarmManager = applicationContext.getSystemService(AlarmManager::class.java)

    override fun schedule(session: FocusSession): MinkBoardFocusSchedulePrecision {
        val operation = alarmPendingIntent(session)
        val canBeExact = canScheduleExactTimers()
        if (canBeExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                session.endsAtEpochMillis,
                operation,
            )
            return MinkBoardFocusSchedulePrecision.Exact
        }
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            session.endsAtEpochMillis,
            operation,
        )
        return MinkBoardFocusSchedulePrecision.Inexact
    }

    override fun cancel(session: FocusSession) {
        alarmManager.cancel(alarmPendingIntent(session))
    }

    fun canScheduleExactTimers(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun alarmPendingIntent(session: FocusSession): PendingIntent = PendingIntent.getBroadcast(
        applicationContext,
        ids.alarmRequest,
        Intent(applicationContext, MinkBoardFocusAlarmReceiver::class.java).apply {
            action = MinkBoardFocusIntents.ACTION_ALARM
            data = MinkBoardFocusIntents.data(MinkBoardFocusIntents.ACTION_ALARM, session.id)
            putExtra(MinkBoardFocusIntents.EXTRA_TIMER_ID, session.id.value)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
