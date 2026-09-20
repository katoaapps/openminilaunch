package com.katoaapps.openminilaunch.features.calendar.model

internal sealed interface CalendarParseResult {
    val draft: CalendarDraft

    data class Success(override val draft: CalendarDraft) : CalendarParseResult

    data class NoTemporalPhrase(override val draft: CalendarDraft) : CalendarParseResult

    data class NeedsReview(
        override val draft: CalendarDraft,
        val issue: CalendarParseIssue,
    ) : CalendarParseResult

    data class Invalid(
        override val draft: CalendarDraft,
        val issue: CalendarParseIssue,
    ) : CalendarParseResult
}

internal enum class CalendarParseIssue {
    EMPTY_INPUT,
    UNSUPPORTED_LANGUAGE,
    UNSUPPORTED_TEMPORAL_PHRASE,
    AMBIGUOUS_DATE,
    INVALID_DATE,
    INVALID_TIME,
    CONFLICTING_TEMPORAL_PHRASES,
    DST_GAP,
}
