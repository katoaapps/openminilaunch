package com.katoaapps.minkboard.core

import com.katoaapps.minkboard.model.PortableBoardSnapshot
import com.katoaapps.minkboard.model.PortableCardSnapshot
import com.katoaapps.minkboard.model.PortableStatusSnapshot
import com.katoaapps.minkboard.model.PortableSubtaskSnapshot
import com.katoaapps.minkboard.model.PortableWorkspaceSnapshot
import com.katoaapps.minkboard.model.BoardId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PortableArchiveValidatorTest {
    private val validator = PortableArchiveValidator()

    @Test
    fun validatesCompleteSnapshotWithoutChangingIt() {
        val snapshot = validSnapshot()

        assertSame(snapshot, validator.validate(snapshot))
    }

    @Test
    fun allowsExplicitlyEmptyWorkspace() {
        validator.validate(validSnapshot().copy(boards = emptyList()))
    }

    @Test
    fun selectsRequestedBoardsAndReindexesTheirPositions() {
        val first = validSnapshot().boards.single()
        fun copiedBoard(number: Int) = first.copy(
            id = "board-$number",
            name = "Board $number",
            position = number - 1,
            statuses = first.statuses.map { status ->
                status.copy(
                    id = "status-$number",
                    cards = status.cards.map { card ->
                        card.copy(
                            id = "card-$number",
                            subtasks = card.subtasks.map { subtask ->
                                subtask.copy(id = "subtask-$number")
                            },
                        )
                    },
                )
            },
        )
        val snapshot = validSnapshot().copy(
            boards = listOf(first, copiedBoard(2), copiedBoard(3)),
        )

        val selected = snapshot.selectBoardsForExport(
            setOf(BoardId("board-2"), BoardId("board-3")),
        )

        assertEquals(listOf("board-2", "board-3"), selected.boards.map { it.id })
        assertEquals(listOf(0, 1), selected.boards.map { it.position })
        validator.validate(selected)
    }

    @Test
    fun rejectsEmptyOrMissingBoardExportSelections() {
        val snapshot = validSnapshot()

        assertThrows(IllegalArgumentException::class.java) {
            snapshot.selectBoardsForExport(emptySet())
        }
        assertThrows(IllegalArgumentException::class.java) {
            snapshot.selectBoardsForExport(setOf(BoardId("missing")))
        }
    }

    @Test
    fun rejectsUnsupportedFormatAndVersion() {
        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(validSnapshot().copy(format = "other"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(validSnapshot().copy(schemaVersion = 2))
        }
    }

    @Test
    fun rejectsDuplicateIdsAcrossWorkspace() {
        val source = validSnapshot()
        val duplicateBoard = source.boards.single().copy(position = 1)

        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(source.copy(boards = source.boards + duplicateBoard))
        }

        val status = source.boards.single().statuses.single()
        val duplicateStatus = status.copy(position = 1)
        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(
                source.copy(
                    boards = listOf(
                        source.boards.single().copy(statuses = listOf(status, duplicateStatus)),
                    ),
                ),
            )
        }
    }

    @Test
    fun rejectsPositionGapsAndMultipleDoneStatuses() {
        val source = validSnapshot()
        val board = source.boards.single()
        val status = board.statuses.single()

        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(source.copy(boards = listOf(board.copy(position = 2))))
        }
        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(
                source.copy(
                    boards = listOf(
                        board.copy(
                            statuses = listOf(
                                status.copy(isDoneStatus = true),
                                status.copy(id = "status-2", position = 1, isDoneStatus = true),
                            ),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun rejectsNonIsoDateAndInvalidTimestamps() {
        val source = validSnapshot()
        val board = source.boards.single()
        val status = board.statuses.single()
        val card = status.cards.single()

        fun withCard(value: PortableCardSnapshot) = source.copy(
            boards = listOf(
                board.copy(statuses = listOf(status.copy(cards = listOf(value)))),
            ),
        )

        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(withCard(card.copy(dueDateIso = "11/12/2026")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(
                withCard(card.copy(createdAtEpochMillis = 200, updatedAtEpochMillis = 100)),
            )
        }
    }

    @Test
    fun rejectsUnnormalizedOrBlankUserText() {
        val source = validSnapshot()
        val board = source.boards.single()

        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(source.copy(boards = listOf(board.copy(name = " Board "))))
        }
        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(
                source.copy(
                    boards = listOf(
                        board.copy(
                            statuses = listOf(board.statuses.single().copy(name = "")),
                        ),
                    ),
                ),
            )
        }
    }

    private fun validSnapshot(): PortableWorkspaceSnapshot = PortableWorkspaceSnapshot(
        exportedAtEpochMillis = 1_700_000_000_000,
        sourceApp = "MinkBoard 0.1.0",
        boards = listOf(
            PortableBoardSnapshot(
                id = "board-1",
                name = "Kitchen remodel",
                isLocked = true,
                position = 0,
                createdAtEpochMillis = 100,
                statuses = listOf(
                    PortableStatusSnapshot(
                        id = "status-1",
                        name = "Doing",
                        colorArgb = 0xFF173529.toInt(),
                        isDoneStatus = false,
                        position = 0,
                        cards = listOf(
                            PortableCardSnapshot(
                                id = "card-1",
                                title = "Install café shelves مرحبا",
                                details = "Use the concealed brackets.",
                                dueDateIso = "2026-11-12",
                                isMajor = true,
                                position = 0,
                                createdAtEpochMillis = 200,
                                updatedAtEpochMillis = 300,
                                subtasks = listOf(
                                    PortableSubtaskSnapshot(
                                        id = "subtask-1",
                                        text = "Measure twice",
                                        isCompleted = true,
                                        position = 0,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )
}
