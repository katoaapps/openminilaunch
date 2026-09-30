package com.katoaapps.openminilaunch.ui.apps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.privatespace.PrivateSpaceRepository
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.apps.private.PrivateAppsScreen
import com.katoaapps.openminilaunch.ui.theme.MinkLauncherTheme

class PrivateAppsActivity : ComponentActivity() {
    private lateinit var repository: PrivateSpaceRepository
    private lateinit var actions: DeviceActions
    private lateinit var store: LauncherStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = PrivateSpaceRepository.get(this)
        actions = DeviceActions(this)
        store = LauncherStore.get(this)
        setContent {
            MinkLauncherTheme(store) {
                PrivateAppsScreen(
                    repository = repository,
                    actions = actions,
                    demoMode = store.demoSearchDataEnabled,
                    userGatewaySelectionKey = store.privateContainerGatewaySelectionKey,
                    onUserGatewayUnavailable = {
                        store.clearPrivateContainerGateway()
                        actions.syncPinnedLauncherShortcuts(store.pinnedLauncherSelectionKeys)
                    },
                    onClose = ::finish,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        actions.invalidateInstalledApps()
        store.privateContainerGatewaySelectionKey?.let { selectionKey ->
            if (!actions.resolveLauncherSelection(selectionKey).isAvailable) {
                store.clearPrivateContainerGateway()
                actions.syncPinnedLauncherShortcuts(store.pinnedLauncherSelectionKeys)
            }
        }
        repository.refresh()
    }
}
