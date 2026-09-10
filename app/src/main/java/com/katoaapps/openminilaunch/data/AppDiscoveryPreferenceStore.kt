package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/** Preferences shared by Magic Box app search and the All Apps carousel. */
internal class AppDiscoveryPreferenceStore(private val prefs: SharedPreferences) {
    var includeAppShortcuts by mutableStateOf(
        prefs.getBoolean(INCLUDE_APP_SHORTCUTS_KEY, false),
    )
        private set

    fun updateIncludeAppShortcuts(enabled: Boolean) {
        includeAppShortcuts = enabled
        prefs.edit { putBoolean(INCLUDE_APP_SHORTCUTS_KEY, enabled) }
    }

    private companion object {
        const val INCLUDE_APP_SHORTCUTS_KEY = "include_app_shortcuts_in_discovery"
    }
}
