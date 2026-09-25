package com.katoaapps.openminilaunch.features.backup

import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.model.IconSource
import com.katoaapps.openminilaunch.features.profile.ProfileState

internal fun LauncherStore.createPortableBackup(appVersion: String): LauncherBackup {
    val profile = profileRepository.state as? ProfileState.Ready
    return LauncherBackup(
        sourceAppVersion = appVersion,
        exportedAtMillis = System.currentTimeMillis(),
        launcher = LauncherBackupLayout(
            shortcutTargets = shortcutTargets.toMap(),
            shortcutOrder = shortcutOrder.toList(),
            confirmedShortcutChoices = confirmedShortcutChoices.toList(),
            drawerTargets = drawerTargets.toList(),
            libraryShortcutTargets = libraryShortcutTargets.toList(),
        ),
        settings = LauncherBackupSettings(
            themePreference = themePreference,
            hideStatusBar = hideStatusBar,
            alignHomePanelBottom = alignHomePanelBottom,
            twoPanelModeForLargeDisplays = twoPanelModeForLargeDisplays,
            showClock = showClock,
            showDate = showDate,
            showBatteryPercentage = showBatteryPercentage,
            use24HourClock = use24HourClock,
            homePanelColorArgb = homePanelColorArgb,
            watermelonModeEnabled = watermelonModeEnabled,
            homePanelTransparency = homePanelTransparency,
            appBackgroundColorArgb = appBackgroundColorArgb,
            iconAppearance = iconAppearance,
            openSoftwareKeyboardOnHome = openSoftwareKeyboardOnHome,
            includeAppShortcutsInDiscovery = includeAppShortcutsInDiscovery,
            sendMessagesAutomatically = sendMessagesAutomatically,
            preferredMessagingPackage = preferredMessagingPackage,
            preferredAiPackage = preferredAiPackage,
            preferredWebPackage = preferredWebPackage,
            usesAutomaticSocialApps = usesAutomaticSocialApps,
            socialPackages = socialPackages.toList(),
            socialGoalHours = socialGoalHours,
            minkAppPauseMode = minkAppPauseMode,
            githubUpdateChecksEnabled = githubUpdateChecksEnabled,
            pinShortcutRequestPresentation = pinShortcutRequestPresentation,
        ),
        todos = savedTodosForBackup,
        profile = profile?.card?.copy(hasPortrait = false),
        profileLinks = profile?.links.orEmpty(),
    )
}

internal fun LauncherStore.restorePortableBackup(backup: LauncherBackup) {
    if (demoSearchDataEnabled) toggleDemoSearchData()
    val settings = backup.settings
    replaceLauncherSelectionsFromBackup(
        shortcutTargets = backup.launcher.shortcutTargets,
        shortcutOrder = backup.launcher.shortcutOrder,
        confirmedShortcutChoices = backup.launcher.confirmedShortcutChoices,
        drawerTargets = backup.launcher.drawerTargets,
        libraryShortcutTargets = backup.launcher.libraryShortcutTargets,
    )
    replaceTodosFromBackup(backup.todos)
    profileRepository.replaceFromBackup(backup.profile, backup.profileLinks)

    setTheme(settings.themePreference)
    updateHideStatusBar(settings.hideStatusBar)
    updateAlignHomePanelBottom(settings.alignHomePanelBottom)
    settings.twoPanelModeForLargeDisplays?.let(::updateTwoPanelModeForLargeDisplays)
    updateShowClock(settings.showClock)
    updateShowDate(settings.showDate)
    updateShowBatteryPercentage(settings.showBatteryPercentage)
    updateUse24HourClock(settings.use24HourClock)
    setHomePanelColor(settings.homePanelColorArgb)
    if (settings.watermelonModeEnabled) activateWatermelonMode()
    setHomePanelTransparency(settings.homePanelTransparency)
    setAppBackgroundColor(settings.appBackgroundColorArgb)
    when (settings.iconAppearance.source) {
        IconSource.MINK -> useMinkIcons()
        IconSource.SYSTEM -> useSystemIcons()
        IconSource.ICON_PACK -> settings.iconAppearance.iconPackPackage
            ?.let(::useIconPack)
            ?: useMinkIcons()
    }
    setMinkIconColor(settings.iconAppearance.minkIconColorArgb)

    updateOpenSoftwareKeyboardOnHome(settings.openSoftwareKeyboardOnHome)
    updateIncludeAppShortcutsInDiscovery(settings.includeAppShortcutsInDiscovery)
    updateSendMessagesAutomatically(settings.sendMessagesAutomatically)
    settings.preferredMessagingPackage?.let(::setPreferredMessagingApp)
        ?: resetPreferredMessagingApp()
    settings.preferredAiPackage?.let(::setPreferredAiApp) ?: resetPreferredAiApp()
    settings.preferredWebPackage?.let(::setPreferredWebApp) ?: resetPreferredWebApp()

    updateSocialGoalHours(settings.socialGoalHours)
    updateMinkAppPauseMode(settings.minkAppPauseMode)
    if (settings.usesAutomaticSocialApps) clearSocialApps()
    else replaceSocialApps(settings.socialPackages.toSet())

    setGitHubUpdateChecksEnabled(settings.githubUpdateChecksEnabled)
    setPinShortcutRequestPresentation(settings.pinShortcutRequestPresentation)
}
