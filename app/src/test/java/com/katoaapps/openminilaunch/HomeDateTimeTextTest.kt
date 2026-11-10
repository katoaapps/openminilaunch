package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.ui.launcher.home.HomeDateTimeParts
import com.katoaapps.openminilaunch.ui.launcher.home.formatHomeDateTimeParts
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.util.Locale

class HomeDateTimeTextTest {
    private val dateTime = LocalDateTime.of(2026, 9, 8, 18, 30)

    @Test fun dateRemainsUnchangedWhenClockIsHidden() {
        assertEquals(
            HomeDateTimeParts(date = "TUE, SEP 8", time = ""),
            formatHomeDateTimeParts(dateTime, "EEE, MMM d", true, false, false, Locale.US),
        )
    }

    @Test fun twelveHourClockIsAddedToDate() {
        assertEquals(
            HomeDateTimeParts(date = "TUE, SEP 8", time = "6:30 PM"),
            formatHomeDateTimeParts(dateTime, "EEE, MMM d", true, true, false, Locale.US),
        )
    }

    @Test fun twentyFourHourClockIsAddedToDate() {
        assertEquals(
            HomeDateTimeParts(date = "TUE, SEP 8", time = "18:30"),
            formatHomeDateTimeParts(dateTime, "EEE, MMM d", true, true, true, Locale.US),
        )
    }

    @Test fun clockCanBeShownWithoutDate() {
        assertEquals(
            HomeDateTimeParts(date = "", time = "6:30 PM"),
            formatHomeDateTimeParts(dateTime, "EEE, MMM d", false, true, false, Locale.US),
        )
    }

    @Test fun headerTextCanBeHidden() {
        assertEquals(
            HomeDateTimeParts(date = "", time = ""),
            formatHomeDateTimeParts(dateTime, "EEE, MMM d", false, false, false, Locale.US),
        )
    }
}
