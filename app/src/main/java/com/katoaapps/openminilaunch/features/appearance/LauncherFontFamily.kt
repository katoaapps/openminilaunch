package com.katoaapps.openminilaunch.features.appearance

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.FontSource
import java.io.File

internal fun launcherFontFamily(source: FontSource, customFontFile: File): FontFamily = when (source) {
    FontSource.DEFAULT -> GeistSans
    FontSource.SERIF -> FontFamily.Serif
    FontSource.MONOSPACE -> FontFamily.Monospace
    FontSource.CUSTOM -> runCatching {
        FontFamily(Typeface.createFromFile(customFontFile))
    }.getOrDefault(GeistSans)
}

internal fun launcherTypeface(
    context: Context,
    source: FontSource,
    customFontFile: File,
): Typeface = when (source) {
    FontSource.DEFAULT -> ResourcesCompat.getFont(context, R.font.geist_variable) ?: Typeface.DEFAULT
    FontSource.SERIF -> Typeface.SERIF
    FontSource.MONOSPACE -> Typeface.MONOSPACE
    FontSource.CUSTOM -> runCatching { Typeface.createFromFile(customFontFile) }
        .getOrElse { ResourcesCompat.getFont(context, R.font.geist_variable) ?: Typeface.DEFAULT }
}
