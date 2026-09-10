package com.katoaapps.openminilaunch.ui.shortcuts

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.apps.PendingPinShortcut
import com.katoaapps.openminilaunch.features.apps.PinShortcutRequestHandler
import com.katoaapps.openminilaunch.model.MAX_DRAWER_APPS
import com.katoaapps.openminilaunch.model.PinShortcutDestination
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.theme.MinkLauncherTheme

/** Receives Add to Home requests while MinkLauncher holds Android's Home role. */
class PinShortcutActivity : ComponentActivity() {
    private lateinit var store: LauncherStore
    private lateinit var actions: DeviceActions
    private lateinit var requestHandler: PinShortcutRequestHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        store = LauncherStore.get(this)
        actions = DeviceActions(this)
        requestHandler = PinShortcutRequestHandler(this)
        val pending = requestHandler.read(intent)

        setContent {
            MinkLauncherTheme(store) {
                val view = LocalView.current
                val useDarkSystemIcons = MaterialTheme.colorScheme.background.luminance() > .5f
                SideEffect {
                    WindowInsetsControllerCompat(window, view).apply {
                        isAppearanceLightStatusBars = useDarkSystemIcons
                        isAppearanceLightNavigationBars = useDarkSystemIcons
                    }
                }
                PinShortcutRequestHost(
                    pending = pending,
                    presentation = store.pinShortcutRequestPresentation,
                    store = store,
                    actions = actions,
                    onConfirm = { destination -> completeRequest(pending, destination) },
                    onCancel = ::finish,
                )
            }
        }
    }

    private fun completeRequest(
        pending: PendingPinShortcut?,
        destination: PinShortcutDestination,
    ): Boolean {
        pending ?: return false
        if (!destinationIsAvailable(destination)) return false
        if (!requestHandler.accept(pending)) return false

        val targetKey = pending.target.selectionKey
        when (destination) {
            is PinShortcutDestination.HomeSlot -> store.assignShortcut(destination.shortcut, targetKey)
            PinShortcutDestination.AddToDrawer -> store.addDrawerTarget(targetKey)
            PinShortcutDestination.AddToLibrary -> store.addLibraryShortcut(targetKey)
            is PinShortcutDestination.ReplaceDrawerSlot -> {
                store.replaceDrawerTarget(destination.index, targetKey)
            }
        }
        actions.invalidateInstalledApps()
        actions.syncPinnedLauncherShortcuts(store.pinnedLauncherSelectionKeys)
        setResult(Activity.RESULT_OK)
        finish()
        return true
    }

    private fun destinationIsAvailable(destination: PinShortcutDestination): Boolean = when (destination) {
        is PinShortcutDestination.HomeSlot -> true
        PinShortcutDestination.AddToDrawer -> store.drawerTargets.size < MAX_DRAWER_APPS
        PinShortcutDestination.AddToLibrary -> true
        is PinShortcutDestination.ReplaceDrawerSlot -> destination.index in store.drawerTargets.indices
    }
}
