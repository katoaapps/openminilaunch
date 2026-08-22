package com.katoaapps.openminilaunch

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Keyboard-first system assistant entry point. ACTION_ASSIST context is deliberately ignored. */
class AssistantActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE,
        )
        val store = LauncherStore(this)
        val actions = DeviceActions(this)

        setContent {
            val context = LocalContext.current
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (store.themePreference) {
                ThemePreference.SYSTEM -> systemDark
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
            val baseColors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else fallback
            val colors = baseColors.withAppBackground(store.appBackgroundColorArgb)
            val view = LocalView.current
            val transparent = MinkTransparent
            SideEffect {
                val activityWindow = (context as Activity).window
                activityWindow.statusBarColor = transparent.toArgb()
                activityWindow.navigationBarColor = transparent.toArgb()
                WindowInsetsControllerCompat(activityWindow, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
            MaterialTheme(colorScheme = colors, typography = Typography()) {
                Box(Modifier.fillMaxSize()) {
                    MagicBox(
                        store = store,
                        actions = actions,
                        modifier = Modifier.fillMaxSize(),
                        keyboardInputEnabled = true,
                        initiallyExpanded = true,
                        showSoftwareKeyboardOnStart = true,
                        onSessionComplete = ::finish,
                    )
                }
            }
        }
    }
}
