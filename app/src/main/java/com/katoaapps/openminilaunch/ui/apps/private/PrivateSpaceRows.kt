package com.katoaapps.openminilaunch.ui.apps.private

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.privatespace.PrivateSpaceApp
import com.katoaapps.openminilaunch.features.privatespace.PrivateSpaceGateway
import com.katoaapps.openminilaunch.features.privatespace.PrivateSpaceGatewayKind
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.apps.ProfileDrawerAppRow
import com.katoaapps.openminilaunch.ui.apps.ProfileDrawerControlRow
import com.katoaapps.openminilaunch.ui.apps.ProfileDrawerMessage
import com.katoaapps.openminilaunch.ui.components.DrawableIcon
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun UserPrivateGatewayRow(
    gateway: LauncherTarget,
    actions: DeviceActions,
    onLaunch: (LauncherTarget) -> Unit,
) {
    GatewayCard(
        icon = { LauncherTargetIcon(gateway, actions, Dimens.dp40) },
        label = gateway.label,
        description = stringResource(R.string.private_container_gateway_launch_description),
        actionLabel = stringResource(R.string.open_app, gateway.label),
        onLaunch = { onLaunch(gateway) },
    )
}

@Composable
internal fun OemGatewayRow(
    gateway: PrivateSpaceGateway,
    onLaunch: (PrivateSpaceGateway) -> Unit,
) {
    GatewayCard(
        icon = {
            DrawableIcon(
                drawable = gateway.icon,
                iconKey = gateway.packageName,
                size = Dimens.dp40,
                contentDescription = gateway.label,
            )
        },
        label = gateway.label,
        description = stringResource(gateway.descriptionRes),
        actionLabel = stringResource(gateway.openActionRes),
        onLaunch = { onLaunch(gateway) },
    )
}

@Composable
private fun GatewayCard(
    icon: @Composable () -> Unit,
    label: String,
    description: String,
    actionLabel: String,
    onLaunch: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                RoundedCornerShape(Dimens.dp16),
            )
            .padding(Dimens.dp16),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp12),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Column(Modifier.weight(1f).padding(start = Dimens.dp12)) {
                Text(label, fontWeight = FontWeight.SemiBold)
                Text(description, color = Muted, fontSize = Dimens.sp11)
            }
        }
        FilledTonalButton(onClick = onLaunch, modifier = Modifier.fillMaxWidth()) {
            Text(actionLabel)
        }
    }
}

@Composable
internal fun PrivateSpaceLockRow(
    unlocked: Boolean,
    onLockChanged: (Boolean) -> Unit,
) {
    ProfileDrawerControlRow(
        icon = if (unlocked) Icons.Default.LockOpen else Icons.Default.Lock,
        title = stringResource(
            if (unlocked) R.string.privacy_apps_unlocked else R.string.privacy_apps_locked,
        ),
        description = stringResource(R.string.privacy_apps_lock_control_description),
        enabled = unlocked,
        onEnabledChange = onLockChanged,
    )
}

@Composable
internal fun PrivateSpaceAppRow(
    app: PrivateSpaceApp,
    onLaunch: (PrivateSpaceApp) -> Unit,
) {
    ProfileDrawerAppRow(
        icon = {
            DrawableIcon(
                drawable = app.icon,
                iconKey = app.key,
                size = Dimens.dp40,
                contentDescription = app.label,
            )
        },
        label = app.label,
        supportingText = app.packageName,
        onClick = { onLaunch(app) },
    )
}

@Composable
internal fun PrivateSpaceMessage(message: Int) {
    ProfileDrawerMessage(stringResource(message))
}

internal val PrivateSpaceGateway.unavailableMessageRes: Int
    get() = when (kind) {
        PrivateSpaceGatewayKind.SAMSUNG_SECURE_FOLDER -> R.string.secure_folder_unavailable
        PrivateSpaceGatewayKind.MOTOROLA_MOTO_SECURE -> R.string.moto_secure_unavailable
    }

private val PrivateSpaceGateway.descriptionRes: Int
    get() = when (kind) {
        PrivateSpaceGatewayKind.SAMSUNG_SECURE_FOLDER ->
            R.string.secure_folder_gateway_description
        PrivateSpaceGatewayKind.MOTOROLA_MOTO_SECURE ->
            R.string.moto_secure_gateway_description
    }

private val PrivateSpaceGateway.openActionRes: Int
    get() = when (kind) {
        PrivateSpaceGatewayKind.SAMSUNG_SECURE_FOLDER -> R.string.open_secure_folder
        PrivateSpaceGatewayKind.MOTOROLA_MOTO_SECURE -> R.string.open_moto_secure
    }
