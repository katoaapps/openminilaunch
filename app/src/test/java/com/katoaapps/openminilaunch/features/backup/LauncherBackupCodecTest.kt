package com.katoaapps.openminilaunch.features.backup

import com.katoaapps.openminilaunch.model.IconAppearance
import com.katoaapps.openminilaunch.model.MinkAppPauseMode
import com.katoaapps.openminilaunch.model.PinShortcutRequestPresentation
import com.katoaapps.openminilaunch.model.ThemePreference
import com.katoaapps.openminilaunch.features.profile.ProfileCard
import com.katoaapps.openminilaunch.features.profile.ProfileLink
import com.katoaapps.openminilaunch.features.profile.ProfileLinkType
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

    @Test fun olderTodoShortcutMigratesToProfile() {
        val json = JSONObject(LauncherBackupCodec.encode(sampleBackup(twoPanelMode = true)))
        json.put("schemaVersion", 2)
        json.getJSONObject("launcher").put("shortcutOrder", org.json.JSONArray(listOf("TODO", "DRAWER")))

        val restored = LauncherBackupCodec.decode(json.toString())

        assertEquals(com.katoaapps.openminilaunch.model.Shortcut.PROFILE, restored.launcher.shortcutOrder.first())
    }

    @Test fun profileTextAndLinksSurviveBackupWithoutPortrait() {
        val original = sampleBackup(twoPanelMode = true).copy(
            profile = ProfileCard("Mink User", selectedLinkIds = listOf("site"), hasPortrait = true),
            profileLinks = listOf(ProfileLink("site", ProfileLinkType.WEBSITE, "Site", "minklauncher.com")),
        )

        val restored = LauncherBackupCodec.decode(LauncherBackupCodec.encode(original))

        assertEquals("Mink User", restored.profile?.fullName)
        assertEquals(false, restored.profile?.hasPortrait)
        assertEquals("site", restored.profileLinks.single().id)
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
            showBatteryPercentage = true,
            use24HourClock = false,
            homePanelColorArgb = 0xFF123456.toInt(),
            watermelonModeEnabled = true,
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
        profile = null,
        profileLinks = emptyList(),
    )
}
