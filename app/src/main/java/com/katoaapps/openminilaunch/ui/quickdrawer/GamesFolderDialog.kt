package com.katoaapps.openminilaunch.ui.quickdrawer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.model.AppFolderColorMode
import com.katoaapps.openminilaunch.model.AppFolderConfig
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.appgrid.AppFolderContentsDialog

/** Presents the embedded game catalog as a launcher-owned collection, not installed APKs. */
@Composable
internal fun GamesFolderDialog(
    store: LauncherStore,
    actions: DeviceActions,
    onLaunch: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = stringResource(R.string.games_beta)
    val targets = remember(actions) { actions.sableGameTargets() }

    AppFolderContentsDialog(
        folder = AppFolderConfig(
            id = GAMES_FOLDER_ID,
            customName = title,
            orderedTargetKeys = targets.map { it.selectionKey },
            colorMode = if (store.quickDrawerColorArgb == null) {
                AppFolderColorMode.DEFAULT
            } else {
                AppFolderColorMode.CUSTOM
            },
            colorArgb = store.quickDrawerColorArgb,
        ),
        animationStyle = store.folderAnimationStyle,
        actions = actions,
        isPaused = { false },
        onTarget = { target ->
            if (actions.launchLauncherTarget(target)) onLaunch()
        },
        onAppearanceChange = { _, _, _ -> },
        onEdit = {},
        onDismiss = onDismiss,
        titleOverride = title,
        targetsOverride = targets,
        appearanceEditable = false,
        showAddAction = false,
    )
}

private const val GAMES_FOLDER_ID = "games-beta"
