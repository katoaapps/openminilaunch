package com.katoaapps.openminilaunch.features.minkboard

import com.katoaapps.minkboard.core.MinkBoardRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMinkCardActionControllerTest {
    @Test
    fun duplicateTitleTrimsTheSourceBeforeAppendingTheSuffix() {
        assertEquals("An existing card (duplicate)", openMinkDuplicateTitle("An existing card", "duplicate"))
    }

    @Test
    fun duplicateTitleNeverExceedsTheSharedCardTitleLimit() {
        val title = openMinkDuplicateTitle("x".repeat(1_000), "duplicate")

        assertTrue(title.endsWith(" (duplicate)"))
        assertEquals(MinkBoardRules.MAX_CARD_TITLE_LENGTH, title.length)
    }
}
