package com.katoaapps.openminilaunch.ui.appgrid

import androidx.compose.ui.graphics.Color
import com.katoaapps.openminilaunch.model.AppFolderColorMode
import com.katoaapps.openminilaunch.model.AppFolderConfig

internal fun AppFolderConfig.backgroundColor(
    defaultColor: Color,
    themeColor: Color,
): Color = when (colorMode) {
    AppFolderColorMode.DEFAULT -> defaultColor
    AppFolderColorMode.THEME -> themeColor
    AppFolderColorMode.CUSTOM -> colorArgb?.let(::Color) ?: defaultColor
}
