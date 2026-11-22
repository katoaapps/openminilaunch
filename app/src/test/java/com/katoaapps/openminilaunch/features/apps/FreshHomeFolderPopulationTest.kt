package com.katoaapps.openminilaunch.features.apps

import android.content.pm.ApplicationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class FreshHomeFolderPopulationTest {
    @Test
    fun mapsAndroidCategoriesToPurposeFolders() {
        assertEquals(HomeFolderPurpose.CONNECT, purposeForApplicationCategory(ApplicationInfo.CATEGORY_SOCIAL))
        assertEquals(HomeFolderPurpose.TASKS, purposeForApplicationCategory(ApplicationInfo.CATEGORY_PRODUCTIVITY))
        assertEquals(HomeFolderPurpose.MEDIA, purposeForApplicationCategory(ApplicationInfo.CATEGORY_AUDIO))
        assertEquals(HomeFolderPurpose.MEDIA, purposeForApplicationCategory(ApplicationInfo.CATEGORY_VIDEO))
        assertEquals(HomeFolderPurpose.MEDIA, purposeForApplicationCategory(ApplicationInfo.CATEGORY_NEWS))
        assertEquals(HomeFolderPurpose.COMPOSE, purposeForApplicationCategory(ApplicationInfo.CATEGORY_IMAGE))
        assertEquals(HomeFolderPurpose.BROWSE, purposeForApplicationCategory(ApplicationInfo.CATEGORY_MAPS))
        assertNull(purposeForApplicationCategory(ApplicationInfo.CATEGORY_GAME))
        assertEquals(HomeFolderPurpose.TOOLS, purposeForApplicationCategory(ApplicationInfo.CATEGORY_UNDEFINED))
    }

    @Test
    fun defaultHandlersOverrideCategoryAndSortFirst() {
        val seeds = freshHomeFolderSeeds(
            candidates = listOf(
                candidate("social", "Social", ApplicationInfo.CATEGORY_SOCIAL),
                candidate(
                    key = "messages",
                    label = "Z Messages",
                    category = ApplicationInfo.CATEGORY_PRODUCTIVITY,
                    preferredPurpose = HomeFolderPurpose.CONNECT,
                ),
                candidate("tasks", "Tasks", ApplicationInfo.CATEGORY_PRODUCTIVITY),
                candidate("media", "Media", ApplicationInfo.CATEGORY_AUDIO),
                candidate("compose", "Compose", ApplicationInfo.CATEGORY_IMAGE),
                candidate("browse", "Browse", ApplicationInfo.CATEGORY_MAPS),
                candidate("tools", "Tools", ApplicationInfo.CATEGORY_UNDEFINED),
                candidate("fun", "Fun", ApplicationInfo.CATEGORY_GAME),
            ),
            locale = Locale.US,
            title = { it.name },
        )

        assertEquals(HomeFolderPurpose.entries, seeds.map(FreshHomeFolderSeed::purpose))
        assertEquals(6, seeds.size)
        assertFalse(seeds.flatMap(FreshHomeFolderSeed::selectionKeys).contains("fun"))
        assertEquals(listOf("messages", "social"), seeds.first().selectionKeys)
        assertEquals(listOf("tasks"), seeds[1].selectionKeys)
    }

    @Test
    fun capsInitialPopulationAtEightAndUsesOneAppPerPackage() {
        val candidates = (1..70).map { index ->
            FreshHomeFolderCandidate(
                selectionKey = "key-$index",
                packageName = "package-$index",
                label = "App $index",
                applicationCategory = ApplicationInfo.CATEGORY_UNDEFINED,
            )
        } + FreshHomeFolderCandidate(
            selectionKey = "duplicate-activity",
            packageName = "package-1",
            label = "Duplicate activity",
            applicationCategory = ApplicationInfo.CATEGORY_UNDEFINED,
        )

        val tools = freshHomeFolderSeeds(candidates, Locale.US) { it.name }
            .single { it.purpose == HomeFolderPurpose.TOOLS }

        assertEquals(8, tools.selectionKeys.size)
        assertEquals(8, tools.selectionKeys.distinct().size)
    }

    private fun candidate(
        key: String,
        label: String,
        category: Int,
        preferredPurpose: HomeFolderPurpose? = null,
    ) = FreshHomeFolderCandidate(
        selectionKey = key,
        packageName = "package.$key",
        label = label,
        applicationCategory = category,
        preferredPurpose = preferredPurpose,
    )
}
