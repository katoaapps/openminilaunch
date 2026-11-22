package org.sableos.games

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaffoldTouchTest {
    private val side = 1000f

    @Test
    fun padDirections() {
        val right = ScaffoldTouch.input(listOf(ScaffoldTouch.PAD_X * side + 100f to ScaffoldTouch.PAD_Y * side), side)
        assertTrue(right.right)
        assertFalse(right.left)
        val up = ScaffoldTouch.input(listOf(ScaffoldTouch.PAD_X * side to ScaffoldTouch.PAD_Y * side - 100f), side)
        assertTrue(up.up)
    }

    @Test
    fun centreOfPadAndFarAwayAreIdle() {
        val idle =
            ScaffoldTouch.input(
                listOf(ScaffoldTouch.PAD_X * side to ScaffoldTouch.PAD_Y * side, 500f to 100f),
                side,
            )
        assertFalse(idle.left || idle.right || idle.up || idle.down || idle.jump)
    }

    @Test
    fun jumpButtonAndTwoFingers() {
        val both =
            ScaffoldTouch.input(
                listOf(
                    ScaffoldTouch.PAD_X * side - 100f to ScaffoldTouch.PAD_Y * side,
                    ScaffoldTouch.JUMP_X * side to ScaffoldTouch.JUMP_Y * side,
                ),
                side,
            )
        assertTrue(both.left && both.jump)
    }

    @Test
    fun pauseCorner() {
        assertTrue(ScaffoldTouch.isPause(side * 0.97f, 10f, side))
        assertFalse(ScaffoldTouch.isPause(side * 0.5f, 10f, side))
    }
}
