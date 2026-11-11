package com.katoaapps.openminilaunch.ui.quickdrawer

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.wellbeing.MinkAppAccessState
import com.katoaapps.openminilaunch.model.AppFolderColorMode
import com.katoaapps.openminilaunch.model.AppFolderConfig
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.appgrid.AppFolderContentsDialog
import com.katoaapps.openminilaunch.ui.appgrid.UnavailableLauncherTargetDialog

/** Reuses the Home folder surface for the system-owned Quick Apps collection. */
@Composable
internal fun QuickAppsFolderDialog(
    store: LauncherStore,
    actions: DeviceActions,
    appAccessState: MinkAppAccessState,
    onEdit: () -> Unit,
    onLaunch: () -> Unit,
    onPausedApp: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val demoModeEnabled = store.demoModeEnabled
    var pendingUnavailableTarget by remember { mutableStateOf<LauncherTarget?>(null) }
    val title = stringResource(R.string.quick_apps)
    val demoTargets = if (demoModeEnabled) demoQuickAppTargets() else null
    val showDemoPreview = {
        Toast.makeText(context, R.string.demo_preview_action, Toast.LENGTH_SHORT).show()
    }

    if (pendingUnavailableTarget == null) {
        AppFolderContentsDialog(
            folder = AppFolderConfig(
                id = QUICK_APPS_FOLDER_ID,
                customName = title,
                orderedTargetKeys = store.drawerTargets.toList(),
                colorMode = if (store.quickDrawerColorArgb == null) {
                    AppFolderColorMode.DEFAULT
                } else {
                    AppFolderColorMode.CUSTOM
                },
                colorArgb = store.quickDrawerColorArgb,
            ),
            animationStyle = store.folderAnimationStyle,
            actions = actions,
            isPaused = appAccessState::isPaused,
            onTarget = { target ->
                when {
                    demoModeEnabled -> showDemoPreview()
                    !target.isAvailable -> pendingUnavailableTarget = target
                    appAccessState.isPaused(target) -> onPausedApp(target.packageName)
                    else -> {
                        actions.launchLauncherTarget(target)
                        onLaunch()
                    }
                }
            },
            onAppearanceChange = { _, _, _ -> },
            onEdit = {
                if (demoModeEnabled) showDemoPreview() else onEdit()
            },
            onDismiss = onDismiss,
            titleOverride = title,
            targetsOverride = demoTargets,
            appearanceEditable = false,
        )
    }

    pendingUnavailableTarget?.let { target ->
        UnavailableLauncherTargetDialog(
            target = target,
            actions = actions,
            preferredWebPackage = store.preferredWebPackage,
            onRemove = {
                if (store.pruneLauncherTargets { it == target.selectionKey }) {
                    actions.syncPinnedLauncherShortcuts(store.pinnedLauncherSelectionKeys)
                }
                pendingUnavailableTarget = null
            },
            onDismiss = { pendingUnavailableTarget = null },
        )
    }
}

@Composable
private fun demoQuickAppTargets(): List<LauncherTarget> = listOf(
    R.string.shortcut_note,
    R.string.shortcut_calendar,
    R.string.shortcut_messenger,
    R.string.shortcut_files,
).mapIndexed { index, labelRes ->
    LauncherShortcutTarget(
        label = stringResource(labelRes),
        packageName = DEMO_QUICK_APPS_PACKAGE,
        shortcutId = "demo-$index",
        userSerial = 0L,
        isWorkProfile = false,
        selectionKey = "$DEMO_QUICK_APPS_PACKAGE:$index",
    )
}

private const val QUICK_APPS_FOLDER_ID = "quick-apps"
private const val DEMO_QUICK_APPS_PACKAGE = "com.katoaapps.openminilaunch.demo.quickapps"
