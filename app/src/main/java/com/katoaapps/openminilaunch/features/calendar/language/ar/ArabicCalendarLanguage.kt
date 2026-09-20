package com.katoaapps.openminilaunch.features.calendar.language.ar

import com.katoaapps.openminilaunch.features.calendar.language.CalendarLanguageModule
import com.katoaapps.openminilaunch.features.calendar.language.withAsciiCalendarDigits
import com.katoaapps.openminilaunch.features.calendar.model.*
import java.time.DayOfWeek
import java.time.Month

internal object ArabicCalendarLanguage : CalendarLanguageModule {
    override val languageTags: Set<String> = setOf("ar")
    override val regionalFallbackLanguage: String = "ar"

    private const val WEEKDAYS =
        "الأحد|الاحد|الإثنين|الاثنين|الثلاثاء|الأربعاء|الاربعاء|الخميس|الجمعة|السبت"
    private const val MONTHS =
        "يناير|فبراير|مارس|أبريل|ابريل|مايو|يونيو|يوليو|أغسطس|اغسطس|سبتمبر|أكتوبر|اكتوبر|نوفمبر|ديسمبر"

    private val dayAfterTomorrow = Regex("بعد\\s+غد(?:ًا|ا)?")
    private val todayTomorrow = Regex("اليوم|غد(?:ًا|ا)?")
    private val relativeDate = Regex(
        "بعد\\s+(\\d+)\\s+(يوم|أيام|ايام|أسبوع|اسبوع|أسابيع|اسابيع|شهر|أشهر|اشهر)",
    )
    private val weekday = Regex("(?:يوم\\s+)?($WEEKDAYS)(?:\\s+(القادم|المقبل))?")
    private val namedDate = Regex("(\\d{1,2})\\s+(?:من\\s+)?($MONTHS)(?:\\s+(\\d{4}))?")
    private val numericDate = Regex("(?<!\\d)(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?(?!\\d)")
    private val namedTime = Regex("منتصف\\s+الليل|الظهر")
    private val markedTime = Regex(
        "(?:الساعة\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(صباح(?:ًا|ا)?|مساء(?:ً|ًا|ا)?|ص|م)",
    )
    private val prefixedTime = Regex("الساعة\\s+(\\d{1,2})(?::(\\d{2}))?")
    private val twentyFourHourTime = Regex("(?<!\\d)(\\d{1,2}):(\\d{2})(?!\\d)")
    private val duration = Regex(
        "لمدة\\s+(\\d+)\\s+(دقائق?|دقيقة|ساعات?|ساعة)",
    )
    private val temporalCue = Regex(
        "اليوم|غد(?:ًا|ا)?|بعد\\s+غد(?:ًا|ا)?|" +
            "(?:يوم\\s+)?(?:$WEEKDAYS)(?:\\s+(?:القادم|المقبل))?|" +
            "(?:الشهر|الأسبوع|الاسبوع)\\s+(?:القادم|المقبل)|" +
            "بعد\\s+\\d+\\s+[\\p{L}]+|" +
            "\\d{1,2}\\s+(?:من\\s+)?(?:$MONTHS)(?:\\s+\\d{4})?|" +
            "(?<!\\d)\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?(?!\\d)|" +
            "(?:الساعة\\s+)?\\d{1,2}(?::\\d{2})?\\s*(?:صباح(?:ًا|ا)?|مساء(?:ً|ًا|ا)?|ص|م)|" +
            "الساعة\\s+\\d{1,2}(?::\\d{2})?|" +
            "منتصف\\s+الليل|الظهر|" +
            "(?<!\\d)\\d{1,2}:\\d{2}(?!\\d)|" +
            "لمدة\\s+\\d+\\s+(?:دقائق?|دقيقة|ساعات?|ساعة)",
    )

    override fun parse(text: String): CalendarLanguageParse {
        val normalized = text.withAsciiCalendarDigits()
        val dates = parseDates(normalized)
        val times = parseTimes(normalized)
        val durations = parseDurations(normalized)
        return CalendarLanguageParse(
            dates = dates.matches,
            times = times.matches,
            durations = durations.matches,
            temporalCues = temporalCue.findAll(normalized).map { SourceSpan.from(it.range) }.toList(),
            issue = dates.issue ?: times.issue ?: durations.issue,
        )
    }

    private fun parseDates(text: String): ParseCollection<CalendarDateExpression> {
        val matches = mutableListOf<TemporalMatch<CalendarDateExpression>>()

        fun add(match: MatchResult, value: CalendarDateExpression) {
            val candidate = TemporalMatch(value, SourceSpan.from(match.range))
            if (matches.none { it.span.overlaps(candidate.span) }) matches += candidate
        }

        dayAfterTomorrow.findAll(text).forEach {
            add(it, CalendarDateExpression.Relative(2, RelativeDateUnit.DAYS))
        }
        todayTomorrow.findAll(text).forEach { match ->
            val span = SourceSpan.from(match.range)
            if (matches.none { it.span.overlaps(span) }) {
                val days = if (match.value == "اليوم") 0L else 1L
                add(match, CalendarDateExpression.Relative(days, RelativeDateUnit.DAYS))
            }
        }
        relativeDate.findAll(text).forEach { match ->
            val amount = match.groupValues[1].toLongOrNull()?.takeIf { it in 0..10_000 }
                ?: return ParseCollection(issue = CalendarParseIssue.INVALID_DATE)
            val unit = when (match.groupValues[2]) {
                "يوم", "أيام", "ايام" -> RelativeDateUnit.DAYS
                "أسبوع", "اسبوع", "أسابيع", "اسابيع" -> RelativeDateUnit.WEEKS
                else -> RelativeDateUnit.MONTHS
            }
            add(match, CalendarDateExpression.Relative(amount, unit))
        }
        namedDate.findAll(text).forEach { match ->
            val day = match.groupValues[1].toIntOrNull()
                ?: return ParseCollection(issue = CalendarParseIssue.INVALID_DATE)
            val month = month(match.groupValues[2])
                ?: return ParseCollection(issue = CalendarParseIssue.INVALID_DATE)
            add(match, CalendarDateExpression.MonthDay(month, day, match.groupValues[3].toIntOrNull()))
        }
        numericDate.findAll(text).forEach { match ->
            val day = match.groupValues[1].toIntOrNull()
                ?: return ParseCollection(issue = CalendarParseIssue.INVALID_DATE)
            val month = match.groupValues[2].toIntOrNull()
                ?.let { runCatching { Month.of(it) }.getOrNull() }
                ?: return ParseCollection(issue = CalendarParseIssue.INVALID_DATE)
            val rawYear = match.groupValues[3].toIntOrNull()
            add(match, CalendarDateExpression.MonthDay(month, day, rawYear?.let { if (it < 100) 2000 + it else it }))
        }
        weekday.findAll(text).forEach { match ->
            val day = weekday(match.groupValues[1]) ?: return@forEach
            val qualifier = if (match.groupValues[2].isNotBlank()) WeekdayQualifier.NEXT else WeekdayQualifier.BARE
            add(match, CalendarDateExpression.Weekday(day, qualifier))
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

        namedTime.findAll(text).forEach { match ->
            add(match, if (match.value == "الظهر") CalendarTimeExpression(12, 0) else CalendarTimeExpression(0, 0))
        }
        markedTime.findAll(text).forEach { match ->
            add(match, parseMarkedTime(match.groupValues[1], match.groupValues[2], match.groupValues[3]))
        }
        prefixedTime.findAll(text).forEach { match ->
            add(match, parse24HourTime(match.groupValues[1], match.groupValues[2]))
        }
        twentyFourHourTime.findAll(text).forEach { match ->
            add(match, parse24HourTime(match.groupValues[1], match.groupValues[2]))
        }
        return ParseCollection(matches.sortedBy { it.span.startInclusive }, issue)
    }

    private fun parseDurations(text: String): ParseCollection<CalendarDurationExpression> {
        var issue: CalendarParseIssue? = null
        val matches = duration.findAll(text).mapNotNull { match ->
            val amount = match.groupValues[1].toLongOrNull()?.takeIf { it in 1..10_000 }
            if (amount == null) {
                issue = CalendarParseIssue.INVALID_TIME
                null
            } else {
                val unit = if (match.groupValues[2].startsWith("ساع")) DurationUnit.HOURS else DurationUnit.MINUTES
                TemporalMatch(CalendarDurationExpression(amount, unit), SourceSpan.from(match.range))
            }
        }.toList()
        return ParseCollection(matches, issue)
    }

    private fun parseMarkedTime(hourText: String, minuteText: String, marker: String): CalendarTimeExpression? {
        var hour = hourText.toIntOrNull() ?: return null
        val minute = minuteText.ifBlank { "0" }.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        if (hour !in 1..12) return null
        val afternoon = marker == "م" || marker.startsWith("مساء")
        if (afternoon && hour != 12) hour += 12
        if (!afternoon && hour == 12) hour = 0
        return CalendarTimeExpression(hour, minute)
    }

    private fun parse24HourTime(hourText: String, minuteText: String): CalendarTimeExpression? {
        val hour = hourText.toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        val minute = minuteText.ifBlank { "0" }.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        return CalendarTimeExpression(hour, minute)
    }

    private fun weekday(value: String): DayOfWeek? = when (value) {
        "الإثنين", "الاثنين" -> DayOfWeek.MONDAY
        "الثلاثاء" -> DayOfWeek.TUESDAY
        "الأربعاء", "الاربعاء" -> DayOfWeek.WEDNESDAY
        "الخميس" -> DayOfWeek.THURSDAY
        "الجمعة" -> DayOfWeek.FRIDAY
        "السبت" -> DayOfWeek.SATURDAY
        "الأحد", "الاحد" -> DayOfWeek.SUNDAY
        else -> null
    }

    private fun month(value: String): Month? = when (value) {
        "يناير" -> Month.JANUARY
        "فبراير" -> Month.FEBRUARY
        "مارس" -> Month.MARCH
        "أبريل", "ابريل" -> Month.APRIL
        "مايو" -> Month.MAY
        "يونيو" -> Month.JUNE
        "يوليو" -> Month.JULY
        "أغسطس", "اغسطس" -> Month.AUGUST
        "سبتمبر" -> Month.SEPTEMBER
        "أكتوبر", "اكتوبر" -> Month.OCTOBER
        "نوفمبر" -> Month.NOVEMBER
        "ديسمبر" -> Month.DECEMBER
        else -> null
    }

    private data class ParseCollection<T>(
        val matches: List<TemporalMatch<T>> = emptyList(),
        val issue: CalendarParseIssue? = null,
    )
}
