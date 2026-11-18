package com.katoaapps.openminilaunch.features.minkboard.focus

import org.junit.Assert.assertEquals
import org.junit.Test

class FinishedFocusAlertWindowTest {
    @Test
    fun alertWindowStartsAtTimerCompletion() {
        assertEquals(
            FINISHED_ALERT_DURATION_MILLIS,
            finishedAlertRemainingMillis(
                endsAtEpochMillis = 10_000L,
                nowEpochMillis = 10_000L,
            ),
        )
    }

    @Test
    fun processRestartResumesOnlyRemainingAlertWindow() {
        assertEquals(
            30_000L,
            finishedAlertRemainingMillis(
                endsAtEpochMillis = 10_000L,
                nowEpochMillis = 100_000L,
            ),
        )
    }

    @Test
    fun expiredAlertWindowDoesNotRestart() {
        assertEquals(
            0L,
            finishedAlertRemainingMillis(
                endsAtEpochMillis = 10_000L,
                nowEpochMillis = 130_000L,
            ),
        )
        assertEquals(
            0L,
            finishedAlertRemainingMillis(
                endsAtEpochMillis = 10_000L,
                nowEpochMillis = 300_000L,
            ),
        )
    }
}
