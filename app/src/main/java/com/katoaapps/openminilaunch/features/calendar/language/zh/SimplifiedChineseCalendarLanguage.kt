package com.katoaapps.openminilaunch.features.calendar.language.zh

import com.katoaapps.openminilaunch.features.calendar.language.CalendarLanguageModule
import com.katoaapps.openminilaunch.features.calendar.language.withAsciiCalendarDigits
import com.katoaapps.openminilaunch.features.calendar.model.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

internal object SimplifiedChineseCalendarLanguage : CalendarLanguageModule {
    override val languageTags: Set<String> = setOf("zh-Hans", "zh-CN", "zh-SG")

    private val dayAfterTomorrow = Regex("后天")
    private val todayTomorrow = Regex("今天|明天")
    private val relativeDate = Regex("(\\d+)\\s*(天|周|星期|个月|月)后")
    private val weekday = Regex("(下|本|这)?(?:周|星期)([一二三四五六日天])")
    private val namedDate = Regex("(?:(\\d{4})年)?\\s*(\\d{1,2})月\\s*(\\d{1,2})[日号]")
    private val isoDate = Regex("\\b(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})\\b")
    private val numericDate = Regex("\\b(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?\\b")
    private val namedTime = Regex("中午|午夜")
    private val periodTime = Regex("(凌晨|早上|上午|中午|下午|晚上)\\s*(\\d{1,2})(?:[:点时]\\s*(\\d{0,2}))?分?")
    private val clockTime = Regex("(?<!\\d)(\\d{1,2})[:点时]\\s*(\\d{1,2})?分?")
    private val duration = Regex("(?:持续)?\\s*(\\d+)\\s*(分钟|小时)")
    private val temporalCue = Regex(
        "今天|明天|后天|" +
            "(?:下|本|这)?(?:周|星期)[一二三四五六日天]|" +
            "下个?(?:月|星期|周)|" +
            "\\d+\\s*(?:天|周|星期|个月|月)后|" +
            "(?:\\d{4}年)?\\s*\\d{1,2}月\\s*\\d{1,2}[日号]|" +
            "\\b\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}\\b|" +
            "\\b\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?\\b|" +
            "(?:凌晨|早上|上午|中午|下午|晚上)\\s*\\d{1,2}(?:[:点时]\\s*\\d{1,2})?分?|" +
            "中午|午夜|" +
            "(?<!\\d)\\d{1,2}[:点时]\\s*\\d{0,2}分?|" +
            "(?:持续)?\\s*\\d+\\s*(?:分钟|小时)",
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
                val days = if (match.value == "今天") 0L else 1L
                add(match, CalendarDateExpression.Relative(days, RelativeDateUnit.DAYS))
            }
        }
        relativeDate.findAll(text).forEach { match ->
            val amount = match.groupValues[1].toLongOrNull()?.takeIf { it in 0..10_000 }
                ?: return ParseCollection(issue = CalendarParseIssue.INVALID_DATE)
            val unit = when (match.groupValues[2]) {
                "天" -> RelativeDateUnit.DAYS
                "周", "星期" -> RelativeDateUnit.WEEKS
                else -> RelativeDateUnit.MONTHS
            }
            add(match, CalendarDateExpression.Relative(amount, unit))
        }
        namedDate.findAll(text).forEach { match ->
            val month = match.groupValues[2].toIntOrNull()
                ?.let { runCatching { Month.of(it) }.getOrNull() }
                ?: return ParseCollection(issue = CalendarParseIssue.INVALID_DATE)
            val day = match.groupValues[3].toIntOrNull()
                ?: return ParseCollection(issue = CalendarParseIssue.INVALID_DATE)
            add(match, CalendarDateExpression.MonthDay(month, day, match.groupValues[1].toIntOrNull()))
        }
        isoDate.findAll(text).forEach { match ->
            val date = runCatching {
                LocalDate.of(
                    match.groupValues[1].toInt(),
                    match.groupValues[2].toInt(),
                    match.groupValues[3].toInt(),
                )
            }.getOrNull() ?: return ParseCollection(issue = CalendarParseIssue.INVALID_DATE)
            add(match, CalendarDateExpression.Exact(date, explicitYear = true))
        }
        numericDate.findAll(text).forEach { match ->
            val span = SourceSpan.from(match.range)
            if (matches.any { it.span.overlaps(span) }) return@forEach
            val month = match.groupValues[1].toIntOrNull()
                ?.let { runCatching { Month.of(it) }.getOrNull() }
                ?: return ParseCollection(issue = CalendarParseIssue.INVALID_DATE)
            val day = match.groupValues[2].toIntOrNull()
                ?: return ParseCollection(issue = CalendarParseIssue.INVALID_DATE)
            val rawYear = match.groupValues[3].toIntOrNull()
            add(match, CalendarDateExpression.MonthDay(month, day, rawYear?.let { if (it < 100) 2000 + it else it }))
        }
        weekday.findAll(text).forEach { match ->
            val day = weekday(match.groupValues[2]) ?: return@forEach
            val qualifier = if (match.groupValues[1] == "下") WeekdayQualifier.NEXT else WeekdayQualifier.BARE
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

        periodTime.findAll(text).forEach { match ->
            add(match, parsePeriodTime(match.groupValues[1], match.groupValues[2], match.groupValues[3]))
        }
        namedTime.findAll(text).forEach { match ->
            val span = SourceSpan.from(match.range)
            if (matches.none { it.span.overlaps(span) }) {
                add(match, if (match.value == "中午") CalendarTimeExpression(12, 0) else CalendarTimeExpression(0, 0))
            }
        }
        clockTime.findAll(text).forEach { match ->
            val span = SourceSpan.from(match.range)
            if (matches.none { it.span.overlaps(span) }) {
                add(match, parseClockTime(match.groupValues[1], match.groupValues[2]))
            }
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
                val unit = if (match.groupValues[2] == "小时") DurationUnit.HOURS else DurationUnit.MINUTES
                TemporalMatch(CalendarDurationExpression(amount, unit), SourceSpan.from(match.range))
            }
        }.toList()
        return ParseCollection(matches, issue)
    }

    private fun parsePeriodTime(period: String, hourText: String, minuteText: String): CalendarTimeExpression? {
        var hour = hourText.toIntOrNull() ?: return null
        val minute = minuteText.ifBlank { "0" }.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        if (hour !in 1..12) return null
        when (period) {
            "凌晨", "早上", "上午" -> if (hour == 12) hour = 0
            "中午" -> if (hour in 1..11) hour += 12
            "下午", "晚上" -> if (hour != 12) hour += 12
        }
        return CalendarTimeExpression(hour, minute)
    }

    private fun parseClockTime(hourText: String, minuteText: String): CalendarTimeExpression? {
        val hour = hourText.toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        val minute = minuteText.ifBlank { "0" }.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        return CalendarTimeExpression(hour, minute)
    }

    private fun weekday(value: String): DayOfWeek? = when (value) {
        "一" -> DayOfWeek.MONDAY
        "二" -> DayOfWeek.TUESDAY
        "三" -> DayOfWeek.WEDNESDAY
        "四" -> DayOfWeek.THURSDAY
        "五" -> DayOfWeek.FRIDAY
        "六" -> DayOfWeek.SATURDAY
        "日", "天" -> DayOfWeek.SUNDAY
        else -> null
    }

    private data class ParseCollection<T>(
        val matches: List<TemporalMatch<T>> = emptyList(),
        val issue: CalendarParseIssue? = null,
    )
}
