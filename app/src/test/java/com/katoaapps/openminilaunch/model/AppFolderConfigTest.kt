package com.katoaapps.openminilaunch.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFolderConfigTest {
    @Test
    fun `folder normalization trims values and removes duplicate targets`() {
        val normalized = AppFolderConfig(
            id = "  folder-1  ",
            customName = "  Travel  ",
            orderedTargetKeys = listOf(" maps ", "", "maps", " transit "),
        ).normalized()

        assertEquals("folder-1", normalized.id)
        assertEquals("Travel", normalized.customName)
        assertEquals(listOf("maps", "transit"), normalized.orderedTargetKeys)
    }

    @Test
    fun `folder collection keeps only valid multi target folders`() {
        val folders = listOf(
            AppFolderConfig(" valid ", "  ", listOf("maps", "messages")),
            AppFolderConfig("single", "Single", listOf("maps")),
            AppFolderConfig("", "Missing id", listOf("maps", "messages")),
        ).normalizedAppFolders()

        assertEquals(1, folders.size)
        assertEquals("valid", folders.single().id)
        assertEquals(null, folders.single().customName)
        assertTrue(folders.single().orderedTargetKeys.size >= 2)
    }

    @Test
    fun `assignment normalization rejects invalid slots and missing folders`() {
        val normalized = mapOf<AppGridSlot, AppGridAssignment>(
            AppGridSlot.MESSAGE to AppGridAssignment.Target(" messages "),
            AppGridSlot.NOTE to AppGridAssignment.Folder(" saved-folder "),
            AppGridSlot.EVENT to AppGridAssignment.Folder("missing-folder"),
            AppGridSlot.CALL to AppGridAssignment.Target(""),
            AppGridSlot.EXTRA to AppGridAssignment.DefaultAction,
            AppGridSlot.DRAWER to AppGridAssignment.Target("drawer-cannot-be-reassigned"),
        ).normalizedAppGridAssignments(setOf("saved-folder"))

        assertEquals(
            mapOf(
                AppGridSlot.MESSAGE to AppGridAssignment.Target("messages"),
                AppGridSlot.NOTE to AppGridAssignment.Folder("saved-folder"),
            ),
            normalized,
        )
    }

    @Test
    fun `legacy target falls back to first folder item`() {
        val folder = AppFolderConfig(
            id = "folder-1",
            orderedTargetKeys = listOf("maps", "messages"),
        )

        assertEquals(
            "maps",
            AppGridAssignment.Folder(folder.id).legacyTargetKey { id ->
                folder.takeIf { it.id == id }
            },
        )
        assertEquals(
            "notes",
            AppGridAssignment.Target("notes").legacyTargetKey { null },
        )
        assertEquals(
            null,
            AppGridAssignment.Folder("missing").legacyTargetKey { null },
        )
    }
}
