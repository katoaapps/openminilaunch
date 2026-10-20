package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.BuildConfig
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.minkpad.MinkPadLauncherEntry
import com.katoaapps.openminilaunch.model.MAX_DRAWER_APPS
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.profile.openVirtualContactCard
import com.katoaapps.openminilaunch.ui.theme.socialGoalLabel

@Composable
internal fun SettingsOverviewPage(
    section: SettingsSection,
    store: LauncherStore,
    actions: DeviceActions,
    permissionState: SettingsPermissionState,
    onPickWeb: () -> Unit,
    onPickAi: () -> Unit,
    onNavigate: (SettingsDestination) -> Unit,
    goBack: () -> Unit,
) {
    when (section) {
        SettingsSection.HOME -> HomeSettingsSection(store, actions, onNavigate, goBack)
        SettingsSection.MAGIC_BOX -> MagicBoxSettingsPage(
            store = store,
            actions = actions,
            mediaGranted = permissionState.mediaGranted,
            onPickWeb = onPickWeb,
            onPickAi = onPickAi,
            onOpenFileSearch = { onNavigate(SettingsDestination.FILE_SEARCH) },
            onOpenAppBubbles = { onNavigate(SettingsDestination.APP_BUBBLES) },
            goBack = goBack,
        )
        SettingsSection.ADVANCED -> AdvancedSettingsSection(
            store = store,
            permissionState = permissionState,
            onNavigate = onNavigate,
            goBack = goBack,
        )
        SettingsSection.MORE -> MoreSettingsSection(permissionState, onNavigate, goBack)
    }
}

@Composable
private fun HomeSettingsSection(
    store: LauncherStore,
    actions: DeviceActions,
    onNavigate: (SettingsDestination) -> Unit,
    goBack: () -> Unit,
) {
    SettingsPage(stringResource(R.string.settings_section_home), goBack) {
        SettingsCategoryRow(
            title = stringResource(R.string.launcher),
            subtitle = stringResource(R.string.settings_launcher_home_summary),
            status = stringResource(store.themePreference.labelRes),
            icon = Icons.Default.Home,
        ) { onNavigate(SettingsDestination.LAUNCHER) }
        SettingsCategoryRow(
            title = stringResource(R.string.shortcuts_and_drawer),
            subtitle = pluralStringResource(
                R.plurals.shortcuts_drawer_summary,
                store.drawerTargets.size,
                store.drawerTargets.size,
                MAX_DRAWER_APPS,
            ),
            icon = Icons.Default.GridView,
        ) { onNavigate(SettingsDestination.SHORTCUTS) }
        SettingsCategoryRow(
            title = stringResource(R.string.messaging),
            subtitle = stringResource(R.string.settings_messaging_summary),
            status = store.preferredMessagingPackage?.let(actions::appLabel)
                ?: actions.defaultMessagingAppLabel(),
            icon = Icons.AutoMirrored.Filled.Chat,
        ) { onNavigate(SettingsDestination.MESSAGING) }
        SettingsCategoryRow(
            title = stringResource(R.string.mink_space_mini_apps),
            subtitle = stringResource(R.string.mink_space_settings_summary),
            icon = Icons.Default.ViewCarousel,
        ) { onNavigate(SettingsDestination.MINK_SPACE) }
        SettingsCategoryRow(
            title = stringResource(R.string.privacy_apps),
            subtitle = stringResource(R.string.privacy_apps_settings_summary),
            icon = Icons.Default.Shield,
        ) { onNavigate(SettingsDestination.PRIVACY_APPS) }
    }
}

@Composable
private fun AdvancedSettingsSection(
    store: LauncherStore,
    permissionState: SettingsPermissionState,
    onNavigate: (SettingsDestination) -> Unit,
    goBack: () -> Unit,
) {
    val context = LocalContext.current
    SettingsPage(stringResource(R.string.settings_section_advanced), goBack) {
        SettingsCategoryRow(
            title = stringResource(R.string.minkpad_name),
            subtitle = stringResource(R.string.minkpad_tagline),
            icon = Icons.Default.Keyboard,
        ) { MinkPadLauncherEntry.open(context) }
        SettingsCategoryRow(
            title = stringResource(R.string.mink_assistant),
            subtitle = stringResource(R.string.settings_assistant_summary),
            status = stringResource(
                if (permissionState.assistantRoleHeld) R.string.status_active
                else R.string.status_optional,
            ),
            icon = Icons.Default.Assistant,
        ) { onNavigate(SettingsDestination.MINK_ASSISTANT) }
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
            title = stringResource(R.string.profile),
            subtitle = stringResource(R.string.vcard_settings_summary),
            icon = Icons.Default.AccountCircle,
        ) { openVirtualContactCard(context) }
    }
}

@Composable
private fun MoreSettingsSection(
    permissionState: SettingsPermissionState,
    onNavigate: (SettingsDestination) -> Unit,
    goBack: () -> Unit,
) {
    SettingsPage(stringResource(R.string.settings_section_more), goBack) {
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
            title = stringResource(R.string.backup_and_restore),
            subtitle = stringResource(R.string.settings_backup_summary),
            status = stringResource(R.string.backup_local_file),
            icon = Icons.Default.ImportExport,
        ) { onNavigate(SettingsDestination.BACKUP_RESTORE) }
        SettingsCategoryRow(
            title = stringResource(R.string.about),
            subtitle = stringResource(R.string.settings_about_summary),
            status = stringResource(R.string.version_summary, BuildConfig.VERSION_NAME),
            icon = Icons.Default.Info,
        ) { onNavigate(SettingsDestination.ABOUT) }
    }
}
