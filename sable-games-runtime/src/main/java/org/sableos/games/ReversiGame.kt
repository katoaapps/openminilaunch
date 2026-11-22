package org.sableos.games

/**
 * Reversi on an 8 x 8 board: you play dark ([DARK]) against the phone (light). A move must flip at
 * least one line of the other colour; a side with no move passes, and the game ends when neither
 * can move. [cells] holds 0 (empty), [DARK] or [LIGHT], row by row.
 */
internal data class ReversiGame(
    val cells: List<Int> = startCells(),
    val turn: Int = DARK,
) {
    fun count(colour: Int): Int = cells.count { it == colour }

    fun flipsFor(
        index: Int,
        colour: Int,
    ): List<Int> {
        if (cells[index] != EMPTY) return emptyList()
        val row = index / SIZE
        val col = index % SIZE
        return DIRECTIONS.flatMap { (dr, dc) ->
            val line = mutableListOf<Int>()
            var r = row + dr
            var c = col + dc
            while (r in 0 until SIZE && c in 0 until SIZE && cells[r * SIZE + c] == -colour) {
                line += r * SIZE + c
                r += dr
                c += dc
            }
            val closed = r in 0 until SIZE && c in 0 until SIZE && cells[r * SIZE + c] == colour
            if (closed) line else emptyList()
        }
    }

    fun legalMoves(colour: Int): List<Int> = cells.indices.filter { flipsFor(it, colour).isNotEmpty() }

    val over: Boolean get() = legalMoves(DARK).isEmpty() && legalMoves(LIGHT).isEmpty()

    /** Plays [index] for the side to move, or returns this unchanged when it isn't legal. */
    fun play(index: Int): ReversiGame {
        val flips = flipsFor(index, turn)
        if (flips.isEmpty()) return this
        val next = cells.toMutableList()
        next[index] = turn
        flips.forEach { next[it] = turn }
        val placed = ReversiGame(next, -turn)
        return if (placed.legalMoves(-turn).isEmpty()) placed.copy(turn = turn) else placed
    }

    /**
     * The phone's move: corners first, never next to an empty corner when there's a choice, then
     * the most flips. Null when the phone has no move.
     */
    fun phoneMove(): Int? =
        legalMoves(turn).maxByOrNull { index ->
            WEIGHTS[index] * WEIGHT_SCALE + flipsFor(index, turn).size
        }

    companion object {
        const val SIZE = 8
        const val EMPTY = 0
        const val DARK = 1
        const val LIGHT = -1
        private const val WEIGHT_SCALE = 100
        private val DIRECTIONS = listOf(-1 to -1, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1)

        /** Corners best, the squares next to corners worst, edges good. */
        private val WEIGHTS: List<Int> =
            listOf(
                "9 -3 2 2 2 2 -3 9",
                "-3 -4 -1 -1 -1 -1 -4 -3",
                "2 -1 1 0 0 1 -1 2",
                "2 -1 0 0 0 0 -1 2",
                "2 -1 0 0 0 0 -1 2",
                "2 -1 1 0 0 1 -1 2",
                "-3 -4 -1 -1 -1 -1 -4 -3",
                "9 -3 2 2 2 2 -3 9",
            ).flatMap { row -> row.split(' ').map(String::toInt) }

        fun startCells(): List<Int> =
            List(SIZE * SIZE) { EMPTY }.toMutableList().also {
                it[3 * SIZE + 3] = LIGHT
                it[4 * SIZE + 4] = LIGHT
                it[3 * SIZE + 4] = DARK
                it[4 * SIZE + 3] = DARK
            }
    }
}
