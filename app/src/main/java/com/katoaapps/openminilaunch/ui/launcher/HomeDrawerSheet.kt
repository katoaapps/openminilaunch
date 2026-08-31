@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.katoaapps.openminilaunch.ui.launcher

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.wellbeing.MinkAppAccessState
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.settings.SettingsDestination
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Sage
import kotlin.math.ceil

@Composable
internal fun HomeDrawerSheet(
    store: LauncherStore,
    actions: DeviceActions,
    appAccessState: MinkAppAccessState,
    launcherAppsRevision: Long,
    launcherShortcutsRevision: Long,
    onDismiss: () -> Unit,
    openSettings: (SettingsDestination) -> Unit,
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Text(
            stringResource(R.string.your_drawer),
            Modifier.padding(horizontal = Dimens.dp24),
            fontWeight = FontWeight.Black,
            letterSpacing = Dimens.sp1,
        )
        when {
            store.drawerTargets.isEmpty() -> EmptyDrawer(
                onChooseApps = {
                    onDismiss()
                    openSettings(SettingsDestination.SHORTCUTS)
                },
            )
            !appAccessState.isResolved -> Box(
                Modifier.fillMaxWidth().height(Dimens.dp84),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            else -> DrawerApps(
                store = store,
                actions = actions,
                appAccessState = appAccessState,
                launcherAppsRevision = launcherAppsRevision,
                launcherShortcutsRevision = launcherShortcutsRevision,
                onLaunched = onDismiss,
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Dimens.dp18, vertical = Dimens.dp4),
            horizontalArrangement = Arrangement.End,
        ) {
            FilledTonalButton(
                onClick = {
                    onDismiss()
                    actions.openAllApps()
                },
            ) {
                Text(stringResource(R.string.see_all_apps))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    null,
                    modifier = Modifier.padding(start = Dimens.dp6),
                )
            }
        }
        Spacer(Modifier.height(Dimens.dp28))
    }
}

@Composable
private fun EmptyDrawer(onChooseApps: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Dimens.dp28, vertical = Dimens.dp24),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Apps, null, Modifier.size(Dimens.dp48), tint = Sage)
        Text(
            stringResource(R.string.empty_drawer),
            fontSize = Dimens.sp22,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = Dimens.dp14),
        )
        Text(
            stringResource(R.string.choose_drawer_apps),
            color = Muted,
            modifier = Modifier.padding(vertical = Dimens.dp10),
        )
        Button(onClick = onChooseApps) {
            Icon(Icons.Default.Add, null)
            Text(stringResource(R.string.choose_apps), Modifier.padding(start = Dimens.dp8))
        }
    }
}

@Composable
private fun DrawerApps(
    store: LauncherStore,
    actions: DeviceActions,
    appAccessState: MinkAppAccessState,
    launcherAppsRevision: Long,
    launcherShortcutsRevision: Long,
    onLaunched: () -> Unit,
) {
    val context = LocalContext.current
    val savedDrawerTargets = store.drawerTargets.toList()
    val drawerTargets = remember(savedDrawerTargets, launcherAppsRevision, launcherShortcutsRevision) {
        savedDrawerTargets.map(actions::resolveLauncherSelection)
    }
    val visibleDrawerTargets = drawerTargets.filterNot(appAccessState::isPaused)
    if (visibleDrawerTargets.isEmpty()) {
        Text(
            stringResource(R.string.drawer_apps_paused),
            Modifier.fillMaxWidth().padding(horizontal = Dimens.dp28, vertical = Dimens.dp24),
            color = Muted,
        )
    }
    val drawerRows = ceil(visibleDrawerTargets.size / 2f).toInt()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth().height(Dimens.dp72 * drawerRows),
        contentPadding = PaddingValues(horizontal = Dimens.dp12, vertical = Dimens.dp8),
    ) {
        items(visibleDrawerTargets, key = LauncherTarget::selectionKey) { target ->
            ListItem(
                headlineContent = {
                    Text(target.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                leadingContent = { LauncherTargetIcon(target, actions, Dimens.dp36) },
                modifier = Modifier.clip(RoundedCornerShape(Dimens.dp16)).clickable {
                    if (actions.launchLauncherTarget(target)) {
                        onLaunched()
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.launcher_app_unavailable, target.label),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            )
        }
    }
}
