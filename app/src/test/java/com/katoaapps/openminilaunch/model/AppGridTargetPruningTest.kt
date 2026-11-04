package com.katoaapps.openminilaunch.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppGridTargetPruningTest {
    @Test fun removedDirectTargetReturnsSlotToDefault() {
        val result = pruneAppGridTargets(
            assignments = mapOf(AppGridSlot.NOTE to AppGridAssignment.Target(TARGET_ONE)),
            folders = emptyList(),
            shouldRemove = { it == TARGET_ONE },
        )

        assertFalse(AppGridSlot.NOTE in result.assignments)
        assertEquals(emptyList<AppFolderConfig>(), result.folders)
    }

    @Test fun twoItemFolderCollapsesToRemainingTarget() {
        val folder = AppFolderConfig(FOLDER_ID, orderedTargetKeys = listOf(TARGET_ONE, TARGET_TWO))
        val result = pruneAppGridTargets(
            assignments = mapOf(AppGridSlot.NOTE to AppGridAssignment.Folder(FOLDER_ID)),
            folders = listOf(folder),
            shouldRemove = { it == TARGET_ONE },
        )

        assertEquals(AppGridAssignment.Target(TARGET_TWO), result.assignments[AppGridSlot.NOTE])
        assertEquals(emptyList<AppFolderConfig>(), result.folders)
    }

    @Test fun largerFolderKeepsItsIdentityAndRemainingOrder() {
        val folder = AppFolderConfig(
            FOLDER_ID,
            orderedTargetKeys = listOf(TARGET_ONE, TARGET_TWO, TARGET_THREE),
        )
        val result = pruneAppGridTargets(
            assignments = mapOf(AppGridSlot.NOTE to AppGridAssignment.Folder(FOLDER_ID)),
            folders = listOf(folder),
            shouldRemove = { it == TARGET_TWO },
        )

        assertEquals(AppGridAssignment.Folder(FOLDER_ID), result.assignments[AppGridSlot.NOTE])
        assertEquals(listOf(TARGET_ONE, TARGET_THREE), result.folders.single().orderedTargetKeys)
    }

    private companion object {
        const val FOLDER_ID = "folder-1"
        const val TARGET_ONE = "app:0:com.example.one/.MainActivity"
        const val TARGET_TWO = "shortcut:0:com.example.two:compose"
        const val TARGET_THREE = "app:0:com.example.three/.MainActivity"
    }
}
