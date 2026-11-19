package com.katoaapps.minklauncher.todos.home

import com.katoaapps.minkboard.core.LauncherBoardDesignation
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.StatusId
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherHomeBoardRepairPolicyTest {
    @Test
    fun `broken persisted designation remains repairable after initialization failure`() {
        assertEquals(
            TodoHomeDesignationPlan.Attempt(expectedGeneration = "broken-generation"),
            planTodoHomeDesignation(
                initialization = TodoInitializationState.Failed,
                persistedDesignation = DESIGNATION,
            ),
        )
    }

    @Test
    fun `ready initialization permits first designation when none exists`() {
        assertEquals(
            TodoHomeDesignationPlan.Attempt(expectedGeneration = null),
            planTodoHomeDesignation(
                initialization = TodoInitializationState.Ready,
                persistedDesignation = null,
            ),
        )
    }

    @Test
    fun `failed initialization without persisted state cannot create a designation`() {
        assertEquals(
            TodoHomeDesignationPlan.Blocked,
            planTodoHomeDesignation(
                initialization = TodoInitializationState.Failed,
                persistedDesignation = null,
            ),
        )
    }

    private companion object {
        val DESIGNATION = LauncherBoardDesignation(
            generation = "broken-generation",
            boardId = BoardId("deleted-board"),
            intakeStatusId = StatusId("deleted-intake"),
            designatedAtEpochMillis = 1L,
            legacyCompatibility = null,
        )
    }
}
