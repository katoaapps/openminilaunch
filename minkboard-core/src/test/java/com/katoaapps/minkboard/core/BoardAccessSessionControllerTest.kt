package com.katoaapps.minkboard.core

import com.katoaapps.minkboard.model.BoardId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardAccessSessionControllerTest {
    private var now = 1_000L
    private val controller = BoardAccessSessionController(
        backgroundTimeoutMillis = 60_000L,
        elapsedRealtimeMillis = { now },
    )
    private val first = BoardId("first")
    private val second = BoardId("second")

    @Test
    fun grantsArePerBoard() {
        controller.grant(first)

        assertTrue(controller.isGranted(first))
        assertFalse(controller.isGranted(second))
    }

    @Test
    fun shortBackgroundDoesNotExpireAccess() {
        controller.grant(first)
        controller.onBackgrounded()
        now += 59_999L

        assertFalse(controller.onForegrounded())
        assertTrue(controller.isGranted(first))
    }

    @Test
    fun timeoutExpiresEveryGrant() {
        controller.grant(first)
        controller.grant(second)
        controller.onBackgrounded()
        now += 60_000L

        assertTrue(controller.onForegrounded())
        assertFalse(controller.isGranted(first))
        assertFalse(controller.isGranted(second))
    }

    @Test
    fun repeatedBackgroundCallbacksKeepTheOriginalStart() {
        controller.grant(first)
        controller.onBackgrounded()
        now += 40_000L
        controller.onBackgrounded()
        now += 20_000L

        assertTrue(controller.onForegrounded())
        assertFalse(controller.isGranted(first))
    }
}
