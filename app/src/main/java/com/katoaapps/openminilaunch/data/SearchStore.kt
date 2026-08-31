package com.katoaapps.openminilaunch.data

import androidx.compose.runtime.mutableStateListOf
import com.katoaapps.openminilaunch.model.SearchFolder

internal class SearchStore(
    private val persistence: LauncherStorePersistence,
    private val maxHistory: Int,
) {
    val folders = mutableStateListOf<SearchFolder>()
    val history = mutableStateListOf<String>()

    fun restore(snapshot: LauncherStoreSnapshot) {
        folders.addAll(snapshot.searchFolders)
        history.addAll(snapshot.searchHistory)
    }

    fun addFolder(uri: String, label: String) {
        if (folders.none { it.uri == uri }) {
            folders += SearchFolder(uri, label)
            persistence.saveSearchFolders(folders)
        }
    }

    fun removeFolder(uri: String) {
        folders.removeAll { it.uri == uri }
        persistence.saveSearchFolders(folders)
    }

    fun addQuery(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        history.removeAll { it.equals(clean, ignoreCase = true) }
        history.add(0, clean)
        while (history.size > maxHistory) history.removeAt(history.lastIndex)
        persistence.saveSearchHistory(history)
    }

    fun removeQuery(query: String) {
        history.removeAll { it == query }
        persistence.saveSearchHistory(history)
    }

    fun clearHistory() {
        history.clear()
        persistence.saveSearchHistory(history)
    }
}
