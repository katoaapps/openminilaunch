package com.katoaapps.openminilaunch.features.minkboard

import com.katoaapps.openminilaunch.features.minkboard.focus.FocusTimerCoordinator
import com.katoaapps.openminilaunch.features.minkboard.focus.StartFocusTimerResult
import com.katoaapps.minkboard.core.MinkBoardRules
import com.katoaapps.minkboard.data.RoomMinkBoardRepository
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.CardPlacement
import com.katoaapps.minkboard.model.CardUpdate
import com.katoaapps.minkboard.model.FocusTimerId
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.minkboard.model.SubtaskId

/** Shared launcher-side card commands used by both Home and the full board host. */
internal class OpenMinkCardActionController(
    private val repository: RoomMinkBoardRepository,
    private val focusTimerCoordinator: FocusTimerCoordinator,
) {
    suspend fun cloneCard(
        board: BoardSnapshot,
        cardId: CardId,
        duplicateSuffix: String,
    ): CardPlacement {
        val source = board.cards.firstOrNull { it.id == cardId }
            ?: throw OpenMinkCardUnavailableException()
        val sourceIndex = board.cardsIn(source.statusId)
            .indexOfFirst { it.id == source.id }
            .takeIf { it >= 0 }
            ?: throw OpenMinkCardUnavailableException()
        return CardPlacement(
            cardId = repository.cloneCard(
                cardId,
                openMinkDuplicateTitle(source.title, duplicateSuffix),
            ),
            statusId = source.statusId,
            position = sourceIndex + 1,
        )
    }

    suspend fun updateCardAndMove(
        board: BoardSnapshot,
        cardId: CardId,
        update: CardUpdate,
        destination: StatusId,
    ): CardPlacement? {
        val source = board.cards.firstOrNull { it.id == cardId }
            ?: throw OpenMinkCardUnavailableException()
        repository.updateCard(cardId, update)
        return if (source.statusId != destination) {
            repository.moveCard(cardId, destination, 0)
            CardPlacement(cardId, destination, 0)
        } else {
            null
        }
    }

    suspend fun moveCard(cardId: CardId, statusId: StatusId, position: Int): CardPlacement {
        repository.moveCard(cardId, statusId, position)
        return CardPlacement(cardId, statusId, position)
    }

    suspend fun deleteCard(cardId: CardId) {
        repository.deleteCard(cardId)
        focusTimerCoordinator.reconcile()
    }

    suspend fun addSubtask(cardId: CardId, text: String) = repository.addSubtask(cardId, text)

    suspend fun updateSubtask(subtaskId: SubtaskId, text: String) =
        repository.updateSubtask(subtaskId, text)

    suspend fun setSubtaskCompleted(subtaskId: SubtaskId, completed: Boolean) =
        repository.setSubtaskCompleted(subtaskId, completed)

    suspend fun deleteSubtask(subtaskId: SubtaskId) = repository.deleteSubtask(subtaskId)

    suspend fun startFocus(
        boardId: BoardId,
        cardId: CardId,
        durationMillis: Long,
        replace: Boolean,
    ): StartFocusTimerResult = focusTimerCoordinator.start(
        boardId = boardId,
        cardId = cardId,
        durationMillis = durationMillis,
        replaceCurrent = replace,
    )

    suspend fun stopFocus(timerId: FocusTimerId) = focusTimerCoordinator.stop(timerId)
}

internal class OpenMinkCardUnavailableException : IllegalStateException()

internal fun openMinkDuplicateTitle(sourceTitle: String, duplicateSuffix: String): String {
    val suffix = duplicateSuffix.trim().takeIf(String::isNotEmpty)
        ?.let { " ($it)" }.orEmpty()
    val baseLength = (MinkBoardRules.MAX_CARD_TITLE_LENGTH - suffix.length).coerceAtLeast(0)
    return (sourceTitle.take(baseLength).trimEnd() + suffix)
        .take(MinkBoardRules.MAX_CARD_TITLE_LENGTH)
}
