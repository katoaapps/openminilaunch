package com.katoaapps.minkboard.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "boards")
internal data class BoardEntity(
    @PrimaryKey @ColumnInfo(name = "board_id") val id: String,
    @ColumnInfo(name = "encrypted_name", typeAffinity = ColumnInfo.BLOB) val encryptedName: ByteArray,
    @ColumnInfo(name = "is_locked") val isLocked: Boolean,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "board_columns",
    foreignKeys = [
        ForeignKey(
            entity = BoardEntity::class,
            parentColumns = ["board_id"],
            childColumns = ["board_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["board_id", "position", "column_id"])],
)
internal data class StatusEntity(
    @PrimaryKey @ColumnInfo(name = "column_id") val id: String,
    @ColumnInfo(name = "board_id") val boardId: String,
    @ColumnInfo(name = "encrypted_name", typeAffinity = ColumnInfo.BLOB) val encryptedName: ByteArray,
    @ColumnInfo(name = "color_argb") val colorArgb: Int,
    @ColumnInfo(name = "is_done") val isDone: Boolean,
    @ColumnInfo(name = "position") val position: Int,
)

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = StatusEntity::class,
            parentColumns = ["column_id"],
            childColumns = ["column_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["column_id", "position", "card_id"])],
)
internal data class CardEntity(
    @PrimaryKey @ColumnInfo(name = "card_id") val id: String,
    @ColumnInfo(name = "column_id") val statusId: String,
    @ColumnInfo(name = "encrypted_title", typeAffinity = ColumnInfo.BLOB) val encryptedTitle: ByteArray,
    @ColumnInfo(name = "encrypted_details", typeAffinity = ColumnInfo.BLOB) val encryptedDetails: ByteArray,
    @ColumnInfo(name = "encrypted_due_date", typeAffinity = ColumnInfo.BLOB) val encryptedDueDate: ByteArray?,
    @ColumnInfo(name = "is_major") val isMajor: Boolean,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["card_id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["card_id", "position", "subtask_id"])],
)
internal data class SubtaskEntity(
    @PrimaryKey @ColumnInfo(name = "subtask_id") val id: String,
    @ColumnInfo(name = "card_id") val cardId: String,
    @ColumnInfo(name = "encrypted_text", typeAffinity = ColumnInfo.BLOB) val encryptedText: ByteArray,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean,
    @ColumnInfo(name = "position") val position: Int,
)

@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = BoardEntity::class,
            parentColumns = ["board_id"],
            childColumns = ["board_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["card_id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("board_id"), Index("card_id")],
)
internal data class FocusSessionEntity(
    /** A constant primary key makes the single-active-timer policy structural. */
    @PrimaryKey @ColumnInfo(name = "singleton_key") val singletonKey: Int = 0,
    @ColumnInfo(name = "timer_id") val timerId: String,
    @ColumnInfo(name = "board_id") val boardId: String,
    @ColumnInfo(name = "card_id") val cardId: String,
    @ColumnInfo(name = "started_at") val startedAtEpochMillis: Long,
    @ColumnInfo(name = "ends_at") val endsAtEpochMillis: Long,
    @ColumnInfo(name = "state") val state: String,
)

/**
 * Singleton launcher board designation. Legacy compatibility columns are nullable because a fresh
 * launcher workspace has no migration/projection lifecycle. It intentionally has no foreign keys:
 * deleting/restoring a board must retain enough evidence to report a broken designation instead of
 * silently choosing another board or reimporting stale plaintext To-Dos.
 */
@Entity(tableName = "launcher_board_designation")
internal data class LauncherBoardDesignationEntity(
    @PrimaryKey @ColumnInfo(name = "singleton_key") val singletonKey: Int = 0,
    @ColumnInfo(name = "designation_generation") val generation: String,
    @ColumnInfo(name = "board_id") val boardId: String,
    @ColumnInfo(name = "intake_status_id") val intakeStatusId: String,
    @ColumnInfo(name = "designated_at") val designatedAtEpochMillis: Long,
    @ColumnInfo(name = "compatibility_state") val compatibilityState: String?,
    @ColumnInfo(name = "source_schema_version") val sourceSchemaVersion: Int?,
    @ColumnInfo(name = "source_fingerprint") val sourceFingerprint: String?,
    @ColumnInfo(name = "last_projection_fingerprint") val lastProjectionFingerprint: String?,
    @ColumnInfo(name = "migrated_at") val migratedAtEpochMillis: Long?,
)
