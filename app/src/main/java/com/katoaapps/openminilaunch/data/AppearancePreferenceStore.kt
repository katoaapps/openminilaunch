package com.katoaapps.openminilaunch.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.ThemePreference

internal class AppearancePreferenceStore(
    context: Context,
    private val prefs: SharedPreferences,
) {
    var themePreference by mutableStateOf(
        runCatching {
            ThemePreference.valueOf(
                prefs.getString(THEME_PREFERENCE_KEY, ThemePreference.SYSTEM.name)
                    ?: ThemePreference.SYSTEM.name,
            )
        }.getOrDefault(ThemePreference.SYSTEM),
    )
        private set
    var hideStatusBar by mutableStateOf(prefs.getBoolean(HIDE_STATUS_BAR_KEY, true))
        private set
    var alignHomePanelBottom by mutableStateOf(prefs.getBoolean(ALIGN_HOME_PANEL_BOTTOM_KEY, false))
        private set
    var homePanelColorArgb by mutableIntStateOf(
        prefs.getInt(HOME_PANEL_COLOR_KEY, ContextCompat.getColor(context, R.color.mink_forest)),
    )
        private set
    var appBackgroundColorArgb by mutableStateOf(
        if (prefs.contains(APP_BACKGROUND_COLOR_KEY)) prefs.getInt(APP_BACKGROUND_COLOR_KEY, 0) else null,
    )
        private set

    fun setTheme(preference: ThemePreference) {
        themePreference = preference
        prefs.edit().putString(THEME_PREFERENCE_KEY, preference.name).apply()
    }

    fun updateHideStatusBar(enabled: Boolean) {
        hideStatusBar = enabled
        prefs.edit().putBoolean(HIDE_STATUS_BAR_KEY, enabled).apply()
    }

    fun updateAlignHomePanelBottom(enabled: Boolean) {
        alignHomePanelBottom = enabled
        prefs.edit().putBoolean(ALIGN_HOME_PANEL_BOTTOM_KEY, enabled).apply()
    }

    fun setHomePanelColor(argb: Int) {
        homePanelColorArgb = argb or 0xFF000000.toInt()
        prefs.edit().putInt(HOME_PANEL_COLOR_KEY, homePanelColorArgb).apply()
    }

    fun setAppBackgroundColor(argb: Int?) {
        appBackgroundColorArgb = argb?.or(0xFF000000.toInt())
        prefs.edit().apply {
            appBackgroundColorArgb?.let { putInt(APP_BACKGROUND_COLOR_KEY, it) }
                ?: remove(APP_BACKGROUND_COLOR_KEY)
        }.apply()
    }

    private companion object {
        const val ALIGN_HOME_PANEL_BOTTOM_KEY = "align_home_panel_bottom"
        const val APP_BACKGROUND_COLOR_KEY = "app_background_color"
        const val HIDE_STATUS_BAR_KEY = "hide_status_bar"
        const val HOME_PANEL_COLOR_KEY = "home_panel_color"
        const val THEME_PREFERENCE_KEY = "theme_preference"
    }
}
