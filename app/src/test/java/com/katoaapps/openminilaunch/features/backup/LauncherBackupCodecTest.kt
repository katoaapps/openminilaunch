package com.katoaapps.openminilaunch.features.backup

import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import com.katoaapps.openminilaunch.features.profile.ProfileCard
import com.katoaapps.openminilaunch.features.profile.ProfileLink
import com.katoaapps.openminilaunch.features.profile.ProfileLinkType
import com.katoaapps.openminilaunch.model.AppFolderConfig
import com.katoaapps.openminilaunch.model.AppFolderAnimationStyle
import com.katoaapps.openminilaunch.model.AppFolderColorMode
import com.katoaapps.openminilaunch.model.AppFolderIndicatorStyle
import com.katoaapps.openminilaunch.model.AppGridAssignment
import com.katoaapps.openminilaunch.model.AppGridSlot
import com.katoaapps.openminilaunch.model.FontAppearance
import com.katoaapps.openminilaunch.model.FontSource
import com.katoaapps.openminilaunch.model.IconAppearance
import com.katoaapps.openminilaunch.model.MinkAppPauseMode
import com.katoaapps.openminilaunch.model.PalettePreference
import com.katoaapps.openminilaunch.model.QuickDrawerConfig
import com.katoaapps.openminilaunch.model.ThemePreference
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LauncherBackupCodecTest {
    @Test fun palettePreferenceSurvivesPortableBackupRoundTrip() {
        val sample = sampleBackup(twoPanelMode = true)
        val original = sample.copy(
            settings = sample.settings.copy(palettePreference = PalettePreference.SYSTEM),
        )

        val restored = LauncherBackupCodec.decode(LauncherBackupCodec.encode(original))

        assertEquals(PalettePreference.SYSTEM, restored.settings.palettePreference)
    }

    @Test fun olderBackupWithoutPaletteUsesMink() {
        val json = JSONObject(LauncherBackupCodec.encode(sampleBackup(twoPanelMode = true)))
        json.getJSONObject("settings").getJSONObject("appearance").remove("palette")

        assertEquals(
            PalettePreference.MINK,
            LauncherBackupCodec.decode(json.toString()).settings.palettePreference,
        )
    }

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

    @Test fun minkSpacePreferencesSurvivePortableBackupRoundTrip() {
        val original = sampleBackup(twoPanelMode = true)
        val encoded = LauncherBackupCodec.encode(original)
        val restored = LauncherBackupCodec.decode(encoded)

        assertEquals(3, JSONObject(encoded).getInt("schemaVersion"))
        assertEquals(
            listOf(
                MinkSpaceMiniApp.MUSIC,
                MinkSpaceMiniApp.TODO,
                MinkSpaceMiniApp.CALCULATOR,
                MinkSpaceMiniApp.VCARD,
            ),
            restored.settings.minkSpaceMiniAppOrder,
        )
        assertEquals(
            setOf(MinkSpaceMiniApp.TODO, MinkSpaceMiniApp.MUSIC),
            restored.settings.enabledMinkSpaceMiniApps,
        )
    }

    @Test fun backupWithoutMinkSpaceLeavesCurrentPreferencesAlone() {
        val json = JSONObject(LauncherBackupCodec.encode(sampleBackup(twoPanelMode = true)))
        json.getJSONObject("settings").remove("minkSpace")

        val restored = LauncherBackupCodec.decode(json.toString())

        assertNull(restored.settings.minkSpaceMiniAppOrder)
        assertNull(restored.settings.enabledMinkSpaceMiniApps)
    }

    @Test fun bundledWallpaperSelectionSurvivesPortableBackupRoundTrip() {
        val sample = sampleBackup(twoPanelMode = true)
        val original = sample.copy(
            settings = sample.settings.copy(
                appBackgroundPresetId = "mink-07",
            ),
        )

        val restored = LauncherBackupCodec.decode(LauncherBackupCodec.encode(original))

        assertEquals("mink-07", restored.settings.appBackgroundPresetId)
    }

    @Test fun olderBackupWithoutBundledWallpaperHasNoPresetSelection() {
        val json = JSONObject(LauncherBackupCodec.encode(sampleBackup(twoPanelMode = true)))
        json.getJSONObject("settings").getJSONObject("appearance").remove("appBackgroundPresetId")

        assertNull(LauncherBackupCodec.decode(json.toString()).settings.appBackgroundPresetId)
    }

    @Test fun fontAppearanceSurvivesPortableBackupRoundTrip() {
        val original = sampleBackup(twoPanelMode = true).let { sample ->
            sample.copy(
                settings = sample.settings.copy(
                    fontAppearance = FontAppearance(
                        source = FontSource.SERIF,
                        sizeScale = 1.3f,
                        colorArgb = 0xFF123456.toInt(),
                    ),
                ),
            )
        }

        val restored = LauncherBackupCodec.decode(LauncherBackupCodec.encode(original))

        assertEquals(original.settings.fontAppearance, restored.settings.fontAppearance)
    }

    @Test fun olderBackupWithoutFontAppearanceUsesDefaults() {
        val json = JSONObject(LauncherBackupCodec.encode(sampleBackup(twoPanelMode = true)))
        val appearance = json.getJSONObject("settings").getJSONObject("appearance")
        appearance.remove("fontSource")
        appearance.remove("fontSizeScale")
        appearance.remove("fontColorArgb")
        appearance.remove("customFontName")

        assertEquals(FontAppearance(), LauncherBackupCodec.decode(json.toString()).settings.fontAppearance)
    }

    @Test fun callingPreferencesSurvivePortableBackupRoundTrip() {
        val original = sampleBackup(twoPanelMode = true).let { sample ->
            sample.copy(
                settings = sample.settings.copy(
                    callAutomatically = true,
                    preferredCallingPackage = "com.example.phone",
                ),
            )
        }

        val restored = LauncherBackupCodec.decode(LauncherBackupCodec.encode(original))

        assertEquals(true, restored.settings.callAutomatically)
        assertEquals("com.example.phone", restored.settings.preferredCallingPackage)
    }

    @Test fun olderBackupWithoutCallingPreferencesUsesSafeDefaults() {
        val json = JSONObject(LauncherBackupCodec.encode(sampleBackup(twoPanelMode = true)))
        json.getJSONObject("settings").remove("calling")

        val restored = LauncherBackupCodec.decode(json.toString())

        assertEquals(false, restored.settings.callAutomatically)
        assertNull(restored.settings.preferredCallingPackage)
    }

    @Test fun quickDrawerPreferencesSurvivePortableBackupRoundTrip() {
        val sample = sampleBackup(twoPanelMode = true)
        val original = sample.copy(
            settings = sample.settings.copy(
                quickDrawer = QuickDrawerConfig(
                    orderedTargetKeys = listOf("app:0:com.example/.MainActivity"),
                    showMinkPad = false,
                    showPrivateSpace = false,
                    showWorkProfile = false,
                    colorArgb = 0xFF123456.toInt(),
                ),
            ),
        )

        val restored = LauncherBackupCodec.decode(LauncherBackupCodec.encode(original))

        assertEquals(original.settings.quickDrawer, restored.settings.quickDrawer)
    }

    @Test fun appFolderAssignmentsSurvivePortableBackupRoundTrip() {
        val sample = sampleBackup(twoPanelMode = true)
        val folder = AppFolderConfig(
            id = "folder-1",
            customName = "Travel",
            orderedTargetKeys = listOf(
                "app:0:com.example.maps/.MainActivity",
                "app:0:com.example.transit/.MainActivity",
            ),
            colorMode = AppFolderColorMode.CUSTOM,
            colorArgb = 0xFF123456.toInt(),
        )
        val original = sample.copy(
            launcher = sample.launcher.copy(
                appGridAssignments = mapOf(
                    AppGridSlot.NOTE to AppGridAssignment.Target("app:0:com.example.notes/.MainActivity"),
                    AppGridSlot.EVENT to AppGridAssignment.Folder(folder.id),
                ),
                appFolders = listOf(folder),
            ),
        )

        val restored = LauncherBackupCodec.decode(LauncherBackupCodec.encode(original))

        assertEquals(original.launcher.appGridAssignments, restored.launcher.appGridAssignments)
        assertEquals(original.launcher.appFolders, restored.launcher.appFolders)
    }

    @Test fun folderIndicatorStyleSurvivesPortableBackupRoundTrip() {
        val sample = sampleBackup(twoPanelMode = true)
        val original = sample.copy(
            settings = sample.settings.copy(folderIndicatorStyle = AppFolderIndicatorStyle.GRID),
        )

        val restored = LauncherBackupCodec.decode(LauncherBackupCodec.encode(original))

        assertEquals(AppFolderIndicatorStyle.GRID, restored.settings.folderIndicatorStyle)
    }

    @Test fun folderAnimationStyleSurvivesPortableBackupRoundTrip() {
        val sample = sampleBackup(twoPanelMode = true)
        val original = sample.copy(
            settings = sample.settings.copy(folderAnimationStyle = AppFolderAnimationStyle.RIPPLE),
        )

        val restored = LauncherBackupCodec.decode(LauncherBackupCodec.encode(original))

        assertEquals(AppFolderAnimationStyle.RIPPLE, restored.settings.folderAnimationStyle)
    }

    @Test fun backupWithoutFolderIndicatorStyleLeavesCurrentPreferenceAlone() {
        val json = JSONObject(LauncherBackupCodec.encode(sampleBackup(twoPanelMode = true)))
        json.getJSONObject("settings").remove("appGrid")

        val restored = LauncherBackupCodec.decode(json.toString())

        assertNull(restored.settings.folderIndicatorStyle)
    }

    @Test fun backupWithoutFolderAnimationStyleLeavesCurrentPreferenceAlone() {
        val sample = sampleBackup(twoPanelMode = true)
        val original = sample.copy(
            settings = sample.settings.copy(folderIndicatorStyle = AppFolderIndicatorStyle.GRID),
        )
        val json = JSONObject(LauncherBackupCodec.encode(original))
        json.getJSONObject("settings").getJSONObject("appGrid").remove("folderAnimationStyle")

        val restored = LauncherBackupCodec.decode(json.toString())

        assertNull(restored.settings.folderAnimationStyle)
    }

    @Test fun unknownFolderPresentationStylesAreIgnored() {
        val sample = sampleBackup(twoPanelMode = true)
        val original = sample.copy(
            settings = sample.settings.copy(
                folderIndicatorStyle = AppFolderIndicatorStyle.GRID,
                folderAnimationStyle = AppFolderAnimationStyle.RIPPLE,
            ),
        )
        val json = JSONObject(LauncherBackupCodec.encode(original))
        json.getJSONObject("settings").getJSONObject("appGrid").apply {
            put("folderIndicatorStyle", "FUTURE_INDICATOR")
            put("folderAnimationStyle", "FUTURE_ANIMATION")
        }

        val restored = LauncherBackupCodec.decode(json.toString())

        assertNull(restored.settings.folderIndicatorStyle)
        assertNull(restored.settings.folderAnimationStyle)
    }

    @Test fun olderFolderColorWithoutModeRestoresAsCustom() {
        val sample = sampleBackup(twoPanelMode = true)
        val folder = AppFolderConfig(
            id = "folder-1",
            orderedTargetKeys = listOf(
                "app:0:com.example.maps/.MainActivity",
                "app:0:com.example.transit/.MainActivity",
            ),
            colorMode = AppFolderColorMode.CUSTOM,
            colorArgb = 0xFF123456.toInt(),
        )
        val original = sample.copy(
            launcher = sample.launcher.copy(
                appGridAssignments = mapOf(AppGridSlot.EVENT to AppGridAssignment.Folder(folder.id)),
                appFolders = listOf(folder),
            ),
        )
        val json = JSONObject(LauncherBackupCodec.encode(original))
        json.getJSONObject("launcher")
            .getJSONArray("appFolders")
            .getJSONObject(0)
            .remove("colorMode")

        val restored = LauncherBackupCodec.decode(json.toString()).launcher.appFolders.single()

        assertEquals(AppFolderColorMode.CUSTOM, restored.colorMode)
        assertEquals(0xFF123456.toInt(), restored.colorArgb)
    }

    @Test fun appFolderWritesFirstItemForPreFolderBackupReaders() {
        val sample = sampleBackup(twoPanelMode = true)
        val folder = AppFolderConfig(
            id = "folder-1",
            orderedTargetKeys = listOf(
                "app:0:com.example.maps/.MainActivity",
                "app:0:com.example.transit/.MainActivity",
            ),
        )
        val backup = sample.copy(
            launcher = sample.launcher.copy(
                appGridAssignments = mapOf(
                    AppGridSlot.EVENT to AppGridAssignment.Folder(folder.id),
                ),
                appFolders = listOf(folder),
            ),
        )

        val launcherJson = JSONObject(LauncherBackupCodec.encode(backup))
            .getJSONObject("launcher")

        assertEquals(
            folder.orderedTargetKeys.first(),
            launcherJson.getJSONObject("shortcutTargets").getString(AppGridSlot.EVENT.name),
        )
    }

    @Test fun targetAssignmentsRestoreFromLegacyShortcutTargets() {
        val sample = sampleBackup(twoPanelMode = true)
        val original = sample.copy(
            launcher = sample.launcher.copy(
                appGridAssignments = mapOf(
                    AppGridSlot.NOTE to AppGridAssignment.Target("app:0:com.example.notes/.MainActivity"),
                ),
            ),
        )
        val json = JSONObject(LauncherBackupCodec.encode(original))
        json.getJSONObject("launcher").apply {
            remove("appGridAssignments")
            remove("appFolders")
        }

        val restored = LauncherBackupCodec.decode(json.toString())

        assertEquals(original.launcher.appGridAssignments, restored.launcher.appGridAssignments)
        assertEquals(emptyList<AppFolderConfig>(), restored.launcher.appFolders)
    }

    @Test fun olderBackupWithoutQuickDrawerPreferencesLeavesCurrentPreferencesAlone() {
        val json = JSONObject(LauncherBackupCodec.encode(sampleBackup(twoPanelMode = true)))
        json.getJSONObject("settings").remove("quickDrawer")

        val restored = LauncherBackupCodec.decode(json.toString())

        assertNull(restored.settings.quickDrawer)
    }

    @Test fun olderBackupWithoutAutomaticMessagingPreferenceKeepsProductDefaultEnabled() {
        val json = JSONObject(LauncherBackupCodec.encode(sampleBackup(twoPanelMode = true)))
        json.getJSONObject("settings").getJSONObject("messaging")
            .remove("sendMessagesAutomatically")

        val restored = LauncherBackupCodec.decode(json.toString())

        assertEquals(true, restored.settings.sendMessagesAutomatically)
    }

    @Test fun interimSchemaFourBackupStillImports() {
        val json = JSONObject(LauncherBackupCodec.encode(sampleBackup(twoPanelMode = true)))
        json.put("schemaVersion", 4)

        val restored = LauncherBackupCodec.decode(json.toString())

        assertEquals(
            setOf(MinkSpaceMiniApp.TODO, MinkSpaceMiniApp.MUSIC),
            restored.settings.enabledMinkSpaceMiniApps,
        )
    }

    @Test fun olderTodoShortcutMigratesToExtraAppSlot() {
        val json = JSONObject(LauncherBackupCodec.encode(sampleBackup(twoPanelMode = true)))
        json.put("schemaVersion", 2)
        json.getJSONObject("launcher").put("shortcutOrder", org.json.JSONArray(listOf("TODO", "DRAWER")))

        val restored = LauncherBackupCodec.decode(json.toString())

        assertEquals(com.katoaapps.openminilaunch.model.AppGridSlot.EXTRA, restored.launcher.appGridOrder.first())
        assertEquals(com.katoaapps.openminilaunch.model.AppGridSlot.DRAWER, restored.launcher.appGridOrder.last())
    }

    @Test fun minkSpaceBackupFromBeforeVCardEnablesVCardDuringMigration() {
        val json = JSONObject(LauncherBackupCodec.encode(sampleBackup(twoPanelMode = true)))
        json.getJSONObject("settings").getJSONObject("minkSpace").apply {
            put("order", org.json.JSONArray(listOf("todo", "music", "calculator")))
            put("enabled", org.json.JSONArray(listOf("todo", "music")))
        }

        val restored = LauncherBackupCodec.decode(json.toString())

        assertEquals(
            setOf(MinkSpaceMiniApp.TODO, MinkSpaceMiniApp.MUSIC, MinkSpaceMiniApp.VCARD),
            restored.settings.enabledMinkSpaceMiniApps,
        )
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
            appGridAssignments = emptyMap(),
            appFolders = emptyList(),
            appGridOrder = emptyList(),
            confirmedAppGridChoices = emptyList(),
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
            appBackgroundPresetId = null,
            iconAppearance = IconAppearance(),
            openSoftwareKeyboardOnHome = true,
            includeAppShortcutsInDiscovery = false,
            automaticAppBubbleTargets = listOf("app:0:com.example/.MainActivity"),
            minkSpaceMiniAppOrder = listOf(
                MinkSpaceMiniApp.MUSIC,
                MinkSpaceMiniApp.TODO,
                MinkSpaceMiniApp.CALCULATOR,
                MinkSpaceMiniApp.VCARD,
            ),
            enabledMinkSpaceMiniApps = setOf(MinkSpaceMiniApp.TODO, MinkSpaceMiniApp.MUSIC),
            sendMessagesAutomatically = false,
            preferredMessagingPackage = null,
            preferredAiPackage = null,
            preferredWebPackage = null,
            usesAutomaticSocialApps = true,
            socialPackages = emptyList(),
            socialGoalHours = 1,
            minkAppPauseMode = MinkAppPauseMode.NEVER,
            githubUpdateChecksEnabled = true,
        ),
        todos = emptyList(),
        profile = null,
        profileLinks = emptyList(),
    )
}
