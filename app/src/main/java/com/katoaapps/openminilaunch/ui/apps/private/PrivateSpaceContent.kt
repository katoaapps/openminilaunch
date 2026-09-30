package com.katoaapps.openminilaunch.ui.apps.private

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.privatespace.PrivateSpaceApp
import com.katoaapps.openminilaunch.features.privatespace.PrivateSpaceGateway
import com.katoaapps.openminilaunch.features.privatespace.PrivateSpaceSnapshot
import com.katoaapps.openminilaunch.features.privatespace.PrivateSpaceStatus
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.apps.ProfileDrawerDemoAppRow
import com.katoaapps.openminilaunch.ui.apps.ProfileDrawerDemoData
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun PrivateSpaceContent(
    snapshot: PrivateSpaceSnapshot,
    actions: DeviceActions,
    demoMode: Boolean,
    userGateway: LauncherTarget?,
    onOpenSettings: () -> Unit,
    onLockChanged: (Boolean) -> Unit,
    onLaunch: (PrivateSpaceApp) -> Unit,
    onLaunchGateway: (PrivateSpaceGateway) -> Unit,
    onLaunchUserGateway: (LauncherTarget) -> Unit,
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
        if (snapshot.status != PrivateSpaceStatus.OEM_GATEWAY) {
            item {
                Text(
                    stringResource(R.string.privacy_apps_description),
                    color = Muted,
                    fontSize = Dimens.sp13,
                )
            }
        }

        when (snapshot.status) {
            PrivateSpaceStatus.UNLOCKED,
            PrivateSpaceStatus.LOCKED -> {
                item {
                    PrivateSpaceLockRow(
                        unlocked = snapshot.status == PrivateSpaceStatus.UNLOCKED,
                        onLockChanged = onLockChanged,
                    )
                }
                when {
                    snapshot.status == PrivateSpaceStatus.LOCKED ->
                        item { PrivateSpaceMessage(R.string.privacy_apps_locked_description) }
                    demoMode -> items(
                        ProfileDrawerDemoData.privateApps,
                        key = { it.packageName },
                    ) { app ->
                        ProfileDrawerDemoAppRow(app)
                    }
                    snapshot.apps.isEmpty() ->
                        item { PrivateSpaceMessage(R.string.privacy_apps_empty) }
                    else -> items(snapshot.apps, key = PrivateSpaceApp::key) { app ->
                        PrivateSpaceAppRow(app, onLaunch)
                    }
                }
            }
            PrivateSpaceStatus.UNSUPPORTED -> if (userGateway == null) {
                item { PrivateSpaceMessage(R.string.privacy_apps_requires_android_15) }
            }
            PrivateSpaceStatus.HOME_ROLE_REQUIRED ->
                item { PrivateSpaceMessage(R.string.privacy_apps_requires_default_launcher) }
            PrivateSpaceStatus.NOT_CONFIGURED -> if (userGateway == null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp12)) {
                        PrivateSpaceMessage(R.string.privacy_apps_not_configured)
                        FilledTonalButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.open_private_space_settings))
                        }
                    }
                }
            }
            PrivateSpaceStatus.OEM_GATEWAY -> snapshot.gateway?.let { gateway ->
                item { OemGatewayRow(gateway, onLaunchGateway) }
            }
        }
        userGateway?.let { gateway ->
            item { UserPrivateGatewayRow(gateway, actions, onLaunchUserGateway) }
        }
        item { Spacer(Modifier.height(Dimens.dp12)) }
    }
}
