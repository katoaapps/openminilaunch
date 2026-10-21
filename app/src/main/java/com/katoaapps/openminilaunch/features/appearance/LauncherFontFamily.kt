package com.katoaapps.openminilaunch.features.appearance

import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import com.katoaapps.openminilaunch.model.FontSource
import java.io.File

internal fun launcherFontFamily(source: FontSource, customFontFile: File): FontFamily = when (source) {
    FontSource.DEFAULT -> FontFamily.Default
    FontSource.SERIF -> FontFamily.Serif
    FontSource.MONOSPACE -> FontFamily.Monospace
    FontSource.CUSTOM -> runCatching {
        FontFamily(Typeface.createFromFile(customFontFile))
    }.getOrDefault(FontFamily.Default)
}
