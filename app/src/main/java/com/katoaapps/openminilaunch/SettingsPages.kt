package com.katoaapps.openminilaunch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class SettingsDestination {
    OVERVIEW,
    LAUNCHER,
    APPEARANCE,
    SHORTCUTS,
    MAGIC_BOX,
    FILE_SEARCH,
    MINK_DAY,
    PERMISSIONS,
    ABOUT,
}

internal val LocalSettingsScrollState = staticCompositionLocalOf { ScrollState(0) }

internal fun pushSettingsDestination(
    stack: List<SettingsDestination>,
    destination: SettingsDestination,
): List<SettingsDestination> = if (stack.lastOrNull() == destination) stack else stack + destination

internal fun popSettingsDestination(stack: List<SettingsDestination>): List<SettingsDestination> =
    if (stack.size > 1) stack.dropLast(1) else stack

internal data class SettingsPermissionState(
    val usageAccessGranted: Boolean,
    val notificationAccessGranted: Boolean,
    val contactsGranted: Boolean,
    val directCallsSupported: Boolean,
    val callsGranted: Boolean,
    val directSmsSupported: Boolean,
    val assistantRoleHeld: Boolean,
    val smsGranted: Boolean,
    val mediaGranted: Boolean,
    val lockSupported: Boolean,
    val lockServiceEnabled: Boolean,
) {
    val supportedCount: Int
        get() = 4 + directCallsSupported.toInt() + directSmsSupported.toInt() + lockSupported.toInt()

    val activeCount: Int
        get() = listOf(
            usageAccessGranted,
            notificationAccessGranted,
            contactsGranted,
            mediaGranted,
        ).count { it } +
            (directCallsSupported && callsGranted).toInt() +
            (directSmsSupported && smsGranted).toInt() +
            (lockSupported && lockServiceEnabled).toInt()
}

private fun Boolean.toInt(): Int = if (this) 1 else 0

@Composable
private fun folderCountLabel(count: Int): String =
    pluralStringResource(R.plurals.folder_count, count, count)

internal data class SettingsPermissionActions(
    val requestUsageAccess: () -> Unit,
    val manageUsageAccess: () -> Unit,
    val requestNotificationAccess: () -> Unit,
    val manageNotificationAccess: () -> Unit,
    val requestContacts: () -> Unit,
    val requestCalls: () -> Unit,
    val requestSms: () -> Unit,
    val requestMedia: () -> Unit,
    val manageAppPermissions: () -> Unit,
    val requestLockService: () -> Unit,
    val manageLockService: () -> Unit,
    val showAssistantSetup: () -> Unit,
)

@Composable
internal fun SettingsOverviewPage(
    store: LauncherStore,
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
            title = stringResource(R.string.mink_day),
            subtitle = stringResource(R.string.settings_mink_day_summary),
            status = socialGoalLabel(store.socialGoalMinutes),
            icon = Icons.Default.Pets,
        ) { onNavigate(SettingsDestination.MINK_DAY) }
        SettingsCategoryRow(
            title = stringResource(R.string.permissions),
            subtitle = stringResource(R.string.settings_permissions_summary),
            status = stringResource(R.string.permission_active_count, permissionState.activeCount, permissionState.supportedCount),
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

@Composable
internal fun LauncherSettingsPage(
    store: LauncherStore,
    assistantRoleHeld: Boolean,
    requestHomeRole: () -> Unit,
    showAssistantDisclosure: () -> Unit,
    onNavigate: (SettingsDestination) -> Unit,
    goBack: () -> Unit,
) {
    val appName = stringResource(R.string.app_name)
    SettingsPage(stringResource(R.string.launcher), goBack) {
        SectionLabel(stringResource(R.string.system_roles))
        SettingsRow(stringResource(R.string.default_home_app), stringResource(R.string.choose_as_launcher, appName), Icons.Default.Home, onClick = requestHomeRole)
        SettingsRow(
            stringResource(R.string.mink_assistant),
            stringResource(if (assistantRoleHeld) R.string.assistant_active_summary else R.string.assistant_optional_summary),
            Icons.Default.Assistant,
            onClick = showAssistantDisclosure,
        )
        HorizontalDivider(color = Sage)
        SectionLabel(stringResource(R.string.customize))
        SettingsRow(
            stringResource(R.string.appearance),
            stringResource(R.string.appearance_summary, stringResource(store.themePreference.labelRes)),
            Icons.Default.Palette,
        ) { onNavigate(SettingsDestination.APPEARANCE) }
        SettingsRow(
            stringResource(R.string.shortcuts_and_drawer),
            pluralStringResource(
                R.plurals.shortcuts_drawer_summary,
                store.drawerPackages.size,
                store.drawerPackages.size,
                MAX_DRAWER_APPS,
            ),
            Icons.Default.GridView,
        ) { onNavigate(SettingsDestination.SHORTCUTS) }
    }
}

@Composable
internal fun AppearanceSettingsPage(store: LauncherStore, goBack: () -> Unit) {
    SettingsPage(stringResource(R.string.appearance), goBack) {
        ThemeChooser(store.themePreference, store::setTheme)
        HomePanelColorSetting(store.homePanelColorArgb, store::setHomePanelColor)
    }
}

@Composable
internal fun ShortcutsSettingsPage(
    store: LauncherStore,
    actions: DeviceActions,
    onPickShortcut: (Shortcut) -> Unit,
    onPickDrawer: () -> Unit,
    goBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    SettingsPage(stringResource(R.string.shortcuts_and_drawer), goBack) {
        SectionLabel(stringResource(R.string.home_shortcuts))
        Text(
            stringResource(R.string.home_shortcuts_description),
            color = Muted,
            fontSize = Dimens.sp13,
        )
        configurableShortcuts.forEach { shortcut ->
            val packageName = store.shortcutPackages[shortcut]
            ShortcutAssignmentRow(
                shortcut = shortcut,
                packageName = packageName,
                actions = actions,
                subtitle = packageName?.let(actions::appLabel)
                    ?: stringResource(R.string.shortcut_default, shortcut.displayLabel()),
            ) { onPickShortcut(shortcut) }
        }
        SettingsRow(
            stringResource(R.string.reset_home_grid_order),
            stringResource(R.string.reset_home_grid_order_description),
            Icons.Default.Restore,
        ) {
            store.resetShortcutOrder()
            android.widget.Toast.makeText(context, context.getString(R.string.shortcut_order_reset), android.widget.Toast.LENGTH_SHORT).show()
        }
        HorizontalDivider(color = Sage)
        SectionLabel(stringResource(R.string.app_drawer))
        SettingsRow(stringResource(R.string.selected_apps), stringResource(R.string.count_of_max, store.drawerPackages.size, MAX_DRAWER_APPS), Icons.Default.Apps, onClick = onPickDrawer)
        Text(stringResource(R.string.magic_box_find_other_apps), color = Muted, fontSize = Dimens.sp13)
    }
}

@Composable
internal fun MagicBoxSettingsPage(
    store: LauncherStore,
    actions: DeviceActions,
    mediaGranted: Boolean,
    onPickWeb: () -> Unit,
    onPickAi: () -> Unit,
    onOpenFileSearch: () -> Unit,
    goBack: () -> Unit,
) {
    SettingsPage(stringResource(R.string.magic_box), goBack) {
        SectionLabel(stringResource(R.string.search))
        SettingsRow(
            stringResource(R.string.web_app),
            store.preferredWebPackage?.let(actions::appLabel) ?: stringResource(R.string.system_browser),
            Icons.Default.Public,
            onClick = onPickWeb,
        )
        SettingsRow(
            stringResource(R.string.ai_app),
            store.preferredAiPackage?.let(actions::appLabel) ?: stringResource(R.string.choose_on_first_use),
            Icons.Default.AutoAwesome,
            onClick = onPickAi,
        )
        Text(
            stringResource(R.string.ai_provider_privacy_description),
            color = Muted,
            fontSize = Dimens.sp13,
        )
        HorizontalDivider(color = Sage)
        SectionLabel(stringResource(R.string.messaging))
        MessageSendModeChooser(store.messageSendMode, store::updateMessageSendMode)
        Text(
            stringResource(R.string.messaging_mode_description),
            color = Muted,
            fontSize = Dimens.sp13,
        )
        HorizontalDivider(color = Sage)
        SectionLabel(stringResource(R.string.files_section))
        SettingsRow(
            stringResource(R.string.file_search),
            stringResource(
                R.string.file_search_status,
                folderCountLabel(store.searchFolders.size),
                stringResource(if (mediaGranted) R.string.status_on else R.string.status_off),
            ),
            Icons.Default.FolderOpen,
            onClick = onOpenFileSearch,
        )
    }
}

@Composable
internal fun FileSearchSettingsPage(
    store: LauncherStore,
    mediaGranted: Boolean,
    onOpenPermissions: () -> Unit,
    onAddFolder: () -> Unit,
    onRemoveFolder: (SearchFolder) -> Unit,
    goBack: () -> Unit,
) {
    val appName = stringResource(R.string.app_name)
    SettingsPage(stringResource(R.string.file_search), goBack) {
        Text(
            stringResource(R.string.file_search_description, appName),
            color = Muted,
            fontSize = Dimens.sp13,
        )
        SettingsRow(
            stringResource(R.string.photos_videos_audio),
            stringResource(if (mediaGranted) R.string.media_access_active else R.string.media_access_open_permissions),
            Icons.Default.PhotoLibrary,
            onClick = onOpenPermissions,
        )
        OutlinedButton(onClick = onAddFolder, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.CreateNewFolder, null)
            Text(stringResource(R.string.add_search_folder), Modifier.padding(start = Dimens.dp8))
        }
        if (store.searchFolders.isEmpty()) {
            Text(stringResource(R.string.no_document_folders), color = Muted, fontSize = Dimens.sp12)
        } else {
            store.searchFolders.forEach { folder ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.dp14))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(start = Dimens.dp14, end = Dimens.dp4),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Folder, null, tint = Rust)
                    Text(folder.label, Modifier.weight(1f).padding(horizontal = Dimens.dp10), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { onRemoveFolder(folder) }) {
                        Icon(Icons.Default.Close, stringResource(R.string.remove_folder, folder.label))
                    }
                }
            }
        }
    }
}

@Composable
internal fun MinkDaySettingsPage(
    store: LauncherStore,
    usageAccessGranted: Boolean,
    onPickSocialApps: () -> Unit,
    onOpenPermissions: () -> Unit,
    goBack: () -> Unit,
) {
    SettingsPage(stringResource(R.string.mink_day), goBack) {
        Text(
            stringResource(R.string.mink_day_settings_description),
            color = Muted,
            fontSize = Dimens.sp13,
        )
        SettingsRow(
            stringResource(R.string.usage_access),
            stringResource(if (usageAccessGranted) R.string.usage_access_active else R.string.usage_access_open_permissions),
            Icons.Default.Security,
            onClick = onOpenPermissions,
        )
        SectionLabel(stringResource(R.string.daily_social_goal))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.dp7)) {
            SOCIAL_GOAL_OPTIONS.forEach { minutes ->
                FilterChip(
                    selected = store.socialGoalMinutes == minutes,
                    onClick = { store.updateSocialGoalMinutes(minutes) },
                    label = { Text(socialGoalLabel(minutes)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        SettingsRow(
            stringResource(R.string.apps_you_want_to_limit),
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
        Text(
            stringResource(R.string.permissions_description),
            color = Muted,
            fontSize = Dimens.sp13,
        )
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
            SettingsRow(stringResource(R.string.direct_calls), stringResource(R.string.direct_calls_unsupported), Icons.Default.Phone, enabled = false) { }
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
            SettingsRow(stringResource(R.string.direct_sms), stringResource(R.string.direct_sms_unsupported), Icons.AutoMirrored.Filled.Send, enabled = false) { }
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
            SettingsRow(stringResource(R.string.double_tap_screen_lock), stringResource(R.string.requires_android_9), Icons.Default.Lock, enabled = false) { }
        }
    }
}

@Composable
internal fun AboutSettingsPage(
    actions: DeviceActions,
    onRepeatTutorial: () -> Unit,
    goBack: () -> Unit,
) {
    val appName = stringResource(R.string.app_name)
    SettingsPage(stringResource(R.string.about), goBack) {
        SettingsRow(stringResource(R.string.email_us), stringResource(R.string.support_email), Icons.Default.Email, onClick = actions::emailSupport)
        SettingsRow(stringResource(R.string.privacy_policy), stringResource(R.string.privacy_policy_summary, appName), Icons.Default.PrivacyTip, onClick = actions::openPrivacyPolicy)
        SettingsRow(stringResource(R.string.terms_of_use), stringResource(R.string.terms_summary), Icons.Default.Gavel, onClick = actions::openTermsOfUse)
        SettingsRow(
            stringResource(R.string.repeat_tutorial),
            stringResource(R.string.repeat_tutorial_summary),
            Icons.Default.School,
            onClick = onRepeatTutorial,
        )
        Row(Modifier.fillMaxWidth().padding(vertical = Dimens.dp12)) {
            Text(stringResource(R.string.version), Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(BuildConfig.VERSION_NAME, color = Muted)
        }
    }
}

@Composable
private fun SettingsPage(
    title: String,
    goBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        PageHeader(title, goBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(LocalSettingsScrollState.current).padding(horizontal = Dimens.dp22),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp14),
        ) {
            content()
            Spacer(Modifier.height(Dimens.dp24))
        }
    }
}

@Composable
private fun SettingsCategoryRow(
    title: String,
    subtitle: String,
    status: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.dp18))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.dp14, vertical = Dimens.dp13),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(Modifier.size(Dimens.dp40), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(Dimens.dp21), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = Dimens.dp12)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = Dimens.sp12, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(status, color = Muted, fontSize = Dimens.sp10, maxLines = 1)
            Icon(Icons.Default.ChevronRight, null, tint = Muted, modifier = Modifier.size(Dimens.dp20))
        }
    }
}
