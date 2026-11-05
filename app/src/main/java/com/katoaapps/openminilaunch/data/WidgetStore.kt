package com.katoaapps.openminilaunch.data

import androidx.compose.runtime.mutableStateListOf
import com.katoaapps.openminilaunch.model.WidgetEntry
import com.katoaapps.openminilaunch.model.WidgetGridSize
import com.katoaapps.openminilaunch.model.WidgetProviderIdentity
import com.katoaapps.openminilaunch.model.WidgetRestoreRecipe
import java.util.UUID

internal data class WidgetRecipeImportResult(
    val added: Int,
    val alreadyPresent: Int,
    val skippedAtLimit: Int,
)

internal data class WidgetRecipeMerge(
    val entries: List<WidgetEntry>,
    val result: WidgetRecipeImportResult,
)

internal class WidgetStore(
    private val persistence: LauncherStorePersistence,
    private val maxWidgets: Int,
) {
    val entries = mutableStateListOf<WidgetEntry>()

    fun restore(snapshot: LauncherStoreSnapshot) {
        entries.addAll(snapshot.widgetEntries.take(maxWidgets))
        // Persist the entry model immediately so legacy widget IDs receive stable UUIDs once.
        if (entries.isNotEmpty()) save()
    }

    fun add(
        appWidgetId: Int,
        size: WidgetGridSize,
        provider: WidgetProviderIdentity,
        replacementEntryId: String? = null,
    ) {
        if (entries.any { it is WidgetEntry.Bound && it.appWidgetId == appWidgetId }) return
        val replacementIndex = replacementEntryId?.let { entryId ->
            entries.indexOfFirst { it.entryId == entryId }.takeIf { it >= 0 }
        }
        if (replacementIndex == null && entries.size >= maxWidgets) return
        val entry = WidgetEntry.Bound(
            entryId = replacementEntryId ?: UUID.randomUUID().toString(),
            appWidgetId = appWidgetId,
            gridSize = size,
            provider = provider,
        )
        if (replacementIndex != null) entries[replacementIndex] = entry else entries += entry
        save()
    }

    fun remove(entryId: String): WidgetEntry? {
        val index = entries.indexOfFirst { it.entryId == entryId }
        if (index < 0) return null
        return entries.removeAt(index).also { save() }
    }

    fun setSize(appWidgetId: Int, size: WidgetGridSize) {
        if (size.columns !in 1..4 || size.rows !in 1..5) return
        val index = entries.indexOfFirst {
            it is WidgetEntry.Bound && it.appWidgetId == appWidgetId
        }
        if (index < 0) return
        val current = entries[index] as WidgetEntry.Bound
        if (current.gridSize == size) return
        entries[index] = current.copy(gridSize = size)
        save()
    }

    fun updateProvider(appWidgetId: Int, provider: WidgetProviderIdentity) {
        val index = entries.indexOfFirst {
            it is WidgetEntry.Bound && it.appWidgetId == appWidgetId
        }
        if (index < 0) return
        val current = entries[index] as WidgetEntry.Bound
        if (current.provider == provider) return
        entries[index] = current.copy(provider = provider)
        save()
    }

    fun setOrder(orderedEntryIds: List<String>) {
        val currentOrder = entries.map(WidgetEntry::entryId)
        val normalizedOrder = normalizeWidgetOrder(currentOrder, orderedEntryIds)
        if (normalizedOrder == currentOrder) return
        val entriesById = entries.associateBy(WidgetEntry::entryId)
        entries.clear()
        entries.addAll(normalizedOrder.mapNotNull(entriesById::get))
        save()
    }

    fun importRestoreRecipes(recipes: List<WidgetRestoreRecipe>): WidgetRecipeImportResult {
        val merge = mergeWidgetRestoreRecipes(entries, recipes, maxWidgets)
        if (merge.result.added > 0) {
            entries.clear()
            entries.addAll(merge.entries)
            save()
        }
        return merge.result
    }

    private fun save() {
        persistence.saveWidgets(entries)
    }
}

internal fun mergeWidgetRestoreRecipes(
    currentEntries: List<WidgetEntry>,
    recipes: List<WidgetRestoreRecipe>,
    maxWidgets: Int,
): WidgetRecipeMerge {
    val existingIds = currentEntries.mapTo(mutableSetOf(), WidgetEntry::entryId)
    val uniqueRecipes = recipes.distinctBy(WidgetRestoreRecipe::entryId)
    val alreadyPresent = uniqueRecipes.count { it.entryId in existingIds }
    val candidates = uniqueRecipes.filterNot { it.entryId in existingIds }
    val availableSlots = (maxWidgets - currentEntries.size).coerceAtLeast(0)
    val accepted = candidates.take(availableSlots)
    return WidgetRecipeMerge(
        entries = currentEntries + accepted.map(WidgetEntry::RestorePlaceholder),
        result = WidgetRecipeImportResult(
            added = accepted.size,
            alreadyPresent = alreadyPresent,
            skippedAtLimit = candidates.size - accepted.size,
        ),
    )
}

/** Keeps every configured widget exactly once while applying a requested display order. */
internal fun normalizeWidgetOrder(
    currentIds: List<String>,
    requestedIds: List<String>,
): List<String> {
    val configuredIds = currentIds.toSet()
    val includedIds = mutableSetOf<String>()
    return buildList {
        requestedIds.forEach { id ->
            if (id in configuredIds && includedIds.add(id)) add(id)
        }
        currentIds.forEach { id ->
            if (includedIds.add(id)) add(id)
        }
    }
}
