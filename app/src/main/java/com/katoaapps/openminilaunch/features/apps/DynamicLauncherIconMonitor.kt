package com.katoaapps.openminilaunch.features.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import java.time.LocalDate

/** Invalidates launcher artwork when time or locale changes can alter an app's dynamic icon. */
internal class DynamicLauncherIconMonitor(
    context: Context,
    private val onRefreshNeeded: () -> Unit,
) {
    private var observedDate = LocalDate.now()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            observedDate = LocalDate.now()
            onRefreshNeeded()
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_LOCALE_CHANGED)
        }
        ContextCompat.registerReceiver(
            context.applicationContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    /** Covers a midnight transition that occurred while the process was suspended. */
    fun refreshIfDateChanged() {
        val currentDate = LocalDate.now()
        if (currentDate != observedDate) {
            observedDate = currentDate
            onRefreshNeeded()
        }
    }
}
