package com.katoaapps.minkboard.core

import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.CardDraft
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.MinkBoardWorkspace
import com.katoaapps.minkboard.model.StatusId
import kotlinx.coroutines.flow.Flow

/** One transaction-stable launcher view; workspace and designation must describe the same commit. */
data class LauncherBoardHostSnapshot(
    val workspace: MinkBoardWorkspace,
    val designation: LauncherBoardDesignation?,
)

data class LauncherFreshBoardRequest(
    val boardName: String,
    val isLocked: Boolean = false,
) {
    init {
        MinkBoardRules.boardName(boardName)
    }
}

sealed interface LauncherFreshBoardDesignationResult {
    data class Created(
        val designation: LauncherBoardDesignation,
    ) : LauncherFreshBoardDesignationResult

    data class AlreadyDesignated(
        val designation: LauncherBoardDesignation,
    ) : LauncherFreshBoardDesignationResult

    data class BrokenExistingDesignation(
        val designation: LauncherBoardDesignation,
        val problem: LauncherBindingProblem,
    ) : LauncherFreshBoardDesignationResult
}

sealed interface LauncherExistingBoardDesignationResult {
    data class Designated(
        val designation: LauncherBoardDesignation,
    ) : LauncherExistingBoardDesignationResult

    data class AlreadyDesignated(
        val designation: LauncherBoardDesignation,
    ) : LauncherExistingBoardDesignationResult

    data class BrokenExistingDesignation(
        val designation: LauncherBoardDesignation,
        val problem: LauncherBindingProblem,
    ) : LauncherExistingBoardDesignationResult

    /** The caller acted on a designation generation that is no longer current. */
    data class StaleDesignation(
        val current: LauncherBoardDesignation?,
    ) : LauncherExistingBoardDesignationResult

    data class InvalidTarget(
        val problem: LauncherBindingProblem,
    ) : LauncherExistingBoardDesignationResult
}

/** Result of atomically resolving the launcher designation and inserting into its intake. */
sealed interface LauncherBoundIntakeWriteResult {
    data class Added(val cardId: CardId) : LauncherBoundIntakeWriteResult
    data object Unbound : LauncherBoundIntakeWriteResult
    data class BrokenBinding(val problem: LauncherBindingProblem) : LauncherBoundIntakeWriteResult
}

/** Transactional persistence seam used by a launcher host, separate from UI/product policy. */
interface LauncherBoardIntegrationStore {
    fun observeLauncherBoardHostSnapshot(): Flow<LauncherBoardHostSnapshot>

    /** Reads workspace plus designation from one persistence transaction for command planning. */
    suspend fun launcherBoardHostSnapshot(): LauncherBoardHostSnapshot

    suspend fun launcherBoardDesignation(): LauncherBoardDesignation?

    /** Creates a default board graph and its launcher designation in one transaction. */
    suspend fun createAndDesignateFreshBoard(
        request: LauncherFreshBoardRequest,
    ): LauncherFreshBoardDesignationResult

    /** Explicitly designates an existing board and non-Done intake in one transaction. */
    suspend fun designateExistingBoard(
        boardId: BoardId,
        intakeStatusId: StatusId,
        expectedGeneration: String?,
    ): LauncherExistingBoardDesignationResult

    /**
     * Commits the encrypted board graph and its content-derived pseudonymous local receipt metadata
     * as one transaction.
     */
    suspend fun commitLauncherTodoMigration(
        plan: LauncherTodoMigrationPlan,
    ): LauncherBoardDesignation

    /** Resolves the current designation, validates its intake, and inserts in one transaction. */
    suspend fun addCardToBoundIntake(draft: CardDraft): LauncherBoundIntakeWriteResult

    /**
     * Called after a successful compatibility write or to adopt an already-canonical durable copy.
     * Compare-and-set prevents an older completion from replacing a newer token.
     */
    suspend fun recordLegacyProjectionWritten(
        generation: String,
        boardId: BoardId,
        fingerprint: String,
        expectedPreviousFingerprint: String?,
    ): Boolean

    /** Ends runtime projection while retaining the receipt and late-upgrader migration reader. */
    suspend fun finishLegacyCompatibility(generation: String, boardId: BoardId): Boolean
}
