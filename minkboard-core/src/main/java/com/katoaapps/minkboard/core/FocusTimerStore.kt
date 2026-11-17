package com.katoaapps.minkboard.core

import com.katoaapps.minkboard.model.FocusSession
import com.katoaapps.minkboard.model.FocusTimerId
import com.katoaapps.minkboard.model.FocusTimerSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Persists the single active/finished focus session. Alarm and notification side effects belong to
 * the Android runtime coordinator rather than the board repository.
 */
interface FocusTimerStore {
    fun observeFocusSession(): Flow<FocusSession?>

    suspend fun currentFocusTimer(): FocusTimerSnapshot?

    /** Replaces the prior session atomically after validating that its board/card still exist. */
    suspend fun replaceFocusTimer(session: FocusSession): FocusTimerSnapshot

    /** Returns null for a stale timer identity. */
    suspend fun finishFocusTimer(timerId: FocusTimerId): FocusTimerSnapshot?

    /** Returns false for a stale timer identity. */
    suspend fun clearFocusTimer(timerId: FocusTimerId): Boolean
}
