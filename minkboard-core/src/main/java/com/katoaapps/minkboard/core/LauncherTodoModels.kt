package com.katoaapps.minkboard.core

import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.PortableWorkspaceSnapshot
import com.katoaapps.minkboard.model.StatusId

/** The complete portable shape of a pre-MinkBoard launcher To-Do. */
data class LegacyLauncherTodo(
    val id: String,
    val text: String,
    val completed: Boolean,
)

/**
 * Reading legacy storage must distinguish a missing feature from an intentionally empty list and
 * from data that could not be parsed. Treating all three as `emptyList()` can erase user data.
 */
sealed interface LegacyTodoSource {
    data object Missing : LegacyTodoSource

    data class Available(
        val items: List<LegacyLauncherTodo>,
    ) : LegacyTodoSource

    /** The reason deliberately carries no user-authored text. */
    data object Corrupt : LegacyTodoSource
}

enum class LauncherTodoCompatibilityState {
    /** MinkBoard is canonical while one rollback-capable release still receives a projection. */
    CANONICAL_COMPAT,

    /** The legacy runtime projection has ended; the installed-data migration reader remains. */
    CANONICAL_FINAL,
}

/**
 * Temporary legacy To-Do compatibility metadata. Fresh launcher workspaces do not manufacture
 * this receipt; `null` means the launcher has no legacy projection lifecycle to complete.
 */
data class LauncherLegacyCompatibilityReceipt(
    val state: LauncherTodoCompatibilityState,
    val sourceSchemaVersion: Int,
    val sourceFingerprint: String,
    val lastProjectionFingerprint: String?,
    val migratedAtEpochMillis: Long,
) {
    init {
        require(sourceSchemaVersion > 0) { "Source schema version must be positive" }
        require(sourceFingerprint.isSha256Fingerprint()) {
            "Source fingerprint must be a lowercase SHA-256 value"
        }
        require(lastProjectionFingerprint == null ||
            lastProjectionFingerprint.isSha256Fingerprint()
        ) {
            "Projection fingerprint must be a lowercase SHA-256 value"
        }
        require(migratedAtEpochMillis >= 0) { "Migration timestamp cannot be negative" }
    }
}

/**
 * Permanent launcher ownership of one board and its Magic Box intake. The immutable local
 * generation invalidates work planned before a restore or explicit redesignation. Legacy
 * compatibility is optional and never travels in portable backup.
 */
data class LauncherBoardDesignation(
    val generation: String,
    val boardId: BoardId,
    val intakeStatusId: StatusId,
    val designatedAtEpochMillis: Long,
    val legacyCompatibility: LauncherLegacyCompatibilityReceipt?,
) {
    init {
        require(generation.isNotBlank() && generation.length <= 200) {
            "Designation generation must contain 1 to 200 characters"
        }
        require(designatedAtEpochMillis >= 0) { "Designation timestamp cannot be negative" }
    }
}

/** Portable identity only; local generation and compatibility receipt are installation-specific. */
data class LauncherBoardBackupDesignation(
    val boardId: BoardId,
    val intakeStatusId: StatusId,
)

data class LauncherTodoMigrationPlan(
    val board: BoardSnapshot,
    val designation: LauncherBoardDesignation,
)

sealed interface LauncherTodoMigrationOutcome {
    data object NoLegacyData : LauncherTodoMigrationOutcome
    data object CorruptLegacyData : LauncherTodoMigrationOutcome

    data class Ready(
        val plan: LauncherTodoMigrationPlan,
    ) : LauncherTodoMigrationOutcome
}

data class LegacyTodoProjection(
    val items: List<LegacyLauncherTodo>,
    val fingerprint: String,
)

enum class LegacyProjectionDecision {
    /** The old store still matches our last write and already reflects canonical data. */
    UNCHANGED,

    /** Canonical data changed while the old store still matches our last known projection. */
    WRITE_CANONICAL_PROJECTION,

    /**
     * The legacy store already contains the canonical projection, but the local receipt is stale.
     * Record the supplied receipt token without rewriting the legacy store. This heals process death
     * after a durable legacy write but before its receipt update.
     */
    ADOPT_CANONICAL_PROJECTION,

    /** An older launcher changed the old store; recovery must be explicit and non-destructive. */
    DIVERGED,
}

/**
 * Launcher backup tri-state. `Present` may intentionally contain zero boards and is not equivalent
 * to `Absent`. Host metadata uses portable IDs and must be remapped with a Merge import.
 */
sealed interface LauncherMinkBoardBackup {
    data object Absent : LauncherMinkBoardBackup

    data class Present(
        val workspace: PortableWorkspaceSnapshot,
        /**
         * Portable designation metadata. Both board and intake-status IDs must be remapped after a
         * Merge; keeping only the board ID would leave Magic Box intake ambiguous after restore.
         */
        val designation: LauncherBoardBackupDesignation?,
    ) : LauncherMinkBoardBackup
}

private fun String.isSha256Fingerprint(): Boolean =
    length == 64 && all { it in '0'..'9' || it in 'a'..'f' }
