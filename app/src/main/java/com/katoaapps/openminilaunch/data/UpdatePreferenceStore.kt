package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class UpdatePreferenceStore(private val prefs: SharedPreferences) {
    var checksEnabled by mutableStateOf(prefs.getBoolean(CHECKS_ENABLED_KEY, true))
        private set
    var latestReleaseTag by mutableStateOf(prefs.getString(LATEST_RELEASE_TAG_KEY, null))
        private set

    fun updateChecksEnabled(enabled: Boolean) {
        checksEnabled = enabled
        val editor = prefs.edit().putBoolean(CHECKS_ENABLED_KEY, enabled)
        if (enabled) editor.remove(LAST_CHECK_KEY)
        editor.apply()
    }

    fun shouldCheck(nowMillis: Long): Boolean {
        if (!checksEnabled) return false
        val lastCheck = prefs.getLong(LAST_CHECK_KEY, 0L)
        return nowMillis < lastCheck || nowMillis - lastCheck >= CHECK_INTERVAL_MILLIS
    }

    fun markCheckStarted(nowMillis: Long) {
        prefs.edit().putLong(LAST_CHECK_KEY, nowMillis).apply()
    }

    fun cacheLatestReleaseTag(tag: String) {
        latestReleaseTag = tag
        prefs.edit().putString(LATEST_RELEASE_TAG_KEY, tag).apply()
    }

    fun shouldShowReminder(releaseTag: String, nowMillis: Long): Boolean {
        val snoozedTag = prefs.getString(REMINDER_TAG_KEY, null)
        val snoozedUntil = prefs.getLong(REMINDER_UNTIL_KEY, 0L)
        return snoozedTag != releaseTag || nowMillis >= snoozedUntil
    }

    fun snoozeReminder(releaseTag: String, nowMillis: Long) {
        prefs.edit()
            .putString(REMINDER_TAG_KEY, releaseTag)
            .putLong(REMINDER_UNTIL_KEY, nowMillis + REMINDER_DELAY_MILLIS)
            .apply()
    }

    private companion object {
        const val CHECKS_ENABLED_KEY = "github_update_checks_enabled"
        const val CHECK_INTERVAL_MILLIS = 12 * 60 * 60 * 1_000L
        const val LAST_CHECK_KEY = "last_github_release_check"
        const val LATEST_RELEASE_TAG_KEY = "latest_github_release_tag"
        const val REMINDER_DELAY_MILLIS = 24 * 60 * 60 * 1_000L
        const val REMINDER_TAG_KEY = "github_update_reminder_tag"
        const val REMINDER_UNTIL_KEY = "github_update_reminder_until"
    }
}
