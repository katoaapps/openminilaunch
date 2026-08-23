package com.katoaapps.openminilaunch.features.files

internal data class FileSearchRequest(
    val id: Long,
    val query: String,
)

/**
 * Prevents a slower file lookup from publishing results after a newer Magic Box query starts.
 * Calls are made from the Compose main thread; the blocking lookup may run on Dispatchers.IO.
 */
internal class FileSearchRequestTracker {
    private var latestId = 0L

    fun begin(query: String): FileSearchRequest = FileSearchRequest(
        id = ++latestId,
        query = query,
    )

    fun invalidate() {
        latestId++
    }

    fun isCurrent(request: FileSearchRequest): Boolean = request.id == latestId
}
