package com.katoaapps.minklauncher.todos.home

import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.StatusId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Process-session navigation state. It is intentionally absent from backup and restore. */
internal class TodoNavigationState {
    private val mutableRequest = MutableStateFlow<TodoNavigationRequest?>(null)
    val request: StateFlow<TodoNavigationRequest?> = mutableRequest

    private var nextToken = 0L
    private var compactHomePosition: TodoCompactHomePosition? = null

    fun request(
        boardId: BoardId,
        cardId: CardId? = null,
        statusId: StatusId? = null,
    ) {
        mutableRequest.value = TodoNavigationRequest(
            token = ++nextToken,
            boardId = boardId,
            cardId = cardId,
            statusId = statusId,
        )
    }

    fun consume(token: Long) {
        if (mutableRequest.value?.token == token) mutableRequest.value = null
    }

    fun compactHomePosition(): TodoCompactHomePosition? = compactHomePosition

    fun rememberCompactHomePosition(position: TodoCompactHomePosition) {
        compactHomePosition = position
    }
}

internal data class TodoCompactHomePosition(
    val boardId: BoardId,
    val statusId: StatusId,
    val statusPage: Int,
)

internal data class TodoNavigationRequest(
    val token: Long,
    val boardId: BoardId,
    val cardId: CardId?,
    val statusId: StatusId?,
)
