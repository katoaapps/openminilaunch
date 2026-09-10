package com.katoaapps.openminilaunch.ui.magic

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.files.FileSearchQueryGate
import com.katoaapps.openminilaunch.features.files.FileSearchRepository
import com.katoaapps.openminilaunch.features.files.FileSearchRequestTracker
import com.katoaapps.openminilaunch.features.files.FileSearchScope
import com.katoaapps.openminilaunch.model.FileSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Stable
internal class MagicFileSearchState internal constructor() {
    var results by mutableStateOf<List<FileSearchResult>>(emptyList())
        internal set
    var loading by mutableStateOf(false)
        internal set

    internal val requests = FileSearchRequestTracker()
    internal val queryGate = FileSearchQueryGate()

    fun clear() {
        requests.invalidate()
        queryGate.reset()
        results = emptyList()
        loading = false
    }
}

/** Owns debouncing, cancellation, and empty-query suppression for local file results. */
@Composable
internal fun rememberMagicFileSearchState(
    query: String,
    store: LauncherStore,
    repository: FileSearchRepository,
    hasMediaAccess: Boolean,
    resultsScroll: ScrollState,
): MagicFileSearchState {
    val state = remember { MagicFileSearchState() }
    val folders = store.searchFolders.toList()
    val folderUris = folders.map { it.uri }
    val demoModeEnabled = store.demoSearchDataEnabled

    LaunchedEffect(query, folderUris, hasMediaAccess, demoModeEnabled) {
        val request = state.requests.begin(query)
        val scope = FileSearchScope(
            folderUris = folderUris,
            includesMedia = hasMediaAccess,
            usesDemoData = demoModeEnabled,
        )
        val shouldSearch = when {
            query.length < 2 -> {
                state.queryGate.reset()
                false
            }
            !scope.hasSearchableSources -> false
            else -> state.queryGate.shouldSearch(query, scope)
        }
        if (!shouldSearch) {
            state.results = emptyList()
            state.loading = false
            resultsScroll.scrollTo(0)
            return@LaunchedEffect
        }

        state.loading = true
        try {
            delay(180)
            val results = withContext(Dispatchers.IO) {
                repository.search(
                    query = request.query,
                    folders = folders,
                    includeMedia = hasMediaAccess,
                    useDemoData = demoModeEnabled,
                )
            }
            if (state.requests.isCurrent(request)) {
                state.results = results
                state.queryGate.recordResult(
                    query = request.query,
                    scope = scope,
                    hasResults = results.isNotEmpty(),
                )
                resultsScroll.scrollTo(0)
            }
        } finally {
            if (state.requests.isCurrent(request)) state.loading = false
        }
    }
    return state
}
