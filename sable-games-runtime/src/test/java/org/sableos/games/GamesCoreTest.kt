package org.sableos.games

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GamesCoreTest {
    private val puzzle = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"

    private fun ok(response: String) = response.removePrefix("OK:").also { assertTrue(response, response.startsWith("OK:")) }

    @Test
    fun sudokuRejectsConflictsAndFixedEdits() {
        val state = ok(GamesCore.sudokuStart(puzzle))
        assertEquals("ERR:FIXED_CELL", GamesCore.sudokuSet(state, 0, 0, 4))
        assertEquals("ERR:CONFLICT", GamesCore.sudokuSet(state, 0, 2, 5))
        assertEquals("OK:S1:$puzzle:0,2,4", GamesCore.sudokuSet(state, 0, 2, 4))
        assertEquals("ERR:INVALID_PUZZLE", GamesCore.sudokuStart("5".repeat(81)))
    }

    @Test
    fun sudokuStateReplaysEntries() {
        var state = ok(GamesCore.sudokuStart("0".repeat(81)))
        state = ok(GamesCore.sudokuSet(state, 0, 0, 5))
        assertEquals("ERR:CONFLICT", GamesCore.sudokuSet(state, 0, 1, 5))
        state = ok(GamesCore.sudokuSet(state, 0, 0, 4))
        assertTrue(GamesCore.sudokuBoard(state).startsWith("OK:4"))
        assertEquals("ERR:INVALID_STATE", GamesCore.sudokuBoard("S1:$puzzle:x"))
    }

    @Test
    fun sudokuKnowsWhenSolved() {
        val solution = "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
        val state = ok(GamesCore.sudokuStart(solution.dropLast(1) + "0"))
        assertEquals(false, GamesCore.sudokuSolved(state))
        assertEquals(true, GamesCore.sudokuSolved(ok(GamesCore.sudokuSet(state, 8, 8, 9))))
    }

    @Test
    fun minesweeperIsDeterministicAndReplayable() {
        val state = ok(GamesCore.minesweeperStart(9, 9, 10, 1803))
        assertEquals("M1:9:9:10:1803:", state)
        val reveal = GamesCore.minesweeperReveal(state, 4, 4).split('|')
        assertEquals(reveal, GamesCore.minesweeperReveal(state, 4, 4).split('|'))
        assertEquals("OK", reveal[0])
        val view = ok(GamesCore.minesweeperView(reveal[1]))
        assertEquals(81, view.length)
        assertTrue(view[4 * 9 + 4] != 'H')
        assertEquals("ERR:INVALID_BOARD", GamesCore.minesweeperStart(3, 3, 9, 1))
        assertEquals("ERR:INVALID_SEED", GamesCore.minesweeperStart(3, 3, 1, -1))
        assertEquals("ERR:OUT_OF_BOUNDS", GamesCore.minesweeperReveal(state, 9, 0))
    }

    @Test
    fun minesweeperFlagsToggle() {
        val state = ok(GamesCore.minesweeperStart(9, 9, 10, 1803))
        val flagged = GamesCore.minesweeperToggleFlag(state, 0, 0).split('|')
        assertEquals("FLAGGED", flagged[2])
        assertEquals("F", ok(GamesCore.minesweeperView(flagged[1])).take(1))
        assertEquals("HIDDEN", GamesCore.minesweeperToggleFlag(flagged[1], 0, 0).split('|')[2])
    }

    @Test
    fun game2048MergesOncePerMove() {
        val board = intArrayOf(2, 2, 4, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val result = GamesCore.game2048Slide(board, 0)
        assertArrayEquals(longArrayOf(4, 8, 0, 0), result.copyOfRange(0, 4))
        assertEquals(12L, result[16])
        val down = GamesCore.game2048Slide(intArrayOf(2, 0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0), 3)
        assertEquals(listOf(0L, 0L, 4L, 4L), listOf(down[0], down[4], down[8], down[12]))
    }
}
