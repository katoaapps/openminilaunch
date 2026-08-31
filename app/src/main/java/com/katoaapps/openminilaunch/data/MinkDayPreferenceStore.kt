package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.katoaapps.openminilaunch.model.MAX_SOCIAL_GOAL_HOURS
import com.katoaapps.openminilaunch.model.MIN_SOCIAL_GOAL_HOURS
import com.katoaapps.openminilaunch.model.MinkAppPauseMode

internal class MinkDayPreferenceStore(private val prefs: SharedPreferences) {
    val socialPackages = mutableStateListOf<String>()
    var usesAutomaticSocialApps by mutableStateOf(!prefs.contains(SOCIAL_PACKAGES_KEY))
        private set
    var socialGoalMinutes by mutableStateOf(
        normalizedSocialGoalMinutes(prefs.getInt(SOCIAL_GOAL_MINUTES_KEY, 60)),
    )
        private set
    val socialGoalHours: Int
        get() = socialGoalMinutes / 60
    var pauseMode by mutableStateOf(
        runCatching {
            MinkAppPauseMode.valueOf(
                prefs.getString(MINK_APP_PAUSE_MODE_KEY, MinkAppPauseMode.NEVER.name)
                    ?: MinkAppPauseMode.NEVER.name,
            )
        }.getOrDefault(MinkAppPauseMode.NEVER),
    )
        private set

    init {
        prefs.edit().putInt(SOCIAL_GOAL_MINUTES_KEY, socialGoalMinutes).apply()
    }

    fun restore(packageNames: List<String>) {
        socialPackages.addAll(packageNames)
    }

    fun updateGoalHours(hours: Int) {
        if (hours !in MIN_SOCIAL_GOAL_HOURS..MAX_SOCIAL_GOAL_HOURS) return
        socialGoalMinutes = hours * 60
        prefs.edit().putInt(SOCIAL_GOAL_MINUTES_KEY, socialGoalMinutes).apply()
    }

    fun updatePauseMode(mode: MinkAppPauseMode) {
        pauseMode = mode
        prefs.edit().putString(MINK_APP_PAUSE_MODE_KEY, mode.name).apply()
    }

    fun reconcileApps(installedPackages: Set<String>) {
        if (usesAutomaticSocialApps) return
        val changed = socialPackages.removeAll { it !in installedPackages }
        if (changed) saveApps()
    }

    fun replaceApps(packageNames: Set<String>) {
        usesAutomaticSocialApps = false
        socialPackages.clear()
        socialPackages.addAll(packageNames.sorted())
        saveApps()
    }

    fun clearApps() {
        usesAutomaticSocialApps = true
        socialPackages.clear()
        prefs.edit().remove(SOCIAL_PACKAGES_KEY).apply()
    }

    private fun saveApps() {
        prefs.edit().putStringSet(SOCIAL_PACKAGES_KEY, socialPackages.toSet()).apply()
    }

    private companion object {
        const val MINK_APP_PAUSE_MODE_KEY = "mink_app_pause_mode"
        const val SOCIAL_GOAL_MINUTES_KEY = "social_goal_minutes"
        const val SOCIAL_PACKAGES_KEY = "social_packages"
    }
}

internal fun normalizedSocialGoalMinutes(savedMinutes: Int): Int {
    val roundedHours = ((savedMinutes.coerceAtLeast(0) + 30) / 60)
        .coerceIn(MIN_SOCIAL_GOAL_HOURS, MAX_SOCIAL_GOAL_HOURS)
    return roundedHours * 60
}
