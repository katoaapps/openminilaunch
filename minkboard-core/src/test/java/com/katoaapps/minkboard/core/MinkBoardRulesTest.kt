package com.katoaapps.minkboard.core

import org.junit.Assert.assertEquals
import org.junit.Test

class MinkBoardRulesTest {
    @Test
    fun trimsRequiredNames() {
        assertEquals("Kitchen", MinkBoardRules.boardName("  Kitchen  "))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankCardTitle() {
        MinkBoardRules.cardTitle("   ")
    }
}
