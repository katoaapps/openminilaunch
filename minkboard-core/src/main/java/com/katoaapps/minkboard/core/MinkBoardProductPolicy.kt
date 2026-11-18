package com.katoaapps.minkboard.core

import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSummary
import com.katoaapps.minkboard.model.MinkBoardWorkspace

data class MinkBoardCapabilities(
    /** Limits the boards this product surface may create or edit, never the boards kept on disk. */
    val maxEditableBoards: Int? = null,
    val maxStatusesPerBoard: Int? = null,
    val supportsTemplates: Boolean = true,
    val supportsAttachments: Boolean = false,
    val supportsAdvancedExport: Boolean = false,
    val supportsFocusTimer: Boolean = true,
    val supportsWidgets: Boolean = true,
) {
    init {
        require(maxEditableBoards == null || maxEditableBoards > 0) {
            "maxEditableBoards must be positive when it is bounded"
        }
        require(maxStatusesPerBoard == null || maxStatusesPerBoard > 0) {
            "maxStatusesPerBoard must be positive when it is bounded"
        }
    }

    fun canCreateBoard(currentEditableBoardCount: Int): Boolean {
        require(currentEditableBoardCount >= 0) { "Board count cannot be negative" }
        return maxEditableBoards?.let { currentEditableBoardCount < it } ?: true
    }
}

interface MinkBoardProductPolicy {
    val capabilities: MinkBoardCapabilities
}

/**
 * A capability request that a product host may route to an explanation, upgrade, or handoff.
 * Shared UI deliberately has no knowledge of billing, package names, or app stores.
 */
enum class MinkBoardCapability {
    CreateAdditionalBoard,
    UseBoardTemplates,
}

/**
 * The boards a host currently exposes for editing. This is presentation policy, not storage
 * policy: [editableBoardIds] never deletes, truncates, or rewrites the complete workspace.
 *
 * A bounded product must identify its editable board explicitly rather than treating the first
 * row in the database as the Lite board. An empty set represents a host that has not created or
 * designated that board yet.
 */
data class MinkBoardHostAccess(
    val capabilities: MinkBoardCapabilities,
    val editableBoardIds: Set<BoardId>? = null,
) {
    init {
        capabilities.maxEditableBoards?.let { maximum ->
            requireNotNull(editableBoardIds) {
                "A bounded MinkBoard host must explicitly identify its editable boards"
            }
            require(editableBoardIds.size <= maximum) {
                "Editable board scope exceeds the product capability"
            }
        }
    }

    fun editableWorkspace(workspace: MinkBoardWorkspace): MinkBoardWorkspace {
        val ids = editableBoardIds ?: return workspace
        return MinkBoardWorkspace(
            boards = workspace.boards.filter { it.board.id in ids },
        )
    }

    fun editableBoards(boards: List<BoardSummary>): List<BoardSummary> {
        val ids = editableBoardIds ?: return boards
        return boards.filter { it.board.id in ids }
    }

    fun missingEditableBoardIds(workspace: MinkBoardWorkspace): Set<BoardId> {
        val ids = editableBoardIds ?: return emptySet()
        val storedIds = workspace.boards.mapTo(mutableSetOf()) { it.board.id }
        return ids - storedIds
    }

    fun missingEditableBoardIds(boards: List<BoardSummary>): Set<BoardId> {
        val ids = editableBoardIds ?: return emptySet()
        val storedIds = boards.mapTo(mutableSetOf()) { it.board.id }
        return ids - storedIds
    }

    fun hasBrokenBinding(workspace: MinkBoardWorkspace): Boolean =
        missingEditableBoardIds(workspace).isNotEmpty()

    fun hasBrokenBinding(boards: List<BoardSummary>): Boolean =
        missingEditableBoardIds(boards).isNotEmpty()

    fun canCreateBoard(workspace: MinkBoardWorkspace): Boolean {
        if (hasBrokenBinding(workspace)) return false
        return capabilities.canCreateBoard(editableWorkspace(workspace).boards.size)
    }

    fun canCreateBoard(boards: List<BoardSummary>): Boolean {
        if (hasBrokenBinding(boards)) return false
        return capabilities.canCreateBoard(editableBoards(boards).size)
    }

    companion object {
        fun full(policy: MinkBoardProductPolicy): MinkBoardHostAccess =
            MinkBoardHostAccess(capabilities = policy.capabilities)
    }
}

private val fullMinkBoardCapabilities = MinkBoardCapabilities(
    supportsTemplates = true,
    supportsAdvancedExport = true,
)

/** Full standalone product sold as MinkBoard Pro. */
object MinkBoardProPolicy : MinkBoardProductPolicy {
    override val capabilities = fullMinkBoardCapabilities
}

/** Full embedded board experience shipped in Mink Launcher Open Source. */
object OpenMinkFullPolicy : MinkBoardProductPolicy {
    override val capabilities = fullMinkBoardCapabilities
}

/**
 * PlayMink exposes one editable board and no template picker. The repository still retains every
 * imported or restored board so a Lite limit can never destroy user data.
 */
object PlayMinkLitePolicy : MinkBoardProductPolicy {
    override val capabilities = MinkBoardCapabilities(
        maxEditableBoards = 1,
        supportsTemplates = false,
        // The agreed Lite boundary is one editable board and no templates. Data portability is
        // not a Pro entitlement, especially because retained imported boards must stay recoverable.
        supportsAdvancedExport = true,
    )
}

@Deprecated("Use MinkBoardProPolicy")
object StandalonePreviewPolicy : MinkBoardProductPolicy by MinkBoardProPolicy
