package com.katoaapps.openminilaunch.features.minkboard

import com.katoaapps.minkboard.core.LauncherBoardDesignation
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.StatusId
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherHomeBoardRepairPolicyTest {
    @Test
    fun `broken persisted designation remains repairable after initialization failure`() {
        assertEquals(
            LauncherHomeDesignationPlan.Attempt(expectedGeneration = "broken-generation"),
            planHomeBoardDesignation(
                initialization = LauncherBoardInitializationState.Failed,
                persistedDesignation = DESIGNATION,
            ),
        )
    }

    @Test
    fun `ready initialization permits first designation when none exists`() {
        assertEquals(
            LauncherHomeDesignationPlan.Attempt(expectedGeneration = null),
            planHomeBoardDesignation(
                initialization = LauncherBoardInitializationState.Ready,
                persistedDesignation = null,
            ),
        )
    }

    @Test
    fun `failed initialization without persisted state cannot create a designation`() {
        assertEquals(
            LauncherHomeDesignationPlan.Blocked,
            planHomeBoardDesignation(
                initialization = LauncherBoardInitializationState.Failed,
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
