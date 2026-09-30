package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.components.SettingsRow
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun PrivacyAppsSettingsPage(
    store: LauncherStore,
    actions: DeviceActions,
    onPickGateway: () -> Unit,
    goBack: () -> Unit,
) {
    val gatewayKey = store.privateContainerGatewaySelectionKey
    val gatewayTarget = gatewayKey
        ?.let(actions::resolveLauncherSelection)
        ?.takeIf { it.isAvailable }
    val gatewayLabel = gatewayTarget?.label
    var confirmRemoval by remember { mutableStateOf(false) }

    LaunchedEffect(gatewayKey, gatewayTarget?.isAvailable) {
        if (gatewayKey != null && gatewayTarget == null) {
            store.clearPrivateContainerGateway()
            actions.syncPinnedLauncherShortcuts(store.pinnedLauncherSelectionKeys)
        }
    }

    SettingsPage(stringResource(R.string.privacy_apps), goBack) {
        Text(
            stringResource(R.string.privacy_apps_beta_description),
            color = Muted,
            fontSize = Dimens.sp13,
        )

        SectionLabel(stringResource(R.string.private_container_gateway))
        Text(
            stringResource(R.string.private_container_gateway_description),
            color = Muted,
            fontSize = Dimens.sp13,
        )
        SettingsRow(
            title = stringResource(R.string.choose_private_container_gateway),
            subtitle = gatewayLabel ?: stringResource(R.string.private_container_gateway_not_selected),
            icon = Icons.Default.Shield,
            onClick = onPickGateway,
        )
        if (gatewayKey != null) {
            SettingsRow(
                title = stringResource(R.string.remove_private_container_gateway),
                subtitle = gatewayLabel.orEmpty(),
                icon = Icons.Default.DeleteOutline,
            ) { confirmRemoval = true }
        }

        if (actions.privateContainerEntryPointAvailable(gatewayKey)) {
            SettingsRow(
                title = stringResource(R.string.open_privacy_apps),
                subtitle = stringResource(R.string.open_privacy_apps_description),
                icon = Icons.Default.Launch,
                onClick = actions::openPrivateApps,
            )
        }
    }

    if (confirmRemoval) {
        AlertDialog(
            onDismissRequest = { confirmRemoval = false },
            title = { Text(stringResource(R.string.remove_private_container_gateway)) },
            text = { Text(stringResource(R.string.remove_private_container_gateway_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.clearPrivateContainerGateway()
                        actions.syncPinnedLauncherShortcuts(store.pinnedLauncherSelectionKeys)
                        confirmRemoval = false
                    },
                ) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemoval = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
