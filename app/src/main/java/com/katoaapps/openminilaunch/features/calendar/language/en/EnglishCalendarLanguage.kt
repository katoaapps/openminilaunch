package com.katoaapps.openminilaunch.features.calendar.language.en

import com.katoaapps.openminilaunch.features.calendar.language.CalendarLanguageModule
import com.katoaapps.openminilaunch.features.calendar.model.*
import java.time.DayOfWeek
import java.time.Month
import java.util.Locale

internal enum class EnglishNumericDateOrder { MONTH_DAY, DAY_MONTH }

internal open class EnglishCalendarLanguage(
    override val languageTags: Set<String>,
    private val numericDateOrder: EnglishNumericDateOrder,
) : CalendarLanguageModule {

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

    private val dayAfterTomorrowPattern = Regex(
        "\\b(?:the\\s+)?day\\s+after\\s+tomorrow\\b",
        RegexOption.IGNORE_CASE,
    )
    private val todayTomorrowPattern = Regex("\\b(today|tomorrow)\\b", RegexOption.IGNORE_CASE)
    private val relativeDatePattern = Regex(
        "\\bin\\s+(\\d+)\\s+(days?|weeks?|months?)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val qualifiedWeekdayPattern = Regex(
        "\\b(this|next)\\s+($weekdayNames)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val bareWeekdayPattern = Regex(
        "\\b(?:on\\s+)?($weekdayNames)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val namedMonthDatePattern = Regex(
        "\\b($monthNames)\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:,?\\s+(\\d{4}))?\\b",
        RegexOption.IGNORE_CASE,
    )
    private val numericDatePattern = Regex("\\b(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?\\b")
    private val ordinalWeekdayAfterPattern = Regex(
        "\\b(?:the\\s+)?(?:1st|first)\\s+($weekdayNames)\\s+after\\s+(?:the\\s+)?" +
            "(\\d{1,2})(?:st|nd|rd|th)?\\b",
        RegexOption.IGNORE_CASE,
    )
    private val namedTimePattern = Regex(
        "(?:\\bat\\s+|@\\s*)(noon|midnight)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val prefixedTimePattern = Regex(
        "(?:\\bat\\s+|@\\s*)(\\d{1,2})(?::(\\d{2}))?\\s*(a\\.?m\\.?|p\\.?m\\.?)?\\b",
        RegexOption.IGNORE_CASE,
    )
    private val meridianTimePattern = Regex(
        "\\b(\\d{1,2})(?::(\\d{2}))?\\s*(a\\.?m\\.?|p\\.?m\\.?)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val twentyFourHourPattern = Regex("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b")
    private val durationPattern = Regex(
        "\\bfor\\s+(\\d+)\\s+(minutes?|mins?|hours?|hrs?)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val temporalCuePattern = Regex(
        "\\b(?:today|tomorrow|(?:the\\s+)?day\\s+after\\s+tomorrow)\\b|" +
            "\\b(?:this|next)\\s+[a-z]+\\b|" +
            "\\bin\\s+\\d+\\s+[a-z]+\\b|" +
            "\\b(?:the\\s+)?(?:first|second|third|fourth|1st|2nd|3rd|4th)\\s+[a-z]+day\\s+" +
            "after\\s+(?:the\\s+)?\\d{1,2}(?:st|nd|rd|th)?\\b|" +
            "\\b(?:$monthNames)\\s+\\d{1,2}(?:st|nd|rd|th)?(?:,?\\s+\\d{4})?\\b|" +
            "\\b(?:$weekdayNames)\\b|" +
            "\\b\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?\\b|" +
            "(?:\\bat\\s+|@\\s*)(?:\\d{1,2}(?::\\d{2})?\\s*(?:a\\.?m\\.?|p\\.?m\\.?)?|noon|midnight)\\b|" +
            "\\b\\d{1,2}(?::\\d{2})?\\s*(?:a\\.?m\\.?|p\\.?m\\.?)\\b|" +
            "\\b\\d{1,2}:\\d{2}\\b|" +
            "\\bfor\\s+\\d+\\s+(?:minutes?|mins?|hours?|hrs?)\\b",
        RegexOption.IGNORE_CASE,
    )

    override fun parse(text: String): CalendarLanguageParse {
        val dates = parseDates(text)
        val times = parseTimes(text)
        val durations = parseDurations(text)
        return CalendarLanguageParse(
            dates = dates.matches,
            times = times.matches,
            durations = durations.matches,
            temporalCues = temporalCuePattern.findAll(text).map { SourceSpan.from(it.range) }.toList(),
            issue = dates.issue ?: times.issue ?: durations.issue,
        )
    }

    private fun parseDates(text: String): ParseCollection<CalendarDateExpression> {
        val matches = mutableListOf<TemporalMatch<CalendarDateExpression>>()

        fun add(match: MatchResult, value: CalendarDateExpression) {
            val candidate = TemporalMatch(value, SourceSpan.from(match.range))
            if (matches.none { it.span.overlaps(candidate.span) }) matches += candidate
        }

        dayAfterTomorrowPattern.findAll(text).forEach {
            add(it, CalendarDateExpression.Relative(2, RelativeDateUnit.DAYS))
        }
        namedMonthDatePattern.findAll(text).forEach { match ->
            val month = parseMonth(match.groupValues[1]) ?: return@forEach
            val day = match.groupValues[2].toIntOrNull()
                ?: return ParseCollection(issue = CalendarParseIssue.INVALID_DATE)
            add(match, CalendarDateExpression.MonthDay(month, day, match.groupValues[3].toIntOrNull()))
        }
        ordinalWeekdayAfterPattern.findAll(text).forEach { match ->
            val weekday = parseWeekday(match.groupValues[1]) ?: return@forEach
            val day = match.groupValues[2].toIntOrNull()
                ?: return ParseCollection(issue = CalendarParseIssue.INVALID_DATE)
            add(match, CalendarDateExpression.FirstWeekdayAfterDay(day, weekday))
        }
        relativeDatePattern.findAll(text).forEach { match ->
            val amount = match.groupValues[1].toLongOrNull()
                ?.takeIf { it in 0..10_000 }
                ?: return ParseCollection(issue = CalendarParseIssue.INVALID_DATE)
            val unit = when (match.groupValues[2].lowercase(Locale.ROOT).removeSuffix("s")) {
                "day" -> RelativeDateUnit.DAYS
                "week" -> RelativeDateUnit.WEEKS
                "month" -> RelativeDateUnit.MONTHS
                else -> return@forEach
            }
            add(match, CalendarDateExpression.Relative(amount, unit))
        }
        qualifiedWeekdayPattern.findAll(text).forEach { match ->
            val weekday = parseWeekday(match.groupValues[2]) ?: return@forEach
            val qualifier = if (match.groupValues[1].equals("next", ignoreCase = true)) {
                WeekdayQualifier.NEXT
            } else {
                WeekdayQualifier.THIS
            }
            add(match, CalendarDateExpression.Weekday(weekday, qualifier))
        }
        todayTomorrowPattern.findAll(text).forEach { match ->
            val span = SourceSpan.from(match.range)
            if (matches.none { it.span.overlaps(span) }) {
                val amount = if (match.groupValues[1].equals("tomorrow", ignoreCase = true)) 1L else 0L
                add(match, CalendarDateExpression.Relative(amount, RelativeDateUnit.DAYS))
            }
        }
        numericDatePattern.findAll(text).forEach { match ->
            val first = match.groupValues[1].toIntOrNull()
                ?: return ParseCollection(issue = CalendarParseIssue.INVALID_DATE)
            val second = match.groupValues[2].toIntOrNull()
                ?: return ParseCollection(issue = CalendarParseIssue.INVALID_DATE)
            val (monthNumber, day) = if (numericDateOrder == EnglishNumericDateOrder.MONTH_DAY) {
                first to second
            } else {
                second to first
            }
            val month = monthNumber
                ?.let { runCatching { Month.of(it) }.getOrNull() }
                ?: return ParseCollection(issue = CalendarParseIssue.INVALID_DATE)
            val rawYear = match.groupValues[3].toIntOrNull()
            val year = rawYear?.let { if (it < 100) 2000 + it else it }
            add(match, CalendarDateExpression.MonthDay(month, day, year))
        }
        bareWeekdayPattern.findAll(text).forEach { match ->
            val span = SourceSpan.from(match.range)
            if (matches.none { it.span.overlaps(span) }) {
                parseWeekday(match.groupValues[1])?.let { weekday ->
                    add(match, CalendarDateExpression.Weekday(weekday, WeekdayQualifier.BARE))
                }
            }
        }
        return ParseCollection(matches.sortedBy { it.span.startInclusive })
    }

    private fun parseTimes(text: String): ParseCollection<CalendarTimeExpression> {
        val matches = mutableListOf<TemporalMatch<CalendarTimeExpression>>()
        var issue: CalendarParseIssue? = null

        fun add(match: MatchResult, value: CalendarTimeExpression?) {
            if (value == null) {
                issue = CalendarParseIssue.INVALID_TIME
                return
            }
            val candidate = TemporalMatch(value, SourceSpan.from(match.range))
            if (matches.none { it.span.overlaps(candidate.span) }) matches += candidate
        }

        namedTimePattern.findAll(text).forEach { match ->
            add(
                match,
                if (match.groupValues[1].equals("noon", ignoreCase = true)) {
                    CalendarTimeExpression(12, 0)
                } else {
                    CalendarTimeExpression(0, 0)
                },
            )
        }
        prefixedTimePattern.findAll(text).forEach { match ->
            add(match, parseNumericTime(match.groupValues[1], match.groupValues[2], match.groupValues[3]))
        }
        meridianTimePattern.findAll(text).forEach { match ->
            add(match, parseNumericTime(match.groupValues[1], match.groupValues[2], match.groupValues[3]))
        }
        twentyFourHourPattern.findAll(text).forEach { match ->
            add(
                match,
                CalendarTimeExpression(match.groupValues[1].toInt(), match.groupValues[2].toInt()),
            )
        }
        return ParseCollection(matches.sortedBy { it.span.startInclusive }, issue)
    }

    private fun parseDurations(text: String): ParseCollection<CalendarDurationExpression> {
        var issue: CalendarParseIssue? = null
        val matches = durationPattern.findAll(text).mapNotNull { match ->
            val amount = match.groupValues[1].toLongOrNull()?.takeIf { it in 1..10_000 }
            if (amount == null) {
                issue = CalendarParseIssue.INVALID_TIME
                null
            } else {
                val unit = if (match.groupValues[2].lowercase(Locale.ROOT).startsWith("h")) {
                    DurationUnit.HOURS
                } else {
                    DurationUnit.MINUTES
                }
                TemporalMatch(CalendarDurationExpression(amount, unit), SourceSpan.from(match.range))
            }
        }.toList()
        return ParseCollection(matches, issue)
    }

    private fun parseNumericTime(
        hourText: String,
        minuteText: String,
        meridianText: String,
    ): CalendarTimeExpression? {
        var hour = hourText.toIntOrNull() ?: return null
        val minute = minuteText.ifBlank { "0" }.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        val meridian = meridianText.lowercase(Locale.ROOT).replace(".", "")
        if (meridian.isNotBlank()) {
            if (hour !in 1..12) return null
            hour = when {
                meridian == "am" && hour == 12 -> 0
                meridian == "pm" && hour != 12 -> hour + 12
                else -> hour
            }
        } else {
            if (hour !in 0..23) return null
            if (hour in 1..7) hour += 12
        }
        return CalendarTimeExpression(hour, minute)
    }

    private fun parseWeekday(value: String): DayOfWeek? = when (value.lowercase(Locale.ROOT)) {
        "monday", "mon" -> DayOfWeek.MONDAY
        "tuesday", "tues", "tue" -> DayOfWeek.TUESDAY
        "wednesday", "weds", "wed" -> DayOfWeek.WEDNESDAY
        "thursday", "thurs", "thur", "thu" -> DayOfWeek.THURSDAY
        "friday", "fri" -> DayOfWeek.FRIDAY
        "saturday", "sat" -> DayOfWeek.SATURDAY
        "sunday", "sun" -> DayOfWeek.SUNDAY
        else -> null
    }

    private fun parseMonth(value: String): Month? = when (value.lowercase(Locale.ROOT)) {
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

    private data class ParseCollection<T>(
        val matches: List<TemporalMatch<T>> = emptyList(),
        val issue: CalendarParseIssue? = null,
    )
}

internal object AmericanEnglishCalendarLanguage : EnglishCalendarLanguage(
    languageTags = setOf("en", "en-US"),
    numericDateOrder = EnglishNumericDateOrder.MONTH_DAY,
)

internal object BritishEnglishCalendarLanguage : EnglishCalendarLanguage(
    languageTags = setOf("en-GB"),
    numericDateOrder = EnglishNumericDateOrder.DAY_MONTH,
)
