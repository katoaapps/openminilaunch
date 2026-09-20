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

class CalendarPhraseEngineTest {
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

    @Test fun reportedNamedDatePhraseIsConfident() {
        val result = parse("dr appointment sept 15th 9am")

        assertTrue(result is CalendarParseResult.Success)
        assertEquals("dr appointment", result.draft.title)
        assertEquals(dateTime(2026, 9, 15, 9, 0), result.draft.startMillis)
    }

    @Test fun reportedNextWeekdayPhraseIsConfident() {
        val result = parse("dr appointment next wed 8am")

        assertTrue(result is CalendarParseResult.Success)
        assertEquals(dateTime(2026, 8, 19, 8, 0), result.draft.startMillis)
    }

    @Test fun unsupportedDateCannotDegradeIntoTimeOnly() {
        val result = parse("review next month at 5pm")

        assertTrue(result is CalendarParseResult.NeedsReview)
        assertEquals(CalendarParseIssue.UNSUPPORTED_TEMPORAL_PHRASE, (result as CalendarParseResult.NeedsReview).issue)
        assertEquals("review next month", result.draft.title)
        assertEquals(null, result.draft.startMillis)
    }

    @Test fun ordinaryForPhraseStaysInTitle() {
        val result = parse("movie for friends tomorrow at 7pm")

        assertTrue(result is CalendarParseResult.Success)
        assertEquals("movie for friends", result.draft.title)
    }

    @Test fun numericDurationIsRemovedFromTitleAndApplied() {
        val result = parse("planning tomorrow at 9am for 30 minutes")

        assertTrue(result is CalendarParseResult.Success)
        assertEquals("planning", result.draft.title)
        assertEquals(dateTime(2026, 8, 19, 9, 0), result.draft.startMillis)
        assertEquals(dateTime(2026, 8, 19, 9, 30), result.draft.endMillis)
    }

    @Test fun multipleDatesRequireReview() {
        val result = parse("planning tomorrow next Friday at 9am")

        assertTrue(result is CalendarParseResult.NeedsReview)
        assertEquals(
            CalendarParseIssue.CONFLICTING_TEMPORAL_PHRASES,
            (result as CalendarParseResult.NeedsReview).issue,
        )
    }

    @Test fun noTemporalPhraseStaysUntimed() {
        val result = parse("planning session for launch")

        assertTrue(result is CalendarParseResult.NoTemporalPhrase)
        assertEquals("planning session for launch", result.draft.title)
        assertEquals(null, result.draft.startMillis)
    }

    @Test fun unsupportedEnglishRegionDoesNotFallBackToUsNumericDates() {
        val result = CalendarPhraseEngine.parse(
            CalendarParseRequest(
                text = "appointment 15/9 at 9am",
                defaultTitle = "New event",
                languageTag = "en-AU",
                now = now,
            ),
        )

        assertTrue(result is CalendarParseResult.NeedsReview)
        assertEquals(
            CalendarParseIssue.UNSUPPORTED_LANGUAGE,
            (result as CalendarParseResult.NeedsReview).issue,
        )
        assertEquals("appointment 15/9 at 9am", result.draft.title)
    }

    private fun parse(text: String) = CalendarPhraseEngine.parse(
        CalendarParseRequest(
            text = text,
            defaultTitle = "New event",
            languageTag = "en-US",
            now = now,
        ),
    )

    private fun dateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, now.zone).toInstant().toEpochMilli()
}
