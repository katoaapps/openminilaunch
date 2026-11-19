package com.katoaapps.minklauncher.todos.home

import android.content.SharedPreferences
import com.katoaapps.minkboard.core.LauncherBoardState
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.StatusId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray

internal sealed interface TodoVisibleStatuses {
    data object All : TodoVisibleStatuses

    data class Selected(
        val statusIds: List<StatusId>,
    ) : TodoVisibleStatuses
}

internal class TodoHomePreferences(
    private val preferences: SharedPreferences,
) {
    private val mutableVisibleStatuses = MutableStateFlow(readVisibleStatuses())
    val visibleStatuses: StateFlow<TodoVisibleStatuses> = mutableVisibleStatuses

    fun snapshot(): TodoVisibleStatuses = mutableVisibleStatuses.value

    fun showAllStatuses() = persist(TodoVisibleStatuses.All)

    fun showSelectedStatuses(statusIds: List<StatusId>) {
        persist(
            TodoVisibleStatuses.Selected(
                statusIds = statusIds.distinctBy(StatusId::value),
            ),
        )
    }

    fun restore(visibility: TodoVisibleStatuses) = persist(visibility)

    /**
     * Removes stale status IDs without allowing a broken selection to leave Home blank. The
     * intake is the deterministic fallback because it is already validated by launcher binding.
     */
    fun reconcile(state: LauncherBoardState.Ready): List<StatusId> {
        val resolved = resolveTodoVisibleStatusIds(
            board = state.board,
            intakeStatusId = state.designation.intakeStatusId,
            preference = mutableVisibleStatuses.value,
        )
        val selected = mutableVisibleStatuses.value as? TodoVisibleStatuses.Selected
        if (selected != null && resolved != selected.statusIds) {
            persist(TodoVisibleStatuses.Selected(resolved))
        }
        return resolved
    }

    private fun persist(value: TodoVisibleStatuses) {
        mutableVisibleStatuses.value = value
        val editor = preferences.edit()
        when (value) {
            TodoVisibleStatuses.All -> editor
                .putString(VISIBLE_STATUS_MODE_KEY, VISIBLE_STATUS_MODE_ALL)
                .remove(VISIBLE_STATUS_IDS_KEY)
            is TodoVisibleStatuses.Selected -> editor
                .putString(VISIBLE_STATUS_MODE_KEY, VISIBLE_STATUS_MODE_SELECTED)
                .putString(
                    VISIBLE_STATUS_IDS_KEY,
                    JSONArray().apply { value.statusIds.forEach { put(it.value) } }.toString(),
                )
        }
        editor.apply()
    }

    private fun readVisibleStatuses(): TodoVisibleStatuses {
        if (preferences.getString(VISIBLE_STATUS_MODE_KEY, null) != VISIBLE_STATUS_MODE_SELECTED) {
            return TodoVisibleStatuses.All
        }
        val ids = runCatching {
            val values = JSONArray(preferences.getString(VISIBLE_STATUS_IDS_KEY, "[]") ?: "[]")
            val seen = mutableSetOf<String>()
            buildList<StatusId> {
                repeat(values.length()) { index ->
                    values.optString(index)
                        .takeIf { it.isNotBlank() && seen.add(it) }
                        ?.let(::StatusId)
                        ?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
        return TodoVisibleStatuses.Selected(ids)
    }

    private companion object {
        const val VISIBLE_STATUS_MODE_KEY = "minkboard_home_visible_status_mode"
        const val VISIBLE_STATUS_IDS_KEY = "minkboard_home_visible_status_ids"
        const val VISIBLE_STATUS_MODE_ALL = "all"
        const val VISIBLE_STATUS_MODE_SELECTED = "selected"
    }
}

internal fun resolveTodoVisibleStatusIds(
    board: BoardSnapshot,
    intakeStatusId: StatusId,
    preference: TodoVisibleStatuses,
): List<StatusId> {
    val ordered = board.statuses.sortedBy { it.position }.map { it.id }
    if (preference == TodoVisibleStatuses.All) return ordered

    val requested = (preference as TodoVisibleStatuses.Selected).statusIds.toSet()
    return ordered.filter { it in requested }.ifEmpty {
        listOf(intakeStatusId).filter { it in ordered }
    }
}
