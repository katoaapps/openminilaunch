package org.sableos.games

import kotlin.math.hypot

/**
 * The on-screen pad and jump button, for touch play. They appear after the first touch and go away at the
 * first key press, so a keyboard phone shows the clean board. Positions are fractions of the board's side.
 */
internal object ScaffoldTouch {
    const val PAD_X = 0.17f
    const val PAD_Y = 0.82f
    const val PAD_RADIUS = 0.14f
    const val JUMP_X = 0.85f
    const val JUMP_Y = 0.82f
    const val JUMP_RADIUS = 0.09f
    private const val GRAB = 1.35f
    private const val DEAD_ZONE = 0.3f

    /** What the fingers at [points] (board pixels) hold, on a board [side] pixels across. */
    fun input(
        points: List<Pair<Float, Float>>,
        side: Float,
    ): ScaffoldInput {
        var input = ScaffoldInput()
        for ((x, y) in points) {
            val dx = x - PAD_X * side
            val dy = y - PAD_Y * side
            val radius = PAD_RADIUS * side
            if (hypot(dx, dy) <= radius * GRAB) {
                input =
                    input.copy(
                        left = input.left || dx < -radius * DEAD_ZONE,
                        right = input.right || dx > radius * DEAD_ZONE,
                        up = input.up || dy < -radius * DEAD_ZONE,
                        down = input.down || dy > radius * DEAD_ZONE,
                    )
            }
            if (hypot(x - JUMP_X * side, y - JUMP_Y * side) <=
                JUMP_RADIUS * side * GRAB
            ) {
                input = input.copy(jump = true)
            }
        }
        return input
    }

    /** True for a touch on the pause corner, the top right of the status bar. */
    fun isPause(
        x: Float,
        y: Float,
        side: Float,
    ): Boolean = x >= side * 0.92f && y <= side * (ScaffoldField.HUD / ScaffoldField.WIDTH)
}
