package org.sableos.games

import kotlin.random.Random

/** One falling piece: its shape index (0-6), its cells relative to [x], [y], and its rotation. */
internal data class FallingPiece(
    val kind: Int,
    val cells: List<Cell>,
    val x: Int,
    val y: Int,
) {
    val absolute: List<Cell> get() = cells.map { Cell(it.x + x, it.y + y) }
}

/**
 * A falling-block puzzle on a [WIDTH] x [HEIGHT] well, square so it fills the square screen. Left/Right move the piece, Up rotates it
 * (with a one-cell nudge off a wall), Down drops it a row, Space drops it to the floor. Full rows
 * clear; 1/2/3/4 rows score 100/300/500/800 times the level, and every 10 rows is a level.
 * [well] holds 0 for empty or shape index + 1.
 */
internal data class BlocksGame(
    val well: List<IntArray>,
    val piece: FallingPiece,
    val next: Int,
    val score: Int = 0,
    val lines: Int = 0,
    val over: Boolean = false,
) {
    val level: Int get() = 1 + lines / LINES_PER_LEVEL

    /** Milliseconds between gravity steps: 700 at level 1, 60 faster a level, never under 100. */
    val fallMillis: Long get() = (START_FALL_MS - (level - 1) * SPEEDUP_MS).coerceAtLeast(MIN_FALL_MS)

    fun move(dx: Int): BlocksGame {
        val moved = piece.copy(x = piece.x + dx)
        return if (!over && fits(moved)) copy(piece = moved) else this
    }

    fun rotate(): BlocksGame {
        if (over || piece.kind == O_KIND) return this
        val turned = piece.copy(cells = piece.cells.map { Cell(-it.y, it.x) })
        val placed = listOf(0, -1, 1, -2, 2).map { turned.copy(x = turned.x + it) }.firstOrNull(::fits)
        return if (placed != null) copy(piece = placed) else this
    }

    /** Gravity, or Down: one row lower, or lock the piece when it can't fall. */
    fun fall(random: Random): BlocksGame {
        if (over) return this
        val lower = piece.copy(y = piece.y + 1)
        return if (fits(lower)) copy(piece = lower) else lock(random)
    }

    fun drop(random: Random): BlocksGame {
        if (over) return this
        var lowest = piece
        while (fits(lowest.copy(y = lowest.y + 1))) lowest = lowest.copy(y = lowest.y + 1)
        return copy(piece = lowest).lock(random)
    }

    /** Where the piece would land, for the shadow drawn under it. */
    fun ghost(): FallingPiece {
        var lowest = piece
        while (fits(lowest.copy(y = lowest.y + 1))) lowest = lowest.copy(y = lowest.y + 1)
        return lowest
    }

    private fun fits(candidate: FallingPiece): Boolean =
        candidate.absolute.all { it.x in 0 until WIDTH && it.y < HEIGHT && (it.y < 0 || well[it.y][it.x] == 0) }

    private fun lock(random: Random): BlocksGame {
        val filled = well.map { it.copyOf() }
        piece.absolute.filter { it.y >= 0 }.forEach { filled[it.y][it.x] = piece.kind + 1 }
        val kept = filled.filter { row -> row.any { it == 0 } }
        val cleared = HEIGHT - kept.size
        val newWell = List(cleared) { IntArray(WIDTH) } + kept
        val spawned = spawn(next)
        val topOut = piece.absolute.any { it.y < 0 }
        val blocked = spawned.absolute.any { it.y >= 0 && newWell[it.y][it.x] != 0 }
        return copy(
            well = newWell,
            piece = spawned,
            next = random.nextInt(SHAPES.size),
            score = score + LINE_SCORES[cleared] * level,
            lines = lines + cleared,
            over = topOut || blocked,
        )
    }

    companion object {
        const val WIDTH = 16
        const val HEIGHT = 16
        private const val O_KIND = 1
        private const val LINES_PER_LEVEL = 10
        private const val START_FALL_MS = 700L
        private const val SPEEDUP_MS = 60L
        private const val MIN_FALL_MS = 100L
        private val LINE_SCORES = intArrayOf(0, 100, 300, 500, 800)

        /** I, O, T, S, Z, J, L as cells around a pivot at (0, 0). */
        val SHAPES: List<List<Cell>> =
            listOf(
                listOf(Cell(-1, 0), Cell(0, 0), Cell(1, 0), Cell(2, 0)),
                listOf(Cell(0, 0), Cell(1, 0), Cell(0, 1), Cell(1, 1)),
                listOf(Cell(-1, 0), Cell(0, 0), Cell(1, 0), Cell(0, -1)),
                listOf(Cell(-1, 0), Cell(0, 0), Cell(0, -1), Cell(1, -1)),
                listOf(Cell(-1, -1), Cell(0, -1), Cell(0, 0), Cell(1, 0)),
                listOf(Cell(-1, -1), Cell(-1, 0), Cell(0, 0), Cell(1, 0)),
                listOf(Cell(1, -1), Cell(-1, 0), Cell(0, 0), Cell(1, 0)),
            )

        fun spawn(kind: Int): FallingPiece = FallingPiece(kind, SHAPES[kind], WIDTH / 2 - 1, 1)

        fun start(random: Random): BlocksGame =
            BlocksGame(
                well = List(HEIGHT) { IntArray(WIDTH) },
                piece = spawn(random.nextInt(SHAPES.size)),
                next = random.nextInt(SHAPES.size),
            )
    }
}
