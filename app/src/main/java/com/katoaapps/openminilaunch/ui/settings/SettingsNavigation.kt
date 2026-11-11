package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.staticCompositionLocalOf

internal enum class SettingsDestination {
    OVERVIEW,
    LANGUAGE,
    LAUNCHER,
    APPEARANCE,
    APPEARANCE_WALLPAPER,
    APPEARANCE_ICONS,
    APPEARANCE_LAYOUT_CLOCK,
    MINK_SPACE,
    APP_GRID,
    QUICK_DRAWER,
    PRIVACY_APPS,
    KEYBOARD,
    APP_BUBBLES,
    MINK_ASSISTANT,
    MESSAGING,
    CALLING,
    FILE_SEARCH,
    MINK_DAY,
    BACKUP_RESTORE,
    PERMISSIONS,
    ABOUT,
    DEMO_STUDIO,
}

internal data class SettingsRoute(
    val section: SettingsSection,
    val destination: SettingsDestination,
)

internal val LocalSettingsScrollState = staticCompositionLocalOf { ScrollState(0) }
internal val LocalSettingsShowBackButton = staticCompositionLocalOf { true }

internal fun settingsRoutesTo(destination: SettingsDestination): List<SettingsRoute> {
    val section = settingsSectionFor(destination)
    return settingsPathTo(destination).map { SettingsRoute(section, it) }
}

internal fun pushSettingsRoute(
    stack: List<SettingsRoute>,
    route: SettingsRoute,
): List<SettingsRoute> = if (stack.lastOrNull() == route) stack else stack + route

/**
 * Opens a destination from a section overview without stacking duplicate copies of the same page.
 *
 * Settings pages can take a moment to compose on older devices. Android may then deliver another
 * tap before the first destination is drawn. Building from the section root makes that repeated
 * tap idempotent instead of adding another copy of the page to the back stack.
 */
internal fun openSettingsDestinationFromOverview(
    stack: List<SettingsRoute>,
    section: SettingsSection,
    destination: SettingsDestination,
): List<SettingsRoute> {
    val destinationRoute = SettingsRoute(section, destination)
    if (stack.lastOrNull() == destinationRoute) return stack

    val sectionRoot = SettingsRoute(section, SettingsDestination.OVERVIEW)
    val rootIndex = stack.indexOfLast { it == sectionRoot }
    val rootedStack = if (rootIndex >= 0) {
        stack.take(rootIndex + 1)
    } else {
        stack + sectionRoot
    }
    val destinationPath = settingsPathTo(destination).drop(1).map { pathDestination ->
        SettingsRoute(section, pathDestination)
    }
    return rootedStack + destinationPath
}

internal fun selectSettingsSection(
    stack: List<SettingsRoute>,
    section: SettingsSection,
): List<SettingsRoute> {
    val sectionRoot = SettingsRoute(section, SettingsDestination.OVERVIEW)
    val existingRootIndex = stack.indexOfLast { it == sectionRoot }
    return if (existingRootIndex >= 0) {
        stack.take(existingRootIndex + 1)
    } else {
        stack + sectionRoot
    }
}

internal fun popSettingsRoute(stack: List<SettingsRoute>): List<SettingsRoute> =
    if (stack.size > 1) stack.dropLast(1) else stack

internal fun settingsPathTo(destination: SettingsDestination): List<SettingsDestination> = when (destination) {
    SettingsDestination.OVERVIEW -> listOf(SettingsDestination.OVERVIEW)
    SettingsDestination.LANGUAGE -> listOf(
        SettingsDestination.OVERVIEW,
        SettingsDestination.LAUNCHER,
        SettingsDestination.LANGUAGE,
    )
    SettingsDestination.LAUNCHER -> listOf(SettingsDestination.OVERVIEW, SettingsDestination.LAUNCHER)
    SettingsDestination.APPEARANCE -> listOf(
        SettingsDestination.OVERVIEW,
        SettingsDestination.LAUNCHER,
        destination,
    )
    SettingsDestination.MINK_SPACE -> listOf(SettingsDestination.OVERVIEW)
    SettingsDestination.APP_GRID,
    SettingsDestination.QUICK_DRAWER -> listOf(SettingsDestination.OVERVIEW, destination)
    SettingsDestination.PRIVACY_APPS -> listOf(
        SettingsDestination.OVERVIEW,
        SettingsDestination.QUICK_DRAWER,
        destination,
    )
    SettingsDestination.APPEARANCE_WALLPAPER,
    SettingsDestination.APPEARANCE_ICONS,
    SettingsDestination.APPEARANCE_LAYOUT_CLOCK -> listOf(
        SettingsDestination.OVERVIEW,
        SettingsDestination.LAUNCHER,
        SettingsDestination.APPEARANCE,
        destination,
    )
    SettingsDestination.KEYBOARD,
    SettingsDestination.FILE_SEARCH -> listOf(
        SettingsDestination.OVERVIEW,
        destination,
    )
    SettingsDestination.APP_BUBBLES -> listOf(
        SettingsDestination.OVERVIEW,
        SettingsDestination.APP_BUBBLES,
    )
    SettingsDestination.MINK_ASSISTANT,
    SettingsDestination.MESSAGING,
    SettingsDestination.CALLING,
    SettingsDestination.MINK_DAY,
    SettingsDestination.BACKUP_RESTORE,
    SettingsDestination.PERMISSIONS,
    SettingsDestination.ABOUT -> listOf(SettingsDestination.OVERVIEW, destination)
    SettingsDestination.DEMO_STUDIO -> listOf(
        SettingsDestination.OVERVIEW,
        SettingsDestination.ABOUT,
        destination,
    )
}

internal data class SettingsPermissionState(
    val homeRoleHeld: Boolean,
    val usageAccessGranted: Boolean,
    val notificationAccessGranted: Boolean,
    val contactsGranted: Boolean,
    val directCallsSupported: Boolean,
    val callsGranted: Boolean,
    val directSmsSupported: Boolean,
    val assistantRoleHeld: Boolean,
    val smsGranted: Boolean,
    val mediaGranted: Boolean,
    val appBubblesSupported: Boolean,
    val appBubbleNotificationsGranted: Boolean,
    val appBubblesAllowed: Boolean,
    val lockSupported: Boolean,
    val lockServiceEnabled: Boolean,
) {
    val supportedCount: Int
        get() = 5 + directCallsSupported.toInt() + directSmsSupported.toInt() +
            appBubblesSupported.toInt() + lockSupported.toInt()

    val activeCount: Int
        get() = listOf(
            homeRoleHeld,
            usageAccessGranted,
            notificationAccessGranted,
            contactsGranted,
            mediaGranted,
        ).count { it } +
            (directCallsSupported && callsGranted).toInt() +
            (directSmsSupported && smsGranted).toInt() +
            (appBubblesSupported && appBubbleNotificationsGranted && appBubblesAllowed).toInt() +
            (lockSupported && lockServiceEnabled).toInt()
}

private fun Boolean.toInt(): Int = if (this) 1 else 0

internal data class SettingsPermissionActions(
    val manageHomeRole: () -> Unit,
    val requestUsageAccess: () -> Unit,
    val manageUsageAccess: () -> Unit,
    val requestNotificationAccess: () -> Unit,
    val manageNotificationAccess: () -> Unit,
    val requestContacts: () -> Unit,
    val requestCalls: () -> Unit,
    val requestSms: () -> Unit,
    val requestMedia: () -> Unit,
    val requestAppBubbleNotifications: () -> Unit,
    val manageAppBubbleNotifications: () -> Unit,
    val manageAppBubbles: () -> Unit,
    val manageAppPermissions: () -> Unit,
    val openAppInfo: () -> Unit,
    val requestLockService: () -> Unit,
    val manageLockService: () -> Unit,
    val showAssistantSetup: () -> Unit,
)
