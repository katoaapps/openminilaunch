package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.features.messaging.MessagingSendRoute
import com.katoaapps.openminilaunch.features.messaging.MessagingProviderCatalog
import com.katoaapps.openminilaunch.features.messaging.PreferredMessageDraftResult
import com.katoaapps.openminilaunch.features.messaging.defaultMessageDraftResult
import com.katoaapps.openminilaunch.features.messaging.packageNames
import com.katoaapps.openminilaunch.features.messaging.resolveInstalledPackage
import com.katoaapps.openminilaunch.features.messaging.restoredAutomaticMessageSend
import com.katoaapps.openminilaunch.features.messaging.messagingSendRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagingBehaviorTest {
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
}
