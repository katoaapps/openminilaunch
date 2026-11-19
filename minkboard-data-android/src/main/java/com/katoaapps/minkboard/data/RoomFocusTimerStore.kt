package com.katoaapps.minkboard.data

import androidx.room.withTransaction
import com.katoaapps.minkboard.core.FocusTimerStore
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.FocusSession
import com.katoaapps.minkboard.model.FocusSessionState
import com.katoaapps.minkboard.model.FocusTimerId
import com.katoaapps.minkboard.model.FocusTimerSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** Room-backed focus operations kept separate from board graph mutations. */
internal class RoomFocusTimerStore(
    private val database: MinkBoardDatabase,
    private val dao: MinkBoardDao,
    private val cipher: FieldCipher,
) : FocusTimerStore {
    override fun observeFocusSession(): Flow<FocusSession?> = dao.observeFocusSession()
        .map { it?.toModel() }
        .flowOn(Dispatchers.Default)

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
            cardTitle = cipher.decrypt(card.encryptedTitle, cardSubject(card.id), FIELD_TITLE),
            cardDetails = cipher.decrypt(card.encryptedDetails, cardSubject(card.id), FIELD_DETAILS),
            boardIsLocked = board.isLocked,
        )
    }
}

internal const val FOCUS_STATE_RUNNING = "running"
internal const val FOCUS_STATE_FINISHED = "finished"

internal fun FocusSession.toEntity() = FocusSessionEntity(
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

internal fun FocusSessionEntity.toModel() = FocusSession(
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
