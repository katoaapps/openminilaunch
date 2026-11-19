package com.katoaapps.minklauncher.todos.home

import com.katoaapps.minklauncher.todos.migration.LegacyTodoMigrationCoordinator
import com.katoaapps.minklauncher.todos.migration.LegacyTodoMigrationResult

import com.katoaapps.minkboard.core.LauncherBoardDesignation
import com.katoaapps.minkboard.core.LauncherBoardIntegrationStore
import com.katoaapps.minkboard.core.LauncherBoardState
import com.katoaapps.minkboard.core.LauncherCardAddResult
import com.katoaapps.minkboard.core.LauncherCardCompletionWriteResult
import com.katoaapps.minkboard.core.LauncherExistingBoardDesignationResult
import com.katoaapps.minkboard.core.LauncherFreshBoardDesignationResult
import com.katoaapps.minkboard.core.LauncherFreshBoardRequest
import com.katoaapps.minkboard.core.MinkBoardLauncherFacade
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.StatusId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal sealed interface TodoInitializationState {
    data object NotStarted : TodoInitializationState
    data object Ready : TodoInitializationState
    data object CorruptLegacyData : TodoInitializationState
    data object Failed : TodoInitializationState
}

internal sealed interface TodoCompletionResult {
    data object Updated : TodoCompletionResult
    data object CardMissing : TodoCompletionResult
    data object BoardUnavailable : TodoCompletionResult
    data object Failed : TodoCompletionResult
}

internal sealed interface TodoHomeConfigurationResult {
    data object Updated : TodoHomeConfigurationResult
    data object InvalidTarget : TodoHomeConfigurationResult
    data object Stale : TodoHomeConfigurationResult
    data object Failed : TodoHomeConfigurationResult
}

/**
 * Narrow launcher Todos contract around the shared board engine.
 *
 * This deliberately does not expose legacy To-Do persistence. Once [initialize] succeeds, every
 * launcher read and write goes through Room. Initialization is explicit so callers can finish
 * backup/UI cutover before old preferences become eligible for deletion.
 */
internal class TodoController(
    private val integrationStore: LauncherBoardIntegrationStore,
    private val launcherFacade: MinkBoardLauncherFacade,
    private val legacyTodoMigration: LegacyTodoMigrationCoordinator,
    val homePreferences: TodoHomePreferences,
    private val defaultBoardName: String,
    applicationScope: CoroutineScope,
) {
    private val initializationMutex = Mutex()
    private val intakeWriteMutex = Mutex()
    private val completionMutex = Mutex()
    private val mutableInitializationState =
        kotlinx.coroutines.flow.MutableStateFlow<TodoInitializationState>(
            TodoInitializationState.NotStarted,
        )

    val initializationState: StateFlow<TodoInitializationState> =
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
    suspend fun initialize(): TodoInitializationState = initializationMutex.withLock {
        if (mutableInitializationState.value == TodoInitializationState.Ready) {
            return@withLock TodoInitializationState.Ready
        }

        val next = runCatching {
            when (legacyTodoMigration.migrateIfNeeded()) {
                LegacyTodoMigrationResult.CorruptLegacyData ->
                    TodoInitializationState.CorruptLegacyData
                is LegacyTodoMigrationResult.BrokenBinding,
                LegacyTodoMigrationResult.CleanupFailed,
                -> TodoInitializationState.Failed
                is LegacyTodoMigrationResult.Migrated,
                is LegacyTodoMigrationResult.AlreadyMigrated,
                -> {
                    finishOneWayCompatibility()
                    TodoInitializationState.Ready
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
                        -> TodoInitializationState.Ready
                        is LauncherFreshBoardDesignationResult.BrokenExistingDesignation ->
                            TodoInitializationState.Failed
                    }
                }
            }
        }.getOrElse { TodoInitializationState.Failed }

        mutableInitializationState.value = next
        next
    }

    suspend fun addToIntake(text: String): LauncherCardAddResult = intakeWriteMutex.withLock {
        if (initialize() != TodoInitializationState.Ready) {
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
    ): TodoHomeConfigurationResult {
        val initialization = initialize()
        val persistedDesignation = runCatching {
            integrationStore.launcherBoardDesignation()
        }.getOrElse {
            return TodoHomeConfigurationResult.Failed
        }
        val plan = planTodoHomeDesignation(initialization, persistedDesignation)

        // Read the compare-and-set token directly from Room instead of the asynchronously
        // hydrated StateFlow. A deleted Home Board intentionally leaves its designation behind
        // as repair evidence, and that broken designation's generation is required to replace it.
        // This also keeps repair available after process restart, when initialization correctly
        // reports the broken binding before the user chooses a replacement.
        val generation = when (plan) {
            TodoHomeDesignationPlan.Blocked ->
                return TodoHomeConfigurationResult.Failed
            is TodoHomeDesignationPlan.Attempt -> plan.expectedGeneration
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
                -> TodoHomeConfigurationResult.Updated
                is LauncherExistingBoardDesignationResult.InvalidTarget,
                is LauncherExistingBoardDesignationResult.BrokenExistingDesignation,
                -> TodoHomeConfigurationResult.InvalidTarget
                is LauncherExistingBoardDesignationResult.StaleDesignation ->
                    TodoHomeConfigurationResult.Stale
            }
        }.getOrElse { TodoHomeConfigurationResult.Failed }
    }

    suspend fun toggleCompletion(cardId: CardId): TodoCompletionResult =
        completionMutex.withLock {
            boardState.value as? LauncherBoardState.Ready
                ?: return@withLock TodoCompletionResult.BoardUnavailable
            runCatching {
                when (integrationStore.toggleBoundCardCompletion(cardId)) {
                    is LauncherCardCompletionWriteResult.Updated ->
                        TodoCompletionResult.Updated
                    LauncherCardCompletionWriteResult.CardMissing ->
                        TodoCompletionResult.CardMissing
                    LauncherCardCompletionWriteResult.Unbound,
                    is LauncherCardCompletionWriteResult.BrokenBinding,
                    -> TodoCompletionResult.BoardUnavailable
                }
            }.getOrElse { TodoCompletionResult.Failed }
        }

    private suspend fun finishOneWayCompatibility() {
        // The launcher intentionally never writes a rollback projection after the cutover.
        launcherFacade.finishLegacyCompatibility()
    }
}

/**
 * A persisted designation means the launcher database has already been initialized far enough for
 * an explicit user choice to repair it. Without one, initialization must succeed before a first
 * designation can be created.
 */
internal sealed interface TodoHomeDesignationPlan {
    data class Attempt(val expectedGeneration: String?) : TodoHomeDesignationPlan
    data object Blocked : TodoHomeDesignationPlan
}

internal fun planTodoHomeDesignation(
    initialization: TodoInitializationState,
    persistedDesignation: LauncherBoardDesignation?,
): TodoHomeDesignationPlan = when {
    persistedDesignation != null -> TodoHomeDesignationPlan.Attempt(
        expectedGeneration = persistedDesignation.generation,
    )
    initialization == TodoInitializationState.Ready ->
        TodoHomeDesignationPlan.Attempt(expectedGeneration = null)
    else -> TodoHomeDesignationPlan.Blocked
}

internal const val LEGACY_LAUNCHER_PREFERENCES = "mini_launch"
