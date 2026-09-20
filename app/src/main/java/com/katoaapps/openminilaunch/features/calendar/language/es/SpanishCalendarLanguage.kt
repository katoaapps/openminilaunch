package com.katoaapps.openminilaunch.features.calendar.language.es

import com.katoaapps.openminilaunch.features.calendar.language.CalendarLanguageModule
import com.katoaapps.openminilaunch.features.calendar.language.withAsciiCalendarDigits
import com.katoaapps.openminilaunch.features.calendar.model.*
import java.time.DayOfWeek
import java.time.Month
import java.util.Locale

internal object SpanishCalendarLanguage : CalendarLanguageModule {
    override val languageTags: Set<String> = setOf("es")
    override val regionalFallbackLanguage: String = "es"

    private const val WEEKDAYS =
        "lunes|martes|mi[eé]rcoles|jueves|viernes|s[aá]bado|domingo"
    private const val MONTHS =
        "enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|setiembre|octubre|noviembre|diciembre"

    private val dayAfterTomorrow = Regex("\\bpasado\\s+mañana\\b", RegexOption.IGNORE_CASE)
    private val todayTomorrow = Regex("\\b(hoy|(?<!la\\s)mañana)\\b", RegexOption.IGNORE_CASE)
    private val relativeDate = Regex(
        "\\b(?:dentro\\s+de|en)\\s+(\\d+)\\s+(d[ií]as?|semanas?|mes(?:es)?)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val weekday = Regex(
        "\\b(?:el\\s+)?(?:(este|esta|pr[oó]xim[oa]|siguiente)\\s+)?($WEEKDAYS)" +
            "(?:\\s+(pr[oó]xim[oa]|siguiente))?\\b",
        RegexOption.IGNORE_CASE,
    )
    private val namedDate = Regex(
        "\\b(\\d{1,2})(?:º|ª)?\\s+de\\s+($MONTHS)(?:\\s+de\\s+(\\d{4}))?\\b",
        RegexOption.IGNORE_CASE,
    )
    private val numericDate = Regex("\\b(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?\\b")
    private val namedTime = Regex("\\b(mediod[ií]a|medianoche)\\b", RegexOption.IGNORE_CASE)
    private val spokenTime = Regex(
        "\\b(?:a\\s+las?\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*" +
            "(?:(a\\.?\\s*m\\.?|p\\.?\\s*m\\.?)|de\\s+la\\s+(mañana|tarde|noche))\\b",
        RegexOption.IGNORE_CASE,
    )
    private val prefixedTime = Regex(
        "\\ba\\s+las?\\s+(\\d{1,2})(?::(\\d{2}))?\\b",
        RegexOption.IGNORE_CASE,
    )
    private val twentyFourHourTime = Regex("\\b(\\d{1,2}):(\\d{2})\\b")
    private val duration = Regex(
        "\\b(?:durante|por)\\s+(\\d+)\\s+(minutos?|mins?|horas?|hrs?)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val temporalCue = Regex(
        "\\b(?:hoy|mañana|pasado\\s+mañana)\\b|" +
            "\\b(?:este|esta|pr[oó]xim[oa]|siguiente)\\s+[\\p{L}]+\\b|" +
            "\\b(?:dentro\\s+de|en)\\s+\\d+\\s+[\\p{L}]+\\b|" +
            "\\b\\d{1,2}(?:º|ª)?\\s+de\\s+(?:$MONTHS)(?:\\s+de\\s+\\d{4})?\\b|" +
            "\\b(?:$WEEKDAYS)\\b|" +
            "\\b\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?\\b|" +
            "\\b(?:a\\s+las?\\s+)?\\d{1,2}(?::\\d{2})?\\s*(?:a\\.?\\s*m\\.?|p\\.?\\s*m\\.?|de\\s+la\\s+(?:mañana|tarde|noche))\\b|" +
            "\\ba\\s+las?\\s+\\d{1,2}(?::\\d{2})?\\b|" +
            "\\b(?:mediod[ií]a|medianoche)\\b|" +
            "\\b\\d{1,2}:\\d{2}\\b|" +
            "\\b(?:durante|por)\\s+\\d+\\s+(?:minutos?|mins?|horas?|hrs?)\\b",
        RegexOption.IGNORE_CASE,
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
                val days = if (match.value.equals("hoy", ignoreCase = true)) 0L else 1L
                add(match, CalendarDateExpression.Relative(days, RelativeDateUnit.DAYS))
            }
        }
        relativeDate.findAll(text).forEach { match ->
            val amount = match.groupValues[1].toLongOrNull()?.takeIf { it in 0..10_000 }
                ?: return ParseCollection(issue = CalendarParseIssue.INVALID_DATE)
            val unit = when {
                match.groupValues[2].lowercase(Locale.ROOT).startsWith("d") -> RelativeDateUnit.DAYS
                match.groupValues[2].lowercase(Locale.ROOT).startsWith("sem") -> RelativeDateUnit.WEEKS
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
            val day = weekday(match.groupValues[2]) ?: return@forEach
            val next = match.groupValues[1].contains("pr", ignoreCase = true) ||
                match.groupValues[1].equals("siguiente", ignoreCase = true) ||
                match.groupValues[3].isNotBlank()
            val qualifier = if (next) WeekdayQualifier.NEXT else WeekdayQualifier.BARE
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
            add(match, if (match.value.startsWith("medio", ignoreCase = true)) CalendarTimeExpression(12, 0) else CalendarTimeExpression(0, 0))
        }
        spokenTime.findAll(text).forEach { match ->
            add(match, parseTime(match.groupValues[1], match.groupValues[2], match.groupValues[3], match.groupValues[4]))
        }
        prefixedTime.findAll(text).forEach { match ->
            add(match, parseTime(match.groupValues[1], match.groupValues[2], "", ""))
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
                val unit = if (match.groupValues[2].startsWith("h", ignoreCase = true)) DurationUnit.HOURS else DurationUnit.MINUTES
                TemporalMatch(CalendarDurationExpression(amount, unit), SourceSpan.from(match.range))
            }
        }.toList()
        return ParseCollection(matches, issue)
    }

    private fun parseTime(hourText: String, minuteText: String, meridiem: String, dayPeriod: String): CalendarTimeExpression? {
        var hour = hourText.toIntOrNull() ?: return null
        val minute = minuteText.ifBlank { "0" }.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        val marker = meridiem.lowercase(Locale.ROOT).replace(".", "").replace(" ", "")
        when {
            marker == "am" || dayPeriod.equals("mañana", ignoreCase = true) -> {
                if (hour !in 1..12) return null
                if (hour == 12) hour = 0
            }
            marker == "pm" || dayPeriod.equals("tarde", ignoreCase = true) || dayPeriod.equals("noche", ignoreCase = true) -> {
                if (hour !in 1..12) return null
                if (hour != 12) hour += 12
            }
            else -> {
                if (hour !in 0..23) return null
                if (hour in 1..7) hour += 12
            }
        }
        return CalendarTimeExpression(hour, minute)
    }

    private fun parse24HourTime(hour: String, minute: String): CalendarTimeExpression? {
        val parsedHour = hour.toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        val parsedMinute = minute.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        return CalendarTimeExpression(parsedHour, parsedMinute)
    }

    private fun weekday(value: String): DayOfWeek? = when (value.lowercase(Locale.ROOT)) {
        "lunes" -> DayOfWeek.MONDAY
        "martes" -> DayOfWeek.TUESDAY
        "miércoles", "miercoles" -> DayOfWeek.WEDNESDAY
        "jueves" -> DayOfWeek.THURSDAY
        "viernes" -> DayOfWeek.FRIDAY
        "sábado", "sabado" -> DayOfWeek.SATURDAY
        "domingo" -> DayOfWeek.SUNDAY
        else -> null
    }

    private fun month(value: String): Month? = when (value.lowercase(Locale.ROOT)) {
        "enero" -> Month.JANUARY
        "febrero" -> Month.FEBRUARY
        "marzo" -> Month.MARCH
        "abril" -> Month.APRIL
        "mayo" -> Month.MAY
        "junio" -> Month.JUNE
        "julio" -> Month.JULY
        "agosto" -> Month.AUGUST
        "septiembre", "setiembre" -> Month.SEPTEMBER
        "octubre" -> Month.OCTOBER
        "noviembre" -> Month.NOVEMBER
        "diciembre" -> Month.DECEMBER
        else -> null
    }

    private data class ParseCollection<T>(
        val matches: List<TemporalMatch<T>> = emptyList(),
        val issue: CalendarParseIssue? = null,
    )
}
