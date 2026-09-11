package com.katoaapps.openminilaunch.features.backup

import com.katoaapps.openminilaunch.model.IconAppearance
import com.katoaapps.openminilaunch.model.MinkAppPauseMode
import com.katoaapps.openminilaunch.model.PinShortcutRequestPresentation
import com.katoaapps.openminilaunch.model.Shortcut
import com.katoaapps.openminilaunch.model.ThemePreference
import com.katoaapps.openminilaunch.model.TodoItem

internal const val LAUNCHER_BACKUP_FORMAT = "minklauncher-open-backup"
internal const val LAUNCHER_BACKUP_SCHEMA_VERSION = 1

/** Portable state that is safe to move between OpenMink distributions on the same device. */
internal data class LauncherBackup(
    val sourceAppVersion: String,
    val exportedAtMillis: Long,
    val launcher: LauncherBackupLayout,
    val settings: LauncherBackupSettings,
    val todos: List<TodoItem>,
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
    val showClock: Boolean,
    val use24HourClock: Boolean,
    val homePanelColorArgb: Int,
    val appBackgroundColorArgb: Int?,
    val iconAppearance: IconAppearance,
    val openSoftwareKeyboardOnHome: Boolean,
    val includeAppShortcutsInDiscovery: Boolean,
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
