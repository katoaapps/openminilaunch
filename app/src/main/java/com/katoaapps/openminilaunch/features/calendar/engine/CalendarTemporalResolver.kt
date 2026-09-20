package com.katoaapps.openminilaunch.features.calendar.engine

import com.katoaapps.openminilaunch.features.calendar.model.*
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

internal object CalendarTemporalResolver {
    data class Resolution(
        val start: ZonedDateTime?,
        val end: ZonedDateTime?,
        val allDay: Boolean,
        val issue: CalendarParseIssue? = null,
        val warnings: Set<CalendarParseWarning> = emptySet(),
    )

    fun resolve(
        dateExpression: CalendarDateExpression?,
        timeExpression: CalendarTimeExpression?,
        durationExpression: CalendarDurationExpression?,
        now: ZonedDateTime,
        zoneId: ZoneId,
        defaultDuration: Duration,
    ): Resolution {
        val date = when (val result = resolveDate(dateExpression, now.toLocalDate())) {
            is DateResolution.Success -> result.date
            DateResolution.Invalid -> return Resolution(null, null, false, CalendarParseIssue.INVALID_DATE)
        }
        val allDay = date != null && timeExpression == null
        val localStart = when {
            allDay -> date!!.atStartOfDay()
            timeExpression != null -> (date ?: now.toLocalDate()).atTime(timeExpression.hour, timeExpression.minute)
            else -> null
        }

        val zonedResolution = localStart?.let { resolveZonedDateTime(it, zoneId) }
        if (zonedResolution is ZonedResolution.Gap) {
            return Resolution(null, null, false, CalendarParseIssue.DST_GAP)
        }
        val warnings = buildSet {
            if (zonedResolution is ZonedResolution.Overlap) add(CalendarParseWarning.DST_OVERLAP)
        }.toMutableSet()
        var start = when (zonedResolution) {
            is ZonedResolution.Success -> zonedResolution.value
            is ZonedResolution.Overlap -> zonedResolution.value
            else -> null
        }
        if (date == null && start != null && !start.isAfter(now)) start = start.plusDays(1)
        start = start?.truncatedTo(ChronoUnit.MINUTES)

        if (date != null && date.isBefore(now.toLocalDate())) {
            warnings += CalendarParseWarning.PAST_EVENT
        }
        val end = start?.let { startValue ->
            when {
                allDay -> startValue.plusDays(1)
                durationExpression != null -> when (durationExpression.unit) {
                    DurationUnit.MINUTES -> startValue.plusMinutes(durationExpression.amount)
                    DurationUnit.HOURS -> startValue.plusHours(durationExpression.amount)
                }
                else -> startValue.plus(defaultDuration)
            }
        }
        return Resolution(start, end, allDay && start != null, warnings = warnings)
    }

    private fun resolveDate(expression: CalendarDateExpression?, today: LocalDate): DateResolution =
        when (expression) {
            null -> DateResolution.Success(null)
            is CalendarDateExpression.Exact -> DateResolution.Success(expression.date)
            is CalendarDateExpression.MonthDay -> {
                val date = resolveMonthDay(today, expression.month, expression.dayOfMonth, expression.year)
                if (date == null) DateResolution.Invalid else DateResolution.Success(date)
            }
            is CalendarDateExpression.Relative -> runCatching {
                when (expression.unit) {
                    RelativeDateUnit.DAYS -> today.plusDays(expression.amount)
                    RelativeDateUnit.WEEKS -> today.plusWeeks(expression.amount)
                    RelativeDateUnit.MONTHS -> today.plusMonths(expression.amount)
                }
            }.fold({ DateResolution.Success(it) }, { DateResolution.Invalid })
            is CalendarDateExpression.Weekday -> {
                val adjuster = when (expression.qualifier) {
                    WeekdayQualifier.NEXT -> TemporalAdjusters.next(expression.dayOfWeek)
                    WeekdayQualifier.THIS, WeekdayQualifier.BARE ->
                        TemporalAdjusters.nextOrSame(expression.dayOfWeek)
                }
                DateResolution.Success(today.with(adjuster))
            }
            is CalendarDateExpression.FirstWeekdayAfterDay -> {
                val date = firstWeekdayAfterDayOfMonth(
                    today,
                    expression.dayOfMonth,
                    expression.dayOfWeek,
                )
                if (date == null) DateResolution.Invalid else DateResolution.Success(date)
            }
        }

    private fun resolveMonthDay(today: LocalDate, month: Month, dayOfMonth: Int, year: Int?): LocalDate? {
        if (year != null) return runCatching { LocalDate.of(year, month, dayOfMonth) }.getOrNull()
        val thisYear = runCatching { LocalDate.of(today.year, month, dayOfMonth) }.getOrNull()
        if (thisYear != null && !thisYear.isBefore(today)) return thisYear
        return runCatching { LocalDate.of(today.year + 1, month, dayOfMonth) }.getOrNull()
    }

    private fun firstWeekdayAfterDayOfMonth(
        today: LocalDate,
        dayOfMonth: Int,
        weekday: java.time.DayOfWeek,
    ): LocalDate? {
        if (dayOfMonth !in 1..31) return null
        var month = today.withDayOfMonth(1)
        repeat(24) {
            if (dayOfMonth <= month.lengthOfMonth()) {
                val candidate = month.withDayOfMonth(dayOfMonth)
                    .plusDays(1)
                    .with(TemporalAdjusters.nextOrSame(weekday))
                if (!candidate.isBefore(today)) return candidate
            }
            month = month.plusMonths(1)
        }
        return null
    }

    private fun resolveZonedDateTime(localDateTime: LocalDateTime, zoneId: ZoneId): ZonedResolution {
        val offsets = zoneId.rules.getValidOffsets(localDateTime)
        return when (offsets.size) {
            0 -> ZonedResolution.Gap
            1 -> ZonedResolution.Success(ZonedDateTime.ofLocal(localDateTime, zoneId, offsets.single()))
            else -> ZonedResolution.Overlap(ZonedDateTime.ofLocal(localDateTime, zoneId, offsets.first()))
        }
    }

    private sealed interface DateResolution {
        data class Success(val date: LocalDate?) : DateResolution
        data object Invalid : DateResolution
    }

    private sealed interface ZonedResolution {
        data class Success(val value: ZonedDateTime) : ZonedResolution
        data class Overlap(val value: ZonedDateTime) : ZonedResolution
        data object Gap : ZonedResolution
    }
}
