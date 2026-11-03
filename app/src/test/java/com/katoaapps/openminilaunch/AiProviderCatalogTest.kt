package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.features.ai.AiHandoffMode
import com.katoaapps.openminilaunch.features.ai.AiProviderCatalog
import com.katoaapps.openminilaunch.features.ai.AiProviderOption
import com.katoaapps.openminilaunch.features.ai.matchesSelectedPackage
import com.katoaapps.openminilaunch.features.ai.packageNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiProviderCatalogTest {
    @Test fun bothLumoBuildsUseCopyAndLaunch() {
        val lumoPackages = setOf(
            AiProviderCatalog.LUMO_PLAY_PACKAGE,
            AiProviderCatalog.LUMO_NO_GMS_PACKAGE,
        )

        assertTrue(AiProviderCatalog.curatedPackages.containsAll(lumoPackages))
        lumoPackages.forEach { packageName ->
            assertEquals(
                AiHandoffMode.COPY_AND_LAUNCH,
                AiProviderCatalog.handoffMode(packageName),
            )
        }
        assertEquals(
            lumoPackages,
            AiProviderCatalog.providerForPackage(AiProviderCatalog.LUMO_NO_GMS_PACKAGE)
                ?.packageNames
                ?.toSet(),
        )
    }

    @Test fun ordinaryAiAppsRetainAndroidTextSharing() {
        assertEquals(
            AiHandoffMode.TEXT_SHARE,
            AiProviderCatalog.handoffMode("com.openai.chatgpt"),
        )
    }

    @Test fun catalogIdsAndPackagesAreUnique() {
        val providers = AiProviderCatalog.providers
        val packages = providers.flatMap { it.packageNames }

        assertEquals(providers.size, providers.map { it.id }.distinct().size)
        assertEquals(packages.size, packages.distinct().size)
    }

    @Test fun unavailableProvidersDoNotMatchAnUnsetPreference() {
        val unavailable = AiProviderOption(
            id = "unavailable",
            label = "Unavailable",
            installedPackageName = null,
            handoffMode = AiHandoffMode.TEXT_SHARE,
            installed = false,
            canHandoff = false,
            installUrl = "https://example.com",
            bundledIconRes = 0,
        )
        val installed = unavailable.copy(
            installedPackageName = "com.example.installed",
            installed = true,
            canHandoff = true,
        )

        assertFalse(unavailable.matchesSelectedPackage(null))
        assertFalse(unavailable.matchesSelectedPackage("com.example.installed"))
        assertFalse(installed.matchesSelectedPackage(null))
        assertTrue(installed.matchesSelectedPackage("com.example.installed"))
    }
}
