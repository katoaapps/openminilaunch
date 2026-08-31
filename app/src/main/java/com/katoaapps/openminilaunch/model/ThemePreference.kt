package com.katoaapps.openminilaunch.model

import androidx.annotation.StringRes
import com.katoaapps.openminilaunch.R

enum class ThemePreference(@param:StringRes val labelRes: Int) {
    SYSTEM(R.string.theme_system),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark),
}
