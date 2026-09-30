package com.katoaapps.openminilaunch.ui.apps.work

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
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
import com.katoaapps.openminilaunch.features.workprofile.WorkProfileSnapshot
import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.apps.ProfileDrawerHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class WorkAppsUiState(
    val profile: WorkProfileSnapshot,
    val apps: List<LauncherAppTarget>,
)

@Composable
internal fun WorkAppsScreen(
    actions: DeviceActions,
    demoMode: Boolean,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var demoPaused by remember { mutableStateOf(false) }
    val appsRevision by actions.launcherAppsRevision.collectAsState()
    val state by produceState<WorkAppsUiState?>(null, appsRevision) {
        value = withContext(Dispatchers.IO) {
            WorkAppsUiState(
                profile = actions.workProfileSnapshot(),
                apps = actions.installedApps().filter(LauncherAppTarget::isWorkProfile),
            )
        }
    }

    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProfileDrawerHeader(
            title = stringResource(R.string.work_apps),
            subtitle = stringResource(R.string.work_profile),
            icon = Icons.Default.BusinessCenter,
            onClose = onClose,
        )
        when (val current = state) {
            null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> WorkAppsContent(
                profile = if (demoMode) {
                    WorkProfileSnapshot(available = true, paused = demoPaused)
                } else {
                    current.profile
                },
                apps = current.apps,
                actions = actions,
                demoMode = demoMode,
                onProfileEnabledChange = { enabled ->
                    if (demoMode) {
                        demoPaused = !enabled
                    } else if (actions.setWorkProfilePaused(paused = !enabled)) {
                        actions.invalidateInstalledApps()
                    } else {
                        Toast.makeText(
                            context,
                            R.string.work_profile_state_change_failed,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
                onLaunch = { app ->
                    if (!actions.launchLauncherTarget(app)) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.launcher_app_unavailable, app.label),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
            )
        }
    }
}
