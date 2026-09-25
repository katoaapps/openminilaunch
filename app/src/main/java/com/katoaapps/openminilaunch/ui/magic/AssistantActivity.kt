package com.katoaapps.openminilaunch.ui.magic

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.applyLargeDisplayOrientation
import com.katoaapps.openminilaunch.ui.theme.MinkLauncherTheme
import com.katoaapps.openminilaunch.ui.theme.MinkTransparent

/** Keyboard-first system assistant entry point. ACTION_ASSIST context is deliberately ignored. */
class AssistantActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE,
        )
        val store = LauncherStore.get(this)
        applyLargeDisplayOrientation(this, store.twoPanelModeForLargeDisplays)
        val actions = DeviceActions(this)

        setContent {
            val context = LocalContext.current
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
            MinkLauncherTheme(store) {
                Box(Modifier.fillMaxSize()) {
                    AssistantMagicBox(
                        store = store,
                        actions = actions,
                        modifier = Modifier.fillMaxSize(),
                        onSessionComplete = ::finish,
                    )
                }
            }
        }
    }
}
