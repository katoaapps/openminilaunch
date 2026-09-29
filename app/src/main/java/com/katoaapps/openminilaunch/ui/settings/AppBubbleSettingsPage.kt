package com.katoaapps.openminilaunch.ui.settings

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.components.PageHeader
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.components.SettingsRow
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Manages the personal apps which Magic Box should open as Android bubbles. */
@Composable
internal fun AppBubbleSettingsPage(
    store: LauncherStore,
    actions: DeviceActions,
    permissionState: SettingsPermissionState,
    permissionActions: SettingsPermissionActions,
    goBack: () -> Unit,
) {
    val appsRevision by actions.launcherAppsRevision.collectAsState()
    val apps by produceState<List<LauncherAppTarget>>(emptyList(), appsRevision) {
        value = withContext(Dispatchers.IO) {
            actions.installedApps()
                .filter { it.isAvailable && !it.isWorkProfile }
                .sortedBy { it.label.lowercase() }
        }
    }
    var search by remember { mutableStateOf(TextFieldValue()) }
    val selectedKeys = store.automaticAppBubbleTargets
    val visibleApps = remember(apps, search.text, selectedKeys) {
        val query = search.text.trim()
        apps.filter { target ->
            query.isBlank() ||
                target.label.contains(query, ignoreCase = true) ||
                target.packageName.contains(query, ignoreCase = true)
        }.sortedWith(
            compareByDescending<LauncherAppTarget> { it.selectionKey in selectedKeys }
                .thenBy { it.label.lowercase() },
        )
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.widthIn(max = Dimens.dp720).fillMaxWidth()) {
            PageHeader(
                title = stringResource(R.string.app_bubbles_beta),
                goBack = goBack,
                showBackButton = LocalSettingsShowBackButton.current,
            )
        }
        LazyColumn(
            modifier = Modifier.widthIn(max = Dimens.dp720).fillMaxWidth().weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = Dimens.dp22,
                end = Dimens.dp22,
                bottom = Dimens.dp24,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp12),
        ) {
            item {
                Text(
                    stringResource(R.string.app_bubbles_description),
                    color = Muted,
                    fontSize = Dimens.sp13,
                )
            }
            if (!permissionState.appBubblesSupported) {
                item {
                    Text(
                        stringResource(R.string.app_bubbles_requires_android_11),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                item { SectionLabel(stringResource(R.string.permissions)) }
                item {
                    SettingsRow(
                        title = stringResource(R.string.app_bubble_notifications),
                        subtitle = stringResource(
                            if (permissionState.appBubbleNotificationsGranted) {
                                R.string.status_on
                            } else {
                                R.string.status_off
                            },
                        ),
                        icon = Icons.Default.Notifications,
                        onClick = if (permissionState.appBubbleNotificationsGranted) {
                            permissionActions.manageAppBubbleNotifications
                        } else {
                            permissionActions.requestAppBubbleNotifications
                        },
                    )
                }
                item {
                    SettingsRow(
                        title = stringResource(R.string.android_bubble_access),
                        subtitle = stringResource(
                            if (permissionState.appBubblesAllowed) {
                                R.string.status_on
                            } else {
                                R.string.status_off
                            },
                        ),
                        icon = Icons.Default.BubbleChart,
                        onClick = permissionActions.manageAppBubbles,
                    )
                }
            }
            item {
                Text(
                    stringResource(R.string.app_bubbles_personal_only),
                    color = Muted,
                    fontSize = Dimens.sp12,
                )
            }
            item { SectionLabel(stringResource(R.string.automatic_app_bubbles)) }
            item {
                Text(
                    stringResource(R.string.automatic_app_bubbles_description),
                    color = Muted,
                    fontSize = Dimens.sp12,
                )
            }
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.search_apps)) },
                )
            }
            if (visibleApps.isEmpty()) {
                item { Text(stringResource(R.string.no_apps_found), color = Muted) }
            }
            items(visibleApps, key = LauncherAppTarget::selectionKey) { app ->
                AppBubblePreferenceRow(
                    target = app,
                    actions = actions,
                    enabled = permissionState.appBubblesSupported,
                    checked = app.selectionKey in selectedKeys,
                    onCheckedChange = { enabled ->
                        store.setAppBubbleAutomatic(app.selectionKey, enabled)
                    },
                )
            }
            item { Spacer(Modifier.height(Dimens.dp12)) }
        }
    }
}

@Composable
private fun AppBubblePreferenceRow(
    target: LauncherAppTarget,
    actions: DeviceActions,
    enabled: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                RoundedCornerShape(Dimens.dp16),
            )
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(Dimens.dp12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LauncherTargetIcon(target, actions, Dimens.dp36)
        Column(Modifier.weight(1f).padding(horizontal = Dimens.dp12)) {
            Text(target.label, fontWeight = FontWeight.SemiBold)
            Text(target.packageName, color = Muted, fontSize = Dimens.sp10)
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
    }
}
