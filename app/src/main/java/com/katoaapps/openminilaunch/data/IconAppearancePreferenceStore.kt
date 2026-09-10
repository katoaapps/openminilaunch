package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.katoaapps.openminilaunch.model.IconAppearance
import com.katoaapps.openminilaunch.model.IconSource

internal class IconAppearancePreferenceStore(private val prefs: SharedPreferences) {
    var appearance by mutableStateOf(load())
        private set

    fun useMinkIcons() = save(appearance.copy(source = IconSource.MINK, iconPackPackage = null))

    fun useSystemIcons() = save(appearance.copy(source = IconSource.SYSTEM, iconPackPackage = null))

    fun useIconPack(packageName: String) {
        save(appearance.copy(source = IconSource.ICON_PACK, iconPackPackage = packageName))
    }

    fun setMinkIconColor(argb: Int?) {
        save(appearance.copy(minkIconColorArgb = argb?.or(0xFF000000.toInt())))
    }

    private fun load(): IconAppearance {
        val source = runCatching {
            IconSource.valueOf(prefs.getString(SOURCE_KEY, IconSource.MINK.name).orEmpty())
        }.getOrDefault(IconSource.MINK)
        val packageName = prefs.getString(PACKAGE_KEY, null)
        val minkIconColor = if (prefs.contains(MINK_COLOR_KEY)) {
            prefs.getInt(MINK_COLOR_KEY, 0)
        } else {
            null
        }
        return if (source == IconSource.ICON_PACK && packageName.isNullOrBlank()) {
            IconAppearance(minkIconColorArgb = minkIconColor)
        } else {
            IconAppearance(source, packageName, minkIconColor)
        }
    }

    private fun save(value: IconAppearance) {
        appearance = value
        prefs.edit()
            .putString(SOURCE_KEY, value.source.name)
            .apply {
                value.iconPackPackage?.let { putString(PACKAGE_KEY, it) } ?: remove(PACKAGE_KEY)
                value.minkIconColorArgb?.let { putInt(MINK_COLOR_KEY, it) } ?: remove(MINK_COLOR_KEY)
            }
            .apply()
    }

    private companion object {
        const val PACKAGE_KEY = "icon_pack_package"
        const val SOURCE_KEY = "icon_source"
        const val MINK_COLOR_KEY = "mink_icon_color"
    }
}
