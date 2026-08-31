package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.staticCompositionLocalOf

internal enum class SettingsDestination {
    OVERVIEW,
    LAUNCHER,
    APPEARANCE,
    SHORTCUTS,
    MAGIC_BOX,
    MINK_ASSISTANT,
    MESSAGING,
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

internal fun settingsPathTo(destination: SettingsDestination): List<SettingsDestination> = when (destination) {
    SettingsDestination.OVERVIEW -> listOf(SettingsDestination.OVERVIEW)
    SettingsDestination.LAUNCHER -> listOf(SettingsDestination.OVERVIEW, SettingsDestination.LAUNCHER)
    SettingsDestination.APPEARANCE,
    SettingsDestination.SHORTCUTS -> listOf(
        SettingsDestination.OVERVIEW,
        SettingsDestination.LAUNCHER,
        destination,
    )
    SettingsDestination.MAGIC_BOX -> listOf(SettingsDestination.OVERVIEW, SettingsDestination.MAGIC_BOX)
    SettingsDestination.FILE_SEARCH -> listOf(
        SettingsDestination.OVERVIEW,
        SettingsDestination.MAGIC_BOX,
        SettingsDestination.FILE_SEARCH,
    )
    SettingsDestination.MINK_ASSISTANT,
    SettingsDestination.MESSAGING,
    SettingsDestination.MINK_DAY,
    SettingsDestination.PERMISSIONS,
    SettingsDestination.ABOUT -> listOf(SettingsDestination.OVERVIEW, destination)
}

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
