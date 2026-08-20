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

private fun folderCountLabel(count: Int): String = if (count == 1) "1 folder" else "$count folders"

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
    SettingsPage("Settings", goBack) {
        Text(
            "Everything has a home. Choose a section to make $appName yours.",
            color = Muted,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        SettingsCategoryRow(
            title = "Launcher",
            subtitle = "Home app, appearance, shortcuts and drawer",
            status = store.themePreference.label,
            icon = Icons.Default.Home,
        ) { onNavigate(SettingsDestination.LAUNCHER) }
        SettingsCategoryRow(
            title = "Magic Box",
            subtitle = "Search, AI, messages and local files",
            status = folderCountLabel(store.searchFolders.size),
            icon = Icons.Default.AutoAwesome,
        ) { onNavigate(SettingsDestination.MAGIC_BOX) }
        SettingsCategoryRow(
            title = "Mink’s Day",
            subtitle = "Choose the social apps you want to limit",
            status = socialGoalLabel(store.socialGoalMinutes),
            icon = Icons.Default.Pets,
        ) { onNavigate(SettingsDestination.MINK_DAY) }
        SettingsCategoryRow(
            title = "Permissions",
            subtitle = "Manage private device access in one place",
            status = "${permissionState.activeCount}/${permissionState.supportedCount} active",
            icon = Icons.Default.Security,
        ) { onNavigate(SettingsDestination.PERMISSIONS) }
        SettingsCategoryRow(
            title = "About",
            subtitle = "Help, legal information and tutorial",
            status = "v${BuildConfig.VERSION_NAME}",
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
    SettingsPage("Launcher", goBack) {
        SectionLabel("SYSTEM ROLES")
        SettingsRow("Default home app", "Choose $appName as your launcher", Icons.Default.Home, onClick = requestHomeRole)
        SettingsRow(
            "Mink Assistant",
            if (assistantRoleHeld) "Active · Magic Box from anywhere" else "Optional · use the system assistant gesture",
            Icons.Default.Assistant,
            onClick = showAssistantDisclosure,
        )
        HorizontalDivider(color = Sage)
        SectionLabel("CUSTOMIZE")
        SettingsRow(
            "Appearance",
            "${store.themePreference.label} theme · home panel color",
            Icons.Default.Palette,
        ) { onNavigate(SettingsDestination.APPEARANCE) }
        SettingsRow(
            "Shortcuts & Drawer",
            "Choose shortcut apps · ${store.drawerPackages.size}/$MAX_DRAWER_APPS drawer apps",
            Icons.Default.GridView,
        ) { onNavigate(SettingsDestination.SHORTCUTS) }
    }
}

@Composable
internal fun AppearanceSettingsPage(store: LauncherStore, goBack: () -> Unit) {
    SettingsPage("Appearance", goBack) {
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
    SettingsPage("Shortcuts & Drawer", goBack) {
        SectionLabel("SHORTCUT APPS")
        Text("Choose the app each shortcut opens. To-do and Drawer stay built in.", color = Muted, fontSize = 13.sp)
        configurableShortcuts.forEach { shortcut ->
            SettingsRow(
                shortcut.label,
                store.shortcutPackages[shortcut]?.let(actions::appLabel) ?: "System default",
                Icons.Default.ChevronRight,
            ) { onPickShortcut(shortcut) }
        }
        SettingsRow(
            "Reset Home grid order",
            "Return shortcuts to the original 4 × 2 order",
            Icons.Default.Restore,
        ) {
            store.resetShortcutOrder()
            android.widget.Toast.makeText(context, "Shortcut order reset", android.widget.Toast.LENGTH_SHORT).show()
        }
        HorizontalDivider(color = Sage)
        SectionLabel("APP DRAWER")
        SettingsRow("Selected apps", "${store.drawerPackages.size} of $MAX_DRAWER_APPS", Icons.Default.Apps, onClick = onPickDrawer)
        Text("Use ? in the Magic Box to find any other installed app.", color = Muted, fontSize = 13.sp)
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
    SettingsPage("Magic Box", goBack) {
        SectionLabel("SEARCH")
        SettingsRow(
            "Web app",
            store.preferredWebPackage?.let(actions::appLabel) ?: "System browser",
            Icons.Default.Public,
            onClick = onPickWeb,
        )
        SettingsRow(
            "AI app",
            store.preferredAiPackage?.let(actions::appLabel) ?: "Choose on first use",
            Icons.Default.AutoAwesome,
            onClick = onPickAi,
        )
        Text(
            "Known AI assistants are shown first. Other compatible apps remain available. Your query is handled under the selected app’s privacy terms.",
            color = Muted,
            fontSize = 13.sp,
        )
        HorizontalDivider(color = Sage)
        SectionLabel("MESSAGING")
        MessageSendModeChooser(store.messageSendMode, store::updateMessageSendMode)
        Text(
            "Always ask offers Send SMS now and Choose messaging app each time. Direct SMS uses your carrier and may incur charges; a chosen provider controls its own final send and delivery method.",
            color = Muted,
            fontSize = 13.sp,
        )
        HorizontalDivider(color = Sage)
        SectionLabel("FILES")
        SettingsRow(
            "File Search",
            "${folderCountLabel(store.searchFolders.size)} · media access ${if (mediaGranted) "on" else "off"}",
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
    SettingsPage("File Search", goBack) {
        Text(
            "Choose the document folders $appName may search. Only filenames are indexed, and everything remains on this device.",
            color = Muted,
            fontSize = 13.sp,
        )
        SettingsRow(
            "Photos, videos & audio",
            if (mediaGranted) "Media filename access is active" else "Open Permissions to enable media search",
            Icons.Default.PhotoLibrary,
            onClick = onOpenPermissions,
        )
        OutlinedButton(onClick = onAddFolder, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.CreateNewFolder, null)
            Text("Add search folder", Modifier.padding(start = 8.dp))
        }
        if (store.searchFolders.isEmpty()) {
            Text("No document folders selected.", color = Muted, fontSize = 12.sp)
        } else {
            store.searchFolders.forEach { folder ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(start = 14.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Folder, null, tint = Rust)
                    Text(folder.label, Modifier.weight(1f).padding(horizontal = 10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { onRemoveFolder(folder) }) {
                        Icon(Icons.Default.Close, "Remove ${folder.label}")
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
    SettingsPage("Mink’s Day", goBack) {
        Text(
            "Mink measures time and opens only for the social apps you choose, then keeps those insights on this device.",
            color = Muted,
            fontSize = 13.sp,
        )
        SettingsRow(
            "Usage access",
            if (usageAccessGranted) "Active for private on-device insights" else "Open Permissions to enable insights",
            Icons.Default.Security,
            onClick = onOpenPermissions,
        )
        SectionLabel("DAILY SOCIAL GOAL")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
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
            "Apps you want to limit",
            if (store.socialPackages.isEmpty()) "Automatic Android categories" else "${store.socialPackages.size} selected",
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
    SettingsPage("Permissions", goBack) {
        Text(
            "Access is optional and can be changed at any time. Device data stays local unless you explicitly hand it to another app.",
            color = Muted,
            fontSize = 13.sp,
        )
        PermissionCard(
            title = "Mink’s Day usage",
            description = "Reads foreground activity for your tracked social apps only to calculate private, on-device daily insights.",
            granted = state.usageAccessGranted,
            icon = Icons.Default.Pets,
            onGrant = actions.requestUsageAccess,
            onManage = actions.manageUsageAccess,
        )
        PermissionCard(
            title = "Conversations",
            description = "Shows active message and email conversations locally and uses reply actions supplied by their apps.",
            granted = state.notificationAccessGranted,
            icon = Icons.Default.Forum,
            onGrant = actions.requestNotificationAccess,
            onManage = actions.manageNotificationAccess,
        )
        PermissionCard(
            title = "Contacts",
            description = "Used only for @ messages and # calls.",
            granted = state.contactsGranted,
            icon = Icons.Default.Contacts,
            onGrant = actions.requestContacts,
            onManage = actions.manageAppPermissions,
        )
        if (state.directCallsSupported) {
            PermissionCard(
                title = "Direct calls",
                description = "Used only after you confirm a # call in $appName.",
                granted = state.callsGranted,
                icon = Icons.Default.Phone,
                onGrant = actions.requestCalls,
                onManage = actions.manageAppPermissions,
            )
        } else {
            SettingsRow("Direct calls", "Not supported on this device; # uses the dialer", Icons.Default.Phone, enabled = false) { }
        }
        if (state.directSmsSupported) {
            PermissionCard(
                title = "Direct SMS",
                description = if (state.assistantRoleHeld) {
                    "Used when you press @ Send with direct SMS selected. Uses carrier SMS, not RCS; messaging rates may apply."
                } else {
                    "Choose Mink Assistant first. Google Play restricts direct SMS access to eligible default handlers."
                },
                granted = state.smsGranted,
                icon = Icons.AutoMirrored.Filled.Send,
                onGrant = if (state.assistantRoleHeld) actions.requestSms else actions.showAssistantSetup,
                onManage = actions.manageAppPermissions,
            )
        } else {
            SettingsRow("Direct SMS", "Not supported on this device; @ opens Messages", Icons.AutoMirrored.Filled.Send, enabled = false) { }
        }
        PermissionCard(
            title = "Photos, videos & audio",
            description = "Searches media filenames locally. $appName never uploads your library.",
            granted = state.mediaGranted,
            icon = Icons.Default.PhotoLibrary,
            onGrant = actions.requestMedia,
            onManage = actions.manageAppPermissions,
        )
        if (state.lockSupported) {
            PermissionCard(
                title = "Double-tap screen lock",
                description = "Uses Android's Lock screen action only after you double-tap. It cannot read screen content.",
                granted = state.lockServiceEnabled,
                icon = Icons.Default.Lock,
                onGrant = actions.requestLockService,
                onManage = actions.manageLockService,
            )
        } else {
            SettingsRow("Double-tap screen lock", "Requires Android 9 or newer", Icons.Default.Lock, enabled = false) { }
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
    SettingsPage("About", goBack) {
        SettingsRow("Email us", "contact@katoaapps.com", Icons.Default.Email, onClick = actions::emailSupport)
        SettingsRow("Privacy policy", "How $appName handles device data", Icons.Default.PrivacyTip, onClick = actions::openPrivacyPolicy)
        SettingsRow("Terms of use", "Responsibilities, warranties, and data-loss limitations", Icons.Default.Gavel, onClick = actions::openTermsOfUse)
        SettingsRow(
            "Repeat tutorial",
            "Review setup, Magic Box, Mink’s Day, Conversations, widgets, and permissions",
            Icons.Default.School,
            onClick = onRepeatTutorial,
        )
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Text("Version", Modifier.weight(1f), fontWeight = FontWeight.Medium)
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
            Modifier.fillMaxSize().verticalScroll(LocalSettingsScrollState.current).padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            content()
            Spacer(Modifier.height(24.dp))
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(21.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(status, color = Muted, fontSize = 10.sp, maxLines = 1)
            Icon(Icons.Default.ChevronRight, null, tint = Muted, modifier = Modifier.size(20.dp))
        }
    }
}
