package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/** Appearance and optional entry points owned by the Quick Drawer surface. */
internal class QuickDrawerPreferenceStore(private val prefs: SharedPreferences) {
    var showMinkPadButton by mutableStateOf(
        prefs.getBoolean(SHOW_MINKPAD_BUTTON_KEY, true),
    )
        private set

    var showPrivateSpaceButton by mutableStateOf(
        prefs.getBoolean(SHOW_PRIVATE_SPACE_BUTTON_KEY, true),
    )
        private set

    var showWorkProfileButton by mutableStateOf(
        prefs.getBoolean(SHOW_WORK_PROFILE_BUTTON_KEY, true),
    )
        private set

    var colorArgb by mutableStateOf(
        if (prefs.contains(COLOR_ARGB_KEY)) prefs.getInt(COLOR_ARGB_KEY, 0) else null,
    )
        private set

    fun updateShowMinkPadButton(enabled: Boolean) {
        showMinkPadButton = enabled
        prefs.edit { putBoolean(SHOW_MINKPAD_BUTTON_KEY, enabled) }
    }

    fun updateShowPrivateSpaceButton(enabled: Boolean) {
        showPrivateSpaceButton = enabled
        prefs.edit { putBoolean(SHOW_PRIVATE_SPACE_BUTTON_KEY, enabled) }
    }

    fun updateShowWorkProfileButton(enabled: Boolean) {
        showWorkProfileButton = enabled
        prefs.edit { putBoolean(SHOW_WORK_PROFILE_BUTTON_KEY, enabled) }
    }

    fun setColor(argb: Int?) {
        colorArgb = argb?.or(0xFF000000.toInt())
        prefs.edit {
            colorArgb?.let { putInt(COLOR_ARGB_KEY, it) } ?: remove(COLOR_ARGB_KEY)
        }
    }

    private companion object {
        const val SHOW_MINKPAD_BUTTON_KEY = "quick_drawer_show_minkpad_button"
        const val SHOW_PRIVATE_SPACE_BUTTON_KEY = "quick_drawer_show_private_space_button"
        const val SHOW_WORK_PROFILE_BUTTON_KEY = "quick_drawer_show_work_profile_button"
        const val COLOR_ARGB_KEY = "quick_drawer_color_argb"
    }
}
