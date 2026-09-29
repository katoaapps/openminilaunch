package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Locally remembers which launcher activities should open through Android's bubble UI. */
internal class AppBubblePreferenceStore(private val prefs: SharedPreferences) {
    var automaticTargetKeys by mutableStateOf(
        prefs.getStringSet(AUTOMATIC_TARGETS_KEY, emptySet()).orEmpty().toSet(),
    )
        private set

    fun setAutomatic(targetKey: String, enabled: Boolean) {
        val updated = automaticTargetKeys.toMutableSet().apply {
            if (enabled) add(targetKey) else remove(targetKey)
        }.toSet()
        automaticTargetKeys = updated
        prefs.edit().putStringSet(AUTOMATIC_TARGETS_KEY, updated).apply()
    }

    fun replaceAutomaticTargets(targetKeys: Collection<String>) {
        val updated = targetKeys.toSet()
        automaticTargetKeys = updated
        prefs.edit().putStringSet(AUTOMATIC_TARGETS_KEY, updated).apply()
    }

    private companion object {
        const val AUTOMATIC_TARGETS_KEY = "app_bubble_automatic_targets"
    }
}
