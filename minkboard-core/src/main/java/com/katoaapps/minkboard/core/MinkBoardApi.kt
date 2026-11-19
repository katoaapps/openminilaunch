package com.katoaapps.minkboard.core

import com.katoaapps.minkboard.model.BoardDraft
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.BoardSummary
import com.katoaapps.minkboard.model.BoardUpdate
import com.katoaapps.minkboard.model.CardDraft
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.CardUpdate
import com.katoaapps.minkboard.model.MinkBoardWorkspace
import com.katoaapps.minkboard.model.PortableWorkspaceSnapshot
import com.katoaapps.minkboard.model.StatusDraft
import com.katoaapps.minkboard.model.StatusDeletionPolicy
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.minkboard.model.StatusUpdate
import com.katoaapps.minkboard.model.SubtaskId
import kotlinx.coroutines.flow.Flow

/** Public board lifecycle and query capability for embedded hosts. */
interface MinkBoardBoards {
    /** Reserve complete workspace hydration for explicit workspace-level operations. */
    fun observeWorkspace(): Flow<MinkBoardWorkspace>

    fun observeBoardSummaries(): Flow<List<BoardSummary>>

    fun observeBoard(boardId: BoardId): Flow<BoardSnapshot?>

    suspend fun board(boardId: BoardId): BoardSnapshot?

    suspend fun ensureDefaultWorkspace()

    suspend fun createBoard(draft: BoardDraft): BoardId

    suspend fun createBoardFromTemplate(draft: BoardDraft, templateId: BoardTemplateId): BoardId

    suspend fun renameBoard(boardId: BoardId, name: String)

    suspend fun updateBoard(boardId: BoardId, update: BoardUpdate)

    suspend fun reorderBoards(orderedBoardIds: List<BoardId>)

    suspend fun deleteBoard(boardId: BoardId)

    suspend fun deleteBoards(boardIds: Set<BoardId>)
}

/** Public ordered status capability. */
interface MinkBoardStatuses {
    suspend fun addStatus(boardId: BoardId, draft: StatusDraft): StatusId

    suspend fun updateStatus(statusId: StatusId, update: StatusUpdate)

    suspend fun reorderStatuses(boardId: BoardId, orderedStatusIds: List<StatusId>)

    suspend fun deleteStatus(statusId: StatusId, policy: StatusDeletionPolicy)
}

/** Public card command capability. Boards remain the source of observable card projections. */
interface MinkBoardCards {
    suspend fun addCard(statusId: StatusId, draft: CardDraft): CardId

    suspend fun cloneCard(cardId: CardId, title: String? = null): CardId

    suspend fun updateCard(cardId: CardId, update: CardUpdate)

    suspend fun moveCard(cardId: CardId, destination: StatusId, position: Int = Int.MAX_VALUE)

    suspend fun deleteCard(cardId: CardId)
}

/** Public subtask command capability. */
interface MinkBoardSubtasks {
    suspend fun addSubtask(cardId: CardId, text: String): SubtaskId

    suspend fun updateSubtask(subtaskId: SubtaskId, text: String)

    suspend fun setSubtaskCompleted(subtaskId: SubtaskId, completed: Boolean)

    suspend fun deleteSubtask(subtaskId: SubtaskId)
}

/** Public focus-session persistence capability. Android alarm/notification policy stays host-owned. */
interface MinkBoardFocus : FocusTimerStore

/** Creates process-memory authorization sessions without prescribing the host authenticator. */
interface MinkBoardAccess {
    fun createSession(
        backgroundTimeoutMillis: Long = BoardAccessSessionController.DEFAULT_BACKGROUND_TIMEOUT_MILLIS,
    ): BoardAccessSessionController
}

/** Public immutable template catalog. */
interface MinkBoardTemplateCatalog {
    val templates: List<BoardTemplate>

    fun require(templateId: BoardTemplateId): BoardTemplate
}

/**
 * Host-backup boundary. Snapshots are plaintext in memory; the host must protect them before
 * durable external storage.
 */
interface MinkBoardBackup {
    suspend fun createSnapshot(sourceApp: String): PortableWorkspaceSnapshot

    /** Imports incoming boards as new local boards and returns every remapped identifier. */
    suspend fun importSnapshot(snapshot: PortableWorkspaceSnapshot): WorkspaceImportResult
}

/** Password-protected `.minkboard` transfer boundary. File picking remains host-owned. */
interface MinkBoardTransfer {
    fun isSupportedArchive(archive: ByteArray): Boolean

    suspend fun exportBoards(
        boardIds: Set<BoardId>,
        sourceApp: String,
        passphrase: CharArray,
    ): ByteArray

    /** Imports incoming boards as new local boards; it never replaces the current workspace. */
    suspend fun importBoards(
        archive: ByteArray,
        passphrase: CharArray,
    ): WorkspaceImportResult
}

/**
 * Complete host-neutral entry point for embedding MinkBoard functionality.
 *
 * Consumers may inject the whole API at their composition root while individual features depend
 * only on the narrow capability they use.
 */
interface MinkBoardApi {
    val boards: MinkBoardBoards
    val statuses: MinkBoardStatuses
    val cards: MinkBoardCards
    val subtasks: MinkBoardSubtasks
    val focus: MinkBoardFocus
    val access: MinkBoardAccess
    val transfer: MinkBoardTransfer
    val backup: MinkBoardBackup
    val templates: MinkBoardTemplateCatalog
}
