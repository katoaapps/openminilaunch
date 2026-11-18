package com.katoaapps.openminilaunch.features.minkboard

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.backup.MinkBoardLauncherBackupCoordinator
import com.katoaapps.openminilaunch.features.minkboard.focus.AlarmManagerFocusTimerScheduler
import com.katoaapps.openminilaunch.features.minkboard.focus.AndroidFocusTimerNotifier
import com.katoaapps.openminilaunch.features.minkboard.focus.FocusNotificationText
import com.katoaapps.openminilaunch.features.minkboard.focus.FocusTimerCoordinator
import com.katoaapps.openminilaunch.features.minkboard.focus.FocusTimerIntents
import com.katoaapps.minkboard.core.BoardAccessSessionController
import com.katoaapps.minkboard.core.IdFactory
import com.katoaapps.minkboard.core.LauncherBoardDesignation
import com.katoaapps.minkboard.core.LauncherBoardState
import com.katoaapps.minkboard.core.LauncherCardAddResult
import com.katoaapps.minkboard.core.LauncherFreshBoardDesignationResult
import com.katoaapps.minkboard.core.LauncherFreshBoardRequest
import com.katoaapps.minkboard.core.LauncherExistingBoardDesignationResult
import com.katoaapps.minkboard.core.LauncherTodoNormalizer
import com.katoaapps.minkboard.core.MinkBoardLauncherFacade
import com.katoaapps.minkboard.core.MinkBoardLauncherBackupWorkflow
import com.katoaapps.minkboard.core.MinkBoardDefaults
import com.katoaapps.minkboard.data.RoomMinkBoardRepository
import com.katoaapps.minkboard.data.PassphraseLauncherBackupEnvelopeCodec
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.FocusSessionState
import com.katoaapps.minkboard.model.FocusTimerId
import com.katoaapps.minkboard.model.StatusDraft
import com.katoaapps.minkboard.model.StatusId
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * OpenMink-owned lifecycle boundary around the pinned reusable MinkBoard modules.
 *
 * Launcher navigation, migration timing, backup wrapping, and product UI stay outside the
 * canonical modules. Keeping one application-scoped repository also prevents separate launcher
 * surfaces from accidentally opening independent Room instances.
 */
internal class OpenMinkMinkBoardRuntime(context: Context) {
    private val applicationContext = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val boardAccessSessions by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        BoardAccessSessionController(elapsedRealtimeMillis = SystemClock::elapsedRealtime)
    }

    private val mutableBoardAccessRevision = MutableStateFlow(0L)
    val boardAccessRevision: StateFlow<Long> = mutableBoardAccessRevision

    private val mutableNavigationRequest = MutableStateFlow<OpenMinkBoardNavigationRequest?>(null)
    val navigationRequest: StateFlow<OpenMinkBoardNavigationRequest?> = mutableNavigationRequest
    private var nextNavigationToken = 0L

    val repository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        RoomMinkBoardRepository.create(applicationContext)
    }

    val launcherFacade by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        MinkBoardLauncherFacade(
            integrationStore = repository,
            workspaceIntegrationStore = repository,
            normalizer = LauncherTodoNormalizer(
                idFactory = IdFactory { UUID.randomUUID().toString() },
                clockMillis = System::currentTimeMillis,
            ),
        )
    }

    val legacyTodoMigration by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LegacyTodoMigrationCoordinator(
            port = FacadeLegacyTodoMigrationPort(launcherFacade),
            legacyStore = SharedPreferencesLegacyTodoStore(
                applicationContext.getSharedPreferences(
                    LEGACY_LAUNCHER_PREFERENCES,
                    Context.MODE_PRIVATE,
                ),
            ),
        )
    }

    val homePreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LauncherBoardHomePreferences(
            applicationContext.getSharedPreferences(
                LEGACY_LAUNCHER_PREFERENCES,
                Context.MODE_PRIVATE,
            ),
        )
    }

    val uiPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        OpenMinkBoardUiPreferences(
            applicationContext.getSharedPreferences(
                LEGACY_LAUNCHER_PREFERENCES,
                Context.MODE_PRIVATE,
            ),
        )
    }

    val launcherController by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        OpenMinkLauncherBoardController(
            repository = repository,
            launcherFacade = launcherFacade,
            legacyTodoMigration = legacyTodoMigration,
            homePreferences = homePreferences,
            defaultBoardName = applicationContext.getString(R.string.todo_page_title),
            applicationScope = applicationScope,
        )
    }

    val backupWorkflow by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        MinkBoardLauncherBackupWorkflow(
            facade = launcherFacade,
            envelopeCodec = PassphraseLauncherBackupEnvelopeCodec(),
        )
    }

    val backupCoordinator by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        MinkBoardLauncherBackupCoordinator(
            workflow = backupWorkflow,
            facade = launcherFacade,
            homePreferences = homePreferences,
        )
    }

    private val focusTimerScheduler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AlarmManagerFocusTimerScheduler(applicationContext)
    }

    val focusTimerNotifier by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AndroidFocusTimerNotifier(
            applicationContext,
            FocusNotificationText(
                runningChannelName = applicationContext.getString(R.string.minkboard_focus_running_channel_name),
                runningChannelDescription = applicationContext.getString(R.string.minkboard_focus_running_channel_description),
                finishedChannelName = applicationContext.getString(R.string.minkboard_focus_finished_channel_name),
                finishedChannelDescription = applicationContext.getString(R.string.minkboard_focus_finished_channel_description),
                genericRunningTitle = applicationContext.getString(R.string.minkboard_focus_generic_running_title),
                genericFinishedTitle = applicationContext.getString(R.string.minkboard_focus_generic_finished_title),
                focusingTitle = { applicationContext.getString(R.string.minkboard_focus_running_title, it) },
                finishedTitle = { applicationContext.getString(R.string.minkboard_focus_finished_title, it) },
                runningBody = applicationContext.getString(R.string.minkboard_focus_running_body),
                finishedBody = applicationContext.getString(R.string.minkboard_focus_finished_body),
                stopAction = applicationContext.getString(R.string.minkboard_focus_stop_action),
                dismissAction = applicationContext.getString(R.string.minkboard_focus_dismiss_action),
            ),
        )
    }

    val focusTimerCoordinator by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        FocusTimerCoordinator(repository, focusTimerScheduler, focusTimerNotifier)
    }

    fun onBackgrounded() {
        boardAccessSessions.onBackgrounded()
    }

    fun onForegrounded() {
        if (boardAccessSessions.onForegrounded()) markBoardAccessChanged()
    }

    fun markBoardAccessChanged() {
        mutableBoardAccessRevision.value++
    }

    fun handleNavigationIntent(intent: Intent?) {
        if (intent?.action != FocusTimerIntents.ACTION_OPEN) return
        val timerId = intent.getStringExtra(FocusTimerIntents.EXTRA_TIMER_ID)
            ?.let { value -> runCatching { FocusTimerId(value) }.getOrNull() } ?: return
        val boardId = intent.getStringExtra(FocusTimerIntents.EXTRA_BOARD_ID)
            ?.let { value -> runCatching { BoardId(value) }.getOrNull() } ?: return
        val cardId = intent.getStringExtra(FocusTimerIntents.EXTRA_CARD_ID)
            ?.let { value -> runCatching { CardId(value) }.getOrNull() } ?: return
        applicationScope.launch {
            val current = repository.currentFocusTimer()?.session ?: return@launch
            if (current.id != timerId || current.boardId != boardId || current.cardId != cardId) {
                return@launch
            }
            if (current.state == FocusSessionState.Finished) {
                focusTimerCoordinator.dismiss(timerId)
            }
            requestNavigation(boardId, cardId)
        }
    }

    fun requestHomeBoardNavigation() {
        applicationScope.launch {
            if (launcherController.initialize() != LauncherBoardInitializationState.Ready) return@launch
            val designation = (launcherController.boardState.value as? LauncherBoardState.Ready)
                ?.designation ?: return@launch
            requestNavigation(designation.boardId)
        }
    }

    fun requestNavigation(boardId: BoardId, cardId: CardId? = null) {
        mutableNavigationRequest.value = OpenMinkBoardNavigationRequest(
            token = ++nextNavigationToken,
            boardId = boardId,
            cardId = cardId,
        )
    }

    fun consumeNavigationRequest(token: Long) {
        if (mutableNavigationRequest.value?.token == token) {
            mutableNavigationRequest.value = null
        }
    }
}

internal data class OpenMinkBoardNavigationRequest(
    val token: Long,
    val boardId: BoardId,
    val cardId: CardId?,
)

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

        val result = launcherFacade.addToIntake(text)
        if (result is LauncherCardAddResult.Added) {
            // MinkBoard appends generic cards, but launcher To-Dos have a long-standing
            // newest-first contract. Resolve the card's actual status from a fresh transactional
            // snapshot instead of trusting a possibly stale designation flow, then move only the
            // newly-created card to the front. The shared MinkBoard modules remain unchanged.
            // The card already exists once addToIntake returns Added. Treat the move as a
            // best-effort launcher ordering step: surfacing a move failure as an add failure
            // would invite a retry that creates a duplicate of the successfully inserted card.
            runCatching {
                val insertedCard = repository.launcherBoardHostSnapshot()
                    .workspace
                    .boards
                    .asSequence()
                    .flatMap { board -> board.cards.asSequence() }
                    .firstOrNull { card -> card.id == result.cardId }
                if (insertedCard != null) {
                    repository.moveCard(
                        cardId = result.cardId,
                        destination = insertedCard.statusId,
                        position = 0,
                    )
                }
            }
        }
        result
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
            val state = boardState.value as? LauncherBoardState.Ready
                ?: return@withLock LauncherCardCompletionResult.BoardUnavailable
            val card = state.board.cards.firstOrNull { it.id == cardId }
                ?: return@withLock LauncherCardCompletionResult.CardMissing
            val currentStatus = state.board.statuses.firstOrNull { it.id == card.statusId }
                ?: return@withLock LauncherCardCompletionResult.CardMissing

            runCatching {
                val destination = if (currentStatus.isDoneStatus) {
                    state.designation.intakeStatusId
                } else {
                    state.board.statuses.firstOrNull { it.isDoneStatus }?.id
                        ?: repository.addStatus(
                            state.board.board.id,
                            MinkBoardDefaults.statuses.first { it.isDone }.let { done ->
                                StatusDraft(
                                    name = done.name,
                                    colorArgb = done.colorArgb,
                                    isDoneStatus = true,
                                )
                            },
                        )
                }
                repository.moveCard(cardId, destination)
                LauncherCardCompletionResult.Updated
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

private const val LEGACY_LAUNCHER_PREFERENCES = "mini_launch"
