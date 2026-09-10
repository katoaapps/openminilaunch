package com.katoaapps.openminilaunch.ui.components

import androidx.compose.ui.graphics.Color
import com.katoaapps.openminilaunch.model.IconAppearance
import com.katoaapps.openminilaunch.model.IconSource

/** Tint for app artwork; null tells the renderer to keep the app's original icon. */
internal fun IconAppearance.minkAppIconTint(automaticTint: Color?): Color? {
    if (source != IconSource.MINK) return null
    return minkIconColorArgb?.let(::Color) ?: automaticTint
}

/** Tint for Mink-owned vector icons, which have no system or icon-pack replacement. */
internal fun IconAppearance.minkBuiltInIconTint(automaticTint: Color): Color {
    if (source != IconSource.MINK) return automaticTint
    return minkIconColorArgb?.let(::Color) ?: automaticTint
}
