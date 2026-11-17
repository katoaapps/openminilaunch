package com.katoaapps.minkboard.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

private val GeistSans = FontFamily(
    Font(R.font.geist_variable, FontWeight.Normal),
    Font(R.font.geist_variable, FontWeight.Medium),
    Font(R.font.geist_variable, FontWeight.SemiBold),
    Font(R.font.geist_variable, FontWeight.Bold),
)

private fun TextStyle.withGeist() = copy(fontFamily = GeistSans)

private val MinkBoardTypography = Typography().let { base ->
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

private val MinkLightColors = lightColorScheme(
    primary = Color(0xFF602C00),
    onPrimary = Color(0xFFFAF9F6),
    primaryContainer = Color(0xFFF7E7CE),
    onPrimaryContainer = Color(0xFF351803),
    secondary = Color(0xFF173529),
    onSecondary = Color(0xFFFAF9F6),
    secondaryContainer = Color(0xFFDCE6DD),
    onSecondaryContainer = Color(0xFF10271F),
    tertiary = Color(0xFFAD4A25),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF4F1E8),
    onBackground = Color(0xFF27221F),
    surface = Color(0xFFF4F1E8),
    onSurface = Color(0xFF27221F),
    surfaceVariant = Color(0xFFE5E3D9),
    onSurfaceVariant = Color(0xFF4B4540),
    outline = Color(0xFF72665F),
    outlineVariant = Color(0xFFB8C7B0),
)

private val MinkDarkColors = darkColorScheme(
    primary = Color(0xFFF7E7CE),
    onPrimary = Color(0xFF351803),
    primaryContainer = Color(0xFF602C00),
    onPrimaryContainer = Color(0xFFF1F3EE),
    secondary = Color(0xFFB8C7B0),
    onSecondary = Color(0xFF1E2D24),
    secondaryContainer = Color(0xFF173529),
    onSecondaryContainer = Color(0xFFDCE6DD),
    tertiary = Color(0xFFFFB68D),
    onTertiary = Color(0xFF54200B),
    background = Color(0xFF101512),
    onBackground = Color(0xFFF1F3EE),
    surface = Color(0xFF171D19),
    onSurface = Color(0xFFF1F3EE),
    surfaceVariant = Color(0xFF3B443E),
    onSurfaceVariant = Color(0xFFC4CBC4),
    outline = Color(0xFFB8C7B0),
    outlineVariant = Color(0xFF425349),
)

@Composable
fun MinkBoardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) MinkDarkColors else MinkLightColors,
        typography = MinkBoardTypography,
        content = content,
    )
}
