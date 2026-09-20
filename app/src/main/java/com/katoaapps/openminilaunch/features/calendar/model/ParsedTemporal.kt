package com.katoaapps.openminilaunch.features.calendar.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

internal data class SourceSpan(
    val startInclusive: Int,
    val endExclusive: Int,
) {
    init {
        require(startInclusive >= 0)
        require(endExclusive >= startInclusive)
    }

    fun overlaps(other: SourceSpan): Boolean =
        startInclusive < other.endExclusive && other.startInclusive < endExclusive

    fun contains(other: SourceSpan): Boolean =
        startInclusive <= other.startInclusive && endExclusive >= other.endExclusive

    companion object {
        fun from(range: IntRange): SourceSpan = SourceSpan(range.first, range.last + 1)
    }
}

internal data class TemporalMatch<T>(
    val value: T,
    val span: SourceSpan,
)

internal sealed interface CalendarDateExpression {
    data class Exact(val date: LocalDate, val explicitYear: Boolean) : CalendarDateExpression

    data class MonthDay(
        val month: Month,
        val dayOfMonth: Int,
        val year: Int?,
    ) : CalendarDateExpression

    data class Relative(
        val amount: Long,
        val unit: RelativeDateUnit,
    ) : CalendarDateExpression

    data class Weekday(
        val dayOfWeek: DayOfWeek,
        val qualifier: WeekdayQualifier,
    ) : CalendarDateExpression

    data class FirstWeekdayAfterDay(
        val dayOfMonth: Int,
        val dayOfWeek: DayOfWeek,
    ) : CalendarDateExpression
}

internal enum class RelativeDateUnit { DAYS, WEEKS, MONTHS }

internal enum class WeekdayQualifier { THIS, NEXT, BARE }

internal data class CalendarTimeExpression(
    val hour: Int,
    val minute: Int,
)

internal data class CalendarDurationExpression(
    val amount: Long,
    val unit: DurationUnit,
)

internal enum class DurationUnit { MINUTES, HOURS }

internal data class CalendarLanguageParse(
    val dates: List<TemporalMatch<CalendarDateExpression>> = emptyList(),
    val times: List<TemporalMatch<CalendarTimeExpression>> = emptyList(),
    val durations: List<TemporalMatch<CalendarDurationExpression>> = emptyList(),
    val temporalCues: List<SourceSpan> = emptyList(),
    val issue: CalendarParseIssue? = null,
) {
    val recognizedSpans: List<SourceSpan>
        get() = dates.map { it.span } + times.map { it.span } + durations.map { it.span }
}
