package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.features.messaging.MessagingDraftKind
import com.katoaapps.openminilaunch.features.messaging.MessagingProviderCatalog
import com.katoaapps.openminilaunch.features.messaging.MessagingSendRoute
import com.katoaapps.openminilaunch.features.messaging.MessagingSupportTier
import com.katoaapps.openminilaunch.features.messaging.MessagingShortcutDraftCatalog
import com.katoaapps.openminilaunch.features.messaging.MessagingProviderOption
import com.katoaapps.openminilaunch.features.messaging.PreferredMessageDraftResult
import com.katoaapps.openminilaunch.features.messaging.automaticMessagingPackage
import com.katoaapps.openminilaunch.features.messaging.defaultMessageDraftResult
import com.katoaapps.openminilaunch.features.messaging.filteredRecentConversations
import com.katoaapps.openminilaunch.features.messaging.packageNames
import com.katoaapps.openminilaunch.features.messaging.resolveInstalledPackage
import com.katoaapps.openminilaunch.features.messaging.restoredAutomaticMessageSend
import com.katoaapps.openminilaunch.features.messaging.messagingSendRoute
import com.katoaapps.openminilaunch.features.messaging.resolveRecentConversationPackages
import com.katoaapps.openminilaunch.features.messaging.recentConversationSendRoute
import com.katoaapps.openminilaunch.features.messaging.resolvedMessageSendRoute
import com.katoaapps.openminilaunch.features.messaging.isDraftReady
import com.katoaapps.openminilaunch.model.looksLikePhoneRecipient
import com.katoaapps.openminilaunch.model.userEnteredRecipient
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagingBehaviorTest {
    @Test fun forcedRecipientPreservesTheUserSuppliedAddress() {
        val recipient = userEnteredRecipient("  @mink-user  ")

        assertEquals("@mink-user", recipient.address)
        assertTrue(recipient.userEntered)
    }

    @Test fun forcedRecipientSeparatesPhoneFormatsFromUsernames() {
        assertTrue("+1 (555) 123-4567".looksLikePhoneRecipient())
        assertFalse("mink-user".looksLikePhoneRecipient())
        assertFalse("mink@example.com".looksLikePhoneRecipient())
    }

    @Test fun userEnteredUsernameNeverUsesDirectCarrierSms() {
        assertEquals(
            MessagingSendRoute.PREFERRED_DRAFT,
            resolvedMessageSendRoute(
                recipient = userEnteredRecipient("mink-user"),
                selectedConversationPackage = "com.example.sms",
                defaultSmsPackage = "com.example.sms",
                sendAutomatically = true,
                preferredPackage = null,
                fallbackRoute = MessagingSendRoute.DIRECT_SMS,
            ),
        )
    }

    @Test fun legacyDirectAndPreferredModesMigrateToAutomatic() {
        assertTrue(restoredAutomaticMessageSend(null, "DIRECT_SMS"))
        assertTrue(restoredAutomaticMessageSend(null, "PREFERRED_APP"))
        assertTrue(restoredAutomaticMessageSend(null, "MESSAGING_APP"))
        assertTrue(restoredAutomaticMessageSend(null, "DEFAULT_MESSENGER"))
    }

    @Test fun legacyChooserAndFreshInstallsRemainManual() {
        assertFalse(restoredAutomaticMessageSend(null, "SYSTEM_CHOOSER"))
        assertFalse(restoredAutomaticMessageSend(null, "ALWAYS_ASK"))
        assertFalse(restoredAutomaticMessageSend(null, null))
        assertFalse(restoredAutomaticMessageSend(null, "UNKNOWN"))
    }

    @Test fun explicitAutomaticPreferenceOverridesLegacyValue() {
        assertFalse(restoredAutomaticMessageSend(false, "DIRECT_SMS"))
        assertTrue(restoredAutomaticMessageSend(true, "SYSTEM_CHOOSER"))
    }

    @Test fun sendRouteUsesPreferredProviderOnlyInAutomaticMode() {
        assertEquals(
            MessagingSendRoute.PROVIDER_PICKER,
            messagingSendRoute(sendAutomatically = false, preferredPackage = "com.whatsapp"),
        )
        assertEquals(
            MessagingSendRoute.DIRECT_SMS,
            messagingSendRoute(sendAutomatically = true, preferredPackage = null),
        )
        assertEquals(
            MessagingSendRoute.PREFERRED_DRAFT,
            messagingSendRoute(sendAutomatically = true, preferredPackage = "com.whatsapp"),
        )
    }

    @Test fun automaticRecipientChipUsesTheActualMessagingPackage() {
        assertEquals(
            "com.whatsapp",
            automaticMessagingPackage(
                sendAutomatically = true,
                preferredPackage = "com.whatsapp",
                defaultMessagingPackage = "com.google.android.apps.messaging",
            ),
        )
        assertEquals(
            "com.google.android.apps.messaging",
            automaticMessagingPackage(
                sendAutomatically = true,
                preferredPackage = null,
                defaultMessagingPackage = "com.google.android.apps.messaging",
            ),
        )
        assertNull(
            automaticMessagingPackage(
                sendAutomatically = false,
                preferredPackage = "com.whatsapp",
                defaultMessagingPackage = "com.google.android.apps.messaging",
            ),
        )
    }

    @Test fun selectedSystemConversationPreservesAutomaticDirectSms() {
        assertEquals(
            MessagingSendRoute.DIRECT_SMS,
            recentConversationSendRoute(
                selectedConversationPackage = "com.example.sms",
                defaultSmsPackage = "com.example.sms",
                sendAutomatically = true,
                preferredPackage = null,
                fallbackRoute = MessagingSendRoute.PREFERRED_DRAFT,
            ),
        )
    }

    @Test fun selectedProviderConversationKeepsProviderOwnedFinalSend() {
        assertEquals(
            MessagingSendRoute.PREFERRED_DRAFT,
            recentConversationSendRoute(
                selectedConversationPackage = "im.molly.app",
                defaultSmsPackage = "com.example.sms",
                sendAutomatically = true,
                preferredPackage = "im.molly.app",
                fallbackRoute = MessagingSendRoute.PREFERRED_DRAFT,
            ),
        )
    }

    @Test fun curatedProvidersHaveUniquePackagesAndBundledBrandAssets() {
        val providers = MessagingProviderCatalog.providers

        assertEquals(providers.size, providers.map { it.id }.distinct().size)
        val packageNames = providers.flatMap { it.packageNames }
        assertEquals(packageNames.size, packageNames.distinct().size)
        assertTrue(providers.all { it.bundledIconRes != null })
        assertTrue(providers.all { it.storeUrl.startsWith("https://") })
        assertTrue(providers.all { it.documentationUrl.startsWith("https://") })
    }

    @Test fun telegramWebsiteAndPlayBuildsResolveToTheSameProvider() {
        val playProvider = MessagingProviderCatalog.providerForPackage("org.telegram.messenger")
        val websiteProvider = MessagingProviderCatalog.providerForPackage("org.telegram.messenger.web")

        assertEquals("telegram", playProvider?.id)
        assertEquals(playProvider, websiteProvider)
    }

    @Test fun beeperUsesShareWhileSignalAndMollyUseTheirDirectContactDrafts() {
        val beeper = MessagingProviderCatalog.providerForPackage("com.beeper.android")!!
        val molly = MessagingProviderCatalog.providerForPackage("im.molly.app")!!
        val signal = MessagingProviderCatalog.providerForPackage("org.thoughtcrime.securesms")!!

        assertEquals("beeper", beeper.id)
        assertEquals(MessagingDraftKind.GENERIC_SHARE, beeper.kind)
        assertEquals(MessagingSupportTier.RECIPIENT_IN_APP, beeper.supportTier)
        assertEquals("molly", molly.id)
        assertEquals(MessagingDraftKind.SIGNAL_CONTACT, molly.kind)
        assertEquals(MessagingSupportTier.CONTACT_AND_DRAFT, molly.supportTier)
        assertEquals("https://molly.im/", molly.storeUrl)
        assertEquals(MessagingDraftKind.SIGNAL_CONTACT, signal.kind)
        assertEquals(MessagingSupportTier.CONTACT_AND_DRAFT, signal.supportTier)
    }

    @Test fun shortcutDraftCatalogRequiresThePublishedShareCategory() {
        assertEquals(
            "slack.app.ui.ShareReceiverActivity",
            MessagingShortcutDraftCatalog.routeFor(
                packageName = "com.Slack",
                shortcutCategories = setOf("slack.services.shareshortcuts.SHARE_SHORTCUT_TARGET"),
            )?.targetActivity,
        )
        assertNull(
            MessagingShortcutDraftCatalog.routeFor(
                packageName = "com.Slack",
                shortcutCategories = setOf("android.shortcut.conversation"),
            ),
        )
    }

    @Test fun pickerCombinesContactAndRecentChatDraftCapabilities() {
        assertTrue(providerOption(MessagingSupportTier.CONTACT_AND_DRAFT).isDraftReady)
        assertTrue(
            providerOption(
                MessagingSupportTier.RECIPIENT_IN_APP,
                supportsRecentChatDrafts = true,
            ).isDraftReady,
        )
        assertFalse(providerOption(MessagingSupportTier.RECIPIENT_IN_APP).isDraftReady)
        assertFalse(providerOption(MessagingSupportTier.CONDITIONAL).isDraftReady)
    }

    @Test fun providerPackageOrderSelectsTheFirstInstalledOfficialBuild() {
        val telegram = MessagingProviderCatalog.providerForPackage("org.telegram.messenger.web")!!

        assertEquals(
            "org.telegram.messenger.web",
            telegram.resolveInstalledPackage { it == "org.telegram.messenger.web" },
        )
        assertEquals(
            "org.telegram.messenger",
            telegram.resolveInstalledPackage { true },
        )
    }

    @Test fun deliberatelySelectedSystemMessagesIsNotReportedAsAFallback() {
        assertEquals(
            PreferredMessageDraftResult.OPENED,
            defaultMessageDraftResult(integratedPackage = null, opened = true),
        )
        assertEquals(
            PreferredMessageDraftResult.FALLBACK_OPENED,
            defaultMessageDraftResult(integratedPackage = "com.whatsapp", opened = true),
        )
        assertEquals(
            PreferredMessageDraftResult.FAILED,
            defaultMessageDraftResult(integratedPackage = null, opened = false),
        )
    }

    @Test fun automaticRecentConversationsFollowTheSelectedProvider() {
        assertEquals(
            setOf("im.molly.app"),
            resolveRecentConversationPackages(
                sendAutomatically = true,
                preferredPackage = "im.molly.app",
                defaultSmsPackage = "com.example.sms",
                isInstalled = { true },
            ),
        )
    }

    @Test fun automaticSystemMessagesUsesOnlyTheDefaultSmsApp() {
        assertEquals(
            setOf("com.example.sms"),
            resolveRecentConversationPackages(
                sendAutomatically = true,
                preferredPackage = null,
                defaultSmsPackage = "com.example.sms",
                isInstalled = { true },
            ),
        )
    }

    @Test fun manualRecentConversationsIncludeInstalledProvidersAndDefaultSms() {
        val installed = setOf("com.example.sms", "im.molly.app", "org.telegram.messenger.web")

        assertEquals(
            installed,
            resolveRecentConversationPackages(
                sendAutomatically = false,
                preferredPackage = "im.molly.app",
                defaultSmsPackage = "com.example.sms",
                isInstalled = installed::contains,
            ),
        )
    }

    @Test fun manualRecentConversationsPutTheChosenProviderFirst() {
        val shortcuts = listOf(
            conversationShortcut("Recent other", "org.telegram.messenger"),
            conversationShortcut("Preferred one", "im.molly.app"),
            conversationShortcut("Older other", "com.whatsapp"),
            conversationShortcut("Preferred two", "im.molly.app"),
        )

        assertEquals(
            listOf("Preferred one", "Preferred two", "Recent other"),
            filteredRecentConversations(
                shortcuts = shortcuts,
                query = "",
                appLabel = { it },
                prioritizedPackage = "im.molly.app",
                limit = 3,
            ).map(LauncherShortcutTarget::label),
        )
    }

    @Test fun unavailableAutomaticProviderFallsBackToDefaultSmsScope() {
        assertEquals(
            setOf("com.example.sms"),
            resolveRecentConversationPackages(
                sendAutomatically = true,
                preferredPackage = "im.molly.app",
                defaultSmsPackage = "com.example.sms",
                isInstalled = { it == "com.example.sms" },
            ),
        )
    }

    private fun providerOption(
        supportTier: MessagingSupportTier,
        supportsRecentChatDrafts: Boolean = false,
    ) = MessagingProviderOption(
        id = "provider",
        label = "Provider",
        preferencePackageName = "com.example.provider",
        installedPackageName = "com.example.provider",
        supportTier = supportTier,
        bundledIconRes = null,
        installed = true,
        supportsRecentChatDrafts = supportsRecentChatDrafts,
    )

    private fun conversationShortcut(
        label: String,
        packageName: String,
    ) = LauncherShortcutTarget(
        label = label,
        packageName = packageName,
        shortcutId = label,
        userSerial = 0,
        isWorkProfile = false,
    )
}
