package com.katoaapps.minkboard.core

import com.katoaapps.minkboard.model.Board
import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.StatusId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class SpencerLegacyBoard(
    val statuses: List<SpencerLegacyStatus>,
)

data class SpencerLegacyStatus(
    val legacyId: Int,
    val name: String,
    val hexColor: String,
    val isDone: Boolean,
    val tasks: List<SpencerLegacyTask>,
)

data class SpencerLegacyTask(
    val legacyId: Int,
    val message: String,
    val dueDate: String,
    val isMajor: Boolean,
)

fun interface IdFactory {
    fun create(): String
}

class SpencerLegacyMapper(
    private val idFactory: IdFactory,
    private val clockMillis: () -> Long,
) {
    fun map(source: SpencerLegacyBoard, requestedBoardName: String): BoardSnapshot {
        require(source.statuses.isNotEmpty()) { "A Spencer board must contain at least one status" }
        require(source.statuses.map { it.legacyId }.distinct().size == source.statuses.size) {
            "A Spencer board cannot contain duplicate status IDs"
        }
        require(source.statuses.count { it.isDone } <= 1) {
            "A Spencer board cannot contain multiple Done statuses"
        }

        val boardId = BoardId(idFactory.create())
        val now = clockMillis()
        val statusesByLegacyId = source.statuses.mapIndexed { position, legacy ->
            legacy.legacyId to BoardStatus(
                id = StatusId(idFactory.create()),
                boardId = boardId,
                name = MinkBoardRules.statusName(legacy.name),
                colorArgb = parseColor(legacy.hexColor),
                isDoneStatus = legacy.isDone,
                position = position,
            )
        }.toMap()

        val cards = source.statuses.flatMap { legacyStatus ->
            val destination = requireNotNull(statusesByLegacyId[legacyStatus.legacyId])
            legacyStatus.tasks.mapIndexed { position, legacyTask ->
                val importedText = legacyTask.message.toImportCardText()
                BoardCard(
                    id = CardId(idFactory.create()),
                    statusId = destination.id,
                    title = MinkBoardRules.cardTitle(importedText.title),
                    // Spencer's public export has one message field rather than separate title and
                    // details fields. Preserve the original only when the board title must be
                    // shortened; copying a complete short title into Details adds no information.
                    details = MinkBoardRules.cardDetails(importedText.details),
                    dueDate = parseLegacyDate(legacyTask.dueDate),
                    isMajor = legacyTask.isMajor,
                    position = position,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                )
            }
        }

        return BoardSnapshot(
            board = Board(
                id = boardId,
                name = MinkBoardRules.boardName(requestedBoardName),
                isLocked = false,
                position = 0,
                createdAtEpochMillis = now,
            ),
            statuses = statusesByLegacyId.values.sortedBy(BoardStatus::position),
            cards = cards,
        )
    }

    private fun parseColor(raw: String): Int {
        val normalized = raw.removePrefix("#")
        val argb = when (normalized.length) {
            6 -> "FF$normalized"
            8 -> normalized
            else -> error("Unsupported Spencer color: $raw")
        }
        return argb.toLong(16).toInt()
    }

    private fun parseLegacyDate(raw: String): LocalDate? {
        val normalized = raw
            .trim()
            .replace(LEGACY_DUE_DATE_PREFIX, "")
            .trim()
        if (normalized.isBlank() || normalized.equals("N/A", ignoreCase = true)) return null
        return try {
            LocalDate.parse(normalized, DateTimeFormatter.ofPattern("M/d/uuuu"))
        } catch (error: DateTimeParseException) {
            throw IllegalArgumentException("Unsupported Spencer due date: $raw", error)
        }
    }

    private fun String.toImportCardText(): ImportCardText {
        val singleLine = replace(WHITESPACE_RUN, " ").trim()
        if (singleLine.length <= IMPORT_TITLE_LENGTH) {
            return ImportCardText(title = singleLine, details = "")
        }
        return ImportCardText(
            title = singleLine.take(IMPORT_TITLE_LENGTH).trimEnd() + ELLIPSIS,
            details = this,
        )
    }

    private companion object {
        const val IMPORT_TITLE_LENGTH = 30
        const val ELLIPSIS = "…"
        val WHITESPACE_RUN = Regex("\\s+")
        val LEGACY_DUE_DATE_PREFIX = Regex("^Due\\s+Date\\s*:\\s*", RegexOption.IGNORE_CASE)
    }
}

private data class ImportCardText(
    val title: String,
    val details: String,
)
