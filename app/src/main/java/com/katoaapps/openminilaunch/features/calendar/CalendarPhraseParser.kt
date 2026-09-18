package com.katoaapps.openminilaunch.features.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

internal data class CalendarDraft(
    val title: String,
    val description: String,
    val startMillis: Long?,
    val endMillis: Long?,
    val allDay: Boolean = false,
)

private data class ParsedDatePhrase(
    val range: IntRange,
    val date: LocalDate,
)

private val weekdayNames = listOf(
    "monday", "mon",
    "tuesday", "tues", "tue",
    "wednesday", "weds", "wed",
    "thursday", "thurs", "thur", "thu",
    "friday", "fri",
    "saturday", "sat",
    "sunday", "sun",
).joinToString("|")
private val monthNames = listOf(
    "january", "jan",
    "february", "feb",
    "march", "mar",
    "april", "apr",
    "may",
    "june", "jun",
    "july", "jul",
    "august", "aug",
    "september", "sept", "sep",
    "october", "oct",
    "november", "nov",
    "december", "dec",
).joinToString("|")
private val calendarTimePattern = Regex(
    pattern = "(?i)(?:\\bat\\s+|@\\s*)(\\d{1,2})(?::(\\d{2}))?\\s*(a\\.?m\\.?|p\\.?m\\.?)?\\b",
)
private val standaloneMeridianTimePattern = Regex(
    pattern = "(?i)\\b(\\d{1,2})(?::(\\d{2}))?\\s*(a\\.?m\\.?|p\\.?m\\.?)\\b",
)
private val namedTimePattern = Regex("(?i)(?:\\bat\\s+|@\\s*)(noon|midnight)\\b")
private val todayTomorrowPattern = Regex("(?i)\\b(today|tomorrow)\\b")
private val relativeDatePattern = Regex("(?i)\\bin\\s+(\\d+)\\s+(days?|weeks?|months?)\\b")
private val qualifiedWeekdayPattern = Regex("(?i)\\b(this|next)\\s+($weekdayNames)\\b")
private val bareWeekdayPattern = Regex("(?i)\\b(?:on\\s+)?($weekdayNames)\\b")
private val namedMonthDatePattern = Regex(
    pattern = "(?i)\\b($monthNames)\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:,?\\s+(\\d{4}))?\\b",
)
private val ordinalWeekdayAfterPattern = Regex(
    pattern = "(?i)\\b(?:the\\s+)?(?:1st|first)\\s+($weekdayNames)\\s+after\\s+(?:the\\s+)?(\\d{1,2})(?:st|nd|rd|th)?\\b",
)
private val calendarForPattern = Regex("(?i)\\s+for\\s+")
private val unsupportedDateCuePattern = Regex(
    pattern = "(?i)\\b(?:next|this)\\s+[a-z]+\\b|" +
        "\\bin\\s+\\d+\\s+[a-z]+\\b|" +
        "\\b(?:the\\s+)?(?:first|second|third|fourth|1st|2nd|3rd|4th)\\s+[a-z]+day\\s+" +
        "after\\s+(?:the\\s+)?\\d{1,2}(?:st|nd|rd|th)?\\b|" +
        "\\bon\\s+(?:the\\s+)?\\d{1,2}(?:st|nd|rd|th)\\b|" +
        "\\b(?:$monthNames)\\s+\\d{1,2}(?:st|nd|rd|th)?(?:,?\\s+\\d{4})?\\b|" +
        "\\b\\d{1,2}/\\d{1,2}(?:/\\d{2,4})?\\b",
)

internal fun parseCalendarPhrase(
    input: String,
    defaultTitle: String,
    now: ZonedDateTime = ZonedDateTime.now(),
): CalendarDraft {
    val clean = input.trim().replace(Regex("\\s+"), " ")
    val timeMatch = findTimePhrase(clean)
    val parsedTime = timeMatch?.let(::parseCalendarTime)
    val datePhrase = parseDatePhrase(clean, now.toLocalDate())
    val unsupportedDateCue = unsupportedDateCuePattern.find(clean)
    val hasUnsupportedDateLanguage = unsupportedDateCue != null && unsupportedDateCue.range != datePhrase?.range
    val acceptedDatePhrase = datePhrase.takeUnless { hasUnsupportedDateLanguage }

    val rangesToRemove = listOfNotNull(timeMatch?.range, acceptedDatePhrase?.range)
        .distinct()
        .sortedByDescending(IntRange::first)
    var eventWords = clean
    rangesToRemove.forEach { range -> eventWords = eventWords.removeRange(range) }
    eventWords = eventWords.replace(Regex("\\s+"), " ").trim(' ', ',', '-', '.')

    val forMatch = calendarForPattern.find(eventWords)
    val title = forMatch?.let { eventWords.substring(0, it.range.first).trim() }
        .orEmpty()
        .ifBlank { eventWords.ifBlank { defaultTitle } }
    val description = forMatch?.let {
        eventWords.substring(it.range.last + 1).trim().takeIf(String::isNotEmpty)?.let { detail -> "for $detail" }
    }.orEmpty()

    val hasInvalidTime = timeMatch != null && parsedTime == null
    val explicitDate = acceptedDatePhrase?.date
    val allDay = explicitDate != null && timeMatch == null
    val start = when {
        hasUnsupportedDateLanguage || hasInvalidTime -> null
        allDay -> explicitDate!!.atStartOfDay(now.zone)
        parsedTime != null -> {
            val (hour, minute) = parsedTime
            var candidate = (explicitDate ?: now.toLocalDate()).atTime(hour, minute).atZone(now.zone)
            if (explicitDate == null && !candidate.isAfter(now)) candidate = candidate.plusDays(1)
            candidate.truncatedTo(ChronoUnit.MINUTES)
        }
        else -> null
    }

    return CalendarDraft(
        title = title,
        description = description,
        startMillis = start?.toInstant()?.toEpochMilli(),
        endMillis = start?.let { if (allDay) it.plusDays(1) else it.plusHours(1) }?.toInstant()?.toEpochMilli(),
        allDay = allDay && start != null,
    )
}

private fun findTimePhrase(input: String): MatchResult? =
    namedTimePattern.find(input)
        ?: calendarTimePattern.find(input)
        ?: standaloneMeridianTimePattern.find(input)

private fun parseCalendarTime(match: MatchResult): Pair<Int, Int>? {
    val namedTime = match.groupValues.getOrNull(1)?.lowercase(Locale.US)
    if (namedTime == "noon") return 12 to 0
    if (namedTime == "midnight") return 0 to 0

    var hour = match.groupValues[1].toIntOrNull() ?: return null
    val minute = match.groupValues[2].ifBlank { "0" }.toIntOrNull() ?: return null
    if (minute !in 0..59) return null

    val meridian = match.groupValues[3].lowercase(Locale.US).replace(".", "")
    if (meridian.isNotBlank()) {
        if (hour !in 1..12) return null
        hour = when {
            meridian == "am" && hour == 12 -> 0
            meridian == "pm" && hour != 12 -> hour + 12
            else -> hour
        }
    } else {
        if (hour !in 0..23) return null
        // With no meridian, conversational 1–7 means afternoon/evening.
        if (hour in 1..7) hour += 12
    }
    return hour to minute
}

private fun parseDatePhrase(input: String, today: LocalDate): ParsedDatePhrase? {
    namedMonthDatePattern.find(input)?.let { match ->
        val month = parseMonth(match.groupValues[1]) ?: return null
        val dayOfMonth = match.groupValues[2].toIntOrNull() ?: return null
        val explicitYear = match.groupValues[3].toIntOrNull()
        val date = resolveNamedDate(today, month, dayOfMonth, explicitYear) ?: return null
        return ParsedDatePhrase(match.range, date)
    }
    ordinalWeekdayAfterPattern.find(input)?.let { match ->
        val weekday = parseWeekday(match.groupValues[1]) ?: return null
        val dayOfMonth = match.groupValues[2].toIntOrNull() ?: return null
        val date = firstWeekdayAfterDayOfMonth(today, dayOfMonth, weekday) ?: return null
        return ParsedDatePhrase(match.range, date)
    }
    relativeDatePattern.find(input)?.let { match ->
        val amount = match.groupValues[1].toLongOrNull()?.takeIf { it in 0..10_000 } ?: return null
        val date = when (match.groupValues[2].lowercase(Locale.US).removeSuffix("s")) {
            "day" -> today.plusDays(amount)
            "week" -> today.plusWeeks(amount)
            "month" -> today.plusMonths(amount)
            else -> return null
        }
        return ParsedDatePhrase(match.range, date)
    }
    qualifiedWeekdayPattern.find(input)?.let { match ->
        val weekday = parseWeekday(match.groupValues[2]) ?: return@let
        val adjuster = if (match.groupValues[1].equals("next", ignoreCase = true)) {
            TemporalAdjusters.next(weekday)
        } else {
            TemporalAdjusters.nextOrSame(weekday)
        }
        return ParsedDatePhrase(match.range, today.with(adjuster))
    }
    todayTomorrowPattern.find(input)?.let { match ->
        val date = if (match.groupValues[1].equals("tomorrow", ignoreCase = true)) today.plusDays(1) else today
        return ParsedDatePhrase(match.range, date)
    }
    bareWeekdayPattern.find(input)?.let { match ->
        val weekday = parseWeekday(match.groupValues[1]) ?: return@let
        return ParsedDatePhrase(match.range, today.with(TemporalAdjusters.nextOrSame(weekday)))
    }
    return null
}

private fun parseWeekday(value: String): DayOfWeek? = when (value.lowercase(Locale.US)) {
    "monday", "mon" -> DayOfWeek.MONDAY
    "tuesday", "tues", "tue" -> DayOfWeek.TUESDAY
    "wednesday", "weds", "wed" -> DayOfWeek.WEDNESDAY
    "thursday", "thurs", "thur", "thu" -> DayOfWeek.THURSDAY
    "friday", "fri" -> DayOfWeek.FRIDAY
    "saturday", "sat" -> DayOfWeek.SATURDAY
    "sunday", "sun" -> DayOfWeek.SUNDAY
    else -> null
}

private fun parseMonth(value: String): Month? = when (value.lowercase(Locale.US)) {
    "january", "jan" -> Month.JANUARY
    "february", "feb" -> Month.FEBRUARY
    "march", "mar" -> Month.MARCH
    "april", "apr" -> Month.APRIL
    "may" -> Month.MAY
    "june", "jun" -> Month.JUNE
    "july", "jul" -> Month.JULY
    "august", "aug" -> Month.AUGUST
    "september", "sept", "sep" -> Month.SEPTEMBER
    "october", "oct" -> Month.OCTOBER
    "november", "nov" -> Month.NOVEMBER
    "december", "dec" -> Month.DECEMBER
    else -> null
}

private fun resolveNamedDate(
    today: LocalDate,
    month: Month,
    dayOfMonth: Int,
    explicitYear: Int?,
): LocalDate? {
    if (explicitYear != null) {
        return runCatching { LocalDate.of(explicitYear, month, dayOfMonth) }.getOrNull()
    }

    val thisYear = runCatching { LocalDate.of(today.year, month, dayOfMonth) }.getOrNull()
    if (thisYear != null && !thisYear.isBefore(today)) return thisYear
    return runCatching { LocalDate.of(today.year + 1, month, dayOfMonth) }.getOrNull()
}

private fun firstWeekdayAfterDayOfMonth(
    today: LocalDate,
    dayOfMonth: Int,
    weekday: DayOfWeek,
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
