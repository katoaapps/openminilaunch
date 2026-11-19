package com.katoaapps.minkboard.focus.android

import org.junit.Assert.assertEquals
import org.junit.Test

class FinishedMinkBoardFocusAlertWindowTest {
    @Test
    fun alertWindowStartsAtTimerCompletion() {
        assertEquals(
            FINISHED_FOCUS_ALERT_DURATION_MILLIS,
            finishedFocusAlertRemainingMillis(
                endsAtEpochMillis = 10_000L,
                nowEpochMillis = 10_000L,
            ),
        )
    }

    @Test
    fun processRestartResumesOnlyRemainingAlertWindow() {
        assertEquals(
            30_000L,
            finishedFocusAlertRemainingMillis(
                endsAtEpochMillis = 10_000L,
                nowEpochMillis = 100_000L,
            ),
        )
    }

    @Test
    fun expiredAlertWindowDoesNotRestart() {
        assertEquals(
            0L,
            finishedFocusAlertRemainingMillis(
                endsAtEpochMillis = 10_000L,
                nowEpochMillis = 130_000L,
            ),
        )
        assertEquals(
            0L,
            finishedFocusAlertRemainingMillis(
                endsAtEpochMillis = 10_000L,
                nowEpochMillis = 300_000L,
            ),
        )
    }
}
