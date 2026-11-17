package com.katoaapps.openminilaunch.features.minkboard

import android.content.SharedPreferences
import com.katoaapps.minkboard.model.BoardId

/** Launcher-owned navigation memory; board contents remain exclusively in the shared repository. */
internal class OpenMinkBoardUiPreferences(
    private val preferences: SharedPreferences,
) {
    fun readLastOpenBoardId(): BoardId? = preferences
        .getString(LAST_OPEN_BOARD_ID_KEY, null)
        ?.takeIf(String::isNotBlank)
        ?.let(::BoardId)

    fun writeLastOpenBoardId(boardId: BoardId) {
        preferences.edit().putString(LAST_OPEN_BOARD_ID_KEY, boardId.value).apply()
    }

    private companion object {
        const val LAST_OPEN_BOARD_ID_KEY = "minkboard_last_open_board_id"
    }
}
