package com.katoaapps.minkboard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
internal interface MinkBoardDao {
    @Transaction
    @Query("SELECT * FROM boards ORDER BY position, board_id")
    fun observeWorkspace(): Flow<List<BoardGraph>>

    @Query(
        """
        SELECT boards.*,
            (SELECT COUNT(*) FROM board_columns WHERE board_columns.board_id = boards.board_id)
                AS status_count,
            (SELECT COUNT(*) FROM cards
                INNER JOIN board_columns ON cards.column_id = board_columns.column_id
                WHERE board_columns.board_id = boards.board_id)
                AS card_count
        FROM boards
        ORDER BY boards.position, boards.board_id
        """,
    )
    fun observeBoardSummaries(): Flow<List<BoardSummaryRow>>

    @Transaction
    @Query("SELECT * FROM boards WHERE board_id = :boardId LIMIT 1")
    fun observeBoard(boardId: String): Flow<BoardGraph?>

    @Transaction
    @Query("SELECT * FROM boards WHERE board_id = :boardId LIMIT 1")
    suspend fun boardGraph(boardId: String): BoardGraph?

    /** A transaction-stable workspace view for export and archive mutation checks. */
    @Transaction
    @Query("SELECT * FROM boards ORDER BY position, board_id")
    suspend fun workspace(): List<BoardGraph>

    @Query("SELECT * FROM focus_sessions WHERE singleton_key = 0 LIMIT 1")
    fun observeFocusSession(): Flow<FocusSessionEntity?>

    /**
     * Emits after any table that contributes to a launcher host snapshot changes. The repository
     * responds by reading the complete workspace and designation inside one Room transaction.
     */
    @RawQuery(
        observedEntities = [
            BoardEntity::class,
            StatusEntity::class,
            CardEntity::class,
            SubtaskEntity::class,
            LauncherBoardDesignationEntity::class,
        ],
    )
    fun observeLauncherHostInvalidations(query: SupportSQLiteQuery): Flow<Int>

    @Query("SELECT COUNT(*) FROM boards")
    suspend fun boardCount(): Int

    @Query("SELECT COALESCE(MAX(position), -1) FROM boards")
    suspend fun maxBoardPosition(): Int

    @Query("SELECT COALESCE(MAX(position), -1) FROM board_columns WHERE board_id = :boardId")
    suspend fun maxStatusPosition(boardId: String): Int

    @Query("SELECT COALESCE(MAX(position), -1) FROM cards WHERE column_id = :statusId")
    suspend fun maxCardPosition(statusId: String): Int

    @Query("SELECT COUNT(*) FROM cards WHERE column_id = :statusId")
    suspend fun cardCount(statusId: String): Int

    @Query("SELECT COALESCE(MAX(position), -1) FROM subtasks WHERE card_id = :cardId")
    suspend fun maxSubtaskPosition(cardId: String): Int

    @Query("SELECT * FROM boards WHERE board_id = :boardId LIMIT 1")
    suspend fun board(boardId: String): BoardEntity?

    @Query("SELECT * FROM board_columns WHERE column_id = :statusId LIMIT 1")
    suspend fun status(statusId: String): StatusEntity?

    @Query("SELECT * FROM cards WHERE card_id = :cardId LIMIT 1")
    suspend fun card(cardId: String): CardEntity?

    @Query("SELECT * FROM subtasks WHERE subtask_id = :subtaskId LIMIT 1")
    suspend fun subtask(subtaskId: String): SubtaskEntity?

    @Query("SELECT * FROM focus_sessions WHERE singleton_key = 0 LIMIT 1")
    suspend fun focusSession(): FocusSessionEntity?

    @Query("SELECT * FROM launcher_board_designation WHERE singleton_key = 0 LIMIT 1")
    suspend fun launcherBoardDesignation(): LauncherBoardDesignationEntity?

    @Query(
        """
        SELECT boards.board_id, boards.is_locked,
            (SELECT board_columns.column_id
                FROM board_columns
                WHERE board_columns.board_id = boards.board_id
                    AND board_columns.is_done = 0
                ORDER BY board_columns.position, board_columns.column_id
                LIMIT 1) AS intake_status_id
        FROM boards
        WHERE boards.board_id = :boardId
        LIMIT 1
        """,
    )
    suspend fun boardDesignationCandidate(boardId: String): BoardDesignationCandidateRow?

    @Query("SELECT card_id FROM cards WHERE column_id = :statusId ORDER BY position, card_id")
    suspend fun cardIds(statusId: String): List<String>

    @Query("SELECT board_id FROM boards ORDER BY position, board_id")
    suspend fun boardIds(): List<String>

    @Query("SELECT column_id FROM board_columns WHERE board_id = :boardId ORDER BY position, column_id")
    suspend fun statusIds(boardId: String): List<String>

    @Query("SELECT subtask_id FROM subtasks WHERE card_id = :cardId ORDER BY position, subtask_id")
    suspend fun subtaskIds(cardId: String): List<String>

    @Query("SELECT * FROM subtasks WHERE card_id = :cardId ORDER BY position, subtask_id")
    suspend fun subtasks(cardId: String): List<SubtaskEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBoard(board: BoardEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStatuses(statuses: List<StatusEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStatus(status: StatusEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCard(card: CardEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSubtask(subtask: SubtaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceFocusSession(session: FocusSessionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLauncherBoardDesignation(designation: LauncherBoardDesignationEntity)

    /** Backup restore or explicit user choice may replace the singleton local designation. */
    @Upsert
    suspend fun upsertLauncherBoardDesignation(designation: LauncherBoardDesignationEntity)

    @Update
    suspend fun updateBoard(board: BoardEntity)

    @Update
    suspend fun updateStatus(status: StatusEntity)

    @Update
    suspend fun updateCard(card: CardEntity)

    @Query("UPDATE board_columns SET is_done = 0 WHERE board_id = :boardId")
    suspend fun clearDoneStatus(boardId: String)

    @Query("UPDATE cards SET column_id = :statusId, position = :position, updated_at = :updatedAt WHERE card_id = :cardId")
    suspend fun moveCard(cardId: String, statusId: String, position: Int, updatedAt: Long)

    @Query("UPDATE cards SET position = :position WHERE card_id = :cardId")
    suspend fun updateCardPosition(cardId: String, position: Int)

    @Query(
        "UPDATE cards SET position = position + :delta " +
            "WHERE column_id = :statusId AND position BETWEEN :fromInclusive AND :toInclusive",
    )
    suspend fun shiftCardPositions(
        statusId: String,
        fromInclusive: Int,
        toInclusive: Int,
        delta: Int,
    )

    @Query("UPDATE boards SET position = :position WHERE board_id = :boardId")
    suspend fun updateBoardPosition(boardId: String, position: Int)

    @Query("UPDATE board_columns SET position = :position WHERE column_id = :statusId")
    suspend fun updateStatusPosition(statusId: String, position: Int)

    @Query("UPDATE cards SET updated_at = :updatedAt WHERE card_id = :cardId")
    suspend fun touchCard(cardId: String, updatedAt: Long)

    @Query("UPDATE subtasks SET is_completed = :completed WHERE subtask_id = :subtaskId")
    suspend fun setSubtaskCompleted(subtaskId: String, completed: Boolean)

    @Query("UPDATE subtasks SET encrypted_text = :encryptedText WHERE subtask_id = :subtaskId")
    suspend fun updateSubtaskText(subtaskId: String, encryptedText: ByteArray)

    @Query("UPDATE focus_sessions SET state = :state WHERE singleton_key = 0 AND timer_id = :timerId")
    suspend fun setFocusSessionState(timerId: String, state: String): Int

    @Query(
        "UPDATE launcher_board_designation SET last_projection_fingerprint = :fingerprint " +
            "WHERE singleton_key = 0 AND designation_generation = :generation AND " +
            "board_id = :boardId AND compatibility_state IS NOT NULL AND " +
            "((last_projection_fingerprint IS NULL AND :expectedPreviousFingerprint IS NULL) OR " +
            "last_projection_fingerprint = :expectedPreviousFingerprint)",
    )
    suspend fun setLauncherProjectionFingerprint(
        generation: String,
        boardId: String,
        fingerprint: String,
        expectedPreviousFingerprint: String?,
    ): Int

    @Query(
        "UPDATE launcher_board_designation SET compatibility_state = :state " +
            "WHERE singleton_key = 0 AND designation_generation = :generation AND " +
            "board_id = :boardId AND compatibility_state IS NOT NULL",
    )
    suspend fun setLauncherCompatibilityState(
        generation: String,
        boardId: String,
        state: String,
    ): Int

    @Query("UPDATE subtasks SET position = :position WHERE subtask_id = :subtaskId")
    suspend fun updateSubtaskPosition(subtaskId: String, position: Int)

    @Query("DELETE FROM cards WHERE card_id = :cardId")
    suspend fun deleteCard(cardId: String)

    @Query("DELETE FROM boards WHERE board_id = :boardId")
    suspend fun deleteBoard(boardId: String)

    /** Cascades through statuses, cards, subtasks, and any active focus session. */
    @Query("DELETE FROM boards")
    suspend fun deleteAllBoards()

    @Query("DELETE FROM board_columns WHERE column_id = :statusId")
    suspend fun deleteStatus(statusId: String)

    @Query("DELETE FROM subtasks WHERE subtask_id = :subtaskId")
    suspend fun deleteSubtask(subtaskId: String)

    @Query("DELETE FROM focus_sessions WHERE singleton_key = 0 AND timer_id = :timerId")
    suspend fun deleteFocusSession(timerId: String): Int
}
