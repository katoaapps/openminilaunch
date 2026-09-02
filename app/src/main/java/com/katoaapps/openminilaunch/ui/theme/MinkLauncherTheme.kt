package com.katoaapps.openminilaunch.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.model.ThemePreference

/** Applies the launcher colors to secondary activities without booting the Home UI. */
@Composable
internal fun MinkLauncherTheme(store: LauncherStore, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val darkTheme = when (store.themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    val fallback = if (darkTheme) {
        darkColorScheme(
            primary = DarkPrimary,
            onPrimary = DarkOnPrimary,
            background = DarkBackground,
            surface = DarkSurface,
            surfaceContainerLow = DarkSurfaceContainerLow,
            onSurface = DarkOnSurface,
            secondary = Rust,
        )
    } else {
        lightColorScheme(
            primary = LightInk,
            onPrimary = LightPaper,
            background = LightPaper,
            surface = LightPaper,
            surfaceContainerLow = MinkWhite,
            onSurface = LightInk,
            secondary = Rust,
        )
    }
    val base = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        fallback
    }
    MaterialTheme(
        colorScheme = base.withAppBackground(store.effectiveAppBackgroundColorArgb),
        typography = Typography(),
        content = content,
    )
}
