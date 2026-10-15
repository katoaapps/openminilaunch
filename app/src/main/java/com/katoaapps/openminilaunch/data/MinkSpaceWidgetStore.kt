package com.katoaapps.openminilaunch.data

import android.appwidget.AppWidgetManager
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/** Local Android widget binding used only by the optional MinkSpace Widget mini-app. */
internal class MinkSpaceWidgetStore(private val prefs: SharedPreferences) {
    var appWidgetId by mutableStateOf(loadAppWidgetId())
        private set

    fun replace(appWidgetId: Int) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        this.appWidgetId = appWidgetId
        prefs.edit { putInt(KEY_APP_WIDGET_ID, appWidgetId) }
    }

    fun clear() {
        appWidgetId = null
        prefs.edit { remove(KEY_APP_WIDGET_ID) }
    }

    private fun loadAppWidgetId(): Int? = prefs
        .getInt(KEY_APP_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        .takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID }

    private companion object {
        const val KEY_APP_WIDGET_ID = "mink_space_app_widget_id"
    }
}
