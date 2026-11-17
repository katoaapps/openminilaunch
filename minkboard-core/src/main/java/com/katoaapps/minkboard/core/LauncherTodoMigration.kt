package com.katoaapps.minkboard.core

import com.katoaapps.minkboard.model.Board
import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.CardDraft
import com.katoaapps.minkboard.model.StatusId
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import java.util.UUID

/** Pure migration and projection rules shared by OpenMink and PlayMink host adapters. */
class LauncherTodoNormalizer(
    private val idFactory: IdFactory,
    private val clockMillis: () -> Long,
) {
    /** Applies the same non-truncating text rule to Magic Box intake as installed-data migration. */
    fun intakeDraft(text: String): CardDraft {
        val (title, details) = splitLegacyText(text)
        return CardDraft(title = title, details = details)
    }

    fun plan(
        source: LegacyTodoSource,
        sourceSchemaVersion: Int,
        boardName: String = DEFAULT_BOARD_NAME,
    ): LauncherTodoMigrationOutcome = when (source) {
        LegacyTodoSource.Missing -> LauncherTodoMigrationOutcome.NoLegacyData
        LegacyTodoSource.Corrupt -> LauncherTodoMigrationOutcome.CorruptLegacyData
        is LegacyTodoSource.Available -> {
            require(sourceSchemaVersion > 0) { "Source schema version must be positive" }
            if (source.items.any { item -> runCatching { intakeDraft(item.text) }.isFailure }) {
                LauncherTodoMigrationOutcome.CorruptLegacyData
            } else {
                LauncherTodoMigrationOutcome.Ready(
                    migrationPlan(source.items, sourceSchemaVersion, boardName),
                )
            }
        }
    }

    fun project(board: BoardSnapshot): LegacyTodoProjection {
        val orderedStatuses = board.statuses.sortedBy(BoardStatus::position)
        val projectionOrder = orderedStatuses.filterNot(BoardStatus::isDoneStatus) +
            orderedStatuses.filter(BoardStatus::isDoneStatus)
        val items = projectionOrder.flatMap { status ->
            board.cardsIn(status.id).map { card ->
                LegacyLauncherTodo(
                    id = card.id.value,
                    text = flattenCardText(card.title, card.details),
                    completed = status.isDoneStatus,
                )
            }
        }
        return LegacyTodoProjection(items, fingerprint(items))
    }

    /**
     * The legacy store has only one text field. Preserve an existing visual separator verbatim;
     * native MinkBoard details without one receive a newline so title and details never run
     * together. The rule stabilizes after one projection/migration cycle without dropping text.
     */
    private fun flattenCardText(title: String, details: String): String = when {
        details.isEmpty() -> title
        details.first().isWhitespace() -> title + details
        else -> "$title\n$details"
    }

    fun projectionDecision(
        currentLegacyItems: List<LegacyLauncherTodo>,
        compatibility: LauncherLegacyCompatibilityReceipt,
        canonicalProjection: LegacyTodoProjection,
    ): LegacyProjectionDecision {
        val currentFingerprint = fingerprint(currentLegacyItems)
        val lastWritten = compatibility.lastProjectionFingerprint

        if (currentFingerprint == canonicalProjection.fingerprint) {
            return if (lastWritten == canonicalProjection.fingerprint) {
                LegacyProjectionDecision.UNCHANGED
            } else {
                LegacyProjectionDecision.ADOPT_CANONICAL_PROJECTION
            }
        }

        if (lastWritten == null) {
            return if (currentFingerprint == compatibility.sourceFingerprint) {
                LegacyProjectionDecision.WRITE_CANONICAL_PROJECTION
            } else {
                LegacyProjectionDecision.DIVERGED
            }
        }
        return if (currentFingerprint == lastWritten) {
            LegacyProjectionDecision.WRITE_CANONICAL_PROJECTION
        } else {
            LegacyProjectionDecision.DIVERGED
        }
    }

    fun fingerprint(items: List<LegacyLauncherTodo>): String {
        val bytes = ByteArrayOutputStream().use { buffer ->
            DataOutputStream(buffer).use { output ->
                output.writeInt(items.size)
                items.forEach { item ->
                    output.writeLengthPrefixed(item.id)
                    output.writeLengthPrefixed(item.text)
                    output.writeBoolean(item.completed)
                }
            }
            buffer.toByteArray()
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }
    }

    private fun migrationPlan(
        items: List<LegacyLauncherTodo>,
        sourceSchemaVersion: Int,
        boardName: String,
    ): LauncherTodoMigrationPlan {
        val usedIds = mutableSetOf<String>()
        val boardId = BoardId(freshId(usedIds))
        val todoStatusId = StatusId(freshId(usedIds))
        val doneStatusId = StatusId(freshId(usedIds))
        val now = clockMillis()
        val defaults = MinkBoardDefaults.statuses
        val todoDefault = defaults.first { !it.isDone }
        val doneDefault = defaults.first { it.isDone }

        val statuses = listOf(
            BoardStatus(
                id = todoStatusId,
                boardId = boardId,
                name = todoDefault.name,
                colorArgb = todoDefault.colorArgb,
                isDoneStatus = false,
                position = 0,
            ),
            BoardStatus(
                id = doneStatusId,
                boardId = boardId,
                name = doneDefault.name,
                colorArgb = doneDefault.colorArgb,
                isDoneStatus = true,
                position = 1,
            ),
        )
        val unfinished = items.filterNot(LegacyLauncherTodo::completed)
        val completed = items.filter(LegacyLauncherTodo::completed)
        val cards = buildList {
            unfinished.forEachIndexed { position, item ->
                add(item.toCard(todoStatusId, position, now, usedIds))
            }
            completed.forEachIndexed { position, item ->
                add(item.toCard(doneStatusId, position, now, usedIds))
            }
        }
        val snapshot = BoardSnapshot(
            board = Board(
                id = boardId,
                name = MinkBoardRules.boardName(boardName),
                isLocked = false,
                position = 0,
                createdAtEpochMillis = now,
            ),
            statuses = statuses,
            cards = cards,
        )
        return LauncherTodoMigrationPlan(
            board = snapshot,
            designation = LauncherBoardDesignation(
                generation = freshId(usedIds),
                boardId = boardId,
                intakeStatusId = todoStatusId,
                designatedAtEpochMillis = now,
                legacyCompatibility = LauncherLegacyCompatibilityReceipt(
                    state = LauncherTodoCompatibilityState.CANONICAL_COMPAT,
                    sourceSchemaVersion = sourceSchemaVersion,
                    sourceFingerprint = fingerprint(items),
                    lastProjectionFingerprint = null,
                    migratedAtEpochMillis = now,
                ),
            ),
        )
    }

    private fun LegacyLauncherTodo.toCard(
        statusId: StatusId,
        position: Int,
        now: Long,
        usedIds: MutableSet<String>,
    ): BoardCard {
        val draft = intakeDraft(text)
        val cardId = id.takeIf { it.isCanonicalUuid() && usedIds.add(it) }
            ?: freshId(usedIds)
        return BoardCard(
            id = CardId(cardId),
            statusId = statusId,
            title = draft.title,
            details = draft.details,
            dueDate = null,
            isMajor = false,
            position = position,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
    }

    /** Keeps ordinary one-line To-Dos title-only without truncating larger meaningful content. */
    private fun splitLegacyText(value: String): Pair<String, String> {
        val normalized = value.trim()
        require(normalized.isNotEmpty()) { "Legacy To-Do text cannot be blank" }
        if (normalized.length <= MinkBoardRules.MAX_CARD_TITLE_LENGTH &&
            '\n' !in normalized &&
            '\r' !in normalized
        ) {
            return MinkBoardRules.cardTitle(normalized) to ""
        }

        val firstLineEnd = normalized.indexOfAny(charArrayOf('\r', '\n'))
            .takeIf { it in 1..MinkBoardRules.MAX_CARD_TITLE_LENGTH }
        val titleEnd = firstLineEnd ?: minOf(
            normalized.length,
            MinkBoardRules.MAX_CARD_TITLE_LENGTH,
        )
        val title = normalized.substring(0, titleEnd).trimEnd()
        val projectedDetails = normalized.substring(titleEnd)
        // A native max-size card may use the full title and details capacities. Projection adds one
        // newline between those fields, so remove only that exact one-character boundary overflow.
        // Every user-authored character remains; the next projection restores the same separator.
        val details = projectedDetails.dropProjectionSeparatorAtExactLimit()
        return MinkBoardRules.cardTitle(title) to MinkBoardRules.cardDetails(details)
    }

    private fun String.dropProjectionSeparatorAtExactLimit(): String =
        if (length == MinkBoardRules.MAX_CARD_DETAILS_LENGTH + 1 && startsWith('\n')) {
            drop(1)
        } else {
            this
        }

    private fun freshId(usedIds: MutableSet<String>): String {
        repeat(MAX_ID_GENERATION_ATTEMPTS) {
            val candidate = idFactory.create()
            if (candidate.isNotBlank() && candidate.length <= MAX_ID_LENGTH && usedIds.add(candidate)) {
                return candidate
            }
        }
        error("Could not generate a unique launcher migration ID")
    }

    private fun String.isCanonicalUuid(): Boolean = runCatching {
        UUID.fromString(this).toString().equals(this, ignoreCase = true)
    }.getOrDefault(false)

    private fun DataOutputStream.writeLengthPrefixed(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeInt(bytes.size)
        write(bytes)
    }

    private companion object {
        const val DEFAULT_BOARD_NAME = "To-Dos"
        const val MAX_ID_GENERATION_ATTEMPTS = 1_000
        const val MAX_ID_LENGTH = 200
    }
}
