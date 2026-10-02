package com.katoaapps.openminilaunch.features.backup

import com.katoaapps.openminilaunch.model.IconAppearance
import com.katoaapps.openminilaunch.model.MinkAppPauseMode
import com.katoaapps.openminilaunch.model.PinShortcutRequestPresentation
import com.katoaapps.openminilaunch.model.Shortcut
import com.katoaapps.openminilaunch.model.ThemePreference
import com.katoaapps.openminilaunch.features.minkspace.todo.TodoItem
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import com.katoaapps.openminilaunch.features.profile.ProfileCard
import com.katoaapps.openminilaunch.features.profile.ProfileLink

internal const val LAUNCHER_BACKUP_FORMAT = "minklauncher-open-backup"
// Keep schema 3 while new fields remain optional. OpenMink 1.5.5 ignores unknown JSON keys,
// which lets a 2.0 export restore there without losing fields that version understands.
internal const val LAUNCHER_BACKUP_SCHEMA_VERSION = 3
// Interim 2.0 development builds wrote schema 4 before downgrade compatibility was restored.
internal const val LAUNCHER_BACKUP_MAX_READABLE_SCHEMA_VERSION = 4

/** Portable state that is safe to move between OpenMink distributions on the same device. */
internal data class LauncherBackup(
    val sourceAppVersion: String,
    val exportedAtMillis: Long,
    val launcher: LauncherBackupLayout,
    val settings: LauncherBackupSettings,
    val todos: List<TodoItem>,
    val profile: ProfileCard?,
    val profileLinks: List<ProfileLink>,
)

internal data class LauncherBackupLayout(
    val shortcutTargets: Map<Shortcut, String>,
    val shortcutOrder: List<Shortcut>,
    val confirmedShortcutChoices: List<Shortcut>,
    val drawerTargets: List<String>,
    val libraryShortcutTargets: List<String>,
)

internal data class LauncherBackupSettings(
    val themePreference: ThemePreference,
    val hideStatusBar: Boolean,
    val alignHomePanelBottom: Boolean,
    /** Null means this backup predates the two-panel preference. */
    val twoPanelModeForLargeDisplays: Boolean?,
    val showClock: Boolean,
    val showDate: Boolean,
    val showBatteryPercentage: Boolean,
    val use24HourClock: Boolean,
    val homePanelColorArgb: Int,
    val watermelonModeEnabled: Boolean,
    val homePanelTransparency: Float,
    val appBackgroundColorArgb: Int?,
    val iconAppearance: IconAppearance,
    val openSoftwareKeyboardOnHome: Boolean,
    val includeAppShortcutsInDiscovery: Boolean,
    val automaticAppBubbleTargets: List<String>,
    /** Null means this backup predates portable MinkSpace preferences. */
    val minkSpaceMiniAppOrder: List<MinkSpaceMiniApp>?,
    /** Null means this backup predates portable MinkSpace preferences. */
    val enabledMinkSpaceMiniApps: Set<MinkSpaceMiniApp>?,
    val sendMessagesAutomatically: Boolean,
    val preferredMessagingPackage: String?,
    val preferredAiPackage: String?,
    val preferredWebPackage: String?,
    val usesAutomaticSocialApps: Boolean,
    val socialPackages: List<String>,
    val socialGoalHours: Int,
    val minkAppPauseMode: MinkAppPauseMode,
    val githubUpdateChecksEnabled: Boolean,
    val pinShortcutRequestPresentation: PinShortcutRequestPresentation,
)
