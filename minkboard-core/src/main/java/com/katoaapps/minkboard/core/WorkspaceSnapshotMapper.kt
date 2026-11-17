package com.katoaapps.minkboard.core

import com.katoaapps.minkboard.model.MinkBoardWorkspace
import com.katoaapps.minkboard.model.PortableBoardSnapshot
import com.katoaapps.minkboard.model.PortableCardSnapshot
import com.katoaapps.minkboard.model.PortableStatusSnapshot
import com.katoaapps.minkboard.model.PortableSubtaskSnapshot
import com.katoaapps.minkboard.model.PortableWorkspaceSnapshot

fun MinkBoardWorkspace.toPortableSnapshot(
    exportedAtEpochMillis: Long,
    sourceApp: String,
): PortableWorkspaceSnapshot = PortableWorkspaceSnapshot(
    exportedAtEpochMillis = exportedAtEpochMillis,
    sourceApp = sourceApp,
    boards = boards.sortedBy { it.board.position }.map { boardSnapshot ->
        val board = boardSnapshot.board
        PortableBoardSnapshot(
            id = board.id.value,
            name = board.name,
            isLocked = board.isLocked,
            position = board.position,
            createdAtEpochMillis = board.createdAtEpochMillis,
            statuses = boardSnapshot.statuses.sortedBy { it.position }.map { status ->
                PortableStatusSnapshot(
                    id = status.id.value,
                    name = status.name,
                    colorArgb = status.colorArgb,
                    isDoneStatus = status.isDoneStatus,
                    position = status.position,
                    cards = boardSnapshot.cardsIn(status.id).map { card ->
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
        )
    },
)
