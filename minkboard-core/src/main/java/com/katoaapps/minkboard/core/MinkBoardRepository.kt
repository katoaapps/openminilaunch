package com.katoaapps.minkboard.core

import com.katoaapps.minkboard.model.BoardDraft
import com.katoaapps.minkboard.model.BoardId
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

interface MinkBoardRepository {
    fun observeWorkspace(): Flow<MinkBoardWorkspace>

    suspend fun ensureDefaultWorkspace()

    suspend fun createBoard(draft: BoardDraft): BoardId
    suspend fun createBoardFromTemplate(draft: BoardDraft, templateId: BoardTemplateId): BoardId
    suspend fun renameBoard(boardId: BoardId, name: String)
    suspend fun updateBoard(boardId: BoardId, update: BoardUpdate)
    suspend fun reorderBoards(orderedBoardIds: List<BoardId>)
    suspend fun deleteBoard(boardId: BoardId)
    suspend fun deleteBoards(boardIds: Set<BoardId>) {
        boardIds.forEach { boardId -> deleteBoard(boardId) }
    }

    suspend fun addStatus(boardId: BoardId, draft: StatusDraft): StatusId
    suspend fun updateStatus(statusId: StatusId, update: StatusUpdate)
    suspend fun reorderStatuses(boardId: BoardId, orderedStatusIds: List<StatusId>)
    suspend fun deleteStatus(statusId: StatusId, policy: StatusDeletionPolicy)

    suspend fun addCard(statusId: StatusId, draft: CardDraft): CardId
    suspend fun cloneCard(cardId: CardId): CardId
    suspend fun updateCard(cardId: CardId, update: CardUpdate)
    suspend fun moveCard(cardId: CardId, destination: StatusId, position: Int = Int.MAX_VALUE)
    suspend fun deleteCard(cardId: CardId)

    suspend fun addSubtask(cardId: CardId, text: String): SubtaskId
    suspend fun updateSubtask(subtaskId: SubtaskId, text: String)
    suspend fun setSubtaskCompleted(subtaskId: SubtaskId, completed: Boolean)
    suspend fun deleteSubtask(subtaskId: SubtaskId)
}
