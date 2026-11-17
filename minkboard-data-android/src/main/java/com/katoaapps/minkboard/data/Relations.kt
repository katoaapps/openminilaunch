package com.katoaapps.minkboard.data

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
