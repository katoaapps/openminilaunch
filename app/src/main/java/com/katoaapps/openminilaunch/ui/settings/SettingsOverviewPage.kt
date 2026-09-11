package com.katoaapps.openminilaunch.ui.settings

import com.katoaapps.openminilaunch.BuildConfig
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.socialGoalLabel

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
internal fun SettingsOverviewPage(
    store: LauncherStore,
    actions: DeviceActions,
    permissionState: SettingsPermissionState,
    onNavigate: (SettingsDestination) -> Unit,
    goBack: () -> Unit,
) {
    val appName = stringResource(R.string.app_name)
    SettingsPage(stringResource(R.string.settings), goBack) {
        Text(
            stringResource(R.string.settings_overview_description, appName),
            color = Muted,
            fontSize = Dimens.sp13,
            modifier = Modifier.padding(bottom = Dimens.dp2),
        )
        SettingsCategoryRow(
            title = stringResource(R.string.launcher),
            subtitle = stringResource(R.string.settings_launcher_summary),
            status = stringResource(store.themePreference.labelRes),
            icon = Icons.Default.Home,
        ) { onNavigate(SettingsDestination.LAUNCHER) }
        SettingsCategoryRow(
            title = stringResource(R.string.magic_box),
            subtitle = stringResource(R.string.settings_magic_box_summary),
            status = folderCountLabel(store.searchFolders.size),
            icon = Icons.Default.AutoAwesome,
        ) { onNavigate(SettingsDestination.MAGIC_BOX) }
        SettingsCategoryRow(
            title = stringResource(R.string.mink_assistant),
            subtitle = stringResource(R.string.settings_assistant_summary),
            status = stringResource(
                if (permissionState.assistantRoleHeld) R.string.status_active else R.string.status_optional,
            ),
            icon = Icons.Default.Assistant,
        ) { onNavigate(SettingsDestination.MINK_ASSISTANT) }
        SettingsCategoryRow(
            title = stringResource(R.string.messaging),
            subtitle = stringResource(R.string.settings_messaging_summary),
            status = store.preferredMessagingPackage?.let(actions::appLabel)
                ?: actions.defaultMessagingAppLabel(),
            icon = Icons.AutoMirrored.Filled.Chat,
        ) { onNavigate(SettingsDestination.MESSAGING) }
        SettingsCategoryRow(
            title = stringResource(R.string.mink_day),
            subtitle = stringResource(R.string.settings_mink_day_summary),
            status = stringResource(
                R.string.two_part_label,
                socialGoalLabel(store.socialGoalMinutes),
                stringResource(store.minkAppPauseMode.labelRes),
            ),
            icon = Icons.Default.Pets,
        ) { onNavigate(SettingsDestination.MINK_DAY) }
        SettingsCategoryRow(
            title = stringResource(R.string.backup_and_restore),
            subtitle = stringResource(R.string.settings_backup_summary),
            status = stringResource(R.string.backup_local_file),
            icon = Icons.Default.ImportExport,
        ) { onNavigate(SettingsDestination.BACKUP_RESTORE) }
        SettingsCategoryRow(
            title = stringResource(R.string.permissions),
            subtitle = stringResource(R.string.settings_permissions_summary),
            status = stringResource(
                R.string.permission_active_count,
                permissionState.activeCount,
                permissionState.supportedCount,
            ),
            icon = Icons.Default.Security,
        ) { onNavigate(SettingsDestination.PERMISSIONS) }
        SettingsCategoryRow(
            title = stringResource(R.string.about),
            subtitle = stringResource(R.string.settings_about_summary),
            status = stringResource(R.string.version_summary, BuildConfig.VERSION_NAME),
            icon = Icons.Default.Info,
        ) { onNavigate(SettingsDestination.ABOUT) }
    }
}
