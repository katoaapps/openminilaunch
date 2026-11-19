package com.katoaapps.minkboard.focus.android

import android.content.Intent
import android.net.Uri
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.FocusTimerId

data class MinkBoardFocusNavigationRequest(
    val timerId: FocusTimerId,
    val boardId: BoardId,
    val cardId: CardId,
)

object MinkBoardFocusIntents {
    const val ACTION_ALARM = "com.katoaapps.minkboard.focus.ALARM"
    const val ACTION_STOP = "com.katoaapps.minkboard.focus.STOP"
    const val ACTION_DISMISS = "com.katoaapps.minkboard.focus.DISMISS"
    const val ACTION_OPEN = "com.katoaapps.minkboard.focus.OPEN"

    const val EXTRA_TIMER_ID = "minkboard_focus_timer_id"
    const val EXTRA_BOARD_ID = "minkboard_focus_board_id"
    const val EXTRA_CARD_ID = "minkboard_focus_card_id"

    fun navigationRequest(intent: Intent?): MinkBoardFocusNavigationRequest? {
        if (intent?.action != ACTION_OPEN) return null
        val timerId = intent.getStringExtra(EXTRA_TIMER_ID)
            ?.let { value -> runCatching { FocusTimerId(value) }.getOrNull() }
            ?: return null
        val boardId = intent.getStringExtra(EXTRA_BOARD_ID)
            ?.let { value -> runCatching { BoardId(value) }.getOrNull() }
            ?: return null
        val cardId = intent.getStringExtra(EXTRA_CARD_ID)
            ?.let { value -> runCatching { CardId(value) }.getOrNull() }
            ?: return null
        return MinkBoardFocusNavigationRequest(timerId, boardId, cardId)
    }

    internal fun timerId(intent: Intent): FocusTimerId? = intent.getStringExtra(EXTRA_TIMER_ID)
        ?.let { value -> runCatching { FocusTimerId(value) }.getOrNull() }

    internal fun data(action: String, timerId: FocusTimerId): Uri = Uri.Builder()
        .scheme("minkboard")
        .authority("focus")
        .appendPath(action.substringAfterLast('.').lowercase())
        .appendPath(timerId.value)
        .build()

    internal fun decorateOpenIntent(
        intent: Intent,
        snapshot: com.katoaapps.minkboard.model.FocusTimerSnapshot,
    ): Intent = intent.apply {
        action = ACTION_OPEN
        data = data(ACTION_OPEN, snapshot.session.id)
        flags = flags or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(EXTRA_TIMER_ID, snapshot.session.id.value)
        putExtra(EXTRA_BOARD_ID, snapshot.session.boardId.value)
        putExtra(EXTRA_CARD_ID, snapshot.session.cardId.value)
    }
}
