package com.katoaapps.openminilaunch

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarPhraseParserTest {
    private val now = ZonedDateTime.of(2026, 8, 18, 10, 0, 0, 0, ZoneId.of("America/Los_Angeles"))

    @Test fun parsesTitleDescriptionAndNaturalTime() {
        val draft = parseCalendarPhrase("movie for friends at 5pm", now)

        assertEquals("movie", draft.title)
        assertEquals("for friends", draft.description)
        assertStart(draft, 2026, 8, 18, 17, 0)
        assertEnd(draft, 2026, 8, 18, 18, 0)
        assertFalse(draft.allDay)
    }

    @Test fun parsesNextWeekday() {
        val draft = parseCalendarPhrase("movie for friends next Friday at 5pm", now)

        assertEquals("movie", draft.title)
        assertEquals("for friends", draft.description)
        assertStart(draft, 2026, 8, 21, 17, 0)
    }

    @Test fun reportedNextFridayPhraseRemainsOnFriday() {
        val draft = parseCalendarPhrase("next friday sceduled a teapart", now)

        assertEquals("sceduled a teapart", draft.title)
        assertStart(draft, 2026, 8, 21, 0, 0)
        assertEnd(draft, 2026, 8, 22, 0, 0)
        assertTrue(draft.allDay)
    }

    @Test fun parsesRelativeWeeksAsAnAllDayEvent() {
        val draft = parseCalendarPhrase("renew subscription in 4 weeks", now)

        assertEquals("renew subscription", draft.title)
        assertStart(draft, 2026, 9, 15, 0, 0)
        assertEnd(draft, 2026, 9, 16, 0, 0)
        assertTrue(draft.allDay)
    }

    @Test fun parsesFirstWeekdayAfterDayOfMonth() {
        val draft = parseCalendarPhrase("team dinner 1st Thursday after the 15th at 6:30pm", now)

        assertEquals("team dinner", draft.title)
        assertStart(draft, 2026, 8, 20, 18, 30)
    }

    @Test fun movesOrdinalRuleToNextMonthWhenThisMonthsOccurrencePassed() {
        val laterNow = now.withDayOfMonth(21)
        val draft = parseCalendarPhrase("billing review first Thursday after the 15th at 9am", laterNow)

        assertStart(draft, 2026, 9, 17, 9, 0)
    }

    @Test fun understandsThisAndBareWeekdays() {
        assertStart(parseCalendarPhrase("lunch this Friday at noon", now), 2026, 8, 21, 12, 0)
        assertStart(parseCalendarPhrase("lunch on Wednesday at noon", now), 2026, 8, 19, 12, 0)
    }

    @Test fun conversationalBareEarlyHourMeansPm() {
        assertStart(parseCalendarPhrase("movie tomorrow at 5", now), 2026, 8, 19, 17, 0)
    }

    @Test fun rollsAnUnqualifiedPastTimeToTomorrow() {
        assertStart(parseCalendarPhrase("walk at 8am", now), 2026, 8, 19, 8, 0)
    }

    @Test fun leavesTimeUnsetWhenNoDateOrTimeWasProvided() {
        val draft = parseCalendarPhrase("team planning for launch", now)

        assertEquals("team planning", draft.title)
        assertEquals("for launch", draft.description)
        assertNull(draft.startMillis)
        assertNull(draft.endMillis)
        assertFalse(draft.allDay)
    }

    @Test fun unsupportedDateLanguageCannotBecomeAWrongTimeOnlyEvent() {
        val draft = parseCalendarPhrase("review next month at 5pm", now)

        assertEquals("review next month", draft.title)
        assertNull(draft.startMillis)
        assertNull(draft.endMillis)
    }

    @Test fun unsupportedOrdinalCannotFallBackToTheMentionedWeekday() {
        val draft = parseCalendarPhrase("review second Thursday after the 15th at 5pm", now)

        assertEquals("review second Thursday after the 15th", draft.title)
        assertNull(draft.startMillis)
        assertNull(draft.endMillis)
    }

    @Test fun invalidFirstOrdinalDateCannotFallBackToTheMentionedWeekday() {
        val draft = parseCalendarPhrase("review first Thursday after the 32nd at 5pm", now)

        assertEquals("review first Thursday after the 32nd", draft.title)
        assertNull(draft.startMillis)
        assertNull(draft.endMillis)
    }

    private fun assertStart(draft: CalendarDraft, year: Int, month: Int, day: Int, hour: Int, minute: Int) {
        assertEquals(dateTime(year, month, day, hour, minute), draft.startMillis)
    }

    private fun assertEnd(draft: CalendarDraft, year: Int, month: Int, day: Int, hour: Int, minute: Int) {
        assertEquals(dateTime(year, month, day, hour, minute), draft.endMillis)
    }

    private fun dateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, now.zone).toInstant().toEpochMilli()

}
