package com.katoaapps.openminilaunch.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.model.PalettePreference
import com.katoaapps.openminilaunch.model.ThemePreference

internal val MinkLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF602C00),
    onPrimary = Color(0xFFFAF9F6),
    primaryContainer = Color(0xFFF7E7CE),
    onPrimaryContainer = Color(0xFF351803),
    secondary = Color(0xFF173529),
    onSecondary = Color(0xFFFAF9F6),
    secondaryContainer = Color(0xFFDCE6DD),
    onSecondaryContainer = Color(0xFF10271F),
    tertiary = Color(0xFF8B3E1F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDBCC),
    onTertiaryContainer = Color(0xFF351000),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF4F1E8),
    onBackground = Color(0xFF27221F),
    surface = Color(0xFFF4F1E8),
    onSurface = Color(0xFF27221F),
    surfaceVariant = Color(0xFFE5E3D9),
    onSurfaceVariant = Color(0xFF4B4540),
    surfaceDim = Color(0xFFD8D5CC),
    surfaceBright = Color(0xFFFFFBF5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF9F6),
    surfaceContainer = Color(0xFFF0EDE5),
    surfaceContainerHigh = Color(0xFFEAE7DF),
    surfaceContainerHighest = Color(0xFFE4E1D9),
    outline = Color(0xFF72665F),
    outlineVariant = Color(0xFFB8C7B0),
    inverseSurface = Color(0xFF302E2A),
    inverseOnSurface = Color(0xFFF7F1E9),
    inversePrimary = Color(0xFFFFB77A),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF602C00),
)

internal val MinkDarkColorScheme: ColorScheme = darkColorScheme(
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
    tertiaryContainer = Color(0xFF73351C),
    onTertiaryContainer = Color(0xFFFFDBCC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101512),
    onBackground = Color(0xFFF1F3EE),
    surface = Color(0xFF171D19),
    onSurface = Color(0xFFF1F3EE),
    surfaceVariant = Color(0xFF3B443E),
    onSurfaceVariant = Color(0xFFC4CBC4),
    surfaceDim = Color(0xFF101512),
    surfaceBright = Color(0xFF353B37),
    surfaceContainerLowest = Color(0xFF0B0F0D),
    surfaceContainerLow = Color(0xFF1C2420),
    surfaceContainer = Color(0xFF202824),
    surfaceContainerHigh = Color(0xFF2A322E),
    surfaceContainerHighest = Color(0xFF353D38),
    outline = Color(0xFFB8C7B0),
    outlineVariant = Color(0xFF425349),
    inverseSurface = Color(0xFFE1E4DE),
    inverseOnSurface = Color(0xFF2E322F),
    inversePrimary = Color(0xFF602C00),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFFF7E7CE),
)

@Composable
internal fun minkDarkTheme(store: LauncherStore): Boolean = when (store.themePreference) {
    ThemePreference.SYSTEM -> isSystemInDarkTheme()
    ThemePreference.LIGHT -> false
    ThemePreference.DARK -> true
}

@Composable
internal fun minkColorScheme(
    store: LauncherStore,
    darkTheme: Boolean = minkDarkTheme(store),
): ColorScheme {
    val context = LocalContext.current
    val base = if (
        store.palettePreference == PalettePreference.SYSTEM &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) MinkDarkColorScheme else MinkLightColorScheme
    }
    return base.withAppBackground(store.effectiveAppBackgroundColorArgb)
}

/** Shared colors and typography for Home, settings, tools, dialogs, and secondary activities. */
@Composable
internal fun MinkLauncherTheme(store: LauncherStore, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = minkColorScheme(store),
        typography = MinkTypography,
        content = content,
    )
}
