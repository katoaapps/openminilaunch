package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.features.calendar.engine.CalendarPhraseEngine
import com.katoaapps.openminilaunch.features.calendar.model.CalendarParseIssue
import com.katoaapps.openminilaunch.features.calendar.model.CalendarParseRequest
import com.katoaapps.openminilaunch.features.calendar.model.CalendarParseResult
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarLanguageCorpusTest {
    private val now = ZonedDateTime.of(
        2026,
        8,
        18,
        10,
        0,
        0,
        0,
        ZoneId.of("America/Los_Angeles"),
    )

    @Test fun britishEnglishUsesDayMonthNumericDates() {
        assertSuccess("appointment 15/9 at 9am", "en-GB", "appointment", 2026, 9, 15, 9, 0)
    }

    @Test fun spanishNamedDateAndTime() {
        assertSuccess("cita 15 de septiembre a las 9", "es", "cita", 2026, 9, 15, 9, 0)
    }

    @Test fun spanishRegionUsesGenericSpanishGrammar() {
        assertSuccess("cita mañana a las 9", "es-MX", "cita", 2026, 8, 19, 9, 0)
    }

    @Test fun simplifiedChineseNamedDateAndPeriodTime() {
        assertSuccess("看医生 9月15日 上午9点", "zh-Hans", "看医生", 2026, 9, 15, 9, 0)
    }

    @Test fun traditionalChineseDoesNotFallBackToSimplifiedGrammar() {
        val result = parse("看醫生 9月15日 上午9點", "zh-Hant")

        assertTrue(result is CalendarParseResult.NeedsReview)
        assertEquals(
            CalendarParseIssue.UNSUPPORTED_LANGUAGE,
            (result as CalendarParseResult.NeedsReview).issue,
        )
    }

    @Test fun arabicNamedDateAndTimeWithArabicIndicDigits() {
        assertSuccess("موعد الطبيب ١٥ سبتمبر الساعة ٩ صباحا", "ar", "موعد الطبيب", 2026, 9, 15, 9, 0)
    }

    @Test fun unsupportedSpanishTemporalWordingRequiresReview() {
        val result = parse("reunión el próximo mes a las 5", "es")

        assertTrue(result is CalendarParseResult.NeedsReview)
        assertEquals(
            CalendarParseIssue.UNSUPPORTED_TEMPORAL_PHRASE,
            (result as CalendarParseResult.NeedsReview).issue,
        )
    }

    private fun assertSuccess(
        text: String,
        languageTag: String,
        title: String,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ) {
        val result = parse(text, languageTag)
        assertTrue(result is CalendarParseResult.Success)
        result as CalendarParseResult.Success
        assertEquals(title, result.draft.title)
        assertEquals(dateTime(year, month, day, hour, minute), result.draft.startMillis)
    }

    private fun parse(text: String, languageTag: String) = CalendarPhraseEngine.parse(
        CalendarParseRequest(
            text = text,
            defaultTitle = "New event",
            languageTag = languageTag,
            now = now,
        ),
    )

    private fun dateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, now.zone).toInstant().toEpochMilli()
}
