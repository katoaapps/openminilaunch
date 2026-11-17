package com.katoaapps.minkboard.core

import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.CardDraft
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.MinkBoardWorkspace
import com.katoaapps.minkboard.model.StatusId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class LauncherBoardSummary(
    val boardId: BoardId,
    val boardName: String,
    val intakeCardCount: Int,
    val unfinishedCardCount: Int,
    val completedCardCount: Int,
    val totalCardCount: Int,
)

enum class LauncherBindingProblem {
    BOARD_MISSING,
    INTAKE_STATUS_MISSING,
    INTAKE_STATUS_BELONGS_TO_ANOTHER_BOARD,
    INTAKE_STATUS_IS_DONE,
}

sealed interface LauncherBoardState {
    data object Unbound : LauncherBoardState

    data class Ready(
        val designation: LauncherBoardDesignation,
        val board: BoardSnapshot,
        val summary: LauncherBoardSummary,
    ) : LauncherBoardState

    data class BrokenBinding(
        val designation: LauncherBoardDesignation,
        val problem: LauncherBindingProblem,
    ) : LauncherBoardState
}

sealed interface LauncherNormalizationResult {
    data object NoLegacyData : LauncherNormalizationResult
    data object CorruptLegacyData : LauncherNormalizationResult

    data class Migrated(
        val designation: LauncherBoardDesignation,
    ) : LauncherNormalizationResult

    data class AlreadyNormalized(
        val designation: LauncherBoardDesignation,
    ) : LauncherNormalizationResult

    data class BrokenBinding(
        val designation: LauncherBoardDesignation,
        val problem: LauncherBindingProblem,
    ) : LauncherNormalizationResult
}

sealed interface LauncherCardAddResult {
    data class Added(val cardId: CardId) : LauncherCardAddResult
    data object Unbound : LauncherCardAddResult
    data object InvalidText : LauncherCardAddResult
    data class BrokenBinding(val problem: LauncherBindingProblem) : LauncherCardAddResult
}

sealed interface LauncherLegacyProjectionState {
    data object Unbound : LauncherLegacyProjectionState
    data object NotApplicable : LauncherLegacyProjectionState
    data object CompatibilityComplete : LauncherLegacyProjectionState

    data class BrokenBinding(
        val problem: LauncherBindingProblem,
    ) : LauncherLegacyProjectionState

    data class Ready(
        val projection: LegacyTodoProjection,
        val decision: LegacyProjectionDecision,
        /** Used after either writing or adopting an already-durable canonical projection. */
        val receiptToken: LauncherProjectionReceiptToken,
    ) : LauncherLegacyProjectionState
}

data class LauncherProjectionReceiptToken(
    val bindingGeneration: String,
    val boardId: BoardId,
    val fingerprint: String,
    val expectedPreviousFingerprint: String?,
)

sealed interface LauncherBindingMutationResult {
    data object Updated : LauncherBindingMutationResult
    data object Unbound : LauncherBindingMutationResult
    data object CompatibilityNotApplicable : LauncherBindingMutationResult
    data object StaleProjectionToken : LauncherBindingMutationResult
    data object StaleBindingGeneration : LauncherBindingMutationResult
    data class BrokenBinding(val problem: LauncherBindingProblem) : LauncherBindingMutationResult
}

sealed interface LauncherBackupCreationResult {
    data class Ready(val backup: LauncherMinkBoardBackup.Present) : LauncherBackupCreationResult
    data object UnboundWorkspace : LauncherBackupCreationResult
    data class BrokenBinding(val problem: LauncherBindingProblem) : LauncherBackupCreationResult
}

/**
 * Host-neutral coordinator for launcher board designation and temporary To-Do compatibility.
 *
 * The facade never reads SharedPreferences, navigates, serializes an archive, or decides which
 * product is Lite. OpenMink and PlayMink own those boundaries and feed explicit legacy/backup
 * inputs into this coordinator.
 */
class MinkBoardLauncherFacade(
    private val integrationStore: LauncherBoardIntegrationStore,
    private val workspaceIntegrationStore: LauncherWorkspaceIntegrationStore,
    private val normalizer: LauncherTodoNormalizer,
) {
    fun observeLauncherBoard(): Flow<LauncherBoardState> =
        integrationStore.observeLauncherBoardHostSnapshot()
            .map { snapshot ->
                launcherBoardState(snapshot.workspace, snapshot.designation)
            }

    suspend fun createAndDesignateFreshBoard(
        request: LauncherFreshBoardRequest,
    ): LauncherFreshBoardDesignationResult =
        integrationStore.createAndDesignateFreshBoard(request)

    suspend fun designateExistingBoard(
        boardId: BoardId,
        intakeStatusId: StatusId,
        expectedGeneration: String?,
    ): LauncherExistingBoardDesignationResult =
        integrationStore.designateExistingBoard(boardId, intakeStatusId, expectedGeneration)

    suspend fun normalizeLegacyTodos(
        source: LegacyTodoSource,
        sourceSchemaVersion: Int,
        boardName: String = "To-Dos",
    ): LauncherNormalizationResult {
        when (val state = currentState()) {
            is LauncherBoardState.Ready ->
                return LauncherNormalizationResult.AlreadyNormalized(state.designation)
            is LauncherBoardState.BrokenBinding ->
                return LauncherNormalizationResult.BrokenBinding(
                    designation = state.designation,
                    problem = state.problem,
                )
            LauncherBoardState.Unbound -> Unit
        }

        return when (val outcome = normalizer.plan(source, sourceSchemaVersion, boardName)) {
            LauncherTodoMigrationOutcome.NoLegacyData -> LauncherNormalizationResult.NoLegacyData
            LauncherTodoMigrationOutcome.CorruptLegacyData ->
                LauncherNormalizationResult.CorruptLegacyData
            is LauncherTodoMigrationOutcome.Ready -> {
                val designation = integrationStore.commitLauncherTodoMigration(outcome.plan)
                when (val state = currentState()) {
                    is LauncherBoardState.Ready -> if (state.designation == designation) {
                        LauncherNormalizationResult.Migrated(state.designation)
                    } else {
                        // Another atomic designation replaced the migration before validation.
                        // Report the transaction-stable current owner, never the supplied value.
                        LauncherNormalizationResult.AlreadyNormalized(state.designation)
                    }
                    is LauncherBoardState.BrokenBinding -> LauncherNormalizationResult.BrokenBinding(
                        designation = state.designation,
                        problem = state.problem,
                    )
                    LauncherBoardState.Unbound ->
                        error("A committed designation cannot resolve as unbound")
                }
            }
        }
    }

    suspend fun addToIntake(text: String): LauncherCardAddResult {
        val draft = runCatching { normalizer.intakeDraft(text) }
            .getOrElse { return LauncherCardAddResult.InvalidText }
        return when (val result = integrationStore.addCardToBoundIntake(draft)) {
            LauncherBoundIntakeWriteResult.Unbound -> LauncherCardAddResult.Unbound
            is LauncherBoundIntakeWriteResult.BrokenBinding ->
                LauncherCardAddResult.BrokenBinding(result.problem)
            is LauncherBoundIntakeWriteResult.Added -> LauncherCardAddResult.Added(result.cardId)
        }
    }

    suspend fun planLegacyProjection(
        currentLegacyItems: List<LegacyLauncherTodo>,
    ): LauncherLegacyProjectionState = when (val state = currentState()) {
        LauncherBoardState.Unbound -> LauncherLegacyProjectionState.Unbound
        is LauncherBoardState.BrokenBinding ->
            LauncherLegacyProjectionState.BrokenBinding(state.problem)
        is LauncherBoardState.Ready -> {
            val compatibility = state.designation.legacyCompatibility
            if (compatibility == null) {
                LauncherLegacyProjectionState.NotApplicable
            } else if (compatibility.state == LauncherTodoCompatibilityState.CANONICAL_FINAL) {
                LauncherLegacyProjectionState.CompatibilityComplete
            } else {
                val projection = normalizer.project(state.board)
                LauncherLegacyProjectionState.Ready(
                    projection = projection,
                    decision = normalizer.projectionDecision(
                        currentLegacyItems = currentLegacyItems,
                        compatibility = compatibility,
                        canonicalProjection = projection,
                    ),
                    receiptToken = LauncherProjectionReceiptToken(
                        bindingGeneration = state.designation.generation,
                        boardId = state.designation.boardId,
                        fingerprint = projection.fingerprint,
                        expectedPreviousFingerprint = compatibility.lastProjectionFingerprint,
                    ),
                )
            }
        }
    }

    /**
     * Call after the host durably writes the projection, or immediately for
     * [LegacyProjectionDecision.ADOPT_CANONICAL_PROJECTION] when it is already durable.
     */
    suspend fun recordLegacyProjectionWritten(
        token: LauncherProjectionReceiptToken,
    ): LauncherBindingMutationResult = when (val state = currentState()) {
        LauncherBoardState.Unbound -> LauncherBindingMutationResult.Unbound
        is LauncherBoardState.BrokenBinding ->
            LauncherBindingMutationResult.BrokenBinding(state.problem)
        is LauncherBoardState.Ready -> {
            if (
                state.designation.generation != token.bindingGeneration ||
                state.designation.boardId != token.boardId
            ) {
                LauncherBindingMutationResult.StaleProjectionToken
            } else {
                if (
                    integrationStore.recordLegacyProjectionWritten(
                        generation = token.bindingGeneration,
                        boardId = token.boardId,
                        fingerprint = token.fingerprint,
                        expectedPreviousFingerprint = token.expectedPreviousFingerprint,
                    )
                ) {
                    LauncherBindingMutationResult.Updated
                } else {
                    LauncherBindingMutationResult.StaleProjectionToken
                }
            }
        }
    }

    suspend fun finishLegacyCompatibility(): LauncherBindingMutationResult = when (
        val state = currentState()
    ) {
        LauncherBoardState.Unbound -> LauncherBindingMutationResult.Unbound
        is LauncherBoardState.BrokenBinding ->
            LauncherBindingMutationResult.BrokenBinding(state.problem)
        is LauncherBoardState.Ready -> {
            if (state.designation.legacyCompatibility == null) {
                LauncherBindingMutationResult.CompatibilityNotApplicable
            } else if (
                integrationStore.finishLegacyCompatibility(
                    generation = state.designation.generation,
                    boardId = state.designation.boardId,
                )
            ) {
                LauncherBindingMutationResult.Updated
            } else {
                LauncherBindingMutationResult.StaleBindingGeneration
            }
        }
    }

    /**
     * Returns an in-memory plaintext model. The host must encrypt it before writing it outside the
     * app process; this method deliberately does not define a launcher-backup wire format.
     */
    suspend fun createInMemoryBackup(sourceApp: String): LauncherBackupCreationResult =
        workspaceIntegrationStore.createLauncherBackupSnapshot(sourceApp)

    suspend fun importRichBackup(
        plan: LauncherBackupRestorePlan.ImportWorkspace,
        mode: WorkspaceImportMode,
        context: LauncherBackupRestoreContext,
    ): LauncherWorkspaceImportOutcome.Imported =
        workspaceIntegrationStore.importLauncherBackupSnapshot(plan, mode, context)

    private suspend fun currentState(): LauncherBoardState =
        integrationStore.launcherBoardHostSnapshot().let { snapshot ->
            launcherBoardState(snapshot.workspace, snapshot.designation)
        }

}

sealed interface LauncherBackupRestorePlan {
    data object LeaveDestinationUntouched : LauncherBackupRestorePlan
    data object CorruptLegacyData : LauncherBackupRestorePlan
    data object InvalidRichBackup : LauncherBackupRestorePlan
    data object ExplicitlyEmptyWorkspace : LauncherBackupRestorePlan

    data class NormalizeLegacy(
        val source: LegacyTodoSource.Available,
    ) : LauncherBackupRestorePlan

    data class ImportWorkspace(
        val workspace: com.katoaapps.minkboard.model.PortableWorkspaceSnapshot,
        val portableDesignation: LauncherBoardBackupDesignation,
    ) : LauncherBackupRestorePlan
}

data class LauncherLocalLegacyCompatibility(
    val sourceSchemaVersion: Int,
    val sourceFingerprint: String,
) {
    init {
        require(sourceSchemaVersion > 0) { "Local legacy schema version must be positive" }
        require(
            sourceFingerprint.length == 64 &&
                sourceFingerprint.all { it in '0'..'9' || it in 'a'..'f' },
        ) { "Local legacy fingerprint must be a lowercase SHA-256 value" }
    }
}

data class LauncherBackupRestoreContext(
    val restoredAtEpochMillis: Long,
    val localLegacyCompatibility: LauncherLocalLegacyCompatibility? = null,
) {
    init {
        require(restoredAtEpochMillis >= 0) { "Restore timestamp cannot be negative" }
    }
}

sealed interface LauncherBackupDesignationResolution {
    data object MissingFromImport : LauncherBackupDesignationResolution
    data class Resolved(
        val designation: LauncherBoardBackupDesignation,
    ) : LauncherBackupDesignationResolution
}

sealed interface LauncherWorkspaceImportOutcome {
    data class Imported(
        val importResult: WorkspaceImportResult,
        val launcherDesignation: LauncherBoardDesignation,
    ) : LauncherWorkspaceImportOutcome
}

object LauncherBackupReconciler {
    fun plan(
        minkBoard: LauncherMinkBoardBackup,
        legacyTodos: LegacyTodoSource,
    ): LauncherBackupRestorePlan = when (minkBoard) {
        LauncherMinkBoardBackup.Absent -> when (legacyTodos) {
            LegacyTodoSource.Missing -> LauncherBackupRestorePlan.LeaveDestinationUntouched
            LegacyTodoSource.Corrupt -> LauncherBackupRestorePlan.CorruptLegacyData
            is LegacyTodoSource.Available -> LauncherBackupRestorePlan.NormalizeLegacy(legacyTodos)
        }
        is LauncherMinkBoardBackup.Present -> {
            val portableDesignation = minkBoard.designation
            val boundBoard = portableDesignation?.let { designation ->
                minkBoard.workspace.boards.firstOrNull { it.id == designation.boardId.value }
            }
            val boundIntake = boundBoard?.statuses?.firstOrNull { status ->
                status.id == portableDesignation?.intakeStatusId?.value
            }
            if (minkBoard.workspace.boards.isNotEmpty() && portableDesignation == null) {
                LauncherBackupRestorePlan.InvalidRichBackup
            } else if (portableDesignation != null &&
                (boundBoard == null || boundIntake == null || boundIntake.isDoneStatus)
            ) {
                LauncherBackupRestorePlan.InvalidRichBackup
            } else if (minkBoard.workspace.boards.isEmpty()) {
                LauncherBackupRestorePlan.ExplicitlyEmptyWorkspace
            } else {
                LauncherBackupRestorePlan.ImportWorkspace(
                    minkBoard.workspace,
                    checkNotNull(portableDesignation),
                )
            }
        }
    }

    fun resolveLauncherDesignation(
        portableDesignation: LauncherBoardBackupDesignation,
        result: WorkspaceImportResult,
    ): LauncherBackupDesignationResolution {
        val boardId = result.ids.boardIds[portableDesignation.boardId.value]
            ?: return LauncherBackupDesignationResolution.MissingFromImport
        val intakeStatusId = result.ids.statusIds[portableDesignation.intakeStatusId.value]
            ?: return LauncherBackupDesignationResolution.MissingFromImport
        return LauncherBackupDesignationResolution.Resolved(
            portableDesignation.copy(
                boardId = boardId,
                intakeStatusId = intakeStatusId,
            ),
        )
    }
}

private fun launcherBoardState(
    workspace: MinkBoardWorkspace,
    designation: LauncherBoardDesignation?,
): LauncherBoardState {
    designation ?: return LauncherBoardState.Unbound
    val board = workspace.board(designation.boardId)
        ?: return LauncherBoardState.BrokenBinding(
            designation,
            LauncherBindingProblem.BOARD_MISSING,
        )
    val intakeStatus = board.statuses.firstOrNull { it.id == designation.intakeStatusId }
        ?: return LauncherBoardState.BrokenBinding(
            designation,
            LauncherBindingProblem.INTAKE_STATUS_MISSING,
        )
    if (intakeStatus.boardId != designation.boardId) {
        return LauncherBoardState.BrokenBinding(
            designation,
            LauncherBindingProblem.INTAKE_STATUS_BELONGS_TO_ANOTHER_BOARD,
        )
    }
    if (intakeStatus.isDoneStatus) {
        return LauncherBoardState.BrokenBinding(
            designation,
            LauncherBindingProblem.INTAKE_STATUS_IS_DONE,
        )
    }
    return LauncherBoardState.Ready(
        designation = designation,
        board = board,
        summary = board.launcherSummary(designation.intakeStatusId),
    )
}

private fun BoardSnapshot.launcherSummary(intakeStatusId: StatusId): LauncherBoardSummary {
    val doneStatusIds = statuses.filter { it.isDoneStatus }.map { it.id }.toSet()
    return LauncherBoardSummary(
        boardId = board.id,
        boardName = board.name,
        intakeCardCount = cards.count { it.statusId == intakeStatusId },
        unfinishedCardCount = cards.count { it.statusId !in doneStatusIds },
        completedCardCount = cards.count { it.statusId in doneStatusIds },
        totalCardCount = cards.size,
    )
}
