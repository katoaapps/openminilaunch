package com.katoaapps.minkboard.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FocusModelsTest {
    @Test
    fun sessionExposesDurationWithoutPersistingTicks() {
        val session = session(startedAt = 1_000L, endsAt = 301_000L)

        assertEquals(300_000L, session.durationMillis)
    }

    @Test
    fun sessionRejectsAnEndBeforeOrAtItsStart() {
        assertThrows(IllegalArgumentException::class.java) {
            session(startedAt = 1_000L, endsAt = 1_000L)
        }
    }

    @Test
    fun timerIdRejectsBlankValues() {
        assertThrows(IllegalArgumentException::class.java) { FocusTimerId(" ") }
    }

    private fun session(startedAt: Long, endsAt: Long) = FocusSession(
        id = FocusTimerId("timer"),
        boardId = BoardId("board"),
        cardId = CardId("card"),
        startedAtEpochMillis = startedAt,
        endsAtEpochMillis = endsAt,
        state = FocusSessionState.Running,
    )
}
