package com.katoaapps.openminilaunch.ui.settings

import com.katoaapps.openminilaunch.features.ai.AiProviderOption
import com.katoaapps.openminilaunch.features.messaging.MessagingProviderOption
import com.katoaapps.openminilaunch.model.LaunchableApp
import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.model.Shortcut

internal sealed interface SettingsPicker {
    data class ShortcutApp(val shortcut: Shortcut) : SettingsPicker
    data object DrawerApps : SettingsPicker
    data object CuratedAiApp : SettingsPicker
    data object CompatibleAiApp : SettingsPicker
    data object WebApp : SettingsPicker
    data object MessagingApp : SettingsPicker
    data object MinkDayApps : SettingsPicker
}

internal data class AppListLoadState(
    val apps: List<LaunchableApp> = emptyList(),
    val loaded: Boolean = false,
)

internal data class LauncherAppListLoadState(
    val apps: List<LauncherAppTarget> = emptyList(),
    val loaded: Boolean = false,
)

internal data class LauncherShortcutListLoadState(
    val shortcuts: List<LauncherShortcutTarget> = emptyList(),
    val loaded: Boolean = false,
)

internal data class MessagingProviderLoadState(
    val options: List<MessagingProviderOption> = emptyList(),
    val loaded: Boolean = false,
)

internal data class AiProviderLoadState(
    val options: List<AiProviderOption> = emptyList(),
    val loaded: Boolean = false,
)
