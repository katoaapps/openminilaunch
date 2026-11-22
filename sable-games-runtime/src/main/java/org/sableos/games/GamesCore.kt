package org.sableos.games

/**
 * Sable Games' rules, in Kotlin (they were the games-core Rust crate).
 *
 * Sudoku and Minesweeper keep their state as replayable strings, as before, so a saved game
 * survives the move: `S1:<puzzle>:<row,col,value;…>` and `M1:<w>:<h>:<mines>:<seed>:<R|F,x,y;…>`.
 * Mines are placed by the same xorshift64 generator, so a seed gives the same board it always did.
 * Answers keep the screens' protocol: `OK:…` or `OK|…`, else `ERR:<reason>`.
 */
internal object GamesCore {
    private const val SUDOKU = "S1"
    private const val MINES = "M1"
    private const val CELLS = 81
    private const val SIDE = 9
    private const val BOX = 3
    private const val ERR_STATE = "ERR:INVALID_STATE"
    private const val ERR_BOUNDS = "ERR:OUT_OF_BOUNDS"

    // --- Sudoku ---

    private class SudokuError(
        val protocol: String,
    ) : Exception(protocol)

    private class Sudoku(
        val cells: IntArray,
        val fixed: BooleanArray,
    ) {
        fun set(
            row: Int,
            col: Int,
            value: Int,
        ) {
            // Out-of-range cells reached the Rust domain's OutOfRange, which mapped to ERR:SUDOKU.
            if (row >= SIDE || col >= SIDE || value > SIDE) throw SudokuError("ERR:SUDOKU")
            val index = row * SIDE + col
            if (fixed[index]) throw SudokuError("ERR:FIXED_CELL")
            val old = cells[index]
            cells[index] = value
            if (!consistent()) {
                cells[index] = old
                throw SudokuError("ERR:CONFLICT")
            }
        }

        fun solved(): Boolean = cells.all { it != 0 } && consistent()

        fun consistent(): Boolean {
            for (i in 0 until SIDE) {
                if (!validGroup { cells[i * SIDE + it] }) return false
                if (!validGroup { cells[it * SIDE + i] }) return false
            }
            for (boxRow in 0 until BOX) {
                for (boxCol in 0 until BOX) {
                    if (!validGroup { cells[(boxRow * BOX + it / BOX) * SIDE + boxCol * BOX + it % BOX] }) return false
                }
            }
            return true
        }

        private inline fun validGroup(value: (Int) -> Int): Boolean {
            val seen = BooleanArray(SIDE + 1)
            for (offset in 0 until SIDE) {
                val v = value(offset)
                if (v == 0) continue
                if (v > SIDE || seen[v]) return false
                seen[v] = true
            }
            return true
        }

        fun board(): String = cells.joinToString("") { it.toString() }

        companion object {
            fun parse(puzzle: String): Sudoku {
                if (puzzle.length != CELLS || !puzzle.all { it in '0'..'9' }) throw SudokuError("ERR:INVALID_PUZZLE")
                val cells = IntArray(CELLS) { puzzle[it] - '0' }
                val board = Sudoku(cells, BooleanArray(CELLS) { cells[it] != 0 })
                if (!board.consistent()) throw SudokuError("ERR:INVALID_PUZZLE")
                return board
            }
        }
    }

    /** Replays a state; null when it is not one this app wrote. */
    private fun replaySudoku(state: String): Triple<Sudoku, String, String>? {
        val fields = state.split(":", limit = 3)
        if (fields.size < 3 || fields[0] != SUDOKU) return null
        val (_, puzzle, operations) = fields
        return try {
            val board = Sudoku.parse(puzzle)
            if (operations.isNotEmpty()) {
                for (encoded in operations.split(';')) {
                    val parts = encoded.split(',')
                    if (parts.size != 3) return null
                    val row = index(parts[0]) ?: return null
                    val col = index(parts[1]) ?: return null
                    val value = parts[2].toIntOrNull()?.takeIf { it in 0..UByte.MAX_VALUE.toInt() } ?: return null
                    board.set(row, col, value)
                }
            }
            Triple(board, puzzle, operations)
        } catch (_: SudokuError) {
            null
        }
    }

    fun sudokuStart(puzzle: String): String =
        try {
            Sudoku.parse(puzzle)
            "OK:$SUDOKU:$puzzle:"
        } catch (error: SudokuError) {
            error.protocol
        }

    fun sudokuSet(
        state: String,
        row: Int,
        col: Int,
        value: Int,
    ): String {
        if (row < 0 || col < 0) return ERR_BOUNDS
        if (value !in 0..SIDE) return "ERR:INVALID_VALUE"
        val (board, puzzle, operations) = replaySudoku(state) ?: return ERR_STATE
        try {
            board.set(row, col, value)
        } catch (error: SudokuError) {
            return error.protocol
        }
        val encoded = "$row,$col,$value"
        return "OK:$SUDOKU:$puzzle:" + if (operations.isEmpty()) encoded else "$operations;$encoded"
    }

    fun sudokuBoard(state: String): String = replaySudoku(state)?.let { "OK:${it.first.board()}" } ?: ERR_STATE

    fun sudokuSolved(state: String): Boolean = replaySudoku(state)?.first?.solved() ?: false

    // --- Minesweeper ---

    private class MineError(
        val protocol: String,
    ) : Exception(protocol)

    private enum class CellState { Hidden, Revealed, Flagged }

    private const val XOR_LEFT_A = 13
    private const val XOR_RIGHT = 7
    private const val XOR_LEFT_B = 17

    private class Minefield(
        val width: Int,
        val height: Int,
        mineCount: Int,
        seed: Long,
    ) {
        val mines: BooleanArray
        val state: Array<CellState>

        init {
            val count = width.toLong() * height.toLong()
            if (count > Int.MAX_VALUE) throw MineError("ERR:BOARD_TOO_LARGE")
            if (width == 0 || height == 0 || mineCount.toLong() >= count) throw MineError("ERR:INVALID_BOARD")
            mines = BooleanArray(count.toInt())
            state = Array(count.toInt()) { CellState.Hidden }
            var placed = 0
            // seed.max(1) as an unsigned 64-bit value, then xorshift64 with logical right shifts.
            var rng = if (seed == 0L) 1L else seed
            while (placed < mineCount) {
                rng = rng xor (rng shl XOR_LEFT_A)
                rng = rng xor (rng ushr XOR_RIGHT)
                rng = rng xor (rng shl XOR_LEFT_B)
                val index = java.lang.Long.remainderUnsigned(rng, count).toInt()
                if (!mines[index]) {
                    mines[index] = true
                    placed++
                }
            }
        }

        fun index(
            x: Int,
            y: Int,
        ): Int {
            if (x >= width || y >= height) throw MineError(ERR_BOUNDS)
            return y * width + x
        }

        fun toggleFlag(
            x: Int,
            y: Int,
        ): CellState {
            val i = index(x, y)
            state[i] =
                when (state[i]) {
                    CellState.Hidden -> CellState.Flagged
                    CellState.Flagged -> CellState.Hidden
                    CellState.Revealed -> CellState.Revealed
                }
            return state[i]
        }

        /** -1 a mine, -2 ignored (already open or flagged), else the neighbouring mine count. */
        fun reveal(
            x: Int,
            y: Int,
        ): Int {
            val i = index(x, y)
            if (state[i] != CellState.Hidden) return IGNORED
            state[i] = CellState.Revealed
            if (mines[i]) return MINE
            var count = 0
            for (dy in -1..1) {
                for (dx in -1..1) {
                    val nx = x + dx
                    val ny = y + dy
                    if ((dx != 0 || dy != 0) && nx in 0 until width && ny in 0 until height && mines[ny * width + nx]) count++
                }
            }
            return count
        }
    }

    private const val MINE = -1
    private const val IGNORED = -2

    private class MineState(
        val width: Int,
        val height: Int,
        val mines: Int,
        val seed: Long,
        val operations: String,
    ) {
        fun encode(): String = "$MINES:$width:$height:$mines:${java.lang.Long.toUnsignedString(seed)}:$operations"

        fun plus(operation: String): MineState =
            MineState(width, height, mines, seed, if (operations.isEmpty()) operation else "$operations;$operation")
    }

    private fun index(text: String): Int? = text.toIntOrNull()?.takeIf { it >= 0 }

    private fun decodeMines(encoded: String): MineState? {
        val fields = encoded.split(":", limit = 6)
        if (fields.size < 6 || fields[0] != MINES) return null
        val width = index(fields[1]) ?: return null
        val height = index(fields[2]) ?: return null
        val mines = index(fields[3]) ?: return null
        val seed = fields[4].toULongOrNull()?.toLong() ?: return null
        return MineState(width, height, mines, seed, fields[5])
    }

    /** Replays a state into a board and the cell view (H hidden, F flag, * mine, 0..8 count). */
    private fun replayMines(encoded: String): Triple<Minefield, MineState, CharArray> {
        val state = decodeMines(encoded) ?: throw MineError(ERR_STATE)
        val board = Minefield(state.width, state.height, state.mines, state.seed)
        val view = CharArray(state.width * state.height) { 'H' }
        if (state.operations.isNotEmpty()) {
            for (operation in state.operations.split(';')) {
                val parts = operation.split(',')
                if (parts.size != 3) throw MineError(ERR_STATE)
                val x = index(parts[1]) ?: throw MineError(ERR_STATE)
                val y = index(parts[2]) ?: throw MineError(ERR_STATE)
                val cell = board.index(x, y)
                when (parts[0]) {
                    "R" ->
                        when (val result = board.reveal(x, y)) {
                            MINE -> view[cell] = '*'
                            IGNORED -> Unit
                            else -> view[cell] = '0' + result
                        }
                    "F" ->
                        when (board.toggleFlag(x, y)) {
                            CellState.Hidden -> view[cell] = 'H'
                            CellState.Flagged -> view[cell] = 'F'
                            CellState.Revealed -> Unit
                        }
                    else -> throw MineError(ERR_STATE)
                }
            }
        }
        return Triple(board, state, view)
    }

    fun minesweeperStart(
        width: Int,
        height: Int,
        mines: Int,
        seed: Long,
    ): String {
        if (width <= 0 || height <= 0 || mines < 0) return "ERR:INVALID_BOARD"
        if (seed < 0) return "ERR:INVALID_SEED"
        return try {
            Minefield(width, height, mines, seed)
            "OK:${MineState(width, height, mines, seed, "").encode()}"
        } catch (error: MineError) {
            error.protocol
        }
    }

    fun minesweeperReveal(
        state: String,
        x: Int,
        y: Int,
    ): String {
        if (x < 0 || y < 0) return ERR_BOUNDS
        val (board, decoded) =
            try {
                replayMines(state)
            } catch (_: MineError) {
                return ERR_STATE
            }
        if (x >= decoded.width || y >= decoded.height) return ERR_BOUNDS
        val next = decoded.plus("R,$x,$y").encode()
        return when (val result = board.reveal(x, y)) {
            MINE -> "OK|$next|MINE"
            IGNORED -> "OK|$next|IGNORED"
            else -> "OK|$next|SAFE|$result"
        }
    }

    fun minesweeperToggleFlag(
        state: String,
        x: Int,
        y: Int,
    ): String {
        if (x < 0 || y < 0) return ERR_BOUNDS
        val (board, decoded) =
            try {
                replayMines(state)
            } catch (_: MineError) {
                return ERR_STATE
            }
        if (x >= decoded.width || y >= decoded.height) return ERR_BOUNDS
        val label =
            when (board.toggleFlag(x, y)) {
                CellState.Hidden -> "HIDDEN"
                CellState.Flagged -> "FLAGGED"
                CellState.Revealed -> "REVEALED"
            }
        return "OK|${decoded.plus("F,$x,$y").encode()}|$label"
    }

    fun minesweeperView(state: String): String =
        try {
            "OK:${String(replayMines(state).third)}"
        } catch (error: MineError) {
            error.protocol
        }

    // --- 2048 ---

    private const val TILES = 16
    private const val LINE = 4

    /** Slides a 16-tile board (0 left, 1 right, 2 up, 3 down): 16 tiles then the move's score. */
    fun game2048Slide(
        board: IntArray,
        direction: Int,
    ): LongArray {
        require(board.size == TILES && direction in 0..3 && board.none { it < 0 }) { "not a 2048 board or move" }
        val cells = LongArray(TILES) { board[it].toLong() }
        var score = 0L
        for (line in 0 until LINE) {
            val slots = IntArray(LINE) { position -> slot(direction, line, position) }
            val compact = slots.map { cells[it] }.filter { it != 0L }
            val merged = LongArray(LINE)
            var src = 0
            var dst = 0
            while (src < compact.size) {
                if (src + 1 < compact.size && compact[src] == compact[src + 1]) {
                    merged[dst] = compact[src] * 2
                    score += merged[dst]
                    src += 2
                } else {
                    merged[dst] = compact[src]
                    src++
                }
                dst++
            }
            slots.forEachIndexed { position, cell -> cells[cell] = merged[position] }
        }
        return cells + score
    }

    private fun slot(
        direction: Int,
        line: Int,
        position: Int,
    ): Int =
        when (direction) {
            0 -> line * LINE + position
            1 -> line * LINE + (LINE - 1 - position)
            2 -> position * LINE + line
            else -> (LINE - 1 - position) * LINE + line
        }
}
