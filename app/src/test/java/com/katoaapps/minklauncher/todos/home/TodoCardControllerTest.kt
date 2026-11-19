package com.katoaapps.minklauncher.todos.home

import com.katoaapps.minkboard.core.MinkBoardRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoCardControllerTest {
    @Test
    fun duplicateTitleTrimsTheSourceBeforeAppendingTheSuffix() {
        assertEquals("An existing card (duplicate)", todoDuplicateTitle("An existing card", "duplicate"))
    }

    @Test
    fun duplicateTitleNeverExceedsTheSharedCardTitleLimit() {
        val title = todoDuplicateTitle("x".repeat(1_000), "duplicate")

        assertTrue(title.endsWith(" (duplicate)"))
        assertEquals(MinkBoardRules.MAX_CARD_TITLE_LENGTH, title.length)
    }
}
