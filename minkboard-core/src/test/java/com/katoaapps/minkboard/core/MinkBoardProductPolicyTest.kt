package com.katoaapps.minkboard.core

import com.katoaapps.minkboard.model.Board
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.MinkBoardWorkspace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MinkBoardProductPolicyTest {
    @Test
    fun openMinkAndStandaloneProExposeTheFullBoardSurface() {
        listOf(OpenMinkFullPolicy, MinkBoardProPolicy).forEach { policy ->
            assertNull(policy.capabilities.maxEditableBoards)
            assertTrue(policy.capabilities.supportsTemplates)
            assertTrue(policy.capabilities.supportsAdvancedExport)
            assertTrue(policy.capabilities.canCreateBoard(500))
        }
    }

    @Test
    fun playMinkLiteAllowsOneEditableBoardWithoutShrinkingStoredData() {
        val capabilities = PlayMinkLitePolicy.capabilities

        assertTrue(capabilities.canCreateBoard(0))
        assertFalse(capabilities.canCreateBoard(1))
        assertFalse(capabilities.canCreateBoard(12))
        assertFalse(capabilities.supportsTemplates)
        assertTrue(capabilities.supportsAdvancedExport)
    }

    @Test(expected = IllegalArgumentException::class)
    fun editableBoardLimitCannotBeZero() {
        MinkBoardCapabilities(maxEditableBoards = 0)
    }

    @Test
    fun fullHostAccessReturnsTheCompleteWorkspaceWithoutCopyingIt() {
        val workspace = workspace("one", "two", "three")
        val access = MinkBoardHostAccess.full(OpenMinkFullPolicy)

        assertSame(workspace, access.editableWorkspace(workspace))
        assertTrue(access.canCreateBoard(workspace))
    }

    @Test
    fun liteAccessExposesOnlyItsDesignatedBoardWithoutChangingStoredData() {
        val workspace = workspace("one", "two", "three")
        val access = MinkBoardHostAccess(
            capabilities = PlayMinkLitePolicy.capabilities,
            editableBoardIds = setOf(BoardId("two")),
        )

        val visible = access.editableWorkspace(workspace)

        assertEquals(listOf("two"), visible.boards.map { it.board.id.value })
        assertEquals(listOf("one", "two", "three"), workspace.boards.map { it.board.id.value })
        assertFalse(access.canCreateBoard(workspace))
    }

    @Test
    fun liteAccessCanCreateItsBoardBeforeOneIsDesignated() {
        val access = MinkBoardHostAccess(
            capabilities = PlayMinkLitePolicy.capabilities,
            editableBoardIds = emptySet(),
        )

        assertTrue(access.canCreateBoard(workspace("preserved-import")))
        assertTrue(access.editableWorkspace(workspace("preserved-import")).boards.isEmpty())
        assertFalse(access.hasBrokenBinding(workspace("preserved-import")))
    }

    @Test
    fun missingDesignatedLiteBoardIsBrokenRatherThanUnbound() {
        val access = MinkBoardHostAccess(
            capabilities = PlayMinkLitePolicy.capabilities,
            editableBoardIds = setOf(BoardId("missing-managed-board")),
        )
        val workspace = workspace("preserved-import")

        assertEquals(
            setOf(BoardId("missing-managed-board")),
            access.missingEditableBoardIds(workspace),
        )
        assertTrue(access.hasBrokenBinding(workspace))
        assertFalse(access.canCreateBoard(workspace))
    }

    @Test(expected = IllegalArgumentException::class)
    fun boundedAccessRequiresAnExplicitBoardScope() {
        MinkBoardHostAccess(capabilities = PlayMinkLitePolicy.capabilities)
    }

    @Test(expected = IllegalArgumentException::class)
    fun boundedAccessRejectsMoreEditableBoardsThanTheProductAllows() {
        MinkBoardHostAccess(
            capabilities = PlayMinkLitePolicy.capabilities,
            editableBoardIds = setOf(BoardId("one"), BoardId("two")),
        )
    }

    private fun workspace(vararg ids: String): MinkBoardWorkspace = MinkBoardWorkspace(
        boards = ids.mapIndexed { position, value ->
            BoardSnapshot(
                board = Board(
                    id = BoardId(value),
                    name = value,
                    isLocked = false,
                    position = position,
                    createdAtEpochMillis = 1L,
                ),
                statuses = emptyList(),
                cards = emptyList(),
            )
        },
    )
}
