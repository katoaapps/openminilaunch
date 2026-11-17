package com.katoaapps.openminilaunch.features.minkboard.focus

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.katoaapps.minkboard.model.FocusSession

internal class AlarmManagerFocusTimerScheduler(context: Context) : FocusTimerScheduler {
    private val applicationContext = context.applicationContext
    private val alarmManager = applicationContext.getSystemService(AlarmManager::class.java)

    override fun schedule(session: FocusSession): FocusTimerSchedulePrecision {
        val operation = alarmPendingIntent(session)
        val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        if (canBeExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                session.endsAtEpochMillis,
                operation,
            )
            return FocusTimerSchedulePrecision.Exact
        }
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            session.endsAtEpochMillis,
            operation,
        )
        return FocusTimerSchedulePrecision.Inexact
    }

    override fun cancel(session: FocusSession) {
        alarmManager.cancel(alarmPendingIntent(session))
    }

    private fun alarmPendingIntent(session: FocusSession): PendingIntent = PendingIntent.getBroadcast(
        applicationContext,
        REQUEST_ALARM,
        Intent(applicationContext, FocusAlarmReceiver::class.java).apply {
            action = FocusTimerIntents.ACTION_ALARM
            data = FocusTimerIntents.data(FocusTimerIntents.ACTION_ALARM, session.id)
            putExtra(FocusTimerIntents.EXTRA_TIMER_ID, session.id.value)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val REQUEST_ALARM = 41_001
    }
}
