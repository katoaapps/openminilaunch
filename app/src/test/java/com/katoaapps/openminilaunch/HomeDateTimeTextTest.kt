package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.ui.launcher.formatHomeDateTime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.util.Locale

class HomeDateTimeTextTest {
    private val dateTime = LocalDateTime.of(2026, 9, 8, 18, 30)

    @Test fun dateRemainsUnchangedWhenClockIsHidden() {
        assertEquals(
            "TUE, SEP 8",
            formatHomeDateTime(dateTime, "EEE, MMM d", false, false, Locale.US),
        )
    }

    @Test fun twelveHourClockIsAddedToDate() {
        assertEquals(
            "TUE, SEP 8 · 6:30 PM",
            formatHomeDateTime(dateTime, "EEE, MMM d", true, false, Locale.US),
        )
    }

    @Test fun twentyFourHourClockIsAddedToDate() {
        assertEquals(
            "TUE, SEP 8 · 18:30",
            formatHomeDateTime(dateTime, "EEE, MMM d", true, true, Locale.US),
        )
    }
}
