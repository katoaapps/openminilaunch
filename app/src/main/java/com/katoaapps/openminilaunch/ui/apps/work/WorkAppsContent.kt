package com.katoaapps.openminilaunch.ui.apps.work

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.workprofile.WorkProfileSnapshot
import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.apps.ProfileDrawerAppRow
import com.katoaapps.openminilaunch.ui.apps.ProfileDrawerControlRow
import com.katoaapps.openminilaunch.ui.apps.ProfileDrawerDemoAppRow
import com.katoaapps.openminilaunch.ui.apps.ProfileDrawerDemoData
import com.katoaapps.openminilaunch.ui.apps.ProfileDrawerMessage
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun WorkAppsContent(
    profile: WorkProfileSnapshot,
    apps: List<LauncherAppTarget>,
    actions: DeviceActions,
    demoMode: Boolean,
    onProfileEnabledChange: (Boolean) -> Unit,
    onLaunch: (LauncherAppTarget) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.widthIn(max = Dimens.dp720).fillMaxWidth().fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.dp22,
            end = Dimens.dp22,
            bottom = Dimens.dp24,
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp12),
    ) {
        item {
            Text(
                stringResource(R.string.work_apps_description),
                color = Muted,
                fontSize = Dimens.sp13,
            )
        }
        item {
            WorkProfileControl(
                enabled = !profile.paused,
                onEnabledChange = onProfileEnabledChange,
            )
        }
        when {
            profile.paused -> item {
                WorkAppsMessage(R.string.work_profile_paused_description)
            }
            demoMode -> items(
                ProfileDrawerDemoData.workApps,
                key = { it.packageName },
            ) { app ->
                ProfileDrawerDemoAppRow(app)
            }
            apps.isEmpty() -> item {
                WorkAppsMessage(R.string.work_apps_empty)
            }
            else -> items(apps, key = LauncherAppTarget::selectionKey) { app ->
                WorkAppRow(app, actions, onLaunch)
            }
        }
    }
}

@Composable
private fun WorkProfileControl(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    ProfileDrawerControlRow(
        icon = Icons.Default.BusinessCenter,
        title = stringResource(
            if (enabled) R.string.work_profile_active else R.string.work_profile_paused,
        ),
        description = stringResource(R.string.work_profile_control_description),
        enabled = enabled,
        onEnabledChange = onEnabledChange,
    )
}

@Composable
private fun WorkAppRow(
    app: LauncherAppTarget,
    actions: DeviceActions,
    onLaunch: (LauncherAppTarget) -> Unit,
) {
    ProfileDrawerAppRow(
        icon = {
            LauncherTargetIcon(app, actions, Dimens.dp40, contentDescription = app.label)
        },
        label = app.label,
        supportingText = app.packageName,
        onClick = { onLaunch(app) },
    )
}

@Composable
private fun WorkAppsMessage(message: Int) {
    ProfileDrawerMessage(stringResource(message))
}
