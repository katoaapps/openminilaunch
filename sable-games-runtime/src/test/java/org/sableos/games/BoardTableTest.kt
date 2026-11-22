package org.sableos.games

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardTableTest {
    private val names = listOf("You", "Maria", "Chen", "Kofi")

    @Test
    fun phonePlayersTakeTheCardGamesPace() {
        assertEquals(IndianPace.CARD_MS, BoardPace.TURN_MS)
        assertEquals(BoardPace.TURN_MS, BoardPace.THINK_MS + BoardPace.TUMBLE_MS)
    }

    @Test
    fun snakesRecordsTheWalkAndTheLadder() {
        val g = SnakesGame.start(names.take(2)).copy(pos = listOf(5, 0))
        val up = g.move(3)
        assertEquals(SnakesMove(0, 3, 5, 8, 30, overshot = false), up.last)
        assertEquals(listOf(6, 7, 8), up.last!!.walk)
        assertEquals("You rolled 3 and climbed a ladder: 8 to 30", up.note)
    }

    @Test
    fun snakesRecordsAnOvershootAndASix() {
        val close = SnakesGame.start(names.take(2)).copy(pos = listOf(98, 0))
        val over = close.move(5)
        assertTrue(over.last!!.overshot)
        assertTrue(over.last!!.walk.isEmpty())
        assertEquals("You rolled 5 but needed exactly 2", over.note)
        val six = SnakesGame.start(names.take(2)).move(6)
        assertTrue(six.rollsAgain)
        assertFalse(six.move(2).rollsAgain)
    }

    @Test
    fun theBoardHasEightLaddersAndEightSnakesThatNeverShareASquare() {
        assertEquals(100, SnakesGame.LAST)
        assertEquals(8, SnakesGame.LADDERS.size)
        assertEquals(8, SnakesGame.SNAKES.size)
        SnakesGame.LADDERS.forEach { (from, to) -> assertTrue(to > from) }
        SnakesGame.SNAKES.forEach { (from, to) -> assertTrue(to < from) }
        val ends =
            SnakesGame.LADDERS.flatMap { listOf(it.key, it.value) } +
                SnakesGame.SNAKES.flatMap { listOf(it.key, it.value) }
        assertEquals(ends.size, ends.toSet().size)
        assertFalse(SnakesGame.LAST in SnakesGame.SNAKES || 1 in SnakesGame.LADDERS)
    }

    @Test
    fun snakesStandingsPutTheWinnerFirst() {
        val g = SnakesGame.start(names.take(3)).copy(pos = listOf(10, 98, 30))
        val won = g.copy(turn = 1).move(2)
        assertEquals(listOf(1, 2, 0), won.standings)
    }

    @Test
    fun ludoRecordsTheWalkCapturesAndLanding() {
        val g =
            LudoGame.start(names).copy(
                tokens =
                    listOf(
                        listOf(10, -1, -1, -1),
                        listOf(47, -1, -1, -1),
                        listOf(-1, -1, -1, -1),
                        listOf(-1, -1, -1, -1),
                    ),
                turn = 1,
                roll = 2,
            )
        assertEquals(49, g.landing(0))
        assertNull(g.landing(1))
        val moved = g.move(0)
        assertEquals(LudoMove(1, 0, 47, 49, listOf(Triple(0, 0, 10))), moved.last)
        assertEquals(listOf(48, 49), moved.last!!.walk)
        assertEquals("Maria sent your token back to the yard", moved.note)
    }

    @Test
    fun ludoComingOutIsOneStep() {
        val out = LudoGame.start(names).copy(roll = 6).move(2)
        assertEquals(listOf(0), out.last!!.walk)
        assertEquals("You brought a token out", out.note)
    }

    @Test
    fun theBoardKeepsItsWidthOnSquarePhones() {
        // Q25, 720 x 720 at 2x: the frame gives a 248 dp square beside the rail, with 320 dp of height.
        assertEquals(242.dp, boardSideFor(248.dp, 320.dp, 78.dp))
        assertEquals(248.dp, boardSideFor(248.dp, 320.dp, 44.dp))
        // A tall phone scrolls: the board gives up the room for the seats and lines.
        assertEquals(282.dp, boardSideFor(360.dp, null, 78.dp))
    }
}
