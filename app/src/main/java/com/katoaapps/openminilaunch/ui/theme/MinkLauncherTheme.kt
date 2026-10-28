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
    secondary = Color(0xFFAD4A25),
    onSecondary = Color(0xFFFAF9F6),
    background = Color(0xFFF4F1E8),
    onBackground = Color(0xFF27221F),
    surface = Color(0xFFF4F1E8),
    onSurface = Color(0xFF27221F),
    surfaceContainerLow = Color(0xFFFAF9F6),
    outline = Color(0xFF72665F),
    outlineVariant = Color(0xFFB8C7B0),
)

internal val MinkDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFF7E7CE),
    onPrimary = Color(0xFF351803),
    primaryContainer = Color(0xFF602C00),
    onPrimaryContainer = Color(0xFFF1F3EE),
    secondary = Color(0xFFAD4A25),
    onSecondary = Color(0xFFFAF9F6),
    background = Color(0xFF101512),
    onBackground = Color(0xFFF1F3EE),
    surface = Color(0xFF171D19),
    onSurface = Color(0xFFF1F3EE),
    surfaceContainerLow = Color(0xFF1C2420),
    outline = Color(0xFFB8C7B0),
    outlineVariant = Color(0xFF425349),
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
