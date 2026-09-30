package com.katoaapps.openminilaunch.ui.apps.private

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.privatespace.PrivateSpaceRepository
import com.katoaapps.openminilaunch.features.privatespace.PrivateSpaceSnapshot
import com.katoaapps.openminilaunch.features.privatespace.PrivateSpaceStatus
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.apps.ProfileDrawerHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun PrivateAppsScreen(
    repository: PrivateSpaceRepository,
    actions: DeviceActions,
    demoMode: Boolean,
    userGatewaySelectionKey: String?,
    onUserGatewayUnavailable: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var demoLocked by remember { mutableStateOf(false) }
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
        ProfileDrawerHeader(
            title = stringResource(R.string.privacy_apps),
            subtitle = if (demoMode) {
                stringResource(R.string.privacy_apps_android_private_space)
            } else {
                snapshot?.gateway?.label
                    ?: userGateway?.label
                    ?: stringResource(R.string.privacy_apps_android_private_space)
            },
            icon = Icons.Default.Shield,
            onClose = onClose,
        )
        when (val current = snapshot) {
            null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> PrivateSpaceContent(
                snapshot = if (demoMode) {
                    PrivateSpaceSnapshot(
                        if (demoLocked) PrivateSpaceStatus.LOCKED else PrivateSpaceStatus.UNLOCKED,
                    )
                } else {
                    current
                },
                actions = actions,
                demoMode = demoMode,
                userGateway = userGateway.takeUnless { demoMode },
                onOpenSettings = {
                    if (!repository.openSettings(context)) {
                        context.showLongToast(R.string.private_space_settings_unavailable)
                    }
                },
                onLockChanged = { unlocked ->
                    if (demoMode) {
                        demoLocked = !unlocked
                    } else if (!repository.setLocked(locked = !unlocked)) {
                        context.showLongToast(R.string.privacy_apps_state_change_failed)
                    }
                },
                onLaunch = { app ->
                    if (!repository.launch(app)) {
                        context.showLongToast(
                            context.getString(R.string.launcher_app_unavailable, app.label),
                        )
                    }
                },
                onLaunchGateway = { gateway ->
                    if (!repository.launchGateway(gateway)) {
                        context.showLongToast(gateway.unavailableMessageRes)
                    }
                },
                onLaunchUserGateway = { gateway ->
                    if (!actions.launchLauncherTarget(gateway)) {
                        onUserGatewayUnavailable()
                        context.showLongToast(
                            context.getString(R.string.launcher_app_unavailable, gateway.label),
                        )
                    }
                },
            )
        }
    }
}

private fun android.content.Context.showLongToast(message: Int) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

private fun android.content.Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
