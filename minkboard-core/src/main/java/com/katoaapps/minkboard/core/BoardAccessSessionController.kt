package com.katoaapps.minkboard.core

import com.katoaapps.minkboard.model.BoardId

/**
 * Process-memory authorization for interaction-locked boards. No result is persisted, backed up,
 * or restored after process death.
 */
class BoardAccessSessionController(
    val backgroundTimeoutMillis: Long = DEFAULT_BACKGROUND_TIMEOUT_MILLIS,
    private val elapsedRealtimeMillis: () -> Long,
) {
    private val unlockedBoardIds = mutableSetOf<BoardId>()
    private var backgroundedAtElapsedMillis: Long? = null

    init {
        require(backgroundTimeoutMillis >= 0L) { "Background timeout cannot be negative" }
    }

    fun grant(boardId: BoardId) {
        unlockedBoardIds += boardId
    }

    fun revoke(boardId: BoardId) {
        unlockedBoardIds -= boardId
    }

    fun revokeAll() {
        unlockedBoardIds.clear()
        backgroundedAtElapsedMillis = null
    }

    fun isGranted(boardId: BoardId): Boolean = boardId in unlockedBoardIds

    fun onBackgrounded() {
        if (backgroundedAtElapsedMillis == null) {
            backgroundedAtElapsedMillis = elapsedRealtimeMillis()
        }
    }

    /**
     * Returns true when a completed background interval expired every board authorization.
     */
    fun onForegrounded(): Boolean {
        val backgroundedAt = backgroundedAtElapsedMillis ?: return false
        backgroundedAtElapsedMillis = null
        val elapsed = (elapsedRealtimeMillis() - backgroundedAt).coerceAtLeast(0L)
        if (elapsed < backgroundTimeoutMillis) return false
        val hadAuthorization = unlockedBoardIds.isNotEmpty()
        unlockedBoardIds.clear()
        return hadAuthorization
    }

    companion object {
        const val DEFAULT_BACKGROUND_TIMEOUT_MILLIS: Long = 60_000L
    }
}
