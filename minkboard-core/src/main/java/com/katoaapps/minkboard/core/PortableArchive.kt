package com.katoaapps.minkboard.core

import com.katoaapps.minkboard.model.PortableWorkspaceSnapshot
import com.katoaapps.minkboard.model.BoardId
import java.time.DateTimeException
import java.time.LocalDate

enum class WorkspaceImportMode {
    Merge,
    Replace,
}

/** Returns a valid portable snapshot containing exactly the requested boards. */
fun PortableWorkspaceSnapshot.selectBoardsForExport(
    boardIds: Set<BoardId>,
): PortableWorkspaceSnapshot {
    require(boardIds.isNotEmpty()) { "Select at least one board to export" }
    val selectedIdValues = boardIds.mapTo(hashSetOf()) { it.value }
    val selectedBoards = boards
        .filter { it.id in selectedIdValues }
        .mapIndexed { position, board -> board.copy(position = position) }
    require(selectedBoards.size == selectedIdValues.size) {
        "One or more selected boards are no longer available"
    }
    return copy(boards = selectedBoards)
}

/**
 * Validates an entire decrypted archive before the data layer starts an import transaction.
 * Validation is intentionally strict: malformed input is rejected rather than normalized.
 */
class PortableArchiveValidator {
    fun validate(snapshot: PortableWorkspaceSnapshot): PortableWorkspaceSnapshot {
        require(snapshot.format == PortableWorkspaceSnapshot.FORMAT) {
            "Unsupported archive format: ${snapshot.format}"
        }
        require(snapshot.schemaVersion == PortableWorkspaceSnapshot.SCHEMA_VERSION) {
            "Unsupported archive schema version: ${snapshot.schemaVersion}"
        }
        require(snapshot.exportedAtEpochMillis >= 0) { "Export timestamp cannot be negative" }
        require(snapshot.sourceApp.isNotBlank()) { "Archive source app cannot be blank" }
        require(snapshot.sourceApp.length <= MAX_SOURCE_APP_LENGTH) {
            "Archive source app is too long"
        }
        require(snapshot.boards.size <= MAX_BOARDS) { "Archive contains too many boards" }
        requireExactPositions("Board", snapshot.boards.map { it.position })
        requireUniqueIds("board", snapshot.boards.map { it.id })

        val statusIds = mutableListOf<String>()
        val cardIds = mutableListOf<String>()
        val subtaskIds = mutableListOf<String>()
        var statusCount = 0
        var cardCount = 0
        var subtaskCount = 0

        snapshot.boards.forEach { board ->
            requireValidId("Board", board.id)
            require(MinkBoardRules.boardName(board.name) == board.name) {
                "Board name must already be normalized"
            }
            require(board.createdAtEpochMillis >= 0) { "Board timestamp cannot be negative" }
            require(board.statuses.isNotEmpty()) { "Every board must contain at least one status" }
            require(board.statuses.size <= MAX_STATUSES_PER_BOARD) {
                "Board contains too many statuses"
            }
            require(board.statuses.count { it.isDoneStatus } <= 1) {
                "A board cannot contain multiple Done statuses"
            }
            requireExactPositions("Status", board.statuses.map { it.position })

            board.statuses.forEach { status ->
                statusCount++
                requireValidId("Status", status.id)
                statusIds += status.id
                require(MinkBoardRules.statusName(status.name) == status.name) {
                    "Status name must already be normalized"
                }
                requireExactPositions("Card", status.cards.map { it.position })

                status.cards.forEach { card ->
                    cardCount++
                    requireValidId("Card", card.id)
                    cardIds += card.id
                    require(MinkBoardRules.cardTitle(card.title) == card.title) {
                        "Card title must already be normalized"
                    }
                    require(MinkBoardRules.cardDetails(card.details) == card.details) {
                        "Card details are invalid"
                    }
                    card.dueDateIso?.let(::requireIsoDate)
                    require(card.createdAtEpochMillis >= 0 && card.updatedAtEpochMillis >= 0) {
                        "Card timestamps cannot be negative"
                    }
                    require(card.updatedAtEpochMillis >= card.createdAtEpochMillis) {
                        "Card update time cannot precede its creation time"
                    }
                    require(card.subtasks.size <= MAX_SUBTASKS_PER_CARD) {
                        "Card contains too many subtasks"
                    }
                    requireExactPositions("Subtask", card.subtasks.map { it.position })

                    card.subtasks.forEach { subtask ->
                        subtaskCount++
                        requireValidId("Subtask", subtask.id)
                        subtaskIds += subtask.id
                        require(MinkBoardRules.subtaskText(subtask.text) == subtask.text) {
                            "Subtask text must already be normalized"
                        }
                    }
                }
            }
        }

        require(statusCount <= MAX_STATUSES) { "Archive contains too many statuses" }
        require(cardCount <= MAX_CARDS) { "Archive contains too many cards" }
        require(subtaskCount <= MAX_SUBTASKS) { "Archive contains too many subtasks" }
        requireUniqueIds("status", statusIds)
        requireUniqueIds("card", cardIds)
        requireUniqueIds("subtask", subtaskIds)
        return snapshot
    }

    private fun requireValidId(label: String, value: String) {
        require(value.isNotBlank()) { "$label ID cannot be blank" }
        require(value.length <= MAX_ID_LENGTH) { "$label ID is too long" }
    }

    private fun requireUniqueIds(label: String, values: List<String>) {
        require(values.size == values.toSet().size) { "Archive contains duplicate $label IDs" }
    }

    private fun requireExactPositions(label: String, positions: List<Int>) {
        require(positions.sorted() == positions.indices.toList()) {
            "$label positions must be unique and contiguous from zero"
        }
    }

    private fun requireIsoDate(value: String) {
        try {
            LocalDate.parse(value)
        } catch (_: DateTimeException) {
            throw IllegalArgumentException("Due date must use ISO-8601 yyyy-MM-dd format")
        }
    }

    private companion object {
        const val MAX_SOURCE_APP_LENGTH = 100
        const val MAX_ID_LENGTH = 200
        const val MAX_BOARDS = 500
        const val MAX_STATUSES_PER_BOARD = 100
        const val MAX_SUBTASKS_PER_CARD = 1_000
        const val MAX_STATUSES = 10_000
        const val MAX_CARDS = 20_000
        const val MAX_SUBTASKS = 100_000
    }
}
