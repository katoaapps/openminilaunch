package com.katoaapps.minkboard.data

import com.katoaapps.minkboard.core.SpencerLegacyBoard
import com.katoaapps.minkboard.core.SpencerLegacyTask
import com.katoaapps.minkboard.core.SpencerLegacyStatus
import org.json.JSONObject

class SpencerLegacyJsonParser {
    fun parse(raw: String): SpencerLegacyBoard {
        require(raw.toByteArray().size <= MAX_BYTES) { "Spencer export is too large" }
        val root = JSONObject(raw)
        val keys = root.keys().asSequence().toList().sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
        require(keys.size in 1..MAX_STATUSES) { "Spencer export has an unsupported status count" }

        var taskCount = 0
        val statuses = keys.map { key ->
            val value = root.getJSONObject(key)
            val status = value.getJSONArray("taskstatus_info")
            val tasks = value.getJSONArray("tasks")
            taskCount += tasks.length()
            require(taskCount <= MAX_TASKS) { "Spencer export contains too many tasks" }

            SpencerLegacyStatus(
                legacyId = status.getInt(0),
                name = status.getString(1),
                hexColor = status.getString(2),
                isDone = status.optBoolean(3, false),
                tasks = (0 until tasks.length()).map { index ->
                    val task = tasks.getJSONArray(index)
                    SpencerLegacyTask(
                        legacyId = task.getInt(0),
                        message = task.getString(1),
                        dueDate = task.optString(3, "N/A"),
                        isMajor = task.optBoolean(4, false),
                    )
                },
            )
        }
        return SpencerLegacyBoard(statuses)
    }

    private companion object {
        const val MAX_BYTES = 10 * 1024 * 1024
        const val MAX_STATUSES = 100
        const val MAX_TASKS = 20_000
    }
}
