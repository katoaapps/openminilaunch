package org.sableos.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class IndianTableTest {
    private val left = -1f to 0f
    private val top = 0f to -1f
    private val right = 1f to 0f
    private val bottom = 0f to 1f

    private fun near(
        want: Pair<Float, Float>,
        got: Pair<Float, Float>,
    ) {
        assertEquals(want.first, got.first, 1e-3f)
        assertEquals(want.second, got.second, 1e-3f)
    }

    @Test
    fun youSitAtTheBottom() {
        near(bottom, seatDirection(0, 4, clockwise = true))
        near(bottom, seatDirection(0, 3, clockwise = false))
    }

    @Test
    fun clockwisePassesToYourLeft() {
        // On a clock face seen from above: you at 6, then 9, 12 and 3.
        near(left, seatDirection(1, 4, clockwise = true))
        near(top, seatDirection(2, 4, clockwise = true))
        near(right, seatDirection(3, 4, clockwise = true))
    }

    @Test
    fun anticlockwisePassesToYourRight() {
        near(right, seatDirection(1, 4, clockwise = false))
        near(top, seatDirection(2, 4, clockwise = false))
        near(left, seatDirection(3, 4, clockwise = false))
    }

    @Test
    fun threePlayersSitUpperLeftAndUpperRight() {
        val first = seatDirection(1, 3, clockwise = true)
        val second = seatDirection(2, 3, clockwise = true)
        assertTrue(first.first < 0f && first.second < 0f)
        assertTrue(second.first > 0f && second.second < 0f)
    }

    @Test
    fun theCollectedTrickStaysOnView() {
        val random = Random(4)
        var game = TrickGame.newGame(TrickVariant.Callbreak, listOf("You", "A", "B", "C"), random)
        var guard = 0
        while (game.tricks.sum() == 0 && guard++ < 500) {
            game =
                when {
                    game.needsStep -> game.step(random)
                    game.phase == TrickPhase.Bid -> game.bid(game.bidRange().first { game.bidAllowed(0, it) })
                    else -> game.play(game.legal().first())
                }
        }
        assertEquals(1, game.tricks.sum())
        assertTrue(game.trick.isEmpty())
        assertEquals(4, game.lastTrick.size)
        assertEquals(4, game.lastTrick.map { it.first }.toSet().size)
    }

    @Test
    fun thePhonePlayersAreSlowEnoughToFollow() {
        assertTrue(IndianPace.CARD_MS >= 1000L)
        assertTrue(IndianPace.TRICK_HOLD_MS >= 2 * IndianPace.CARD_MS)
    }
}
