package com.katoaapps.openminilaunch.features.backup

import com.katoaapps.minkboard.core.IdFactory
import com.katoaapps.minkboard.core.LauncherBackupApplyResult
import com.katoaapps.minkboard.core.LauncherBackupCreationResult
import com.katoaapps.minkboard.core.LauncherEncryptedBackupCreationResult
import com.katoaapps.minkboard.core.LauncherBackupPreparationResult
import com.katoaapps.minkboard.core.LauncherBackupRestoreContext
import com.katoaapps.minkboard.core.LauncherBackupRestorePlan
import com.katoaapps.minkboard.core.LauncherBoardBackupDesignation
import com.katoaapps.minkboard.core.LauncherMinkBoardBackupField
import com.katoaapps.minkboard.core.LauncherTodoMigrationOutcome
import com.katoaapps.minkboard.core.LauncherTodoNormalizer
import com.katoaapps.minkboard.core.LegacyLauncherTodo
import com.katoaapps.minkboard.core.LegacyTodoSource
import com.katoaapps.minkboard.core.MinkBoardLauncherBackupWorkflow
import com.katoaapps.minkboard.core.MinkBoardLauncherFacade
import com.katoaapps.minkboard.core.WorkspaceImportMode
import com.katoaapps.minkboard.core.toPortableSnapshot
import com.katoaapps.minkboard.model.MinkBoardWorkspace
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.openminilaunch.features.minkboard.LauncherBoardHomePreferences
import com.katoaapps.openminilaunch.features.minkboard.LauncherVisibleStatuses
import java.util.UUID

internal sealed interface OpenMinkEncryptedBoardBackupResult {
    data class Ready(
        val boardEnvelope: ByteArray,
        val homeEnvelope: ByteArray,
    ) : OpenMinkEncryptedBoardBackupResult

    data object UnboundWorkspace : OpenMinkEncryptedBoardBackupResult
    data object BrokenBinding : OpenMinkEncryptedBoardBackupResult
    data object EncryptionFailed : OpenMinkEncryptedBoardBackupResult
}

internal sealed interface OpenMinkBoardRestorePreparationResult {
    data class Prepared(
        val board: LauncherBackupPreparationResult.Prepared,
        val home: LauncherBoardHomeBackup?,
    ) : OpenMinkBoardRestorePreparationResult

    data object UnsupportedEnvelope : OpenMinkBoardRestorePreparationResult
    data object CorruptEnvelope : OpenMinkBoardRestorePreparationResult
    data object MissingSecret : OpenMinkBoardRestorePreparationResult
    data object InvalidRichBackup : OpenMinkBoardRestorePreparationResult
    data object CorruptLegacyData : OpenMinkBoardRestorePreparationResult
}

/** OpenMink-owned orchestration around the shared encrypted launcher-backup workflow. */
internal class MinkBoardLauncherBackupCoordinator(
    private val workflow: MinkBoardLauncherBackupWorkflow,
    private val facade: MinkBoardLauncherFacade,
    private val homePreferences: LauncherBoardHomePreferences,
    private val homeCodec: LauncherBoardHomeBackupCodec = LauncherBoardHomeBackupCodec(),
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    private val legacyNormalizer = LauncherTodoNormalizer(
        idFactory = IdFactory { UUID.randomUUID().toString() },
        clockMillis = clockMillis,
    )

    suspend fun createEncryptedBackup(
        sourceApp: String,
        secret: CharArray,
    ): OpenMinkEncryptedBoardBackupResult = when (
        val board = workflow.createEncryptedBackup(sourceApp, secret)
    ) {
        is LauncherEncryptedBackupCreationResult.Ready -> {
            val home = homeCodec.encode(homePreferences.snapshot(), secret)
                ?: return OpenMinkEncryptedBoardBackupResult.EncryptionFailed
            OpenMinkEncryptedBoardBackupResult.Ready(board.envelope, home)
        }
        LauncherEncryptedBackupCreationResult.UnboundWorkspace ->
            OpenMinkEncryptedBoardBackupResult.UnboundWorkspace
        is LauncherEncryptedBackupCreationResult.BrokenBinding ->
            OpenMinkEncryptedBoardBackupResult.BrokenBinding
        LauncherEncryptedBackupCreationResult.EncryptionFailed ->
            OpenMinkEncryptedBoardBackupResult.EncryptionFailed
    }

    fun prepareRestore(
        backup: LauncherBackup,
        secret: CharArray?,
    ): OpenMinkBoardRestorePreparationResult {
        val board = workflow.prepareRestore(
            minkBoardField = backup.minkBoardEnvelope?.let(LauncherMinkBoardBackupField::Encrypted)
                ?: LauncherMinkBoardBackupField.Missing,
            secret = secret,
            legacyTodos = when {
                backup.minkBoardEnvelope != null -> LegacyTodoSource.Missing
                backup.legacyTodosPresent -> LegacyTodoSource.Available(
                    backup.todos.map { todo ->
                        LegacyLauncherTodo(todo.id, todo.text, todo.completed)
                    },
                )
                else -> LegacyTodoSource.Missing
            },
        )
        if (board !is LauncherBackupPreparationResult.Prepared) {
            return when (board) {
                LauncherBackupPreparationResult.UnsupportedEnvelope ->
                    OpenMinkBoardRestorePreparationResult.UnsupportedEnvelope
                LauncherBackupPreparationResult.CorruptEnvelope ->
                    OpenMinkBoardRestorePreparationResult.CorruptEnvelope
                LauncherBackupPreparationResult.MissingSecret ->
                    OpenMinkBoardRestorePreparationResult.MissingSecret
                LauncherBackupPreparationResult.InvalidRichBackup ->
                    OpenMinkBoardRestorePreparationResult.InvalidRichBackup
                LauncherBackupPreparationResult.CorruptLegacyData ->
                    OpenMinkBoardRestorePreparationResult.CorruptLegacyData
                is LauncherBackupPreparationResult.Prepared -> error("Handled above")
            }
        }

        val home = backup.minkBoardHomeEnvelope?.let { envelope ->
            val recoverySecret = secret
                ?: return OpenMinkBoardRestorePreparationResult.MissingSecret
            when (val decoded = homeCodec.decode(envelope, recoverySecret)) {
                is LauncherBoardHomeDecodeResult.Decoded -> decoded.backup
                LauncherBoardHomeDecodeResult.Unsupported ->
                    return OpenMinkBoardRestorePreparationResult.UnsupportedEnvelope
                LauncherBoardHomeDecodeResult.Corrupt ->
                    return OpenMinkBoardRestorePreparationResult.CorruptEnvelope
            }
        }
        return OpenMinkBoardRestorePreparationResult.Prepared(board, home)
    }

    suspend fun applyPrepared(
        prepared: OpenMinkBoardRestorePreparationResult.Prepared,
    ): LauncherBackupApplyResult {
        val legacy = prepared.board.plan as? LauncherBackupRestorePlan.NormalizeLegacy
        val result = if (legacy == null) {
            workflow.apply(
                prepared = prepared.board,
                mode = WorkspaceImportMode.Merge,
                context = LauncherBackupRestoreContext(clockMillis()),
            )
        } else {
            // Old launcher backups are imported as a new two-status board even when this
            // installation already has a designated board. Reusing normalizeLegacyTodos here
            // would treat the existing binding as already migrated and skip the imported tasks.
            val migration = legacyNormalizer.plan(
                source = legacy.source,
                sourceSchemaVersion = LEGACY_BACKUP_TODO_SCHEMA_VERSION,
                boardName = LEGACY_BACKUP_BOARD_NAME,
            ) as? LauncherTodoMigrationOutcome.Ready
                ?: return LauncherBackupApplyResult.RejectedCorruptLegacyData
            val workspace = MinkBoardWorkspace(listOf(migration.plan.board)).toPortableSnapshot(
                exportedAtEpochMillis = clockMillis(),
                sourceApp = LEGACY_BACKUP_SOURCE,
            )
            val outcome = facade.importRichBackup(
                plan = LauncherBackupRestorePlan.ImportWorkspace(
                    workspace = workspace,
                    portableDesignation = LauncherBoardBackupDesignation(
                        boardId = migration.plan.designation.boardId,
                        intakeStatusId = migration.plan.designation.intakeStatusId,
                    ),
                ),
                mode = WorkspaceImportMode.Merge,
                context = LauncherBackupRestoreContext(clockMillis()),
            )
            LauncherBackupApplyResult.WorkspaceImported(outcome)
        }

        if (result is LauncherBackupApplyResult.WorkspaceImported) {
            val visibility = prepared.home?.visibleStatusIds
            if (visibility == null) {
                homePreferences.showAllStatuses()
            } else {
                val mapped = visibility.mapNotNull { portableId ->
                    result.outcome.importResult.ids.statusIds[portableId]
                }.distinct()
                homePreferences.restore(
                    LauncherVisibleStatuses.Selected(
                        mapped.ifEmpty {
                            listOf(result.outcome.launcherDesignation.intakeStatusId)
                        },
                    ),
                )
            }
        }
        return result
    }

    private companion object {
        const val LEGACY_BACKUP_TODO_SCHEMA_VERSION = 1
        const val LEGACY_BACKUP_BOARD_NAME = "Imported To-Dos"
        const val LEGACY_BACKUP_SOURCE = "OpenMink legacy launcher backup"
    }
}
