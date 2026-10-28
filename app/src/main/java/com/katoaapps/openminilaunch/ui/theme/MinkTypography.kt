package com.katoaapps.openminilaunch.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import com.katoaapps.openminilaunch.features.appearance.GeistSans

/** Official Mink UI typography. Android supplies fallback glyphs when Geist lacks a script. */
private fun TextStyle.withGeist() = copy(fontFamily = GeistSans)

internal val MinkTypography: Typography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.withGeist(),
        displayMedium = base.displayMedium.withGeist(),
        displaySmall = base.displaySmall.withGeist(),
        headlineLarge = base.headlineLarge.withGeist(),
        headlineMedium = base.headlineMedium.withGeist(),
        headlineSmall = base.headlineSmall.withGeist(),
        titleLarge = base.titleLarge.withGeist(),
        titleMedium = base.titleMedium.withGeist(),
        titleSmall = base.titleSmall.withGeist(),
        bodyLarge = base.bodyLarge.withGeist(),
        bodyMedium = base.bodyMedium.withGeist(),
        bodySmall = base.bodySmall.withGeist(),
        labelLarge = base.labelLarge.withGeist(),
        labelMedium = base.labelMedium.withGeist(),
        labelSmall = base.labelSmall.withGeist(),
    )
}
