package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.katoaapps.openminilaunch.model.DEFAULT_KEYBOARD_HEIGHT_DP
import com.katoaapps.openminilaunch.model.DEFAULT_KEYBOARD_PRIMARY_ARGB
import com.katoaapps.openminilaunch.model.DEFAULT_KEYBOARD_SECONDARY_ARGB
import com.katoaapps.openminilaunch.model.LauncherKeyboardColorSource
import com.katoaapps.openminilaunch.model.LauncherKeyboardLayout
import com.katoaapps.openminilaunch.model.LauncherKeyboardPreferences
import com.katoaapps.openminilaunch.model.LauncherKeyboardStyle
import com.katoaapps.openminilaunch.model.LauncherKeyboardHeightPreset
import com.katoaapps.openminilaunch.model.normalizeLauncherKeyboardHeight

internal class LauncherKeyboardPreferenceStore(private val prefs: SharedPreferences) {
    var preferences by mutableStateOf(load())
        private set

    fun update(updated: LauncherKeyboardPreferences) {
        val normalized = updated.copy(heightDp = normalizeLauncherKeyboardHeight(updated.heightDp))
        preferences = normalized
        prefs.edit {
            putString(STYLE_KEY, normalized.style.name)
            putString(LAYOUT_KEY, normalized.layout.name)
            putString(COLOR_SOURCE_KEY, normalized.colorSource.name)
            putInt(CUSTOM_PRIMARY_KEY, normalized.customPrimaryArgb)
            putInt(CUSTOM_SECONDARY_KEY, normalized.customSecondaryArgb)
            putBoolean(SPLIT_WIDE_KEY, normalized.splitOnWideDisplays)
            putBoolean(HAPTIC_KEY, normalized.hapticFeedbackEnabled)
            putFloat(HEIGHT_KEY, normalized.heightDp)
            putString(PORTRAIT_HEIGHT_PRESET_KEY, normalized.portraitHeightPreset.name)
            normalized.keyColorOverrideArgb?.let { color ->
                putInt(KEY_COLOR_OVERRIDE_KEY, color)
            } ?: remove(KEY_COLOR_OVERRIDE_KEY)
        }
    }

    private fun load(): LauncherKeyboardPreferences {
        val savedHeight = normalizeLauncherKeyboardHeight(
            prefs.getFloat(HEIGHT_KEY, DEFAULT_KEYBOARD_HEIGHT_DP),
        )
        return LauncherKeyboardPreferences(
            style = prefs.enumValue(STYLE_KEY, LauncherKeyboardStyle.FLAT_PRIMARY),
            layout = prefs.enumValue(LAYOUT_KEY, LauncherKeyboardLayout.QWERTY),
            colorSource = prefs.enumValue(COLOR_SOURCE_KEY, LauncherKeyboardColorSource.THEME),
            customPrimaryArgb = prefs.getInt(CUSTOM_PRIMARY_KEY, DEFAULT_KEYBOARD_PRIMARY_ARGB),
            customSecondaryArgb = prefs.getInt(CUSTOM_SECONDARY_KEY, DEFAULT_KEYBOARD_SECONDARY_ARGB),
            keyColorOverrideArgb = if (prefs.contains(KEY_COLOR_OVERRIDE_KEY)) {
                prefs.getInt(KEY_COLOR_OVERRIDE_KEY, DEFAULT_KEYBOARD_PRIMARY_ARGB)
            } else {
                null
            },
            splitOnWideDisplays = prefs.getBoolean(SPLIT_WIDE_KEY, true),
            hapticFeedbackEnabled = prefs.getBoolean(HAPTIC_KEY, true),
            heightDp = savedHeight,
            // This preset intentionally starts at 40% for every existing installation. The key
            // only exists after the user chooses one of the new snapped sizes.
            portraitHeightPreset = prefs.enumValue(
                PORTRAIT_HEIGHT_PRESET_KEY,
                LauncherKeyboardHeightPreset.BALANCED,
            ),
        )
    }

    private inline fun <reified T : Enum<T>> SharedPreferences.enumValue(
        key: String,
        fallback: T,
    ): T = getString(key, null)
        ?.let { saved -> runCatching { enumValueOf<T>(saved) }.getOrNull() }
        ?: fallback

    private companion object {
        const val STYLE_KEY = "launcher_keyboard_style"
        const val LAYOUT_KEY = "launcher_keyboard_layout"
        const val COLOR_SOURCE_KEY = "launcher_keyboard_color_source"
        const val CUSTOM_PRIMARY_KEY = "launcher_keyboard_custom_primary"
        const val CUSTOM_SECONDARY_KEY = "launcher_keyboard_custom_secondary"
        const val KEY_COLOR_OVERRIDE_KEY = "launcher_keyboard_key_color_override"
        const val SPLIT_WIDE_KEY = "launcher_keyboard_split_wide"
        const val HAPTIC_KEY = "launcher_keyboard_haptic"
        const val HEIGHT_KEY = "launcher_keyboard_height_dp"
        const val PORTRAIT_HEIGHT_PRESET_KEY = "launcher_keyboard_portrait_height_preset"
    }
}
