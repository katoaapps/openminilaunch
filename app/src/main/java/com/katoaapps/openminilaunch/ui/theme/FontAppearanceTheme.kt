package com.katoaapps.openminilaunch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.appearance.launcherFontFamily

@Composable
internal fun FontAppearanceTheme(
    store: LauncherStore,
    applyFontColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val appearance = store.fontAppearance
    val currentDensity = LocalDensity.current
    val currentColors = MaterialTheme.colorScheme
    val currentTypography = MaterialTheme.typography
    val family = remember(
        appearance.source,
        appearance.customFontName,
        store.customFontRevision,
    ) {
        launcherFontFamily(appearance.source, store.customFontFile)
    }
    val typography = remember(currentTypography, family) {
        currentTypography.withFontFamily(family)
    }
    val scaledDensity = remember(currentDensity.density, currentDensity.fontScale, appearance.sizeScale) {
        Density(currentDensity.density, currentDensity.fontScale * appearance.sizeScale)
    }
    val chosenColor = if (applyFontColor) appearance.colorArgb?.let(::Color) else null
    val colors = remember(currentColors, chosenColor) {
        chosenColor?.let { color ->
            currentColors.copy(
                onBackground = color,
                onSurface = color,
                onSurfaceVariant = color.copy(alpha = .72f),
                outline = color.copy(alpha = .48f),
                outlineVariant = color.copy(alpha = .24f),
            )
        } ?: currentColors
    }
    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalMutedColor provides chosenColor?.copy(alpha = .68f),
    ) {
        MaterialTheme(colorScheme = colors, typography = typography, content = content)
    }
}

private fun Typography.withFontFamily(family: FontFamily) = copy(
    displayLarge = displayLarge.withFontFamily(family),
    displayMedium = displayMedium.withFontFamily(family),
    displaySmall = displaySmall.withFontFamily(family),
    headlineLarge = headlineLarge.withFontFamily(family),
    headlineMedium = headlineMedium.withFontFamily(family),
    headlineSmall = headlineSmall.withFontFamily(family),
    titleLarge = titleLarge.withFontFamily(family),
    titleMedium = titleMedium.withFontFamily(family),
    titleSmall = titleSmall.withFontFamily(family),
    bodyLarge = bodyLarge.withFontFamily(family),
    bodyMedium = bodyMedium.withFontFamily(family),
    bodySmall = bodySmall.withFontFamily(family),
    labelLarge = labelLarge.withFontFamily(family),
    labelMedium = labelMedium.withFontFamily(family),
    labelSmall = labelSmall.withFontFamily(family),
)

private fun TextStyle.withFontFamily(family: FontFamily) = copy(fontFamily = family)
