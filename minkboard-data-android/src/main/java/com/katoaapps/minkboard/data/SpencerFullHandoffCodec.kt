package com.katoaapps.minkboard.data

import com.katoaapps.minkboard.core.IdFactory
import com.katoaapps.minkboard.core.PortableArchiveValidator
import com.katoaapps.minkboard.core.SpencerLegacyMapper
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.PortableBoardSnapshot
import com.katoaapps.minkboard.model.PortableCardSnapshot
import com.katoaapps.minkboard.model.PortableStatusSnapshot
import com.katoaapps.minkboard.model.PortableSubtaskSnapshot
import com.katoaapps.minkboard.model.PortableWorkspaceSnapshot
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

data class SpencerFullHandoff(
    val exportedAtEpochMillis: Long,
    val sourceAppVersion: String,
    val boards: List<SpencerHandoffBoard>,
)

data class SpencerHandoffBoard(
    val legacyId: Int,
    val name: String,
    val isLocked: Boolean,
    val position: Int,
    val createdAtEpochMillis: Long,
    val statuses: List<SpencerHandoffStatus>,
)

data class SpencerHandoffStatus(
    val legacyId: Int,
    val name: String,
    val colorArgb: Int,
    val isDoneStatus: Boolean,
    val position: Int,
    val cards: List<SpencerHandoffCard>,
)

data class SpencerHandoffCard(
    val legacyId: Int,
    val title: String,
    val details: String,
    val dueDateIso: String?,
    val isMajor: Boolean,
    val position: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val subtasks: List<SpencerHandoffSubtask>,
)

data class SpencerHandoffSubtask(
    val legacyId: Int,
    val text: String,
    val isCompleted: Boolean,
    val position: Int,
)

/** Codec for the complete, named-field handoff that must be emitted by Spencer. */
class SpencerFullHandoffCodec(
    private val idFactory: IdFactory,
    private val validator: PortableArchiveValidator = PortableArchiveValidator(),
) {
    fun encode(source: SpencerFullHandoff): String {
        // Mapping also performs the complete MinkBoard-side validation before export fixtures ship.
        var validationId = 0
        mapToPortable(source, IdFactory { "validation-${validationId++}" })
        val payloadJson = encodePayload(source).toString()
        return JSONObject()
            .put("format", FORMAT)
            .put("schemaVersion", SCHEMA_VERSION)
            .put("checksumSha256", sha256(payloadJson))
            .put("payloadJson", payloadJson)
            .toString()
    }

    fun decode(raw: String): PortableWorkspaceSnapshot {
        require(raw.toByteArray(StandardCharsets.UTF_8).size <= MAX_BYTES) {
            "Spencer handoff is too large"
        }
        val envelope = JSONObject(raw)
        require(envelope.getString("format") == FORMAT) { "Unsupported Spencer handoff format" }
        require(envelope.getInt("schemaVersion") == SCHEMA_VERSION) {
            "Unsupported Spencer handoff schema version"
        }
        val payloadJson = envelope.getString("payloadJson")
        val expectedChecksum = envelope.getString("checksumSha256")
        val actualChecksum = sha256(payloadJson)
        require(
            MessageDigest.isEqual(
                expectedChecksum.toByteArray(StandardCharsets.US_ASCII),
                actualChecksum.toByteArray(StandardCharsets.US_ASCII),
            ),
        ) { "Spencer handoff checksum is invalid" }
        return mapToPortable(decodePayload(JSONObject(payloadJson)), idFactory)
    }

    private fun mapToPortable(
        source: SpencerFullHandoff,
        destinationIdFactory: IdFactory,
    ): PortableWorkspaceSnapshot {
        require(source.sourceAppVersion.isNotBlank()) { "Spencer source version cannot be blank" }
        require(source.boards.size <= MAX_BOARDS) { "Spencer handoff contains too many boards" }
        requireUniqueLegacyIds("board", source.boards.map { it.legacyId })
        val snapshot = PortableWorkspaceSnapshot(
            exportedAtEpochMillis = source.exportedAtEpochMillis,
            sourceApp = "Spencer ${source.sourceAppVersion}",
            boards = source.boards.map { board ->
                requireUniqueLegacyIds("status", board.statuses.map { it.legacyId })
                requireUniqueLegacyIds("card", board.statuses.flatMap { it.cards }.map { it.legacyId })
                requireUniqueLegacyIds(
                    "subtask",
                    board.statuses.flatMap { status -> status.cards.flatMap { it.subtasks } }
                        .map { it.legacyId },
                )
                PortableBoardSnapshot(
                    id = destinationIdFactory.create(),
                    name = board.name,
                    isLocked = board.isLocked,
                    position = board.position,
                    createdAtEpochMillis = board.createdAtEpochMillis,
                    statuses = board.statuses.map { status ->
                        requireUniqueLegacyIds("card", status.cards.map { it.legacyId })
                        PortableStatusSnapshot(
                            id = destinationIdFactory.create(),
                            name = status.name,
                            colorArgb = status.colorArgb,
                            isDoneStatus = status.isDoneStatus,
                            position = status.position,
                            cards = status.cards.map { card ->
                                requireUniqueLegacyIds("subtask", card.subtasks.map { it.legacyId })
                                PortableCardSnapshot(
                                    id = destinationIdFactory.create(),
                                    title = card.title,
                                    details = card.details,
                                    dueDateIso = card.dueDateIso,
                                    isMajor = card.isMajor,
                                    position = card.position,
                                    createdAtEpochMillis = card.createdAtEpochMillis,
                                    updatedAtEpochMillis = card.updatedAtEpochMillis,
                                    subtasks = card.subtasks.map { subtask ->
                                        PortableSubtaskSnapshot(
                                            id = destinationIdFactory.create(),
                                            text = subtask.text,
                                            isCompleted = subtask.isCompleted,
                                            position = subtask.position,
                                        )
                                    },
                                )
                            },
                        )
                    },
                )
            },
        )
        return validator.validate(snapshot)
    }

    private fun encodePayload(source: SpencerFullHandoff): JSONObject = JSONObject()
        .put("exportedAtEpochMillis", source.exportedAtEpochMillis)
        .put("sourceAppVersion", source.sourceAppVersion)
        .put("boards", JSONArray().apply {
            source.boards.forEach { board ->
                put(
                    JSONObject()
                        .put("legacyId", board.legacyId)
                        .put("name", board.name)
                        .put("isLocked", board.isLocked)
                        .put("position", board.position)
                        .put("createdAtEpochMillis", board.createdAtEpochMillis)
                        .put("statuses", JSONArray().apply {
                            board.statuses.forEach { status -> put(encodeStatus(status)) }
                        }),
                )
            }
        })

    private fun encodeStatus(status: SpencerHandoffStatus): JSONObject = JSONObject()
        .put("legacyId", status.legacyId)
        .put("name", status.name)
        .put("colorArgb", status.colorArgb)
        .put("isDoneStatus", status.isDoneStatus)
        .put("position", status.position)
        .put("cards", JSONArray().apply {
            status.cards.forEach { card ->
                put(
                    JSONObject()
                        .put("legacyId", card.legacyId)
                        .put("title", card.title)
                        .put("details", card.details)
                        .put("dueDateIso", card.dueDateIso ?: JSONObject.NULL)
                        .put("isMajor", card.isMajor)
                        .put("position", card.position)
                        .put("createdAtEpochMillis", card.createdAtEpochMillis)
                        .put("updatedAtEpochMillis", card.updatedAtEpochMillis)
                        .put("subtasks", JSONArray().apply {
                            card.subtasks.forEach { subtask ->
                                put(
                                    JSONObject()
                                        .put("legacyId", subtask.legacyId)
                                        .put("text", subtask.text)
                                        .put("isCompleted", subtask.isCompleted)
                                        .put("position", subtask.position),
                                )
                            }
                        }),
                )
            }
        })

    private fun decodePayload(payload: JSONObject): SpencerFullHandoff = SpencerFullHandoff(
        exportedAtEpochMillis = payload.getLong("exportedAtEpochMillis"),
        sourceAppVersion = payload.getString("sourceAppVersion"),
        boards = payload.getJSONArray("boards").mapObjects { board ->
            SpencerHandoffBoard(
                legacyId = board.getInt("legacyId"),
                name = board.getString("name"),
                isLocked = board.getBoolean("isLocked"),
                position = board.getInt("position"),
                createdAtEpochMillis = board.getLong("createdAtEpochMillis"),
                statuses = board.getJSONArray("statuses").mapObjects(::decodeStatus),
            )
        },
    )

    private fun decodeStatus(status: JSONObject): SpencerHandoffStatus = SpencerHandoffStatus(
        legacyId = status.getInt("legacyId"),
        name = status.getString("name"),
        colorArgb = status.getInt("colorArgb"),
        isDoneStatus = status.getBoolean("isDoneStatus"),
        position = status.getInt("position"),
        cards = status.getJSONArray("cards").mapObjects { card ->
            SpencerHandoffCard(
                legacyId = card.getInt("legacyId"),
                title = card.getString("title"),
                details = card.getString("details"),
                dueDateIso = if (card.isNull("dueDateIso")) null else card.getString("dueDateIso"),
                isMajor = card.getBoolean("isMajor"),
                position = card.getInt("position"),
                createdAtEpochMillis = card.getLong("createdAtEpochMillis"),
                updatedAtEpochMillis = card.getLong("updatedAtEpochMillis"),
                subtasks = card.getJSONArray("subtasks").mapObjects { subtask ->
                    SpencerHandoffSubtask(
                        legacyId = subtask.getInt("legacyId"),
                        text = subtask.getString("text"),
                        isCompleted = subtask.getBoolean("isCompleted"),
                        position = subtask.getInt("position"),
                    )
                },
            )
        },
    )

    private fun requireUniqueLegacyIds(kind: String, values: List<Int>) {
        require(values.size == values.toSet().size) { "Spencer handoff contains duplicate $kind IDs" }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        (0 until length()).map { transform(getJSONObject(it)) }

    companion object {
        const val FORMAT = "spencer-full-handoff"
        const val SCHEMA_VERSION = 1
        const val MAX_BYTES = 32 * 1024 * 1024
        private const val MAX_BOARDS = 500
    }
}

enum class SpencerImportFormat {
    FullHandoffV1,
    PublicBoardExportV1,
}

/** Detects both Spencer formats and adapts either one into the same validated portable snapshot. */
class SpencerImportAdapter(
    private val idFactory: IdFactory,
    private val clockMillis: () -> Long,
) {
    fun detect(raw: String): SpencerImportFormat {
        val root = JSONObject(raw)
        return when (val format = root.optString("format")) {
            SpencerFullHandoffCodec.FORMAT -> SpencerImportFormat.FullHandoffV1
            "" -> SpencerImportFormat.PublicBoardExportV1
            else -> throw IllegalArgumentException("Unsupported Spencer import format: $format")
        }
    }

    fun parse(raw: String, publicExportBoardName: String? = null): PortableWorkspaceSnapshot =
        when (detect(raw)) {
            SpencerImportFormat.FullHandoffV1 -> SpencerFullHandoffCodec(idFactory).decode(raw)
            SpencerImportFormat.PublicBoardExportV1 -> {
                val requestedName = requireNotNull(publicExportBoardName) {
                    "A board name is required for the public Spencer export"
                }
                val snapshot = SpencerLegacyMapper(idFactory, clockMillis).map(
                    SpencerLegacyJsonParser().parse(raw),
                    requestedName,
                )
                PortableArchiveValidator().validate(snapshot.toPortable(clockMillis()))
            }
        }

    private fun BoardSnapshot.toPortable(exportedAt: Long): PortableWorkspaceSnapshot =
        PortableWorkspaceSnapshot(
            exportedAtEpochMillis = exportedAt,
            sourceApp = "Spencer public export v1",
            boards = listOf(
                PortableBoardSnapshot(
                    id = board.id.value,
                    name = board.name,
                    isLocked = board.isLocked,
                    position = board.position,
                    createdAtEpochMillis = board.createdAtEpochMillis,
                    statuses = statuses.sortedBy { it.position }.map { status ->
                        PortableStatusSnapshot(
                            id = status.id.value,
                            name = status.name,
                            colorArgb = status.colorArgb,
                            isDoneStatus = status.isDoneStatus,
                            position = status.position,
                            cards = cards
                                .filter { it.statusId == status.id }
                                .sortedBy { it.position }
                                .map { card ->
                                    PortableCardSnapshot(
                                        id = card.id.value,
                                        title = card.title,
                                        details = card.details,
                                        dueDateIso = card.dueDate?.toString(),
                                        isMajor = card.isMajor,
                                        position = card.position,
                                        createdAtEpochMillis = card.createdAtEpochMillis,
                                        updatedAtEpochMillis = card.updatedAtEpochMillis,
                                        subtasks = card.subtasks.sortedBy { it.position }.map { subtask ->
                                            PortableSubtaskSnapshot(
                                                id = subtask.id.value,
                                                text = subtask.text,
                                                isCompleted = subtask.isCompleted,
                                                position = subtask.position,
                                            )
                                        },
                                    )
                                },
                        )
                    },
                ),
            ),
        )
}
