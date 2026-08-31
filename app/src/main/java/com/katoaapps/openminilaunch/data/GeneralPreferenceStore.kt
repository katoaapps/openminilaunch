package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class GeneralPreferenceStore(private val prefs: SharedPreferences) {
    var onboardingComplete by mutableStateOf(prefs.getBoolean(ONBOARDING_COMPLETE_KEY, false))
        private set
    var hasOpenedClockFromDate by mutableStateOf(prefs.getBoolean(CLOCK_DATE_OPENED_KEY, false))
        private set
    var openSoftwareKeyboardOnHome by mutableStateOf(
        prefs.getBoolean(OPEN_SOFTWARE_KEYBOARD_ON_HOME_KEY, true),
    )
        private set

    fun completeOnboarding() {
        onboardingComplete = true
        prefs.edit().putBoolean(ONBOARDING_COMPLETE_KEY, true).apply()
    }

    fun markClockOpenedFromDate() {
        hasOpenedClockFromDate = true
        prefs.edit().putBoolean(CLOCK_DATE_OPENED_KEY, true).apply()
    }

    fun updateOpenSoftwareKeyboardOnHome(enabled: Boolean) {
        openSoftwareKeyboardOnHome = enabled
        prefs.edit().putBoolean(OPEN_SOFTWARE_KEYBOARD_ON_HOME_KEY, enabled).apply()
    }

    fun hasSeenUpdate(updateId: String): Boolean =
        updateId in prefs.getStringSet(SEEN_UPDATES_KEY, emptySet()).orEmpty()

    fun markUpdateSeen(updateId: String) {
        val seen = prefs.getStringSet(SEEN_UPDATES_KEY, emptySet()).orEmpty().toMutableSet()
        seen += updateId
        prefs.edit().putStringSet(SEEN_UPDATES_KEY, seen).apply()
    }

    private companion object {
        const val CLOCK_DATE_OPENED_KEY = "clock_date_opened"
        const val ONBOARDING_COMPLETE_KEY = "onboarding_complete_v2"
        const val OPEN_SOFTWARE_KEYBOARD_ON_HOME_KEY = "open_software_keyboard_on_home"
        const val SEEN_UPDATES_KEY = "seen_updates"
    }
}
