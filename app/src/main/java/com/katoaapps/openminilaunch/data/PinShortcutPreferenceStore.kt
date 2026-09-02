package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.katoaapps.openminilaunch.model.PinShortcutRequestPresentation

internal class PinShortcutPreferenceStore(private val prefs: SharedPreferences) {
    var requestPresentation by mutableStateOf(loadRequestPresentation())
        private set

    fun updateRequestPresentation(presentation: PinShortcutRequestPresentation) {
        requestPresentation = presentation
        prefs.edit { putString(REQUEST_PRESENTATION_KEY, presentation.name) }
    }

    private fun loadRequestPresentation(): PinShortcutRequestPresentation = runCatching {
        PinShortcutRequestPresentation.valueOf(
            prefs.getString(
                REQUEST_PRESENTATION_KEY,
                PinShortcutRequestPresentation.FULL_PAGE.name,
            ) ?: PinShortcutRequestPresentation.FULL_PAGE.name,
        )
    }.getOrDefault(PinShortcutRequestPresentation.FULL_PAGE)

    private companion object {
        const val REQUEST_PRESENTATION_KEY = "pin_shortcut_request_presentation"
    }
}
