package com.katoaapps.openminilaunch.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppGridTargetPlacementTest {
    @Test fun pinnedTargetReplacesDefaultSlot() {
        val result = AppGridAssignment.DefaultAction.placePinnedTarget(
            TARGET_TWO,
            folder = null,
            newFolderId = { FOLDER_ID },
        )

        assertEquals(AppGridAssignment.Target(TARGET_TWO), result.assignment)
        assertNull(result.updatedFolder)
    }

    @Test fun pinnedTargetCreatesFolderFromOccupiedDirectSlot() {
        val result = AppGridAssignment.Target(TARGET_ONE).placePinnedTarget(
            TARGET_TWO,
            folder = null,
            newFolderId = { FOLDER_ID },
        )

        assertEquals(AppGridAssignment.Folder(FOLDER_ID), result.assignment)
        assertEquals(listOf(TARGET_ONE, TARGET_TWO), result.updatedFolder?.orderedTargetKeys)
    }

    @Test fun pinnedTargetAlreadyOccupyingDirectSlotIsUnchanged() {
        val result = AppGridAssignment.Target(TARGET_ONE).placePinnedTarget(
            TARGET_ONE,
            folder = null,
            newFolderId = { FOLDER_ID },
        )

        assertEquals(AppGridAssignment.Target(TARGET_ONE), result.assignment)
        assertNull(result.updatedFolder)
    }

    @Test fun pinnedTargetIsAppendedToExistingFolder() {
        val folder = AppFolderConfig(
            id = FOLDER_ID,
            orderedTargetKeys = listOf(TARGET_ONE, TARGET_TWO),
        )

        val result = AppGridAssignment.Folder(FOLDER_ID).placePinnedTarget(
            TARGET_THREE,
            folder,
            newFolderId = { "unused" },
        )

        assertEquals(AppGridAssignment.Folder(FOLDER_ID), result.assignment)
        assertEquals(listOf(TARGET_ONE, TARGET_TWO, TARGET_THREE), result.updatedFolder?.orderedTargetKeys)
    }

    @Test fun pinnedTargetAlreadyInFolderIsNotDuplicated() {
        val folder = AppFolderConfig(
            id = FOLDER_ID,
            orderedTargetKeys = listOf(TARGET_ONE, TARGET_TWO),
        )

        val result = AppGridAssignment.Folder(FOLDER_ID).placePinnedTarget(
            TARGET_TWO,
            folder,
            newFolderId = { "unused" },
        )

        assertEquals(listOf(TARGET_ONE, TARGET_TWO), result.updatedFolder?.orderedTargetKeys)
    }

    @Test fun missingFolderFallsBackToDirectTarget() {
        val result = AppGridAssignment.Folder(FOLDER_ID).placePinnedTarget(
            TARGET_TWO,
            folder = null,
            newFolderId = { "unused" },
        )

        assertEquals(AppGridAssignment.Target(TARGET_TWO), result.assignment)
        assertNull(result.updatedFolder)
    }

    private companion object {
        const val FOLDER_ID = "folder-1"
        const val TARGET_ONE = "app:0:com.example.one/.MainActivity"
        const val TARGET_TWO = "shortcut:0:com.example.two:compose"
        const val TARGET_THREE = "app:0:com.example.three/.MainActivity"
    }
}
