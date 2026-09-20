package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.katoaapps.openminilaunch.features.calendar.language.CalendarInputLanguage

internal class CalendarPreferenceStore(private val prefs: SharedPreferences) {
    var inputLanguage by mutableStateOf(
        CalendarInputLanguage.fromStorage(prefs.getString(INPUT_LANGUAGE_KEY, null)),
    )
        private set

    fun updateInputLanguage(language: CalendarInputLanguage) {
        inputLanguage = language
        prefs.edit().putString(INPUT_LANGUAGE_KEY, language.storageValue).apply()
    }

    private companion object {
        const val INPUT_LANGUAGE_KEY = "calendar_input_language"
    }
}
