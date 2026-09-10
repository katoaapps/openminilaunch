package com.katoaapps.openminilaunch.features.files

import java.util.Locale

internal data class FileSearchScope(
    val folderUris: List<String>,
    val includesMedia: Boolean,
    val usesDemoData: Boolean,
) {
    val hasSearchableSources: Boolean
        get() = folderUris.isNotEmpty() || includesMedia || usesDemoData
}

/** Stops descendant queries after a local filename prefix has already returned no results. */
internal class FileSearchQueryGate {
    private var knownEmptySearch: KnownEmptySearch? = null

    fun shouldSearch(query: String, scope: FileSearchScope): Boolean {
        val cleanQuery = query.normalizedSearchQuery()
        val knownEmpty = knownEmptySearch ?: return true
        if (knownEmpty.scope != scope || !cleanQuery.startsWith(knownEmpty.query)) {
            knownEmptySearch = null
            return true
        }
        return false
    }

    fun recordResult(query: String, scope: FileSearchScope, hasResults: Boolean) {
        knownEmptySearch = if (hasResults) {
            null
        } else {
            KnownEmptySearch(query.normalizedSearchQuery(), scope)
        }
    }

    fun reset() {
        knownEmptySearch = null
    }

    private data class KnownEmptySearch(
        val query: String,
        val scope: FileSearchScope,
    )
}

private fun String.normalizedSearchQuery(): String = trim().lowercase(Locale.ROOT)
