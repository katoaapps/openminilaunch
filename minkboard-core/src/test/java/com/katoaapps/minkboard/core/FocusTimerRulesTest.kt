package com.katoaapps.minkboard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FocusTimerRulesTest {
    @Test
    fun defaultIsFiveMinutes() {
        assertEquals(300_000L, FocusTimerRules.DEFAULT_DURATION_MILLIS)
    }

    @Test
    fun fieldsProduceDuration() {
        assertEquals(3_723_000L, FocusTimerRules.durationMillis(1, 2, 3))
    }

    @Test
    fun zeroAndTwentyFourHoursAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            FocusTimerRules.durationMillis(0, 0, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            FocusTimerRules.durationMillis(FocusTimerRules.MAX_DURATION_MILLIS + 1L)
        }
    }
}
