package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.runtime.Composable
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.model.SearchFolder
import com.katoaapps.openminilaunch.model.Shortcut
import com.katoaapps.openminilaunch.platform.DeviceActions

/** Renders one Settings destination. Navigation and system launchers stay in the coordinator. */
@Composable
internal fun SettingsDestinationContent(
    destination: SettingsDestination,
    store: LauncherStore,
    actions: DeviceActions,
    permissionState: SettingsPermissionState,
    permissionActions: SettingsPermissionActions,
    requestHomeRole: () -> Unit,
    onRepeatTutorial: () -> Unit,
    onNavigate: (SettingsDestination) -> Unit,
    onNavigateBack: () -> Unit,
    onExitSettings: () -> Unit,
    onIconStyleApplied: () -> Unit,
    onPickShortcut: (Shortcut) -> Unit,
    onPickDrawer: () -> Unit,
    onPickWeb: () -> Unit,
    onPickAi: () -> Unit,
    onPickMessagingApp: () -> Unit,
    onPickSocialApps: () -> Unit,
    onAddFolder: () -> Unit,
    onRemoveFolder: (SearchFolder) -> Unit,
) {
    when (destination) {
        SettingsDestination.OVERVIEW -> SettingsOverviewPage(
            store = store,
            actions = actions,
            permissionState = permissionState,
            onNavigate = onNavigate,
            goBack = onExitSettings,
        )
        SettingsDestination.LAUNCHER -> LauncherSettingsPage(
            store = store,
            requestHomeRole = requestHomeRole,
            onNavigate = onNavigate,
            goBack = onNavigateBack,
        )
        SettingsDestination.APPEARANCE -> AppearanceSettingsPage(
            store = store,
            goBack = onNavigateBack,
            onIconStyleApplied = onIconStyleApplied,
        )
        SettingsDestination.SHORTCUTS -> ShortcutsSettingsPage(
            store = store,
            actions = actions,
            onPickShortcut = onPickShortcut,
            onPickDrawer = onPickDrawer,
            onNavigate = onNavigate,
            goBack = onNavigateBack,
        )
        SettingsDestination.PIN_SHORTCUT_REQUESTS -> PinShortcutRequestsSettingsPage(
            store = store,
            goBack = onNavigateBack,
        )
        SettingsDestination.MAGIC_BOX -> MagicBoxSettingsPage(
            store = store,
            actions = actions,
            mediaGranted = permissionState.mediaGranted,
            onPickWeb = onPickWeb,
            onPickAi = onPickAi,
            onOpenFileSearch = { onNavigate(SettingsDestination.FILE_SEARCH) },
            goBack = onNavigateBack,
        )
        SettingsDestination.MINK_ASSISTANT -> MinkAssistantSettingsPage(
            assistantRoleHeld = permissionState.assistantRoleHeld,
            showAssistantDisclosure = permissionActions.showAssistantSetup,
            goBack = onNavigateBack,
        )
        SettingsDestination.MESSAGING -> MessagingSettingsPage(
            store = store,
            actions = actions,
            onPickMessagingApp = onPickMessagingApp,
            goBack = onNavigateBack,
        )
        SettingsDestination.FILE_SEARCH -> FileSearchSettingsPage(
            store = store,
            mediaGranted = permissionState.mediaGranted,
            onOpenPermissions = { onNavigate(SettingsDestination.PERMISSIONS) },
            onAddFolder = onAddFolder,
            onRemoveFolder = onRemoveFolder,
            goBack = onNavigateBack,
        )
        SettingsDestination.MINK_DAY -> MinkDaySettingsPage(
            store = store,
            usageAccessGranted = permissionState.usageAccessGranted,
            onPickSocialApps = onPickSocialApps,
            onOpenPermissions = { onNavigate(SettingsDestination.PERMISSIONS) },
            goBack = onNavigateBack,
        )
        SettingsDestination.PERMISSIONS -> PermissionsSettingsPage(
            state = permissionState,
            actions = permissionActions,
            goBack = onNavigateBack,
        )
        SettingsDestination.ABOUT -> AboutSettingsPage(
            store,
            actions,
            onRepeatTutorial,
            onNavigateBack,
        )
    }
}
