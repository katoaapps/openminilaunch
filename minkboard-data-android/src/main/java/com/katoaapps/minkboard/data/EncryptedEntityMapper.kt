package com.katoaapps.minkboard.data

import com.katoaapps.minkboard.model.Board
import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.BoardSubtask
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.minkboard.model.SubtaskId
import java.time.LocalDate

internal class EncryptedEntityMapper(
    private val cipher: FieldCipher,
) {
    fun mapBoard(graph: BoardGraph): BoardSnapshot {
        val boardEntity = graph.board
        val statuses = graph.statuses.sortedBy { it.status.position }
        return BoardSnapshot(
            board = Board(
                id = BoardId(boardEntity.id),
                name = cipher.decrypt(
                    boardEntity.encryptedName,
                    boardSubject(boardEntity.id),
                    FIELD_NAME,
                ),
                isLocked = boardEntity.isLocked,
                position = boardEntity.position,
                createdAtEpochMillis = boardEntity.createdAtEpochMillis,
            ),
            statuses = statuses.map { graphStatus ->
                val entity = graphStatus.status
                BoardStatus(
                    id = StatusId(entity.id),
                    boardId = BoardId(entity.boardId),
                    name = cipher.decrypt(
                        entity.encryptedName,
                        statusSubject(entity.id),
                        FIELD_NAME,
                    ),
                    colorArgb = entity.colorArgb,
                    isDoneStatus = entity.isDone,
                    position = entity.position,
                )
            },
            cards = statuses.flatMap { status ->
                status.cards.sortedBy { it.card.position }.map(::mapCard)
            },
        )
    }

    private fun mapCard(graph: CardGraph): BoardCard {
        val entity = graph.card
        val subject = cardSubject(entity.id)
        return BoardCard(
            id = CardId(entity.id),
            statusId = StatusId(entity.statusId),
            title = cipher.decrypt(entity.encryptedTitle, subject, FIELD_TITLE),
            details = cipher.decrypt(entity.encryptedDetails, subject, FIELD_DETAILS),
            dueDate = entity.encryptedDueDate
                ?.let { cipher.decrypt(it, subject, FIELD_DUE_DATE) }
                ?.let(LocalDate::parse),
            isMajor = entity.isMajor,
            position = entity.position,
            createdAtEpochMillis = entity.createdAtEpochMillis,
            updatedAtEpochMillis = entity.updatedAtEpochMillis,
            subtasks = graph.subtasks.sortedBy(SubtaskEntity::position).map { subtask ->
                BoardSubtask(
                    id = SubtaskId(subtask.id),
                    cardId = CardId(subtask.cardId),
                    text = cipher.decrypt(
                        subtask.encryptedText,
                        subtaskSubject(subtask.id),
                        FIELD_TEXT,
                    ),
                    isCompleted = subtask.isCompleted,
                    position = subtask.position,
                )
            },
        )
    }
}

internal const val FIELD_NAME = "name"
internal const val FIELD_TITLE = "title"
internal const val FIELD_DETAILS = "details"
internal const val FIELD_DUE_DATE = "due-date"
internal const val FIELD_TEXT = "text"

internal fun boardSubject(id: String) = "board:$id"
// Preserve the V1 authenticated-data prefix so existing ciphertext remains decryptable.
internal fun statusSubject(id: String) = "column:$id"
internal fun cardSubject(id: String) = "card:$id"
internal fun subtaskSubject(id: String) = "subtask:$id"
