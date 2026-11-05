package com.katoaapps.openminilaunch.data

import com.katoaapps.openminilaunch.model.WidgetEntry
import com.katoaapps.openminilaunch.model.WidgetGridSize
import com.katoaapps.openminilaunch.model.WidgetProviderIdentity
import com.katoaapps.openminilaunch.model.WidgetRestoreRecipe
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetStoreTest {
    private val provider = WidgetProviderIdentity(
        packageName = "com.example.widgets",
        className = "com.example.widgets.SampleWidget",
        appLabel = "Example",
        widgetLabel = "Sample",
    )

    @Test
    fun normalizeWidgetOrderAppliesRequestedOrder() {
        assertEquals(
            listOf("30", "10", "20"),
            normalizeWidgetOrder(
                currentIds = listOf("10", "20", "30"),
                requestedIds = listOf("30", "10", "20"),
            ),
        )
    }

    @Test
    fun normalizeWidgetOrderDoesNotLoseConfiguredWidgets() {
        assertEquals(
            listOf("30", "10", "20"),
            normalizeWidgetOrder(
                currentIds = listOf("10", "20", "30"),
                requestedIds = listOf("30"),
            ),
        )
    }

    @Test
    fun normalizeWidgetOrderIgnoresUnknownAndDuplicateIds() {
        assertEquals(
            listOf("20", "10", "30"),
            normalizeWidgetOrder(
                currentIds = listOf("10", "20", "30"),
                requestedIds = listOf("99", "20", "20", "10"),
            ),
        )
    }

    @Test
    fun importingWidgetRecipesKeepsCurrentWidgetsAndAppendsPlaceholders() {
        val current = listOf(
            WidgetEntry.Bound("local", 101, WidgetGridSize(2, 2), provider),
        )
        val imported = WidgetRestoreRecipe("imported", provider, WidgetGridSize(4, 1))

        val merge = mergeWidgetRestoreRecipes(current, listOf(imported), maxWidgets = 64)

        assertEquals(listOf("local", "imported"), merge.entries.map(WidgetEntry::entryId))
        assertEquals(1, merge.result.added)
        assertEquals(0, merge.result.alreadyPresent)
    }

    @Test
    fun importingSameWidgetRecipeTwiceDoesNotDuplicateIt() {
        val recipe = WidgetRestoreRecipe("same", provider, WidgetGridSize(2, 2))
        val current = listOf(WidgetEntry.RestorePlaceholder(recipe))

        val merge = mergeWidgetRestoreRecipes(current, listOf(recipe), maxWidgets = 64)

        assertEquals(current, merge.entries)
        assertEquals(0, merge.result.added)
        assertEquals(1, merge.result.alreadyPresent)
    }

    @Test
    fun importingWidgetRecipesReportsEntriesSkippedAtLimit() {
        val current = listOf(
            WidgetEntry.Bound("local", 101, WidgetGridSize(2, 2), provider),
        )
        val recipes = listOf(
            WidgetRestoreRecipe("one", provider, WidgetGridSize(2, 2)),
            WidgetRestoreRecipe("two", provider, WidgetGridSize(2, 2)),
        )

        val merge = mergeWidgetRestoreRecipes(current, recipes, maxWidgets = 2)

        assertEquals(listOf("local", "one"), merge.entries.map(WidgetEntry::entryId))
        assertEquals(1, merge.result.added)
        assertEquals(1, merge.result.skippedAtLimit)
    }

    @Test
    fun legacyBindingsChangedByOlderBuildReplaceStaleBoundEntriesButKeepPlaceholders() {
        val placeholder = WidgetEntry.RestorePlaceholder(
            WidgetRestoreRecipe("portable", provider, WidgetGridSize(4, 1)),
        )
        val saved = listOf(
            WidgetEntry.Bound("removed", 101, WidgetGridSize(2, 2), provider),
            placeholder,
        )

        val reconciled = reconcileWidgetEntriesWithLegacyBindings(
            savedEntries = saved,
            legacyWidgetIds = listOf(202),
            legacyWidgetSizes = mapOf(202 to WidgetGridSize(3, 1)),
            maxWidgets = 64,
        )

        assertEquals(listOf(202), reconciled.filterIsInstance<WidgetEntry.Bound>().map { it.appWidgetId })
        assertEquals(WidgetGridSize(3, 1), reconciled.first().gridSize)
        assertEquals(placeholder, reconciled.last())
    }

    @Test
    fun legacyResizeUpdatesBoundEntryWithoutChangingPlaceholderOrder() {
        val bound = WidgetEntry.Bound("local", 101, WidgetGridSize(2, 2), provider)
        val placeholder = WidgetEntry.RestorePlaceholder(
            WidgetRestoreRecipe("portable", provider, WidgetGridSize(4, 1)),
        )

        val reconciled = reconcileWidgetEntriesWithLegacyBindings(
            savedEntries = listOf(placeholder, bound),
            legacyWidgetIds = listOf(101),
            legacyWidgetSizes = mapOf(101 to WidgetGridSize(4, 3)),
            maxWidgets = 64,
        )

        assertEquals("portable", reconciled.first().entryId)
        assertEquals(WidgetGridSize(4, 3), reconciled.last().gridSize)
    }
}
