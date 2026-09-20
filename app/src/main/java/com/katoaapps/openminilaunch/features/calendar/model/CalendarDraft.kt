package com.katoaapps.openminilaunch.features.calendar.model

internal data class CalendarDraft(
    val title: String,
    val description: String = "",
    val startMillis: Long? = null,
    val endMillis: Long? = null,
    val allDay: Boolean = false,
    val warnings: Set<CalendarParseWarning> = emptySet(),
)

internal enum class CalendarParseWarning {
    PAST_EVENT,
    DST_OVERLAP,
}
