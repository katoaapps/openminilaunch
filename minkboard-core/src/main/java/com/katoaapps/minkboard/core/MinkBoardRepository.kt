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
import com.katoaapps.minkboard.model.StatusDraft
import com.katoaapps.minkboard.model.StatusDeletionPolicy
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.minkboard.model.StatusUpdate
import com.katoaapps.minkboard.model.SubtaskId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Compatibility aggregate for code that still expects one repository object.
 *
 * New host integrations should depend on [MinkBoardApi] or one of its narrow capabilities.
 */
interface MinkBoardRepository :
    MinkBoardBoards,
    MinkBoardStatuses,
    MinkBoardCards,
    MinkBoardSubtasks {
    /**
     * Hydrates the complete workspace. Reserve this for explicit whole-workspace operations such
     * as archive creation; interactive surfaces should use [observeBoardSummaries] and
     * [observeBoard].
     */
    override fun observeWorkspace(): Flow<MinkBoardWorkspace>

    /** Lightweight metadata for menus and board management. */
    override fun observeBoardSummaries(): Flow<List<BoardSummary>> =
        observeWorkspace().map { workspace ->
            workspace.boards.map { snapshot ->
                BoardSummary(
                    board = snapshot.board,
                    statusCount = snapshot.statuses.size,
                    cardCount = snapshot.cards.size,
                )
            }
        }

    /** Fully hydrates only the requested board. */
    override fun observeBoard(boardId: BoardId): Flow<BoardSnapshot?> =
        observeWorkspace().map { workspace ->
        workspace.board(boardId)
    }

    override suspend fun board(boardId: BoardId): BoardSnapshot? = observeBoard(boardId).first()

    override suspend fun ensureDefaultWorkspace()

    override suspend fun createBoard(draft: BoardDraft): BoardId
    override suspend fun createBoardFromTemplate(
        draft: BoardDraft,
        templateId: BoardTemplateId,
    ): BoardId
    override suspend fun renameBoard(boardId: BoardId, name: String)
    override suspend fun updateBoard(boardId: BoardId, update: BoardUpdate)
    override suspend fun reorderBoards(orderedBoardIds: List<BoardId>)
    override suspend fun deleteBoard(boardId: BoardId)
    override suspend fun deleteBoards(boardIds: Set<BoardId>) {
        boardIds.forEach { boardId -> deleteBoard(boardId) }
    }

    override suspend fun addStatus(boardId: BoardId, draft: StatusDraft): StatusId
    override suspend fun updateStatus(statusId: StatusId, update: StatusUpdate)
    override suspend fun reorderStatuses(boardId: BoardId, orderedStatusIds: List<StatusId>)
    override suspend fun deleteStatus(statusId: StatusId, policy: StatusDeletionPolicy)

    override suspend fun addCard(statusId: StatusId, draft: CardDraft): CardId
    /**
     * Deep-copies [cardId] immediately after its source. When supplied, [title] is encrypted as
     * part of the same transaction so hosts never observe a half-renamed duplicate.
     */
    override suspend fun cloneCard(cardId: CardId, title: String?): CardId
    override suspend fun updateCard(cardId: CardId, update: CardUpdate)
    override suspend fun moveCard(cardId: CardId, destination: StatusId, position: Int)
    override suspend fun deleteCard(cardId: CardId)

    override suspend fun addSubtask(cardId: CardId, text: String): SubtaskId
    override suspend fun updateSubtask(subtaskId: SubtaskId, text: String)
    override suspend fun setSubtaskCompleted(subtaskId: SubtaskId, completed: Boolean)
    override suspend fun deleteSubtask(subtaskId: SubtaskId)
}
