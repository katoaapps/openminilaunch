package org.sableos.games

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Bricks on a square field from (0, 0) at the top left to (1, 1). The ball sits on the paddle until
 * Space or a tap launches it; it bounces off the walls, the paddle (the further from the paddle's
 * centre it lands, the wider the angle) and the bricks, each brick breaking on the first hit.
 * Missing the ball costs one of three lives. Clearing the wall starts the next, faster level.
 * [bricks] holds the hit points left per brick (0 = gone), row by row.
 */
internal data class BricksGame(
    val paddleX: Float = 0.5f,
    val ballX: Float = 0.5f,
    val ballY: Float = PADDLE_Y - BALL_RADIUS,
    val velX: Float = 0f,
    val velY: Float = 0f,
    val bricks: List<Int> = fullWall(),
    val lives: Int = START_LIVES,
    val score: Int = 0,
    val level: Int = 1,
    val launched: Boolean = false,
) {
    val over: Boolean get() = lives <= 0

    /** Moves the paddle by [dx] of the field width; a resting ball moves with it. */
    fun movePaddle(dx: Float): BricksGame = paddleTo(paddleX + dx)

    fun paddleTo(x: Float): BricksGame {
        val clamped = x.coerceIn(PADDLE_WIDTH / 2, 1f - PADDLE_WIDTH / 2)
        return if (over) this else copy(paddleX = clamped, ballX = if (launched) ballX else clamped)
    }

    fun launch(): BricksGame {
        if (over || launched) return this
        val speed = speedFor(level)
        return copy(launched = true, velX = speed * LAUNCH_SLANT, velY = -speed)
    }

    /** Advances the ball by [seconds], in small sub-steps so it never passes through a brick. */
    fun step(seconds: Float): BricksGame {
        if (over || !launched) return this
        var state = this
        val steps = (seconds / MAX_SUBSTEP).toInt() + 1
        repeat(steps) { state = state.substep(seconds / steps) }
        return state
    }

    private fun substep(dt: Float): BricksGame {
        if (!launched) return this
        var x = ballX + velX * dt
        var y = ballY + velY * dt
        var vx = velX
        var vy = velY
        if (x < BALL_RADIUS || x > 1f - BALL_RADIUS) {
            vx = -vx
            x = x.coerceIn(BALL_RADIUS, 1f - BALL_RADIUS)
        }
        if (y < BALL_RADIUS) {
            vy = abs(vy)
            y = BALL_RADIUS
        }
        if (vy > 0 && y + BALL_RADIUS >= PADDLE_Y && y < PADDLE_Y + PADDLE_HEIGHT &&
            abs(x - paddleX) <= PADDLE_WIDTH / 2 + BALL_RADIUS
        ) {
            val speed = sqrt(vx * vx + vy * vy)
            val offset = ((x - paddleX) / (PADDLE_WIDTH / 2)).coerceIn(-1f, 1f)
            vx = speed * offset * MAX_SLANT
            vy = -sqrt((speed * speed - vx * vx).coerceAtLeast(speed * speed * MIN_RISE))
            y = PADDLE_Y - BALL_RADIUS
        }
        if (y > 1f) return loseBall()
        val hit = brickAt(x, y)
        if (hit < 0) return copy(ballX = x, ballY = y, velX = vx, velY = vy)
        val left = bricks.toMutableList().also { it[hit] -= 1 }
        val cleared = left.all { it == 0 }
        val next =
            copy(ballX = x, ballY = y, velX = vx, velY = -vy, bricks = left, score = score + BRICK_POINTS * level)
        return if (cleared) next.nextLevel() else next
    }

    private fun loseBall(): BricksGame =
        copy(
            lives = lives - 1,
            launched = false,
            ballX = paddleX,
            ballY =
                PADDLE_Y - BALL_RADIUS,
            velX = 0f,
            velY = 0f,
        )

    private fun nextLevel(): BricksGame =
        copy(
            level = level + 1,
            bricks = fullWall(),
            launched = false,
            ballX = paddleX,
            ballY = PADDLE_Y - BALL_RADIUS,
            velX = 0f,
            velY = 0f,
        )

    private fun brickAt(
        x: Float,
        y: Float,
    ): Int {
        if (y < WALL_TOP || y >= WALL_TOP + ROWS * BRICK_HEIGHT) return -1
        val row = ((y - WALL_TOP) / BRICK_HEIGHT).toInt()
        val col = (x * COLUMNS).toInt().coerceIn(0, COLUMNS - 1)
        val index = row * COLUMNS + col
        return if (bricks[index] > 0) index else -1
    }

    companion object {
        const val COLUMNS = 8
        const val ROWS = 6
        const val WALL_TOP = 0.1f
        const val BRICK_HEIGHT = 0.045f
        const val PADDLE_Y = 0.93f
        const val PADDLE_HEIGHT = 0.025f
        const val PADDLE_WIDTH = 0.2f
        const val BALL_RADIUS = 0.015f
        const val BRICK_POINTS = 10
        private const val START_LIVES = 3
        private const val LAUNCH_SLANT = 0.5f
        private const val MAX_SLANT = 0.8f
        private const val MIN_RISE = 0.3f
        private const val MAX_SUBSTEP = 0.004f
        private const val BASE_SPEED = 0.7f
        private const val SPEED_PER_LEVEL = 0.1f

        fun speedFor(level: Int): Float = BASE_SPEED + (level - 1) * SPEED_PER_LEVEL

        fun fullWall(): List<Int> = List(COLUMNS * ROWS) { 1 }
    }
}
