package org.sableos.games

import kotlin.random.Random

/** A cell on a game grid: [x] column, [y] row. */
internal data class Cell(
    val x: Int,
    val y: Int,
)

/**
 * Snake on a [size] x [size] board, one immutable state per step so it is testable on the JVM:
 * arrows turn (never straight back), each [step] moves one cell, food grows the snake by one,
 * and running into a wall or the snake ends the game.
 */
internal data class SnakeGame(
    val size: Int,
    val body: List<Cell>,
    val direction: Cell,
    val pending: Cell,
    val food: Cell,
    val score: Int = 0,
    val alive: Boolean = true,
) {
    /** The direction for the next step; a turn back into the snake's own neck is ignored. */
    fun turn(
        dx: Int,
        dy: Int,
    ): SnakeGame {
        val next = Cell(dx, dy)
        val reverse = next.x == -direction.x && next.y == -direction.y
        return if (!alive || reverse || (dx == 0 && dy == 0)) this else copy(pending = next)
    }

    fun step(random: Random): SnakeGame {
        if (!alive) return this
        val head = body.first()
        val next = Cell(head.x + pending.x, head.y + pending.y)
        val eats = next == food
        val rest = if (eats) body else body.dropLast(1)
        val hitsWall = next.x !in 0 until size || next.y !in 0 until size
        return when {
            hitsWall || next in rest -> {
                copy(direction = pending, alive = false)
            }

            eats -> {
                val grown = listOf(next) + rest
                copy(body = grown, direction = pending, food = placeFood(size, grown, random), score = score + 1)
            }

            else -> {
                copy(body = listOf(next) + rest, direction = pending)
            }
        }
    }

    /** Milliseconds between steps: 160 at the start, 8 faster per food, never under 70. */
    val stepMillis: Long get() = (START_STEP_MS - score * SPEEDUP_MS).coerceAtLeast(MIN_STEP_MS)

    companion object {
        const val SIZE = 16
        private const val START_STEP_MS = 160L
        private const val SPEEDUP_MS = 8L
        private const val MIN_STEP_MS = 70L

        fun start(
            random: Random,
            size: Int = SIZE,
        ): SnakeGame {
            val mid = size / 2
            val body = listOf(Cell(mid, mid), Cell(mid - 1, mid), Cell(mid - 2, mid))
            val right = Cell(1, 0)
            return SnakeGame(size, body, right, right, placeFood(size, body, random))
        }

        fun placeFood(
            size: Int,
            body: List<Cell>,
            random: Random,
        ): Cell {
            val taken = body.toSet()
            val free = (0 until size * size).map { Cell(it % size, it / size) }.filter { it !in taken }
            return if (free.isEmpty()) body.first() else free[random.nextInt(free.size)]
        }
    }
}
