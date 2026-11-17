package com.katoaapps.minkboard.data

import android.content.Context
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.katoaapps.minkboard.core.MinkBoardDefaults
import com.katoaapps.minkboard.core.FocusTimerStore
import com.katoaapps.minkboard.core.LauncherBoardBackupDesignation
import com.katoaapps.minkboard.core.LauncherBoardDesignation
import com.katoaapps.minkboard.core.LauncherBoardIntegrationStore
import com.katoaapps.minkboard.core.LauncherBoardHostSnapshot
import com.katoaapps.minkboard.core.LauncherBindingProblem
import com.katoaapps.minkboard.core.LauncherBackupCreationResult
import com.katoaapps.minkboard.core.LauncherBackupRestoreContext
import com.katoaapps.minkboard.core.LauncherBackupRestorePlan
import com.katoaapps.minkboard.core.LauncherBoundIntakeWriteResult
import com.katoaapps.minkboard.core.LauncherExistingBoardDesignationResult
import com.katoaapps.minkboard.core.LauncherFreshBoardDesignationResult
import com.katoaapps.minkboard.core.LauncherFreshBoardRequest
import com.katoaapps.minkboard.core.LauncherLegacyCompatibilityReceipt
import com.katoaapps.minkboard.core.LauncherMinkBoardBackup
import com.katoaapps.minkboard.core.LauncherTodoCompatibilityState
import com.katoaapps.minkboard.core.LauncherTodoMigrationPlan
import com.katoaapps.minkboard.core.LauncherWorkspaceImportOutcome
import com.katoaapps.minkboard.core.LauncherWorkspaceIntegrationStore
import com.katoaapps.minkboard.core.MinkBoardRepository
import com.katoaapps.minkboard.core.MinkBoardRules
import com.katoaapps.minkboard.core.MinkBoardTemplates
import com.katoaapps.minkboard.core.PortableArchiveValidator
import com.katoaapps.minkboard.core.WorkspaceArchiveStore
import com.katoaapps.minkboard.core.WorkspaceImportIdMap
import com.katoaapps.minkboard.core.WorkspaceImportMode
import com.katoaapps.minkboard.core.WorkspaceImportResult
import com.katoaapps.minkboard.core.toPortableSnapshot
import com.katoaapps.minkboard.core.BoardTemplateId
import com.katoaapps.minkboard.core.BoardTemplateStatus
import com.katoaapps.minkboard.model.BoardDraft
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardUpdate
import com.katoaapps.minkboard.model.CardDraft
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.CardUpdate
import com.katoaapps.minkboard.model.MinkBoardWorkspace
import com.katoaapps.minkboard.model.PortableBoardSnapshot
import com.katoaapps.minkboard.model.PortableWorkspaceSnapshot
import com.katoaapps.minkboard.model.FocusSession
import com.katoaapps.minkboard.model.FocusSessionState
import com.katoaapps.minkboard.model.FocusTimerId
import com.katoaapps.minkboard.model.FocusTimerSnapshot
import com.katoaapps.minkboard.model.StatusDraft
import com.katoaapps.minkboard.model.StatusDeletionPolicy
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.minkboard.model.StatusUpdate
import com.katoaapps.minkboard.model.SubtaskId
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class RoomMinkBoardRepository internal constructor(
    private val database: MinkBoardDatabase,
    private val cipher: FieldCipher,
    private val idFactory: () -> String,
    private val clockMillis: () -> Long,
) : MinkBoardRepository,
    FocusTimerStore,
    WorkspaceArchiveStore,
    LauncherBoardIntegrationStore,
    LauncherWorkspaceIntegrationStore {
    private val dao = database.boardDao()
    private val entityMapper = EncryptedEntityMapper(cipher)
    private val archiveValidator = PortableArchiveValidator()

    override fun observeWorkspace(): Flow<MinkBoardWorkspace> = dao.observeWorkspace()
        .map { graphs ->
            MinkBoardWorkspace(
                boards = graphs.map(entityMapper::mapBoard).sortedBy { it.board.position },
            )
        }
        .flowOn(Dispatchers.Default)

    override fun observeFocusSession(): Flow<FocusSession?> = dao.observeFocusSession()
        .map { it?.toModel() }
        .flowOn(Dispatchers.Default)

    override fun observeLauncherBoardHostSnapshot(): Flow<LauncherBoardHostSnapshot> =
        dao.observeLauncherHostInvalidations(LAUNCHER_HOST_INVALIDATION_QUERY)
            .map { database.withTransaction { launcherBoardHostSnapshotInTransaction() } }
            .flowOn(Dispatchers.Default)

    override suspend fun launcherBoardHostSnapshot(): LauncherBoardHostSnapshot =
        database.withTransaction { launcherBoardHostSnapshotInTransaction() }

    override suspend fun launcherBoardDesignation(): LauncherBoardDesignation? =
        database.withTransaction {
            dao.launcherBoardDesignation()?.toModel()
        }

    /** Caller must already hold [database]'s transaction. */
    private suspend fun launcherBoardHostSnapshotInTransaction(): LauncherBoardHostSnapshot {
        val workspace = MinkBoardWorkspace(
            boards = dao.workspace().map(entityMapper::mapBoard)
                .sortedBy { board -> board.board.position },
        )
        return LauncherBoardHostSnapshot(
            workspace = workspace,
            designation = dao.launcherBoardDesignation()?.toModel(),
        )
    }

    override suspend fun createAndDesignateFreshBoard(
        request: LauncherFreshBoardRequest,
    ): LauncherFreshBoardDesignationResult = database.withTransaction {
        dao.launcherBoardDesignation()?.toModel()?.let { existing ->
            return@withTransaction designationProblem(existing)?.let { problem ->
                LauncherFreshBoardDesignationResult.BrokenExistingDesignation(existing, problem)
            } ?: LauncherFreshBoardDesignationResult.AlreadyDesignated(existing)
        }

        val boardId = createBoardGraph(
            boardName = MinkBoardRules.boardName(request.boardName),
            isLocked = request.isLocked,
            boardPosition = dao.maxBoardPosition() + 1,
        )
        val intake = dao.statusIds(boardId.value)
            .map { statusId -> requireNotNull(dao.status(statusId)) }
            .first { !it.isDone }
        val designatedAt = clockMillis()
        val designation = LauncherBoardDesignation(
            generation = freshDesignationGeneration(previousGeneration = null),
            boardId = boardId,
            intakeStatusId = StatusId(intake.id),
            designatedAtEpochMillis = designatedAt,
            legacyCompatibility = null,
        )
        dao.insertLauncherBoardDesignation(designation.toEntity())
        LauncherFreshBoardDesignationResult.Created(designation)
    }

    override suspend fun designateExistingBoard(
        boardId: BoardId,
        intakeStatusId: StatusId,
        expectedGeneration: String?,
    ): LauncherExistingBoardDesignationResult = database.withTransaction {
        val existing = dao.launcherBoardDesignation()?.toModel()
        if (existing?.boardId == boardId && existing.intakeStatusId == intakeStatusId) {
            designationProblem(existing)?.let { problem ->
                return@withTransaction LauncherExistingBoardDesignationResult
                    .BrokenExistingDesignation(existing, problem)
            }
            return@withTransaction LauncherExistingBoardDesignationResult
                .AlreadyDesignated(existing)
        }
        if (existing?.generation != expectedGeneration) {
            return@withTransaction LauncherExistingBoardDesignationResult.StaleDesignation(existing)
        }

        designationTargetProblem(boardId, intakeStatusId)?.let { problem ->
            return@withTransaction LauncherExistingBoardDesignationResult.InvalidTarget(problem)
        }
        val designation = LauncherBoardDesignation(
            generation = freshDesignationGeneration(existing?.generation),
            boardId = boardId,
            intakeStatusId = intakeStatusId,
            designatedAtEpochMillis = clockMillis(),
            legacyCompatibility = null,
        )
        dao.upsertLauncherBoardDesignation(designation.toEntity())
        LauncherExistingBoardDesignationResult.Designated(designation)
    }

    override suspend fun addCardToBoundIntake(
        draft: CardDraft,
    ): LauncherBoundIntakeWriteResult = database.withTransaction {
        val designation = dao.launcherBoardDesignation()?.toModel()
            ?: return@withTransaction LauncherBoundIntakeWriteResult.Unbound
        val board = dao.board(designation.boardId.value)
            ?: return@withTransaction LauncherBoundIntakeWriteResult.BrokenBinding(
                LauncherBindingProblem.BOARD_MISSING,
            )
        val intake = dao.status(designation.intakeStatusId.value)
            ?: return@withTransaction LauncherBoundIntakeWriteResult.BrokenBinding(
                LauncherBindingProblem.INTAKE_STATUS_MISSING,
            )
        if (intake.boardId != board.id) {
            return@withTransaction LauncherBoundIntakeWriteResult.BrokenBinding(
                LauncherBindingProblem.INTAKE_STATUS_BELONGS_TO_ANOTHER_BOARD,
            )
        }
        if (intake.isDone) {
            return@withTransaction LauncherBoundIntakeWriteResult.BrokenBinding(
                LauncherBindingProblem.INTAKE_STATUS_IS_DONE,
            )
        }
        LauncherBoundIntakeWriteResult.Added(
            insertCardInTransaction(designation.intakeStatusId, draft),
        )
    }

    override suspend fun commitLauncherTodoMigration(
        plan: LauncherTodoMigrationPlan,
    ): LauncherBoardDesignation = database.withTransaction {
        dao.launcherBoardDesignation()?.let { return@withTransaction it.toModel() }
        validateLauncherMigrationPlan(plan)
        archiveValidator.validate(
            MinkBoardWorkspace(listOf(plan.board)).toPortableSnapshot(
                exportedAtEpochMillis = checkNotNull(plan.designation.legacyCompatibility)
                    .migratedAtEpochMillis,
                sourceApp = LAUNCHER_MIGRATION_SOURCE,
            ),
        )

        // A launcher can normalize To-Dos after MinkBoard data already exists (for example after
        // a restore). Preserve a valid legacy UUID when it is genuinely free, but never let an ID
        // collision abort the otherwise atomic migration or overwrite an existing graph.
        val collisionFreePlan = remapLauncherMigrationCollisions(plan)

        val destinationPosition = dao.maxBoardPosition() + 1
        val destinationBoard = collisionFreePlan.board.copy(
            board = collisionFreePlan.board.board.copy(position = destinationPosition),
        )
        // The graph was validated above while its standalone position was zero. Assigning its
        // final append position does not change graph validity, but a one-board archive validator
        // would (correctly) reject an isolated board whose position begins at one or greater.
        val portableBoard = MinkBoardWorkspace(listOf(destinationBoard)).toPortableSnapshot(
            exportedAtEpochMillis = checkNotNull(collisionFreePlan.designation.legacyCompatibility)
                .migratedAtEpochMillis,
            sourceApp = LAUNCHER_MIGRATION_SOURCE,
        ).boards.single()
        insertPortableBoard(portableBoard)
        dao.insertLauncherBoardDesignation(collisionFreePlan.designation.toEntity())
        collisionFreePlan.designation
    }

    override suspend fun recordLegacyProjectionWritten(
        generation: String,
        boardId: BoardId,
        fingerprint: String,
        expectedPreviousFingerprint: String?,
    ): Boolean {
        requireFingerprint(fingerprint)
        expectedPreviousFingerprint?.let(::requireFingerprint)
        return database.withTransaction {
            dao.setLauncherProjectionFingerprint(
                generation = generation,
                boardId = boardId.value,
                fingerprint = fingerprint,
                expectedPreviousFingerprint = expectedPreviousFingerprint,
            ) == 1
        }
    }

    override suspend fun finishLegacyCompatibility(
        generation: String,
        boardId: BoardId,
    ): Boolean = database.withTransaction {
        dao.setLauncherCompatibilityState(
            generation = generation,
            boardId = boardId.value,
            state = LauncherTodoCompatibilityState.CANONICAL_FINAL.name,
        ) == 1
    }

    override suspend fun createPortableSnapshot(sourceApp: String): PortableWorkspaceSnapshot =
        database.withTransaction {
            createPortableSnapshotInTransaction(sourceApp).snapshot
        }

    override suspend fun createLauncherBackupSnapshot(
        sourceApp: String,
    ): LauncherBackupCreationResult = database.withTransaction {
        val captured = createPortableSnapshotInTransaction(sourceApp)
        val designation = dao.launcherBoardDesignation()?.toModel()

        if (captured.workspace.boards.isEmpty() && designation == null) {
            return@withTransaction LauncherBackupCreationResult.Ready(
                LauncherMinkBoardBackup.Present(captured.snapshot, designation = null),
            )
        }
        designation ?: return@withTransaction LauncherBackupCreationResult.UnboundWorkspace

        val board = captured.workspace.board(designation.boardId)
            ?: return@withTransaction LauncherBackupCreationResult.BrokenBinding(
                LauncherBindingProblem.BOARD_MISSING,
            )
        val intake = board.statuses.firstOrNull { it.id == designation.intakeStatusId }
            ?: return@withTransaction LauncherBackupCreationResult.BrokenBinding(
                LauncherBindingProblem.INTAKE_STATUS_MISSING,
            )
        if (intake.boardId != designation.boardId) {
            return@withTransaction LauncherBackupCreationResult.BrokenBinding(
                LauncherBindingProblem.INTAKE_STATUS_BELONGS_TO_ANOTHER_BOARD,
            )
        }
        if (intake.isDoneStatus) {
            return@withTransaction LauncherBackupCreationResult.BrokenBinding(
                LauncherBindingProblem.INTAKE_STATUS_IS_DONE,
            )
        }

        LauncherBackupCreationResult.Ready(
            LauncherMinkBoardBackup.Present(
                workspace = captured.snapshot,
                designation = LauncherBoardBackupDesignation(
                    boardId = designation.boardId,
                    intakeStatusId = designation.intakeStatusId,
                ),
            ),
        )
    }

    override suspend fun importPortableSnapshot(
        snapshot: PortableWorkspaceSnapshot,
        mode: WorkspaceImportMode,
    ): WorkspaceImportResult {
        val validated = archiveValidator.validate(snapshot)
        require(mode != WorkspaceImportMode.Replace || validated.boards.isNotEmpty()) {
            "Replace import requires at least one board"
        }

        return database.withTransaction { importValidatedWorkspaceInTransaction(validated, mode) }
    }

    override suspend fun importLauncherBackupSnapshot(
        plan: LauncherBackupRestorePlan.ImportWorkspace,
        mode: WorkspaceImportMode,
        context: LauncherBackupRestoreContext,
    ): LauncherWorkspaceImportOutcome.Imported {
        val validated = archiveValidator.validate(plan.workspace)
        // Present-empty has explicit non-destructive reconciliation semantics and must never reach
        // the mutating rich-import path, especially when the host requests Replace.
        require(validated.boards.isNotEmpty()) {
            "An explicitly empty launcher workspace must leave the destination untouched"
        }
        requirePortableDesignation(validated, plan.portableDesignation)

        return database.withTransaction {
            val importResult = importValidatedWorkspaceInTransaction(validated, mode)
            val boardId = importResult.ids.boardIds[plan.portableDesignation.boardId.value]
                ?: error("Launcher board was not included in the imported workspace")
            val intakeStatusId = importResult.ids.statusIds[plan.portableDesignation.intakeStatusId.value]
                ?: error("Launcher intake status was not included in the imported workspace")
            val importedIntake = requireNotNull(dao.status(intakeStatusId.value)) {
                "Imported launcher intake status is missing"
            }
            check(importedIntake.boardId == boardId.value) {
                "Imported launcher intake status belongs to another board"
            }
            check(!importedIntake.isDone) {
                "Imported launcher intake status cannot be Done"
            }

            val previousGeneration = dao.launcherBoardDesignation()?.generation
            val compatibility = context.localLegacyCompatibility?.let { local ->
                LauncherLegacyCompatibilityReceipt(
                    state = LauncherTodoCompatibilityState.CANONICAL_COMPAT,
                    sourceSchemaVersion = local.sourceSchemaVersion,
                    sourceFingerprint = local.sourceFingerprint,
                    lastProjectionFingerprint = null,
                    migratedAtEpochMillis = context.restoredAtEpochMillis,
                )
            }
            val localDesignation = LauncherBoardDesignation(
                generation = freshDesignationGeneration(previousGeneration),
                boardId = boardId,
                intakeStatusId = intakeStatusId,
                designatedAtEpochMillis = context.restoredAtEpochMillis,
                legacyCompatibility = compatibility,
            )
            dao.upsertLauncherBoardDesignation(localDesignation.toEntity())
            LauncherWorkspaceImportOutcome.Imported(
                importResult = importResult,
                launcherDesignation = localDesignation,
            )
        }
    }

    override suspend fun ensureDefaultWorkspace() {
        database.withTransaction {
            if (dao.boardCount() == 0) {
                createBoardGraph(
                    boardName = "My first MinkBoard",
                    isLocked = false,
                    boardPosition = 0,
                )
            }
        }
    }

    /** Caller must already hold [database]'s transaction. */
    private suspend fun createPortableSnapshotInTransaction(
        sourceApp: String,
    ): CapturedWorkspace {
        val workspace = MinkBoardWorkspace(
            boards = dao.workspace().map(entityMapper::mapBoard)
                .sortedBy { it.board.position },
        )
        val snapshot = archiveValidator.validate(
            workspace.toPortableSnapshot(
                exportedAtEpochMillis = clockMillis(),
                sourceApp = sourceApp,
            ),
        )
        return CapturedWorkspace(workspace, snapshot)
    }

    /** Caller must already hold [database]'s transaction. */
    private suspend fun importValidatedWorkspaceInTransaction(
        validated: PortableWorkspaceSnapshot,
        mode: WorkspaceImportMode,
    ): WorkspaceImportResult {
        val prepared = when (mode) {
            WorkspaceImportMode.Merge -> remapForMerge(validated)
            WorkspaceImportMode.Replace -> PreparedWorkspace(
                snapshot = validated,
                ids = validated.identityImportIdMap(),
            )
        }
        if (mode == WorkspaceImportMode.Replace) dao.deleteAllBoards()

        prepared.snapshot.boards.sortedBy { it.position }.forEach { board ->
            insertPortableBoard(board)
        }
        return WorkspaceImportResult(
            firstBoardId = prepared.snapshot.boards.minByOrNull { it.position }
                ?.let { BoardId(it.id) },
            ids = prepared.ids,
        )
    }

    private fun requirePortableDesignation(
        workspace: PortableWorkspaceSnapshot,
        designation: LauncherBoardBackupDesignation,
    ) {
        val board = workspace.boards.firstOrNull { it.id == designation.boardId.value }
        requireNotNull(board) { "Launcher designation board is absent from the workspace" }
        val intake = board.statuses.firstOrNull { it.id == designation.intakeStatusId.value }
        requireNotNull(intake) { "Launcher designation intake status is absent from its board" }
        require(!intake.isDoneStatus) { "Launcher designation intake status cannot be Done" }
    }

    override suspend fun createBoard(draft: BoardDraft): BoardId = database.withTransaction {
        createBoardGraph(
            boardName = MinkBoardRules.boardName(draft.name),
            isLocked = draft.isLocked,
            boardPosition = dao.maxBoardPosition() + 1,
        )
    }

    override suspend fun createBoardFromTemplate(
        draft: BoardDraft,
        templateId: BoardTemplateId,
    ): BoardId = database.withTransaction {
        val template = MinkBoardTemplates.require(templateId)
        createBoardGraph(
            boardName = MinkBoardRules.boardName(draft.name),
            isLocked = draft.isLocked,
            boardPosition = dao.maxBoardPosition() + 1,
            statuses = template.statuses,
        )
    }

    override suspend fun renameBoard(boardId: BoardId, name: String) {
        database.withTransaction {
            val board = requireNotNull(dao.board(boardId.value)) { "Unknown board" }
            dao.updateBoard(
                board.copy(
                    encryptedName = cipher.encrypt(
                        MinkBoardRules.boardName(name),
                        boardSubject(board.id),
                        FIELD_NAME,
                    ),
                ),
            )
        }
    }

    override suspend fun updateBoard(boardId: BoardId, update: BoardUpdate) {
        database.withTransaction {
            val board = requireNotNull(dao.board(boardId.value)) { "Unknown board" }
            dao.updateBoard(
                board.copy(
                    encryptedName = cipher.encrypt(
                        MinkBoardRules.boardName(update.name),
                        boardSubject(board.id),
                        FIELD_NAME,
                    ),
                    isLocked = update.isLocked,
                ),
            )
        }
    }

    override suspend fun reorderBoards(orderedBoardIds: List<BoardId>) {
        database.withTransaction {
            val currentIds = dao.boardIds()
            val requestedIds = orderedBoardIds.map(BoardId::value)
            requireCompleteOrder("boards", currentIds, requestedIds)
            requestedIds.forEachIndexed { position, id ->
                dao.updateBoardPosition(id, position)
            }
        }
    }

    override suspend fun deleteBoard(boardId: BoardId) = deleteBoards(setOf(boardId))

    override suspend fun deleteBoards(boardIds: Set<BoardId>) {
        database.withTransaction {
            require(boardIds.isNotEmpty()) { "Choose at least one board" }
            val requestedIds = boardIds.map(BoardId::value).toSet()
            val currentIds = dao.boardIds()
            require(currentIds.containsAll(requestedIds)) { "Unknown board" }
            check(currentIds.size > requestedIds.size) {
                "A workspace must keep at least one board"
            }
            requestedIds.forEach { id -> dao.deleteBoard(id) }
            dao.boardIds().forEachIndexed { position, id ->
                dao.updateBoardPosition(id, position)
            }
        }
    }

    override suspend fun addStatus(boardId: BoardId, draft: StatusDraft): StatusId =
        database.withTransaction {
            requireNotNull(dao.board(boardId.value)) { "Unknown board" }
            val id = StatusId(idFactory())
            if (draft.isDoneStatus) dao.clearDoneStatus(boardId.value)
            dao.insertStatus(
                StatusEntity(
                    id = id.value,
                    boardId = boardId.value,
                    encryptedName = cipher.encrypt(
                        MinkBoardRules.statusName(draft.name),
                        subject = statusSubject(id.value),
                        field = FIELD_NAME,
                    ),
                    colorArgb = draft.colorArgb,
                    isDone = draft.isDoneStatus,
                    position = dao.maxStatusPosition(boardId.value) + 1,
                ),
            )
            id
        }

    override suspend fun updateStatus(statusId: StatusId, update: StatusUpdate) {
        database.withTransaction {
            val status = requireNotNull(dao.status(statusId.value)) { "Unknown status" }
            if (update.isDoneStatus) dao.clearDoneStatus(status.boardId)
            dao.updateStatus(
                status.copy(
                    encryptedName = cipher.encrypt(
                        MinkBoardRules.statusName(update.name),
                        statusSubject(status.id),
                        FIELD_NAME,
                    ),
                    colorArgb = update.colorArgb,
                    isDone = update.isDoneStatus,
                ),
            )
        }
    }

    override suspend fun reorderStatuses(
        boardId: BoardId,
        orderedStatusIds: List<StatusId>,
    ) {
        database.withTransaction {
            requireNotNull(dao.board(boardId.value)) { "Unknown board" }
            val currentIds = dao.statusIds(boardId.value)
            val requestedIds = orderedStatusIds.map(StatusId::value)
            requireCompleteOrder("statuses", currentIds, requestedIds)
            requestedIds.forEachIndexed { position, id ->
                dao.updateStatusPosition(id, position)
            }
        }
    }

    override suspend fun deleteStatus(statusId: StatusId, policy: StatusDeletionPolicy) {
        database.withTransaction {
            val status = requireNotNull(dao.status(statusId.value)) { "Unknown status" }
            check(dao.statusIds(status.boardId).size > 1) {
                "A board must keep at least one status"
            }

            when (policy) {
                StatusDeletionPolicy.DeleteCards -> Unit
                is StatusDeletionPolicy.MoveCardsTo -> {
                    val destination = requireNotNull(
                        dao.status(policy.destinationStatusId.value),
                    ) { "Unknown destination status" }
                    require(destination.id != status.id) {
                        "A deleted status cannot be its own destination"
                    }
                    require(destination.boardId == status.boardId) {
                        "Cards cannot move between boards"
                    }

                    val firstDestinationPosition = dao.maxCardPosition(destination.id) + 1
                    val movedAt = clockMillis()
                    dao.cardIds(status.id).forEachIndexed { offset, cardId ->
                        dao.moveCard(
                            cardId = cardId,
                            statusId = destination.id,
                            position = firstDestinationPosition + offset,
                            updatedAt = movedAt,
                        )
                    }
                }
            }

            dao.deleteStatus(status.id)
            dao.statusIds(status.boardId).forEachIndexed { position, id ->
                dao.updateStatusPosition(id, position)
            }
        }
    }

    override suspend fun addCard(statusId: StatusId, draft: CardDraft): CardId =
        database.withTransaction { insertCardInTransaction(statusId, draft) }

    /** Caller must already hold [database]'s transaction. */
    private suspend fun insertCardInTransaction(statusId: StatusId, draft: CardDraft): CardId {
        requireNotNull(dao.status(statusId.value)) { "Unknown destination status" }
        val id = CardId(idFactory())
        val now = clockMillis()
        dao.insertCard(
            CardEntity(
                id = id.value,
                statusId = statusId.value,
                encryptedTitle = cipher.encrypt(
                    MinkBoardRules.cardTitle(draft.title),
                    cardSubject(id.value),
                    FIELD_TITLE,
                ),
                encryptedDetails = cipher.encrypt(
                    MinkBoardRules.cardDetails(draft.details),
                    cardSubject(id.value),
                    FIELD_DETAILS,
                ),
                encryptedDueDate = draft.dueDate?.toString()?.let { date ->
                    cipher.encrypt(date, cardSubject(id.value), FIELD_DUE_DATE)
                },
                isMajor = draft.isMajor,
                position = dao.maxCardPosition(statusId.value) + 1,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            ),
        )
        return id
    }

    override suspend fun cloneCard(cardId: CardId): CardId = database.withTransaction {
        val source = requireNotNull(dao.card(cardId.value)) { "Unknown card" }
        val sourceSubject = cardSubject(source.id)
        val cloneId = CardId(idFactory())
        val cloneSubject = cardSubject(cloneId.value)
        val now = clockMillis()
        val orderedCardIds = dao.cardIds(source.statusId).toMutableList()
        val sourceIndex = orderedCardIds.indexOf(source.id)
        check(sourceIndex >= 0) { "Card order is inconsistent" }
        orderedCardIds.add(sourceIndex + 1, cloneId.value)

        dao.insertCard(
            CardEntity(
                id = cloneId.value,
                statusId = source.statusId,
                encryptedTitle = cipher.reencrypt(
                    source.encryptedTitle,
                    sourceSubject,
                    cloneSubject,
                    FIELD_TITLE,
                ),
                encryptedDetails = cipher.reencrypt(
                    source.encryptedDetails,
                    sourceSubject,
                    cloneSubject,
                    FIELD_DETAILS,
                ),
                encryptedDueDate = source.encryptedDueDate?.let { dueDate ->
                    cipher.reencrypt(
                        dueDate,
                        sourceSubject,
                        cloneSubject,
                        FIELD_DUE_DATE,
                    )
                },
                isMajor = source.isMajor,
                position = orderedCardIds.lastIndex,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            ),
        )
        orderedCardIds.forEachIndexed { position, id ->
            dao.updateCardPosition(id, position)
        }

        dao.subtasks(source.id).forEach { sourceSubtask ->
            val cloneSubtaskId = SubtaskId(idFactory())
            dao.insertSubtask(
                SubtaskEntity(
                    id = cloneSubtaskId.value,
                    cardId = cloneId.value,
                    encryptedText = cipher.reencrypt(
                        sourceSubtask.encryptedText,
                        subtaskSubject(sourceSubtask.id),
                        subtaskSubject(cloneSubtaskId.value),
                        FIELD_TEXT,
                    ),
                    isCompleted = sourceSubtask.isCompleted,
                    position = sourceSubtask.position,
                ),
            )
        }
        cloneId
    }

    override suspend fun updateCard(cardId: CardId, update: CardUpdate) {
        database.withTransaction {
            val current = requireNotNull(dao.card(cardId.value)) { "Unknown card" }
            val subject = cardSubject(cardId.value)
            dao.updateCard(
                current.copy(
                    encryptedTitle = cipher.encrypt(
                        MinkBoardRules.cardTitle(update.title),
                        subject,
                        FIELD_TITLE,
                    ),
                    encryptedDetails = cipher.encrypt(
                        MinkBoardRules.cardDetails(update.details),
                        subject,
                        FIELD_DETAILS,
                    ),
                    encryptedDueDate = update.dueDate?.toString()?.let { date ->
                        cipher.encrypt(date, subject, FIELD_DUE_DATE)
                    },
                    isMajor = update.isMajor,
                    updatedAtEpochMillis = clockMillis(),
                ),
            )
        }
    }

    override suspend fun moveCard(cardId: CardId, destination: StatusId, position: Int) {
        database.withTransaction {
            val card = requireNotNull(dao.card(cardId.value)) { "Unknown card" }
            val sourceStatus = requireNotNull(dao.status(card.statusId)) { "Unknown source status" }
            val destinationStatus = requireNotNull(dao.status(destination.value)) {
                "Unknown destination status"
            }
            require(sourceStatus.boardId == destinationStatus.boardId) {
                "Cards cannot move between boards"
            }

            val sourceIds = dao.cardIds(card.statusId).filterNot { it == cardId.value }
            val destinationIds = if (card.statusId == destination.value) {
                sourceIds.toMutableList()
            } else {
                dao.cardIds(destination.value).filterNot { it == cardId.value }.toMutableList()
            }
            val destinationIndex = position.coerceIn(0, destinationIds.size)
            destinationIds.add(destinationIndex, cardId.value)

            if (card.statusId != destination.value) {
                sourceIds.forEachIndexed { index, id -> dao.updateCardPosition(id, index) }
            }
            destinationIds.forEachIndexed { index, id ->
                if (id == cardId.value) {
                    dao.moveCard(id, destination.value, index, clockMillis())
                } else {
                    dao.updateCardPosition(id, index)
                }
            }
        }
    }

    override suspend fun deleteCard(cardId: CardId) {
        database.withTransaction {
            val card = dao.card(cardId.value) ?: return@withTransaction
            dao.deleteCard(cardId.value)
            dao.cardIds(card.statusId).forEachIndexed { index, id ->
                dao.updateCardPosition(id, index)
            }
        }
    }

    override suspend fun addSubtask(cardId: CardId, text: String): SubtaskId =
        database.withTransaction {
            requireNotNull(dao.card(cardId.value)) { "Unknown card" }
            val id = SubtaskId(idFactory())
            dao.insertSubtask(
                SubtaskEntity(
                    id = id.value,
                    cardId = cardId.value,
                    encryptedText = cipher.encrypt(
                        MinkBoardRules.subtaskText(text),
                        subtaskSubject(id.value),
                        FIELD_TEXT,
                    ),
                    isCompleted = false,
                    position = dao.maxSubtaskPosition(cardId.value) + 1,
                ),
            )
            dao.touchCard(cardId.value, clockMillis())
            id
        }

    override suspend fun setSubtaskCompleted(subtaskId: SubtaskId, completed: Boolean) {
        database.withTransaction {
            val subtask = requireNotNull(dao.subtask(subtaskId.value)) { "Unknown subtask" }
            dao.setSubtaskCompleted(subtaskId.value, completed)
            dao.touchCard(subtask.cardId, clockMillis())
        }
    }

    override suspend fun updateSubtask(subtaskId: SubtaskId, text: String) {
        database.withTransaction {
            val subtask = requireNotNull(dao.subtask(subtaskId.value)) { "Unknown subtask" }
            dao.updateSubtaskText(
                subtaskId = subtaskId.value,
                encryptedText = cipher.encrypt(
                    MinkBoardRules.subtaskText(text),
                    subtaskSubject(subtaskId.value),
                    FIELD_TEXT,
                ),
            )
            dao.touchCard(subtask.cardId, clockMillis())
        }
    }

    override suspend fun deleteSubtask(subtaskId: SubtaskId) {
        database.withTransaction {
            val subtask = dao.subtask(subtaskId.value) ?: return@withTransaction
            dao.deleteSubtask(subtaskId.value)
            dao.subtaskIds(subtask.cardId).forEachIndexed { index, id ->
                dao.updateSubtaskPosition(id, index)
            }
            dao.touchCard(subtask.cardId, clockMillis())
        }
    }

    override suspend fun currentFocusTimer(): FocusTimerSnapshot? = database.withTransaction {
        dao.focusSession()?.let { focusSnapshot(it) }
    }

    override suspend fun replaceFocusTimer(session: FocusSession): FocusTimerSnapshot =
        database.withTransaction {
            val board = requireNotNull(dao.board(session.boardId.value)) { "Unknown timer board" }
            val card = requireNotNull(dao.card(session.cardId.value)) { "Unknown timer card" }
            val status = requireNotNull(dao.status(card.statusId)) { "Unknown timer status" }
            require(status.boardId == board.id) { "Timer card does not belong to its board" }
            dao.replaceFocusSession(session.toEntity())
            focusSnapshot(session.toEntity())
        }

    override suspend fun finishFocusTimer(timerId: FocusTimerId): FocusTimerSnapshot? =
        database.withTransaction {
            if (dao.setFocusSessionState(timerId.value, FOCUS_STATE_FINISHED) == 0) {
                return@withTransaction null
            }
            dao.focusSession()?.let { focusSnapshot(it) }
        }

    override suspend fun clearFocusTimer(timerId: FocusTimerId): Boolean =
        database.withTransaction { dao.deleteFocusSession(timerId.value) > 0 }

    private suspend fun focusSnapshot(entity: FocusSessionEntity): FocusTimerSnapshot {
        val card = requireNotNull(dao.card(entity.cardId)) { "Timer card is missing" }
        val board = requireNotNull(dao.board(entity.boardId)) { "Timer board is missing" }
        return FocusTimerSnapshot(
            session = entity.toModel(),
            cardTitle = cipher.decrypt(
                card.encryptedTitle,
                cardSubject(card.id),
                FIELD_TITLE,
            ),
            boardIsLocked = board.isLocked,
        )
    }

    /**
     * Builds the complete merge graph before the first insert. Incoming IDs are never reused,
     * even when an archive happens to contain an ID generated by [idFactory].
     */
    private suspend fun remapForMerge(
        snapshot: PortableWorkspaceSnapshot,
    ): PreparedWorkspace {
        val usedIds = buildSet {
            dao.workspace().forEach { boardGraph ->
                add(boardGraph.board.id)
                boardGraph.statuses.forEach { statusGraph ->
                    add(statusGraph.status.id)
                    statusGraph.cards.forEach { cardGraph ->
                        add(cardGraph.card.id)
                        cardGraph.subtasks.forEach { add(it.id) }
                    }
                }
            }
            snapshot.boards.forEach { board ->
                add(board.id)
                board.statuses.forEach { status ->
                    add(status.id)
                    status.cards.forEach { card ->
                        add(card.id)
                        card.subtasks.forEach { add(it.id) }
                    }
                }
            }
        }.toMutableSet()

        fun freshId(): String {
            repeat(MAX_ID_GENERATION_ATTEMPTS) {
                val candidate = idFactory()
                if (candidate.isNotBlank() &&
                    candidate.length <= MAX_PORTABLE_ID_LENGTH &&
                    usedIds.add(candidate)
                ) {
                    return candidate
                }
            }
            error("Could not generate a unique archive import ID")
        }

        val firstImportedPosition = dao.maxBoardPosition() + 1
        val boardIds = linkedMapOf<String, BoardId>()
        val statusIds = linkedMapOf<String, StatusId>()
        val cardIds = linkedMapOf<String, CardId>()
        val subtaskIds = linkedMapOf<String, SubtaskId>()
        val remapped = snapshot.copy(
            boards = snapshot.boards.map { board ->
                val destinationBoardId = BoardId(freshId())
                boardIds[board.id] = destinationBoardId
                board.copy(
                    id = destinationBoardId.value,
                    position = firstImportedPosition + board.position,
                    statuses = board.statuses.map { status ->
                        val destinationStatusId = StatusId(freshId())
                        statusIds[status.id] = destinationStatusId
                        status.copy(
                            id = destinationStatusId.value,
                            cards = status.cards.map { card ->
                                val destinationCardId = CardId(freshId())
                                cardIds[card.id] = destinationCardId
                                card.copy(
                                    id = destinationCardId.value,
                                    subtasks = card.subtasks.map { subtask ->
                                        val destinationSubtaskId = SubtaskId(freshId())
                                        subtaskIds[subtask.id] = destinationSubtaskId
                                        subtask.copy(id = destinationSubtaskId.value)
                                    },
                                )
                            },
                        )
                    },
                )
            },
        )
        return PreparedWorkspace(
            snapshot = remapped,
            ids = WorkspaceImportIdMap(
                boardIds = boardIds,
                statusIds = statusIds,
                cardIds = cardIds,
                subtaskIds = subtaskIds,
            ),
        )
    }

    private suspend fun insertPortableBoard(board: PortableBoardSnapshot) {
        dao.insertBoard(
            BoardEntity(
                id = board.id,
                encryptedName = cipher.encrypt(
                    board.name,
                    boardSubject(board.id),
                    FIELD_NAME,
                ),
                isLocked = board.isLocked,
                position = board.position,
                createdAtEpochMillis = board.createdAtEpochMillis,
            ),
        )
        board.statuses.sortedBy { it.position }.forEach { status ->
            dao.insertStatus(
                StatusEntity(
                    id = status.id,
                    boardId = board.id,
                    encryptedName = cipher.encrypt(
                        status.name,
                        statusSubject(status.id),
                        FIELD_NAME,
                    ),
                    colorArgb = status.colorArgb,
                    isDone = status.isDoneStatus,
                    position = status.position,
                ),
            )
            status.cards.sortedBy { it.position }.forEach { card ->
                dao.insertCard(
                    CardEntity(
                        id = card.id,
                        statusId = status.id,
                        encryptedTitle = cipher.encrypt(
                            card.title,
                            cardSubject(card.id),
                            FIELD_TITLE,
                        ),
                        encryptedDetails = cipher.encrypt(
                            card.details,
                            cardSubject(card.id),
                            FIELD_DETAILS,
                        ),
                        encryptedDueDate = card.dueDateIso?.let { dueDate ->
                            cipher.encrypt(
                                dueDate,
                                cardSubject(card.id),
                                FIELD_DUE_DATE,
                            )
                        },
                        isMajor = card.isMajor,
                        position = card.position,
                        createdAtEpochMillis = card.createdAtEpochMillis,
                        updatedAtEpochMillis = card.updatedAtEpochMillis,
                    ),
                )
                card.subtasks.sortedBy { it.position }.forEach { subtask ->
                    dao.insertSubtask(
                        SubtaskEntity(
                            id = subtask.id,
                            cardId = card.id,
                            encryptedText = cipher.encrypt(
                                subtask.text,
                                subtaskSubject(subtask.id),
                                FIELD_TEXT,
                            ),
                            isCompleted = subtask.isCompleted,
                            position = subtask.position,
                        ),
                    )
                }
            }
        }
    }

    private suspend fun createBoardGraph(
        boardName: String,
        isLocked: Boolean,
        boardPosition: Int,
        statuses: List<BoardTemplateStatus> = MinkBoardDefaults.statuses.map { default ->
            BoardTemplateStatus(
                name = default.name,
                colorArgb = default.colorArgb,
                isDoneStatus = default.isDone,
            )
        },
    ): BoardId {
        val boardId = BoardId(idFactory())
        dao.insertBoard(
            BoardEntity(
                id = boardId.value,
                encryptedName = cipher.encrypt(
                    boardName,
                    boardSubject(boardId.value),
                    FIELD_NAME,
                ),
                isLocked = isLocked,
                position = boardPosition,
                createdAtEpochMillis = clockMillis(),
            ),
        )
        dao.insertStatuses(
            statuses.mapIndexed { index, default ->
                val statusId = idFactory()
                StatusEntity(
                    id = statusId,
                    boardId = boardId.value,
                    encryptedName = cipher.encrypt(
                        default.name,
                        statusSubject(statusId),
                        FIELD_NAME,
                    ),
                    colorArgb = default.colorArgb,
                    isDone = default.isDoneStatus,
                    position = index,
                )
            },
        )
        return boardId
    }

    private fun validateLauncherMigrationPlan(plan: LauncherTodoMigrationPlan) {
        val designation = plan.designation
        val compatibility = requireNotNull(designation.legacyCompatibility) {
            "Launcher migration requires legacy compatibility metadata"
        }
        require(plan.board.board.id == designation.boardId) {
            "Launcher designation must identify the migrated board"
        }
        require(plan.board.statuses.size == 2) {
            "Launcher migration requires exactly To do and Done statuses"
        }
        require(plan.board.statuses.count { it.isDoneStatus } == 1) {
            "Launcher migration requires exactly one Done status"
        }
        val intake = plan.board.statuses.firstOrNull { it.id == designation.intakeStatusId }
        require(intake != null && !intake.isDoneStatus) {
            "Launcher intake status must be a non-Done status on the migrated board"
        }
        require(compatibility.state == LauncherTodoCompatibilityState.CANONICAL_COMPAT) {
            "A new launcher migration must start in compatibility mode"
        }
        require(compatibility.sourceSchemaVersion > 0) { "Source schema version must be positive" }
        require(compatibility.lastProjectionFingerprint == null) {
            "A projection cannot be recorded before migration commits"
        }
        requireFingerprint(compatibility.sourceFingerprint)
    }

    /**
     * Remaps only IDs that collide with an existing graph. This differs from archive Merge, which
     * intentionally gives every imported entity a fresh identity. Launcher migration should retain
     * a valid legacy card UUID whenever doing so is safe.
     */
    private suspend fun remapLauncherMigrationCollisions(
        plan: LauncherTodoMigrationPlan,
    ): LauncherTodoMigrationPlan {
        val usedIds = buildSet {
            dao.workspace().forEach { boardGraph ->
                add(boardGraph.board.id)
                boardGraph.statuses.forEach { statusGraph ->
                    add(statusGraph.status.id)
                    statusGraph.cards.forEach { cardGraph ->
                        add(cardGraph.card.id)
                        cardGraph.subtasks.forEach { add(it.id) }
                    }
                }
            }
        }.toMutableSet()

        fun availableOrFresh(sourceId: String): String {
            if (usedIds.add(sourceId)) return sourceId
            repeat(MAX_ID_GENERATION_ATTEMPTS) {
                val candidate = idFactory()
                if (candidate.isNotBlank() &&
                    candidate.length <= MAX_PORTABLE_ID_LENGTH &&
                    usedIds.add(candidate)
                ) {
                    return candidate
                }
            }
            error("Could not generate a unique launcher migration ID")
        }

        val destinationBoardId = BoardId(availableOrFresh(plan.board.board.id.value))
        val statusIds = plan.board.statuses.associate { status ->
            status.id to StatusId(availableOrFresh(status.id.value))
        }
        val cardIds = plan.board.cards.associate { card ->
            card.id to CardId(availableOrFresh(card.id.value))
        }
        val subtaskIds = plan.board.cards
            .flatMap { it.subtasks }
            .associate { subtask ->
                subtask.id to SubtaskId(availableOrFresh(subtask.id.value))
            }

        val destinationBoard = plan.board.copy(
            board = plan.board.board.copy(id = destinationBoardId),
            statuses = plan.board.statuses.map { status ->
                status.copy(
                    id = checkNotNull(statusIds[status.id]),
                    boardId = destinationBoardId,
                )
            },
            cards = plan.board.cards.map { card ->
                val destinationCardId = checkNotNull(cardIds[card.id])
                card.copy(
                    id = destinationCardId,
                    statusId = checkNotNull(statusIds[card.statusId]),
                    subtasks = card.subtasks.map { subtask ->
                        subtask.copy(
                            id = checkNotNull(subtaskIds[subtask.id]),
                            cardId = destinationCardId,
                        )
                    },
                )
            },
        )
        return plan.copy(
            board = destinationBoard,
            designation = plan.designation.copy(
                boardId = destinationBoardId,
                intakeStatusId = checkNotNull(statusIds[plan.designation.intakeStatusId]),
            ),
        )
    }

    private fun requireCompleteOrder(
        kind: String,
        currentIds: List<String>,
        requestedIds: List<String>,
    ) {
        require(
            requestedIds.size == currentIds.size &&
                requestedIds.toSet() == currentIds.toSet(),
        ) { "The $kind order must contain every current ID exactly once" }
    }

    /** Caller must already hold [database]'s transaction. */
    private suspend fun designationProblem(
        designation: LauncherBoardDesignation,
    ): LauncherBindingProblem? = designationTargetProblem(
        boardId = designation.boardId,
        intakeStatusId = designation.intakeStatusId,
    )

    /** Caller must already hold [database]'s transaction. */
    private suspend fun designationTargetProblem(
        boardId: BoardId,
        intakeStatusId: StatusId,
    ): LauncherBindingProblem? {
        val board = dao.board(boardId.value) ?: return LauncherBindingProblem.BOARD_MISSING
        val intake = dao.status(intakeStatusId.value)
            ?: return LauncherBindingProblem.INTAKE_STATUS_MISSING
        if (intake.boardId != board.id) {
            return LauncherBindingProblem.INTAKE_STATUS_BELONGS_TO_ANOTHER_BOARD
        }
        if (intake.isDone) return LauncherBindingProblem.INTAKE_STATUS_IS_DONE
        return null
    }

    private fun freshDesignationGeneration(previousGeneration: String?): String {
        repeat(MAX_ID_GENERATION_ATTEMPTS) {
            val candidate = idFactory()
            if (candidate.isNotBlank() &&
                candidate.length <= MAX_PORTABLE_ID_LENGTH &&
                candidate != previousGeneration
            ) {
                return candidate
            }
        }
        error("Could not generate a fresh launcher designation generation")
    }

    companion object {
        fun create(context: Context): RoomMinkBoardRepository = RoomMinkBoardRepository(
            database = MinkBoardDatabase.create(context),
            cipher = KeystoreFieldCipher(),
            idFactory = { UUID.randomUUID().toString() },
            clockMillis = System::currentTimeMillis,
        )

        private const val MAX_ID_GENERATION_ATTEMPTS = 1_000
        private const val MAX_PORTABLE_ID_LENGTH = 200
        private const val LAUNCHER_MIGRATION_SOURCE = "MinkLauncher legacy To-Dos"
        private val LAUNCHER_HOST_INVALIDATION_QUERY = SimpleSQLiteQuery("SELECT 1")
    }
}

private data class PreparedWorkspace(
    val snapshot: PortableWorkspaceSnapshot,
    val ids: WorkspaceImportIdMap,
)

private data class CapturedWorkspace(
    val workspace: MinkBoardWorkspace,
    val snapshot: PortableWorkspaceSnapshot,
)

private fun PortableWorkspaceSnapshot.identityImportIdMap(): WorkspaceImportIdMap =
    WorkspaceImportIdMap(
        boardIds = boards.associate { board -> board.id to BoardId(board.id) },
        statusIds = boards
            .flatMap { it.statuses }
            .associate { status -> status.id to StatusId(status.id) },
        cardIds = boards
            .flatMap { board -> board.statuses.flatMap { it.cards } }
            .associate { card -> card.id to CardId(card.id) },
        subtaskIds = boards
            .flatMap { board ->
                board.statuses.flatMap { status -> status.cards.flatMap { it.subtasks } }
            }
            .associate { subtask -> subtask.id to SubtaskId(subtask.id) },
    )

private fun LauncherBoardDesignation.toEntity() = LauncherBoardDesignationEntity(
    generation = generation,
    boardId = boardId.value,
    intakeStatusId = intakeStatusId.value,
    designatedAtEpochMillis = designatedAtEpochMillis,
    compatibilityState = legacyCompatibility?.state?.name,
    sourceSchemaVersion = legacyCompatibility?.sourceSchemaVersion,
    sourceFingerprint = legacyCompatibility?.sourceFingerprint,
    lastProjectionFingerprint = legacyCompatibility?.lastProjectionFingerprint,
    migratedAtEpochMillis = legacyCompatibility?.migratedAtEpochMillis,
)

private fun LauncherBoardDesignationEntity.toModel(): LauncherBoardDesignation {
    val requiredCompatibilityFields = listOf(
        compatibilityState,
        sourceSchemaVersion,
        sourceFingerprint,
        migratedAtEpochMillis,
    )
    require(requiredCompatibilityFields.all { it == null } ||
        requiredCompatibilityFields.all { it != null }
    ) {
        "Launcher compatibility metadata must be either complete or absent"
    }
    require(compatibilityState != null || lastProjectionFingerprint == null) {
        "A projection fingerprint requires launcher compatibility metadata"
    }
    val compatibility = compatibilityState?.let { state ->
        LauncherLegacyCompatibilityReceipt(
            state = LauncherTodoCompatibilityState.valueOf(state),
            sourceSchemaVersion = checkNotNull(sourceSchemaVersion),
            sourceFingerprint = checkNotNull(sourceFingerprint),
            lastProjectionFingerprint = lastProjectionFingerprint,
            migratedAtEpochMillis = checkNotNull(migratedAtEpochMillis),
        )
    }
    return LauncherBoardDesignation(
        generation = generation,
        boardId = BoardId(boardId),
        intakeStatusId = StatusId(intakeStatusId),
        designatedAtEpochMillis = designatedAtEpochMillis,
        legacyCompatibility = compatibility,
    )
}

private fun requireFingerprint(value: String) {
    require(value.length == SHA_256_HEX_LENGTH && value.all { it in '0'..'9' || it in 'a'..'f' }) {
        "Fingerprint must be a lowercase SHA-256 value"
    }
}

private const val FOCUS_STATE_RUNNING = "running"
private const val FOCUS_STATE_FINISHED = "finished"
private const val SHA_256_HEX_LENGTH = 64

private fun FocusSession.toEntity() = FocusSessionEntity(
    timerId = id.value,
    boardId = boardId.value,
    cardId = cardId.value,
    startedAtEpochMillis = startedAtEpochMillis,
    endsAtEpochMillis = endsAtEpochMillis,
    state = when (state) {
        FocusSessionState.Running -> FOCUS_STATE_RUNNING
        FocusSessionState.Finished -> FOCUS_STATE_FINISHED
    },
)

private fun FocusSessionEntity.toModel() = FocusSession(
    id = FocusTimerId(timerId),
    boardId = BoardId(boardId),
    cardId = CardId(cardId),
    startedAtEpochMillis = startedAtEpochMillis,
    endsAtEpochMillis = endsAtEpochMillis,
    state = when (state) {
        FOCUS_STATE_RUNNING -> FocusSessionState.Running
        FOCUS_STATE_FINISHED -> FocusSessionState.Finished
        else -> error("Unknown focus timer state")
    },
)

private fun FieldCipher.reencrypt(
    ciphertext: ByteArray,
    sourceSubject: String,
    destinationSubject: String,
    field: String,
): ByteArray = encrypt(
    value = decrypt(ciphertext, sourceSubject, field),
    subject = destinationSubject,
    field = field,
)
