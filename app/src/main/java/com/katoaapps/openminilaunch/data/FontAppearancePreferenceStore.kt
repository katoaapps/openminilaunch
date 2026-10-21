package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.katoaapps.openminilaunch.model.FontAppearance
import com.katoaapps.openminilaunch.model.FontSource
import com.katoaapps.openminilaunch.model.normalizeFontSizeScale

internal class FontAppearancePreferenceStore(private val prefs: SharedPreferences) {
    var appearance by mutableStateOf(load())
        private set

    fun useSource(source: FontSource) = save(appearance.copy(source = source))

    fun useCustomFont(displayName: String?) = save(
        appearance.copy(source = FontSource.CUSTOM, customFontName = displayName),
    )

    fun setSizeScale(scale: Float) = save(appearance.copy(sizeScale = normalizeFontSizeScale(scale)))

    fun setColor(argb: Int?) = save(appearance.copy(colorArgb = argb?.or(0xFF000000.toInt())))

    private fun load(): FontAppearance {
        val source = runCatching {
            FontSource.valueOf(prefs.getString(SOURCE_KEY, FontSource.DEFAULT.name).orEmpty())
        }.getOrDefault(FontSource.DEFAULT)
        return FontAppearance(
            source = source,
            sizeScale = normalizeFontSizeScale(prefs.getFloat(SIZE_SCALE_KEY, 1f)),
            colorArgb = if (prefs.contains(COLOR_KEY)) prefs.getInt(COLOR_KEY, 0) else null,
            customFontName = prefs.getString(CUSTOM_NAME_KEY, null),
        )
    }

    private fun save(value: FontAppearance) {
        appearance = value
        prefs.edit()
            .putString(SOURCE_KEY, value.source.name)
            .putFloat(SIZE_SCALE_KEY, value.sizeScale)
            .apply {
                value.colorArgb?.let { putInt(COLOR_KEY, it) } ?: remove(COLOR_KEY)
                value.customFontName?.let { putString(CUSTOM_NAME_KEY, it) } ?: remove(CUSTOM_NAME_KEY)
            }
            .apply()
    }

    private companion object {
        const val SOURCE_KEY = "font_source"
        const val SIZE_SCALE_KEY = "font_size_scale"
        const val COLOR_KEY = "font_color"
        const val CUSTOM_NAME_KEY = "custom_font_name"
    }
}
