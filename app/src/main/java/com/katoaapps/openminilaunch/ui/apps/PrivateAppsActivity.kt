package com.katoaapps.openminilaunch.ui.apps

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.privatespace.PrivateSpaceApp
import com.katoaapps.openminilaunch.features.privatespace.PrivateSpaceGateway
import com.katoaapps.openminilaunch.features.privatespace.PrivateSpaceGatewayKind
import com.katoaapps.openminilaunch.features.privatespace.PrivateSpaceRepository
import com.katoaapps.openminilaunch.features.privatespace.PrivateSpaceSnapshot
import com.katoaapps.openminilaunch.features.privatespace.PrivateSpaceStatus
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.DrawableIcon
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MinkLauncherTheme
import com.katoaapps.openminilaunch.ui.theme.Muted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

@Composable
private fun PrivateAppsScreen(
    repository: PrivateSpaceRepository,
    actions: DeviceActions,
    userGatewaySelectionKey: String?,
    onUserGatewayUnavailable: () -> Unit,
    onClose: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val revision by repository.revision.collectAsState()
    val snapshot by produceState<PrivateSpaceSnapshot?>(null, revision) {
        value = withContext(Dispatchers.IO) { repository.snapshot() }
    }
    val userGateway = userGatewaySelectionKey
        ?.let(actions::resolveLauncherSelection)
        ?.takeIf(LauncherTarget::isAvailable)
        ?.takeUnless { it.packageName == snapshot?.gateway?.packageName }

    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.widthIn(max = Dimens.dp720).fillMaxWidth()
                .height(Dimens.dp64)
                .padding(horizontal = Dimens.dp8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.privacy_apps),
                    fontWeight = FontWeight.Black,
                )
                Text(
                    snapshot?.gateway?.label
                        ?: userGateway?.label
                        ?: stringResource(R.string.privacy_apps_android_private_space),
                    color = Muted,
                    fontSize = Dimens.sp11,
                )
            }
            Icon(Icons.Default.Shield, null)
        }

        when (val current = snapshot) {
            null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> PrivateSpaceContent(
                snapshot = current,
                actions = actions,
                onOpenSettings = {
                    if (!repository.openSettings(context)) {
                        Toast.makeText(
                            context,
                            R.string.private_space_settings_unavailable,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
                onLockChanged = { unlocked ->
                    if (!repository.setLocked(locked = !unlocked)) {
                        Toast.makeText(
                            context,
                            R.string.privacy_apps_state_change_failed,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
                onLaunch = { app ->
                    if (!repository.launch(app)) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.launcher_app_unavailable, app.label),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
                onLaunchGateway = { gateway ->
                    if (!repository.launchGateway(gateway)) {
                        Toast.makeText(
                            context,
                            gateway.unavailableMessageRes,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
                userGateway = userGateway,
                onLaunchUserGateway = { gateway ->
                    if (!actions.launchLauncherTarget(gateway)) {
                        onUserGatewayUnavailable()
                        Toast.makeText(
                            context,
                            context.getString(R.string.launcher_app_unavailable, gateway.label),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
            )
        }
    }
}

@Composable
private fun PrivateSpaceContent(
    snapshot: PrivateSpaceSnapshot,
    actions: DeviceActions,
    onOpenSettings: () -> Unit,
    onLockChanged: (Boolean) -> Unit,
    onLaunch: (PrivateSpaceApp) -> Unit,
    onLaunchGateway: (PrivateSpaceGateway) -> Unit,
    userGateway: LauncherTarget?,
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
                if (snapshot.status == PrivateSpaceStatus.LOCKED) {
                    item { PrivateSpaceMessage(R.string.privacy_apps_locked_description) }
                } else if (snapshot.apps.isEmpty()) {
                    item { PrivateSpaceMessage(R.string.privacy_apps_empty) }
                } else {
                    items(snapshot.apps, key = PrivateSpaceApp::key) { app ->
                        PrivateSpaceAppRow(app, onLaunch)
                    }
                }
            }
            PrivateSpaceStatus.UNSUPPORTED -> item {
                if (userGateway == null) {
                    PrivateSpaceMessage(R.string.privacy_apps_requires_android_15)
                }
            }
            PrivateSpaceStatus.HOME_ROLE_REQUIRED -> item {
                PrivateSpaceMessage(R.string.privacy_apps_requires_default_launcher)
            }
            PrivateSpaceStatus.NOT_CONFIGURED -> item {
                if (userGateway == null) {
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
            PrivateSpaceStatus.OEM_GATEWAY -> item {
                snapshot.gateway?.let { gateway ->
                    OemGatewayRow(gateway, onLaunchGateway)
                }
            }
        }
        userGateway?.let { gateway ->
            item { UserPrivateGatewayRow(gateway, actions, onLaunchUserGateway) }
        }
        item { Spacer(Modifier.height(Dimens.dp12)) }
    }
}

@Composable
private fun UserPrivateGatewayRow(
    gateway: LauncherTarget,
    actions: DeviceActions,
    onLaunch: (LauncherTarget) -> Unit,
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
            LauncherTargetIcon(
                target = gateway,
                actions = actions,
                size = Dimens.dp40,
            )
            Column(Modifier.weight(1f).padding(start = Dimens.dp12)) {
                Text(gateway.label, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.private_container_gateway_launch_description),
                    color = Muted,
                    fontSize = Dimens.sp11,
                )
            }
        }
        FilledTonalButton(
            onClick = { onLaunch(gateway) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.open_app, gateway.label))
        }
    }
}

@Composable
private fun OemGatewayRow(
    gateway: PrivateSpaceGateway,
    onLaunch: (PrivateSpaceGateway) -> Unit,
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
            DrawableIcon(
                drawable = gateway.icon,
                iconKey = gateway.packageName,
                size = Dimens.dp40,
                contentDescription = gateway.label,
            )
            Column(Modifier.weight(1f).padding(start = Dimens.dp12)) {
                Text(gateway.label, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(gateway.descriptionRes),
                    color = Muted,
                    fontSize = Dimens.sp11,
                )
            }
        }
        FilledTonalButton(
            onClick = { onLaunch(gateway) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(gateway.openActionRes))
        }
    }
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

private val PrivateSpaceGateway.unavailableMessageRes: Int
    get() = when (kind) {
        PrivateSpaceGatewayKind.SAMSUNG_SECURE_FOLDER -> R.string.secure_folder_unavailable
        PrivateSpaceGatewayKind.MOTOROLA_MOTO_SECURE -> R.string.moto_secure_unavailable
    }

@Composable
private fun PrivateSpaceLockRow(
    unlocked: Boolean,
    onLockChanged: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                RoundedCornerShape(Dimens.dp16),
            )
            .clickable { onLockChanged(!unlocked) }
            .padding(Dimens.dp14),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (unlocked) Icons.Default.LockOpen else Icons.Default.Lock,
            null,
            Modifier.size(Dimens.dp32),
        )
        Column(Modifier.weight(1f).padding(horizontal = Dimens.dp12)) {
            Text(
                stringResource(if (unlocked) R.string.privacy_apps_unlocked else R.string.privacy_apps_locked),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.privacy_apps_lock_control_description),
                color = Muted,
                fontSize = Dimens.sp11,
            )
        }
        Switch(checked = unlocked, onCheckedChange = onLockChanged)
    }
}

@Composable
private fun PrivateSpaceAppRow(
    app: PrivateSpaceApp,
    onLaunch: (PrivateSpaceApp) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                RoundedCornerShape(Dimens.dp16),
            )
            .clickable { onLaunch(app) }
            .padding(Dimens.dp12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DrawableIcon(
            drawable = app.icon,
            iconKey = app.key,
            size = Dimens.dp40,
            contentDescription = app.label,
        )
        Column(Modifier.weight(1f).padding(horizontal = Dimens.dp12)) {
            Text(app.label, fontWeight = FontWeight.SemiBold)
            Text(app.packageName, color = Muted, fontSize = Dimens.sp10)
        }
    }
}

@Composable
private fun PrivateSpaceMessage(message: Int) {
    Text(
        stringResource(message),
        modifier = Modifier.fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                RoundedCornerShape(Dimens.dp16),
            )
            .padding(Dimens.dp18),
        color = Muted,
    )
}
