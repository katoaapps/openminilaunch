package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.runtime.Composable
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.demo.DemoCaptureMoment
import com.katoaapps.openminilaunch.model.SearchFolder
import com.katoaapps.openminilaunch.model.AppGridSlot
import com.katoaapps.openminilaunch.platform.DeviceActions

/** Renders one Settings destination. Navigation and system launchers stay in the coordinator. */
@Composable
internal fun SettingsDestinationContent(
    destination: SettingsDestination,
    selectedSection: SettingsSection,
    store: LauncherStore,
    actions: DeviceActions,
    permissionState: SettingsPermissionState,
    permissionActions: SettingsPermissionActions,
    requestHomeRole: () -> Unit,
    onRepeatTutorial: () -> Unit,
    onReplayHomeTour: () -> Unit,
    onNavigate: (SettingsDestination) -> Unit,
    onOverviewNavigate: (SettingsDestination) -> Unit,
    onNavigateBack: () -> Unit,
    onExitSettings: () -> Unit,
    onDemoCaptureMoment: (DemoCaptureMoment) -> Unit,
    onIconStyleApplied: () -> Unit,
    onHomeLayoutApplied: () -> Unit,
    onPickAppGridSlot: (AppGridSlot) -> Unit,
    onPickDrawer: () -> Unit,
    onPickPrivateContainerGateway: () -> Unit,
    onPickWeb: () -> Unit,
    onPickAi: () -> Unit,
    onPickMessagingApp: () -> Unit,
    onPickCallingApp: () -> Unit,
    onPickSocialApps: () -> Unit,
    onAddFolder: () -> Unit,
    onRemoveFolder: (SearchFolder) -> Unit,
) {
    when (destination) {
        SettingsDestination.OVERVIEW -> SettingsOverviewPage(
            section = selectedSection,
            store = store,
            actions = actions,
            permissionState = permissionState,
            onPickWeb = onPickWeb,
            onPickAi = onPickAi,
            onNavigate = onOverviewNavigate,
            goBack = onExitSettings,
        )
        SettingsDestination.LANGUAGE -> AppLanguageSettingsPage(goBack = onNavigateBack)
        SettingsDestination.LAUNCHER -> LauncherSettingsPage(
            store = store,
            onNavigate = onNavigate,
            onHomeLayoutApplied = onHomeLayoutApplied,
            goBack = onNavigateBack,
        )
        SettingsDestination.APPEARANCE -> AppearanceSettingsPage(
            store = store,
            onNavigate = onNavigate,
            goBack = onNavigateBack,
        )
        SettingsDestination.APPEARANCE_WALLPAPER -> WallpaperAndBackgroundSettingsPage(
            store = store,
            onIconStyleApplied = onIconStyleApplied,
            goBack = onNavigateBack,
        )
        SettingsDestination.APPEARANCE_ICONS -> IconsAndFontsSettingsPage(
            store = store,
            onIconStyleApplied = onIconStyleApplied,
            goBack = onNavigateBack,
        )
        SettingsDestination.APPEARANCE_LAYOUT_CLOCK -> LayoutAndClockSettingsPage(
            store = store,
            goBack = onNavigateBack,
        )
        SettingsDestination.MINK_SPACE -> MinkSpaceSettingsPage(
            store = store,
            actions = actions,
            openMinkBoardSettings = { onNavigate(SettingsDestination.MINKBOARD_HOME) },
            goBack = onNavigateBack,
        )
        SettingsDestination.MINKBOARD_HOME -> MinkBoardHomeSettingsPage(
            goBack = onNavigateBack,
        )
        SettingsDestination.APP_GRID -> AppGridSettingsPage(
            store = store,
            actions = actions,
            onPickAppGridSlot = onPickAppGridSlot,
            onNavigate = onNavigate,
            goBack = onNavigateBack,
        )
        SettingsDestination.QUICK_DRAWER -> QuickDrawerSettingsPage(
            store = store,
            actions = actions,
            onPickDrawer = onPickDrawer,
            onNavigate = onNavigate,
            goBack = onNavigateBack,
        )
        SettingsDestination.PRIVACY_APPS -> PrivacyAppsSettingsPage(
            store = store,
            actions = actions,
            onPickGateway = onPickPrivateContainerGateway,
            goBack = onNavigateBack,
        )
        SettingsDestination.APP_BUBBLES -> AppBubbleSettingsPage(
            store = store,
            actions = actions,
            permissionState = permissionState,
            permissionActions = permissionActions,
            goBack = onNavigateBack,
        )
        SettingsDestination.KEYBOARD -> KeyboardSettingsPage(
            store = store,
            goBack = onNavigateBack,
        )
        SettingsDestination.MINK_ASSISTANT -> MinkAssistantSettingsPage(
            assistantRoleHeld = permissionState.assistantRoleHeld,
            showAssistantDisclosure = permissionActions.showAssistantSetup,
            onOpenQuickApps = onPickDrawer,
            goBack = onNavigateBack,
        )
        SettingsDestination.MESSAGING -> MessagingSettingsPage(
            store = store,
            actions = actions,
            onPickMessagingApp = onPickMessagingApp,
            goBack = onNavigateBack,
        )
        SettingsDestination.CALLING -> CallingSettingsPage(
            store = store,
            actions = actions,
            onPickCallingApp = onPickCallingApp,
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
        SettingsDestination.BACKUP_RESTORE -> BackupRestoreSettingsPage(
            store = store,
            goBack = onNavigateBack,
        )
        SettingsDestination.PERMISSIONS -> PermissionsSettingsPage(
            state = permissionState,
            actions = permissionActions,
            requestHomeRole = requestHomeRole,
            goBack = onNavigateBack,
        )
        SettingsDestination.ABOUT -> AboutSettingsPage(
            store,
            actions,
            onRepeatTutorial,
            onReplayHomeTour,
            { onNavigate(SettingsDestination.DEMO_STUDIO) },
            onNavigateBack,
        )
        SettingsDestination.DEMO_STUDIO -> DemoStudioSettingsPage(
            store = store,
            onCaptureMoment = onDemoCaptureMoment,
            goBack = onNavigateBack,
        )
    }
}
