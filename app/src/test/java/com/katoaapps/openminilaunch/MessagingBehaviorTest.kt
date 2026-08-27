package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.data.restoredAutomaticMessageSend
import com.katoaapps.openminilaunch.features.messaging.MessagingSendRoute
import com.katoaapps.openminilaunch.features.messaging.MessagingProviderCatalog
import com.katoaapps.openminilaunch.features.messaging.messagingSendRoute
import com.katoaapps.openminilaunch.platform.PreferredMessageDraftResult
import com.katoaapps.openminilaunch.platform.defaultMessageDraftResult
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
        assertEquals(providers.size, providers.map { it.packageName }.distinct().size)
        assertTrue(providers.all { it.bundledIconRes != null })
        assertTrue(providers.all { it.storeUrl.startsWith("https://") })
        assertTrue(providers.all { it.documentationUrl.startsWith("https://") })
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
