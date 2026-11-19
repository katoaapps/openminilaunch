package com.katoaapps.openminilaunch.features.minkboard

import com.katoaapps.minkboard.core.LauncherBoardDesignation
import com.katoaapps.minkboard.core.LauncherBoardState
import com.katoaapps.minkboard.core.LauncherCardAddResult
import com.katoaapps.minkboard.core.LauncherCardCompletionWriteResult
import com.katoaapps.minkboard.core.LauncherExistingBoardDesignationResult
import com.katoaapps.minkboard.core.LauncherFreshBoardDesignationResult
import com.katoaapps.minkboard.core.LauncherFreshBoardRequest
import com.katoaapps.minkboard.core.MinkBoardLauncherFacade
import com.katoaapps.minkboard.data.RoomMinkBoardRepository
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.StatusId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal sealed interface LauncherBoardInitializationState {
    data object NotStarted : LauncherBoardInitializationState
    data object Ready : LauncherBoardInitializationState
    data object CorruptLegacyData : LauncherBoardInitializationState
    data object Failed : LauncherBoardInitializationState
}

internal sealed interface LauncherCardCompletionResult {
    data object Updated : LauncherCardCompletionResult
    data object CardMissing : LauncherCardCompletionResult
    data object BoardUnavailable : LauncherCardCompletionResult
    data object Failed : LauncherCardCompletionResult
}

internal sealed interface LauncherHomeConfigurationResult {
    data object Updated : LauncherHomeConfigurationResult
    data object InvalidTarget : LauncherHomeConfigurationResult
    data object Stale : LauncherHomeConfigurationResult
    data object Failed : LauncherHomeConfigurationResult
}

/**
 * OpenMink's narrow host contract around the shared board engine.
 *
 * This deliberately does not expose legacy To-Do persistence. Once [initialize] succeeds, every
 * launcher read and write goes through Room. Initialization is explicit so callers can finish
 * backup/UI cutover before old preferences become eligible for deletion.
 */
internal class OpenMinkLauncherBoardController(
    private val repository: RoomMinkBoardRepository,
    private val launcherFacade: MinkBoardLauncherFacade,
    private val legacyTodoMigration: LegacyTodoMigrationCoordinator,
    val homePreferences: LauncherBoardHomePreferences,
    private val defaultBoardName: String,
    applicationScope: CoroutineScope,
) {
    private val initializationMutex = Mutex()
    private val intakeWriteMutex = Mutex()
    private val completionMutex = Mutex()
    private val mutableInitializationState =
        kotlinx.coroutines.flow.MutableStateFlow<LauncherBoardInitializationState>(
            LauncherBoardInitializationState.NotStarted,
        )

    val initializationState: StateFlow<LauncherBoardInitializationState> =
        mutableInitializationState

    val boardState: StateFlow<LauncherBoardState> = launcherFacade.observeLauncherBoard()
        .stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = LauncherBoardState.Unbound,
        )

    /**
     * Runs at most one migration attempt at a time. A failure is returned to the caller; it never
     * schedules or recursively triggers another attempt. A later screen entry or user action may
     * explicitly retry, which lets a transient cleanup/storage failure recover without duplicating
     * the already-committed board graph.
     */
    suspend fun initialize(): LauncherBoardInitializationState = initializationMutex.withLock {
        if (mutableInitializationState.value == LauncherBoardInitializationState.Ready) {
            return@withLock LauncherBoardInitializationState.Ready
        }

        val next = runCatching {
            when (legacyTodoMigration.migrateIfNeeded()) {
                LegacyTodoMigrationResult.CorruptLegacyData ->
                    LauncherBoardInitializationState.CorruptLegacyData
                is LegacyTodoMigrationResult.BrokenBinding,
                LegacyTodoMigrationResult.CleanupFailed,
                -> LauncherBoardInitializationState.Failed
                is LegacyTodoMigrationResult.Migrated,
                is LegacyTodoMigrationResult.AlreadyMigrated,
                -> {
                    finishOneWayCompatibility()
                    LauncherBoardInitializationState.Ready
                }
                LegacyTodoMigrationResult.NoLegacyData -> {
                    // Fresh setup is not a migration. The shared store creates the default graph
                    // and its launcher designation atomically without legacy receipt metadata.
                    when (
                        launcherFacade.createAndDesignateFreshBoard(
                            LauncherFreshBoardRequest(boardName = defaultBoardName),
                        )
                    ) {
                        is LauncherFreshBoardDesignationResult.Created,
                        is LauncherFreshBoardDesignationResult.AlreadyDesignated,
                        -> LauncherBoardInitializationState.Ready
                        is LauncherFreshBoardDesignationResult.BrokenExistingDesignation ->
                            LauncherBoardInitializationState.Failed
                    }
                }
            }
        }.getOrElse { LauncherBoardInitializationState.Failed }

        mutableInitializationState.value = next
        next
    }

    suspend fun addToIntake(text: String): LauncherCardAddResult = intakeWriteMutex.withLock {
        if (initialize() != LauncherBoardInitializationState.Ready) {
            return@withLock LauncherCardAddResult.Unbound
        }

        // The shared Room insertion is atomic and newest-first. Do not follow it with a second
        // snapshot read/move: that formerly made Magic Box submission slower and introduced a
        // needless partial-success path after the card already existed.
        launcherFacade.addToIntake(text)
    }

    suspend fun designateHomeBoard(
        boardId: BoardId,
        intakeStatusId: StatusId,
    ): LauncherHomeConfigurationResult {
        val initialization = initialize()
        val persistedDesignation = runCatching {
            repository.launcherBoardDesignation()
        }.getOrElse {
            return LauncherHomeConfigurationResult.Failed
        }
        val plan = planHomeBoardDesignation(initialization, persistedDesignation)

        // Read the compare-and-set token directly from Room instead of the asynchronously
        // hydrated StateFlow. A deleted Home Board intentionally leaves its designation behind
        // as repair evidence, and that broken designation's generation is required to replace it.
        // This also keeps repair available after process restart, when initialization correctly
        // reports the broken binding before the user chooses a replacement.
        val generation = when (plan) {
            LauncherHomeDesignationPlan.Blocked ->
                return LauncherHomeConfigurationResult.Failed
            is LauncherHomeDesignationPlan.Attempt -> plan.expectedGeneration
        }
        return runCatching {
            when (
                launcherFacade.designateExistingBoard(
                    boardId = boardId,
                    intakeStatusId = intakeStatusId,
                    expectedGeneration = generation,
                )
            ) {
                is LauncherExistingBoardDesignationResult.Designated,
                is LauncherExistingBoardDesignationResult.AlreadyDesignated,
                -> LauncherHomeConfigurationResult.Updated
                is LauncherExistingBoardDesignationResult.InvalidTarget,
                is LauncherExistingBoardDesignationResult.BrokenExistingDesignation,
                -> LauncherHomeConfigurationResult.InvalidTarget
                is LauncherExistingBoardDesignationResult.StaleDesignation ->
                    LauncherHomeConfigurationResult.Stale
            }
        }.getOrElse { LauncherHomeConfigurationResult.Failed }
    }

    suspend fun toggleCompletion(cardId: CardId): LauncherCardCompletionResult =
        completionMutex.withLock {
            boardState.value as? LauncherBoardState.Ready
                ?: return@withLock LauncherCardCompletionResult.BoardUnavailable
            runCatching {
                when (repository.toggleBoundCardCompletion(cardId)) {
                    is LauncherCardCompletionWriteResult.Updated ->
                        LauncherCardCompletionResult.Updated
                    LauncherCardCompletionWriteResult.CardMissing ->
                        LauncherCardCompletionResult.CardMissing
                    LauncherCardCompletionWriteResult.Unbound,
                    is LauncherCardCompletionWriteResult.BrokenBinding,
                    -> LauncherCardCompletionResult.BoardUnavailable
                }
            }.getOrElse { LauncherCardCompletionResult.Failed }
        }

    private suspend fun finishOneWayCompatibility() {
        // OpenMink intentionally never writes a rollback projection after the cutover.
        launcherFacade.finishLegacyCompatibility()
    }
}

/**
 * A persisted designation means the launcher database has already been initialized far enough for
 * an explicit user choice to repair it. Without one, initialization must succeed before a first
 * designation can be created.
 */
internal sealed interface LauncherHomeDesignationPlan {
    data class Attempt(val expectedGeneration: String?) : LauncherHomeDesignationPlan
    data object Blocked : LauncherHomeDesignationPlan
}

internal fun planHomeBoardDesignation(
    initialization: LauncherBoardInitializationState,
    persistedDesignation: LauncherBoardDesignation?,
): LauncherHomeDesignationPlan = when {
    persistedDesignation != null -> LauncherHomeDesignationPlan.Attempt(
        expectedGeneration = persistedDesignation.generation,
    )
    initialization == LauncherBoardInitializationState.Ready ->
        LauncherHomeDesignationPlan.Attempt(expectedGeneration = null)
    else -> LauncherHomeDesignationPlan.Blocked
}

internal const val LEGACY_LAUNCHER_PREFERENCES = "mini_launch"
