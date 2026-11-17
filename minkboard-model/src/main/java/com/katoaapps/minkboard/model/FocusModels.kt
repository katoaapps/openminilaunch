package com.katoaapps.minkboard.model

private const val MAX_FOCUS_TIMER_ID_LENGTH = 128

@JvmInline
value class FocusTimerId(val value: String) {
    init {
        require(value.isNotBlank()) { "FocusTimerId cannot be blank" }
        require(value.length <= MAX_FOCUS_TIMER_ID_LENGTH) {
            "FocusTimerId cannot exceed $MAX_FOCUS_TIMER_ID_LENGTH characters"
        }
    }
}

enum class FocusSessionState {
    Running,
    Finished,
}

/**
 * Durable countdown truth. The UI derives remaining time from [endsAtEpochMillis]; it never
 * persists per-second ticks.
 */
data class FocusSession(
    val id: FocusTimerId,
    val boardId: BoardId,
    val cardId: CardId,
    val startedAtEpochMillis: Long,
    val endsAtEpochMillis: Long,
    val state: FocusSessionState,
) {
    init {
        require(startedAtEpochMillis >= 0L) { "Focus start time cannot be negative" }
        require(endsAtEpochMillis > startedAtEpochMillis) {
            "Focus end time must be after its start time"
        }
    }

    val durationMillis: Long
        get() = endsAtEpochMillis - startedAtEpochMillis
}

/**
 * The notification/runtime view of a session. Card text is fetched only when needed and is not
 * duplicated into the timer row.
 */
data class FocusTimerSnapshot(
    val session: FocusSession,
    val cardTitle: String,
    val boardIsLocked: Boolean,
)
