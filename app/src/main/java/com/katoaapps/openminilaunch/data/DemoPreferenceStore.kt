package com.katoaapps.openminilaunch.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.katoaapps.openminilaunch.features.demo.DemoHomeData
import com.katoaapps.openminilaunch.features.demo.DemoHomeProfile
import com.katoaapps.openminilaunch.model.Shortcut

internal class DemoPreferenceStore(
    private val context: Context,
    private val prefs: SharedPreferences,
    private val todoStore: TodoStore,
) {
    var profile by mutableStateOf(
        DemoHomeProfile.fromStoredName(prefs.getString(DEMO_HOME_PROFILE_KEY, null)),
    )
        private set
    var enabled by mutableStateOf(prefs.getBoolean(DEMO_SEARCH_DATA_KEY, false))
        private set
    private var panelColorArgb by mutableIntStateOf(
        ContextCompat.getColor(context, profile.panelColorRes),
    )
    private var backgroundColorArgb by mutableStateOf<Int?>(
        ContextCompat.getColor(context, profile.backgroundColorRes),
    )

    fun applyIfEnabled() {
        if (enabled) applyProfile(profile)
    }

    fun toggle(): Boolean {
        enabled = !enabled
        if (enabled) applyProfile(profile) else todoStore.restoreSaved()
        prefs.edit().putBoolean(DEMO_SEARCH_DATA_KEY, enabled).apply()
        return enabled
    }

    fun selectProfile(newProfile: DemoHomeProfile) {
        if (!enabled || newProfile == profile) return
        profile = newProfile
        applyProfile(newProfile)
        prefs.edit().putString(DEMO_HOME_PROFILE_KEY, newProfile.name).apply()
    }

    fun setPanelColor(argb: Int): Boolean {
        if (!enabled) return false
        panelColorArgb = argb or 0xFF000000.toInt()
        return true
    }

    fun setBackgroundColor(argb: Int?): Boolean {
        if (!enabled) return false
        backgroundColorArgb = argb?.or(0xFF000000.toInt())
        return true
    }

    fun effectivePanelColor(normalColor: Int): Int = if (enabled) panelColorArgb else normalColor

    fun effectiveBackgroundColor(normalColor: Int?): Int? =
        if (enabled) backgroundColorArgb else normalColor

    fun effectiveShortcutOrder(normalOrder: List<Shortcut>): List<Shortcut> =
        if (enabled) DemoHomeData.shortcutOrder(profile) else normalOrder

    private fun applyProfile(newProfile: DemoHomeProfile) {
        panelColorArgb = ContextCompat.getColor(context, newProfile.panelColorRes)
        backgroundColorArgb = ContextCompat.getColor(context, newProfile.backgroundColorRes)
        todoStore.showDemo(DemoHomeData.todos(newProfile))
    }

    private companion object {
        const val DEMO_HOME_PROFILE_KEY = "demo_home_profile"
        const val DEMO_SEARCH_DATA_KEY = "demo_search_data_enabled"
    }
}
