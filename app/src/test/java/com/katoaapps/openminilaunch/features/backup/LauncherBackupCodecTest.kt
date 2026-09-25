package com.katoaapps.openminilaunch.features.backup

import com.katoaapps.openminilaunch.model.IconAppearance
import com.katoaapps.openminilaunch.model.MinkAppPauseMode
import com.katoaapps.openminilaunch.model.PinShortcutRequestPresentation
import com.katoaapps.openminilaunch.model.ThemePreference
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LauncherBackupCodecTest {
    @Test fun twoPanelPreferenceSurvivesPortableBackupRoundTrip() {
        val restored = LauncherBackupCodec.decode(LauncherBackupCodec.encode(sampleBackup(twoPanelMode = true)))

        assertEquals(true, restored.settings.twoPanelModeForLargeDisplays)
    }

    @Test fun explicitTwoPanelOptOutSurvivesPortableBackupRoundTrip() {
        val restored = LauncherBackupCodec.decode(LauncherBackupCodec.encode(sampleBackup(twoPanelMode = false)))

        assertEquals(false, restored.settings.twoPanelModeForLargeDisplays)
    }

    @Test fun olderBackupWithoutTwoPanelPreferenceLeavesCurrentDefaultAlone() {
        val json = JSONObject(LauncherBackupCodec.encode(sampleBackup(twoPanelMode = true)))
        json.getJSONObject("settings").getJSONObject("appearance")
            .remove("twoPanelModeForLargeDisplays")

        assertNull(LauncherBackupCodec.decode(json.toString()).settings.twoPanelModeForLargeDisplays)
    }

    private fun sampleBackup(twoPanelMode: Boolean) = LauncherBackup(
        sourceAppVersion = "test",
        exportedAtMillis = 0L,
        launcher = LauncherBackupLayout(
            shortcutTargets = emptyMap(),
            shortcutOrder = emptyList(),
            confirmedShortcutChoices = emptyList(),
            drawerTargets = emptyList(),
            libraryShortcutTargets = emptyList(),
        ),
        settings = LauncherBackupSettings(
            themePreference = ThemePreference.SYSTEM,
            hideStatusBar = true,
            alignHomePanelBottom = false,
            twoPanelModeForLargeDisplays = twoPanelMode,
            showClock = false,
            showDate = true,
            use24HourClock = false,
            homePanelColorArgb = 0xFF123456.toInt(),
            homePanelTransparency = 0f,
            appBackgroundColorArgb = null,
            iconAppearance = IconAppearance(),
            openSoftwareKeyboardOnHome = true,
            includeAppShortcutsInDiscovery = false,
            sendMessagesAutomatically = false,
            preferredMessagingPackage = null,
            preferredAiPackage = null,
            preferredWebPackage = null,
            usesAutomaticSocialApps = true,
            socialPackages = emptyList(),
            socialGoalHours = 1,
            minkAppPauseMode = MinkAppPauseMode.NEVER,
            githubUpdateChecksEnabled = true,
            pinShortcutRequestPresentation = PinShortcutRequestPresentation.FULL_PAGE,
        ),
        todos = emptyList(),
    )
}
