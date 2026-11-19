package com.katoaapps.minkboard.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Relation

internal data class CardGraph(
    @Embedded val card: CardEntity,
    @Relation(
        parentColumn = "card_id",
        entityColumn = "card_id",
    )
    val subtasks: List<SubtaskEntity>,
)

internal data class StatusGraph(
    @Embedded val status: StatusEntity,
    @Relation(
        entity = CardEntity::class,
        parentColumn = "column_id",
        entityColumn = "column_id",
    )
    val cards: List<CardGraph>,
)

internal data class BoardGraph(
    @Embedded val board: BoardEntity,
    @Relation(
        entity = StatusEntity::class,
        parentColumn = "board_id",
        entityColumn = "board_id",
    )
    val statuses: List<StatusGraph>,
)

internal data class BoardSummaryRow(
    @Embedded val board: BoardEntity,
    @ColumnInfo(name = "status_count") val statusCount: Int,
    @ColumnInfo(name = "card_count") val cardCount: Int,
)

internal data class BoardDesignationCandidateRow(
    @ColumnInfo(name = "board_id") val boardId: String,
    @ColumnInfo(name = "is_locked") val isLocked: Boolean,
    @ColumnInfo(name = "intake_status_id") val intakeStatusId: String?,
)
