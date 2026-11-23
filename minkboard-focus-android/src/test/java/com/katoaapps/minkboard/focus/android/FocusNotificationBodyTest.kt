package com.katoaapps.minkboard.focus.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusNotificationBodyTest {
    @Test
    fun cardDetailsReplaceFallbackAndTrimOuterWhitespace() {
        assertEquals(
            "Finish the release notes",
            focusNotificationBody(
                cardDetails = "  Finish the release notes  ",
                fallback = "Focus finished",
            ),
        )
    }

    @Test
    fun emptyCardDetailsUseStateSpecificFallback() {
        assertEquals(
            "Focus finished",
            focusNotificationBody(
                cardDetails = " \n ",
                fallback = "Focus finished",
            ),
        )
    }

    @Test
    fun longCardDetailsAreBoundedForNotificationDelivery() {
        val body = focusNotificationBody(
            cardDetails = "a".repeat(MAX_FOCUS_NOTIFICATION_DETAILS_LENGTH + 100),
            fallback = "Focus finished",
        )

        assertEquals(MAX_FOCUS_NOTIFICATION_DETAILS_LENGTH, body.length)
        assertTrue(body.endsWith("…"))
    }
}
