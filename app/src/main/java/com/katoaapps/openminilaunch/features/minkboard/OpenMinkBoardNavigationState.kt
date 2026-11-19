package com.katoaapps.openminilaunch.features.minkboard

import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.StatusId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Process-session navigation state. It is intentionally absent from backup and restore. */
internal class OpenMinkBoardNavigationState {
    private val mutableRequest = MutableStateFlow<OpenMinkBoardNavigationRequest?>(null)
    val request: StateFlow<OpenMinkBoardNavigationRequest?> = mutableRequest

    private var nextToken = 0L
    private var compactHomePosition: OpenMinkCompactHomePosition? = null

    fun request(
        boardId: BoardId,
        cardId: CardId? = null,
        statusId: StatusId? = null,
    ) {
        mutableRequest.value = OpenMinkBoardNavigationRequest(
            token = ++nextToken,
            boardId = boardId,
            cardId = cardId,
            statusId = statusId,
        )
    }

    fun consume(token: Long) {
        if (mutableRequest.value?.token == token) mutableRequest.value = null
    }

    fun compactHomePosition(): OpenMinkCompactHomePosition? = compactHomePosition

    fun rememberCompactHomePosition(position: OpenMinkCompactHomePosition) {
        compactHomePosition = position
    }
}

internal data class OpenMinkCompactHomePosition(
    val boardId: BoardId,
    val statusId: StatusId,
    val statusPage: Int,
)

internal data class OpenMinkBoardNavigationRequest(
    val token: Long,
    val boardId: BoardId,
    val cardId: CardId?,
    val statusId: StatusId?,
)
