package com.katoaapps.minkboard.data

import com.katoaapps.minkboard.model.Board
import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.BoardSubtask
import com.katoaapps.minkboard.model.BoardSummary
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.minkboard.model.SubtaskId
import java.time.LocalDate

internal class EncryptedEntityMapper(
    private val cipher: FieldCipher,
) {
    // Room invalidates a board relation whenever any card changes. Keep a bounded plaintext cache
    // so remapping a large active board decrypts only fields whose ciphertext actually changed.
    private val boardNameCache = CiphertextCache(maxEntries = 128)
    private val statusNameCache = CiphertextCache(maxEntries = 512)
    private val cardTitleCache = CiphertextCache(maxEntries = 2_048)
    private val cardDetailsCache = CiphertextCache(maxEntries = 2_048)
    private val cardDueDateCache = CiphertextCache(maxEntries = 2_048)
    private val subtaskTextCache = CiphertextCache(maxEntries = 4_096)
    private var cachedContentBoardId: String? = null

    @Synchronized
    fun mapBoard(graph: BoardGraph): BoardSnapshot {
        val boardEntity = graph.board
        prepareContentCache(boardEntity.id)
        val statuses = graph.statuses.sortedBy { it.status.position }
        return BoardSnapshot(
            board = mapBoard(boardEntity),
            statuses = statuses.map { graphStatus ->
                val entity = graphStatus.status
                BoardStatus(
                    id = StatusId(entity.id),
                    boardId = BoardId(entity.boardId),
                    name = decryptCached(
                        cache = statusNameCache,
                        cacheKey = entity.id,
                        ciphertext = entity.encryptedName,
                        subject = statusSubject(entity.id),
                        field = FIELD_NAME,
                    ),
                    colorArgb = entity.colorArgb,
                    isDoneStatus = entity.isDone,
                    position = entity.position,
                )
            },
            cards = statuses.flatMap { status ->
                status.cards.sortedBy { it.card.position }.map(::mapCard)
            },
        )
    }

    fun mapBoard(entity: BoardEntity): Board = Board(
        id = BoardId(entity.id),
        name = decryptCached(
            cache = boardNameCache,
            cacheKey = entity.id,
            ciphertext = entity.encryptedName,
            subject = boardSubject(entity.id),
            field = FIELD_NAME,
        ),
        isLocked = entity.isLocked,
        position = entity.position,
        createdAtEpochMillis = entity.createdAtEpochMillis,
    )

    fun mapBoardSummary(row: BoardSummaryRow): BoardSummary = BoardSummary(
        board = mapBoard(row.board),
        statusCount = row.statusCount,
        cardCount = row.cardCount,
    )

    private fun mapCard(graph: CardGraph): BoardCard {
        val entity = graph.card
        val subject = cardSubject(entity.id)
        return BoardCard(
            id = CardId(entity.id),
            statusId = StatusId(entity.statusId),
            title = decryptCached(
                cache = cardTitleCache,
                cacheKey = entity.id,
                ciphertext = entity.encryptedTitle,
                subject = subject,
                field = FIELD_TITLE,
            ),
            details = decryptCached(
                cache = cardDetailsCache,
                cacheKey = entity.id,
                ciphertext = entity.encryptedDetails,
                subject = subject,
                field = FIELD_DETAILS,
            ),
            dueDate = entity.encryptedDueDate
                ?.let { ciphertext ->
                    decryptCached(
                        cache = cardDueDateCache,
                        cacheKey = entity.id,
                        ciphertext = ciphertext,
                        subject = subject,
                        field = FIELD_DUE_DATE,
                    )
                }
                ?.let(LocalDate::parse),
            isMajor = entity.isMajor,
            position = entity.position,
            createdAtEpochMillis = entity.createdAtEpochMillis,
            updatedAtEpochMillis = entity.updatedAtEpochMillis,
            subtasks = graph.subtasks.sortedBy(SubtaskEntity::position).map { subtask ->
                BoardSubtask(
                    id = SubtaskId(subtask.id),
                    cardId = CardId(subtask.cardId),
                    text = decryptCached(
                        cache = subtaskTextCache,
                        cacheKey = subtask.id,
                        ciphertext = subtask.encryptedText,
                        subject = subtaskSubject(subtask.id),
                        field = FIELD_TEXT,
                    ),
                    isCompleted = subtask.isCompleted,
                    position = subtask.position,
                )
            },
        )
    }

    private fun decryptCached(
        cache: CiphertextCache,
        cacheKey: String,
        ciphertext: ByteArray,
        subject: String,
        field: String,
    ): String = cache.value(cacheKey, ciphertext) {
        cipher.decrypt(ciphertext, subject, field)
    }

    @Synchronized
    private fun prepareContentCache(boardId: String) {
        if (cachedContentBoardId == boardId) return
        statusNameCache.clear()
        cardTitleCache.clear()
        cardDetailsCache.clear()
        cardDueDateCache.clear()
        subtaskTextCache.clear()
        cachedContentBoardId = boardId
    }
}

/** Small synchronized LRU because board and launcher flows can map on different dispatchers. */
private class CiphertextCache(
    private val maxEntries: Int,
) {
    private data class Entry(
        val ciphertext: ByteArray,
        val plaintext: String,
    )

    private val entries = object : LinkedHashMap<String, Entry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?): Boolean =
            size > maxEntries
    }

    @Synchronized
    fun value(key: String, ciphertext: ByteArray, decrypt: () -> String): String {
        entries[key]?.takeIf { it.ciphertext.contentEquals(ciphertext) }?.let { cached ->
            return cached.plaintext
        }
        return decrypt().also { plaintext ->
            entries[key] = Entry(ciphertext.copyOf(), plaintext)
        }
    }

    @Synchronized
    fun clear() {
        entries.clear()
    }
}

internal const val FIELD_NAME = "name"
internal const val FIELD_TITLE = "title"
internal const val FIELD_DETAILS = "details"
internal const val FIELD_DUE_DATE = "due-date"
internal const val FIELD_TEXT = "text"

internal fun boardSubject(id: String) = "board:$id"
// Preserve the V1 authenticated-data prefix so existing ciphertext remains decryptable.
internal fun statusSubject(id: String) = "column:$id"
internal fun cardSubject(id: String) = "card:$id"
internal fun subtaskSubject(id: String) = "subtask:$id"
