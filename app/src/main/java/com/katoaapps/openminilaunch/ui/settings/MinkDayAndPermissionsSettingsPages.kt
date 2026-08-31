package com.katoaapps.openminilaunch.ui.settings

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.components.SettingsRow
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.wellbeing.MinkDailyLimitControl
import com.katoaapps.openminilaunch.ui.wellbeing.MinkPauseModeControl

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
internal fun MinkDaySettingsPage(
    store: LauncherStore,
    usageAccessGranted: Boolean,
    onPickSocialApps: () -> Unit,
    onOpenPermissions: () -> Unit,
    goBack: () -> Unit,
) {
    SettingsPage(stringResource(R.string.mink_day), goBack) {
        Text(stringResource(R.string.mink_day_settings_description), color = Muted, fontSize = Dimens.sp13)
        SettingsRow(
            stringResource(R.string.usage_access),
            stringResource(if (usageAccessGranted) R.string.usage_access_active else R.string.usage_access_open_permissions),
            Icons.Default.Security,
            onClick = onOpenPermissions,
        )
        SectionLabel(stringResource(R.string.daily_social_goal))
        MinkDailyLimitControl(store)
        MinkPauseModeControl(store)
        SettingsRow(
            stringResource(R.string.apps_for_mink_day),
            if (store.usesAutomaticSocialApps) stringResource(R.string.automatic_android_categories)
            else stringResource(R.string.selected_count, store.socialPackages.size),
            Icons.Default.Apps,
            onClick = onPickSocialApps,
        )
    }
}

@Composable
internal fun PermissionsSettingsPage(
    state: SettingsPermissionState,
    actions: SettingsPermissionActions,
    goBack: () -> Unit,
) {
    val appName = stringResource(R.string.app_name)
    SettingsPage(stringResource(R.string.permissions), goBack) {
        Text(stringResource(R.string.permissions_description), color = Muted, fontSize = Dimens.sp13)
        PermissionCard(
            title = stringResource(R.string.mink_day_usage),
            description = stringResource(R.string.mink_day_usage_permission_description),
            granted = state.usageAccessGranted,
            icon = Icons.Default.Pets,
            onGrant = actions.requestUsageAccess,
            onManage = actions.manageUsageAccess,
        )
        PermissionCard(
            title = stringResource(R.string.conversations),
            description = stringResource(R.string.conversations_permission_description),
            granted = state.notificationAccessGranted,
            icon = Icons.Default.Forum,
            onGrant = actions.requestNotificationAccess,
            onManage = actions.manageNotificationAccess,
        )
        PermissionCard(
            title = stringResource(R.string.contacts),
            description = stringResource(R.string.contacts_permission_description),
            granted = state.contactsGranted,
            icon = Icons.Default.Contacts,
            onGrant = actions.requestContacts,
            onManage = actions.manageAppPermissions,
        )
        if (state.directCallsSupported) {
            PermissionCard(
                title = stringResource(R.string.direct_calls),
                description = stringResource(R.string.direct_calls_permission_description, appName),
                granted = state.callsGranted,
                icon = Icons.Default.Phone,
                onGrant = actions.requestCalls,
                onManage = actions.manageAppPermissions,
            )
        } else {
            SettingsRow(
                stringResource(R.string.direct_calls),
                stringResource(R.string.direct_calls_unsupported),
                Icons.Default.Phone,
                enabled = false,
            ) { }
        }
        if (state.directSmsSupported) {
            PermissionCard(
                title = stringResource(R.string.direct_sms),
                description = if (state.assistantRoleHeld) {
                    stringResource(R.string.direct_sms_permission_description)
                } else {
                    stringResource(R.string.direct_sms_requires_assistant)
                },
                granted = state.smsGranted,
                icon = Icons.AutoMirrored.Filled.Send,
                onGrant = if (state.assistantRoleHeld) actions.requestSms else actions.showAssistantSetup,
                onManage = actions.manageAppPermissions,
            )
        } else {
            SettingsRow(
                stringResource(R.string.direct_sms),
                stringResource(R.string.direct_sms_unsupported),
                Icons.AutoMirrored.Filled.Send,
                enabled = false,
            ) { }
        }
        PermissionCard(
            title = stringResource(R.string.photos_videos_audio),
            description = stringResource(R.string.media_permission_description, appName),
            granted = state.mediaGranted,
            icon = Icons.Default.PhotoLibrary,
            onGrant = actions.requestMedia,
            onManage = actions.manageAppPermissions,
        )
        if (state.lockSupported) {
            PermissionCard(
                title = stringResource(R.string.double_tap_screen_lock),
                description = stringResource(R.string.double_tap_permission_description),
                granted = state.lockServiceEnabled,
                icon = Icons.Default.Lock,
                onGrant = actions.requestLockService,
                onManage = actions.manageLockService,
            )
        } else {
            SettingsRow(
                stringResource(R.string.double_tap_screen_lock),
                stringResource(R.string.requires_android_9),
                Icons.Default.Lock,
                enabled = false,
            ) { }
        }
    }
}
