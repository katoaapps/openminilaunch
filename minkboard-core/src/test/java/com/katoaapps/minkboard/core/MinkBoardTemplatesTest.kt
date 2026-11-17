package com.katoaapps.minkboard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MinkBoardTemplatesTest {
    @Test
    fun templatesHaveStableUniqueIdsAndAtMostOneDoneStatus() {
        assertEquals(7, MinkBoardTemplates.all.size)
        assertEquals(
            MinkBoardTemplates.all.size,
            MinkBoardTemplates.all.map { it.id }.toSet().size,
        )
        MinkBoardTemplates.all.forEach { template ->
            assertTrue(template.statuses.isNotEmpty())
            assertTrue(template.statuses.count { it.isDoneStatus } <= 1)
        }
        assertFalse(MinkBoardTemplates.QuarterlyGoals.statuses.any { it.isDoneStatus })
    }

    @Test
    fun unknownTemplateIsRejected() {
        val failure = runCatching {
            MinkBoardTemplates.require(BoardTemplateId("not-a-template"))
        }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }
}
