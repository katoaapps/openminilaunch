package com.katoaapps.minkboard.data

import com.katoaapps.minkboard.model.PortableBoardSnapshot
import com.katoaapps.minkboard.model.PortableCardSnapshot
import com.katoaapps.minkboard.model.PortableStatusSnapshot
import com.katoaapps.minkboard.model.PortableSubtaskSnapshot
import com.katoaapps.minkboard.model.PortableWorkspaceSnapshot
import org.json.JSONArray
import org.json.JSONObject

/** Named-field JSON for the decrypted, versioned portable snapshot. */
internal class PortableArchiveJsonCodec {
    fun encode(snapshot: PortableWorkspaceSnapshot): String = JSONObject()
        .put("format", snapshot.format)
        .put("schemaVersion", snapshot.schemaVersion)
        .put("exportedAtEpochMillis", snapshot.exportedAtEpochMillis)
        .put("sourceApp", snapshot.sourceApp)
        .put("boards", JSONArray().apply {
            snapshot.boards.forEach { put(encodeBoard(it)) }
        })
        .toString()

    fun decode(raw: String): PortableWorkspaceSnapshot {
        val root = JSONObject(raw)
        return PortableWorkspaceSnapshot(
            format = root.getString("format"),
            schemaVersion = root.getInt("schemaVersion"),
            exportedAtEpochMillis = root.getLong("exportedAtEpochMillis"),
            sourceApp = root.getString("sourceApp"),
            boards = root.getJSONArray("boards").mapObjects(::decodeBoard),
        )
    }

    private fun encodeBoard(board: PortableBoardSnapshot): JSONObject = JSONObject()
        .put("id", board.id)
        .put("name", board.name)
        .put("isLocked", board.isLocked)
        .put("position", board.position)
        .put("createdAtEpochMillis", board.createdAtEpochMillis)
        .put("statuses", JSONArray().apply {
            board.statuses.forEach { put(encodeStatus(it)) }
        })

    private fun decodeBoard(value: JSONObject): PortableBoardSnapshot = PortableBoardSnapshot(
        id = value.getString("id"),
        name = value.getString("name"),
        isLocked = value.getBoolean("isLocked"),
        position = value.getInt("position"),
        createdAtEpochMillis = value.getLong("createdAtEpochMillis"),
        statuses = value.getJSONArray("statuses").mapObjects(::decodeStatus),
    )

    private fun encodeStatus(status: PortableStatusSnapshot): JSONObject = JSONObject()
        .put("id", status.id)
        .put("name", status.name)
        .put("colorArgb", status.colorArgb)
        .put("isDoneStatus", status.isDoneStatus)
        .put("position", status.position)
        .put("cards", JSONArray().apply {
            status.cards.forEach { put(encodeCard(it)) }
        })

    private fun decodeStatus(value: JSONObject): PortableStatusSnapshot = PortableStatusSnapshot(
        id = value.getString("id"),
        name = value.getString("name"),
        colorArgb = value.getInt("colorArgb"),
        isDoneStatus = value.getBoolean("isDoneStatus"),
        position = value.getInt("position"),
        cards = value.getJSONArray("cards").mapObjects(::decodeCard),
    )

    private fun encodeCard(card: PortableCardSnapshot): JSONObject = JSONObject()
        .put("id", card.id)
        .put("title", card.title)
        .put("details", card.details)
        .put("dueDateIso", card.dueDateIso ?: JSONObject.NULL)
        .put("isMajor", card.isMajor)
        .put("position", card.position)
        .put("createdAtEpochMillis", card.createdAtEpochMillis)
        .put("updatedAtEpochMillis", card.updatedAtEpochMillis)
        .put("subtasks", JSONArray().apply {
            card.subtasks.forEach { put(encodeSubtask(it)) }
        })

    private fun decodeCard(value: JSONObject): PortableCardSnapshot = PortableCardSnapshot(
        id = value.getString("id"),
        title = value.getString("title"),
        details = value.getString("details"),
        dueDateIso = if (value.isNull("dueDateIso")) null else value.getString("dueDateIso"),
        isMajor = value.getBoolean("isMajor"),
        position = value.getInt("position"),
        createdAtEpochMillis = value.getLong("createdAtEpochMillis"),
        updatedAtEpochMillis = value.getLong("updatedAtEpochMillis"),
        subtasks = value.getJSONArray("subtasks").mapObjects(::decodeSubtask),
    )

    private fun encodeSubtask(subtask: PortableSubtaskSnapshot): JSONObject = JSONObject()
        .put("id", subtask.id)
        .put("text", subtask.text)
        .put("isCompleted", subtask.isCompleted)
        .put("position", subtask.position)

    private fun decodeSubtask(value: JSONObject): PortableSubtaskSnapshot = PortableSubtaskSnapshot(
        id = value.getString("id"),
        text = value.getString("text"),
        isCompleted = value.getBoolean("isCompleted"),
        position = value.getInt("position"),
    )

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        (0 until length()).map { index -> transform(getJSONObject(index)) }
}
