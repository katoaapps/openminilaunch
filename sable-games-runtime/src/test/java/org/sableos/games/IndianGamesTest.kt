package org.sableos.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

private fun c(
    rank: Int,
    suit: Int,
) = PlayCard(rank, suit)

private val NAMES4 = listOf("You", "Maria", "Chen", "Kofi")

class IndianGamesTest {
    // ------------------------------------------------------------ Teen Patti

    @Test
    fun teenPattiHandsRankFromTrailToHighCard() {
        val trail = TeenPattiGame.handScore(listOf(c(5, 0), c(5, 1), c(5, 2)))
        val pure = TeenPattiGame.handScore(listOf(c(4, 0), c(5, 0), c(6, 0)))
        val seq = TeenPattiGame.handScore(listOf(c(4, 0), c(5, 1), c(6, 0)))
        val colour = TeenPattiGame.handScore(listOf(c(2, 0), c(9, 0), c(12, 0)))
        val pair = TeenPattiGame.handScore(listOf(c(9, 0), c(9, 1), c(2, 2)))
        val high = TeenPattiGame.handScore(listOf(c(2, 0), c(9, 1), c(12, 2)))
        assertTrue(trail > pure && pure > seq && seq > colour && colour > pair && pair > high)
    }

    @Test
    fun teenPattiSequencesRankAceKingQueenOverAceTwoThree() {
        val akq = TeenPattiGame.handScore(listOf(c(1, 0), c(13, 1), c(12, 2)))
        val a23 = TeenPattiGame.handScore(listOf(c(1, 0), c(2, 1), c(3, 2)))
        val kqj = TeenPattiGame.handScore(listOf(c(13, 0), c(12, 1), c(11, 2)))
        val s234 = TeenPattiGame.handScore(listOf(c(2, 0), c(3, 1), c(4, 2)))
        assertTrue(akq > a23 && a23 > kqj && kqj > s234)
    }

    @Test
    fun teenPattiRoundsEndAndKeepEveryChip() {
        repeat(200) { seed ->
            val random = Random(seed)
            var game = TeenPattiGame.deal(random, NAMES4, List(4) { 1000 })
            val total = game.chips.sum() + game.pot
            var guard = 0
            while (!game.over && guard++ < 100) {
                val action =
                    when {
                        game.turn != 0 -> break
                        game.canShow(0) && random.nextBoolean() -> TeenPattiGame.Action.Show
                        game.canChaal(0) && random.nextInt(4) > 0 -> TeenPattiGame.Action.Chaal
                        game.raiseAllowed(0) && random.nextBoolean() -> TeenPattiGame.Action.Raise
                        else -> TeenPattiGame.Action.Pack
                    }
                game = game.act(action, random)
            }
            assertTrue("seed $seed ends", game.over)
            assertEquals("seed $seed chips", total, game.chips.sum() + game.pot)
            assertTrue(game.chips.all { it >= 0 })
        }
    }

    // ------------------------------------------------------------ Rummy

    @Test
    fun rummyRecognisesSequencesAndSets() {
        assertTrue(RummyMelds.isSequence(listOf(c(4, 1), c(5, 1), c(6, 1)), wild = 9, pure = true))
        assertTrue(RummyMelds.isSequence(listOf(c(12, 0), c(13, 0), c(1, 0)), wild = 9, pure = true))
        assertTrue(RummyMelds.isSequence(listOf(c(1, 0), c(2, 0), c(3, 0)), wild = 9, pure = true))
        assertFalse(RummyMelds.isSequence(listOf(c(13, 0), c(1, 0), c(2, 0)), wild = 9, pure = true))
        assertFalse(RummyMelds.isSequence(listOf(c(4, 1), c(5, 2), c(6, 1)), wild = 9, pure = true))
        // A joker fills a gap in an impure sequence, never in a pure one.
        val gap = listOf(c(4, 1), jokerCard(), c(6, 1))
        assertTrue(RummyMelds.isSequence(gap, wild = 9, pure = false))
        assertFalse(RummyMelds.isSequence(gap, wild = 9, pure = true))
        // The wild-rank card stands in too.
        assertTrue(RummyMelds.isSequence(listOf(c(4, 1), c(9, 3), c(6, 1)), wild = 9, pure = false))
        assertTrue(RummyMelds.isSet(listOf(c(9, 0), c(9, 1), c(9, 2)), wild = 5))
        assertFalse(RummyMelds.isSet(listOf(c(9, 0), c(9, 0), c(9, 2)), wild = 5))
        assertTrue(RummyMelds.isSet(listOf(c(9, 0), jokerCard(), c(9, 2)), wild = 5))
        assertFalse(RummyMelds.isSet(listOf(c(9, 0), c(8, 1), c(9, 2)), wild = 5))
    }

    @Test
    fun rummyDeclarationNeedsAPureSequenceAndASecondSequence() {
        val good =
            listOf(
                c(4, 1),
                c(5, 1),
                c(6, 1), // pure
                c(10, 0),
                c(11, 0),
                c(12, 0), // second sequence
                c(9, 3),
                c(9, 2),
                c(9, 0), // set
                c(2, 0),
                c(2, 1),
                c(2, 2), // set
                c(7, 3), // the finishing discard
            )
        assertTrue(RummyMelds.declarable(good, 12, wild = 13))
        assertFalse(RummyMelds.declarable(good, 0, wild = 13))
        val noSecondSequence =
            listOf(
                c(4, 1),
                c(5, 1),
                c(6, 1),
                c(9, 3),
                c(9, 2),
                c(9, 0),
                c(2, 0),
                c(2, 1),
                c(2, 2),
                c(12, 0),
                c(12, 1),
                c(12, 2),
                c(7, 3),
            )
        assertFalse(RummyMelds.declarable(noSecondSequence, 12, wild = 13))
    }

    @Test
    fun rummyJokerCanCompleteAnImpureSequenceButNotThePureOne() {
        val hand =
            listOf(
                c(4, 1),
                c(5, 1),
                c(6, 1),
                c(10, 0),
                jokerCard(),
                c(12, 0),
                c(9, 3),
                c(9, 2),
                c(9, 0),
                c(2, 0),
                c(2, 1),
                c(2, 2),
                c(7, 3),
            )
        assertTrue(RummyMelds.declarable(hand, 12, wild = 13))
        val jokerPure =
            listOf(
                c(4, 1),
                jokerCard(),
                c(6, 1),
                c(10, 0),
                c(11, 0),
                c(12, 0),
                c(9, 3),
                c(9, 2),
                c(9, 0),
                c(2, 0),
                c(2, 1),
                c(2, 2),
                c(7, 3),
            )
        // The 10-J-Q is pure, so this still wins: only one pure sequence is needed.
        assertTrue(RummyMelds.declarable(jokerPure, 12, wild = 13))
    }

    @Test
    fun rummyPenaltyIsEightyWithoutTwoSequences() {
        val noSeq = List(13) { c(it % 13 + 1, it % 4) }
        assertEquals(80, RummyMelds.penalty(noSeq.shuffled(Random(1)), 13))
    }

    @Test
    fun rummyGamesPlayThroughWithoutGettingStuck() {
        repeat(40) { seed ->
            val random = Random(seed)
            var game = RummyGame.deal(random, listOf("You", "Maria", "Chen"))
            assertEquals(13, game.hands[0].size)
            var turns = 0
            while (!game.over && turns++ < 400) {
                game = if (random.nextBoolean()) game.drawFromStock(random) else game.drawFromOpen()
                if (!game.drawn) continue
                val win = game.hands[0].indices.firstOrNull { game.canDeclare(it) }
                game = if (win != null) game.declare(win) else game.discard(random.nextInt(game.hands[0].size), random)
            }
            // Either someone declared or the game was still running when the cap was hit.
            if (game.over) {
                assertTrue(game.points.all { it in 0..80 })
                assertTrue(game.winner == null || game.points[game.winner!!] == 0)
            }
        }
    }

    // ------------------------------------------------------------ trick games

    private fun playOut(
        variant: TrickVariant,
        seed: Int,
    ): TrickGame {
        val random = Random(seed)
        var game = TrickGame.newGame(variant, NAMES4.take(variant.players), random)
        var guard = 0
        while (!game.gameOver && guard++ < 5000) {
            when {
                game.needsStep -> {
                    game = game.step(random)
                }

                game.phase == TrickPhase.Bid -> {
                    val value = game.bidRange().first { game.bidAllowed(0, it) }
                    game = game.bid(value)
                }

                game.phase == TrickPhase.PickTrump -> {
                    game = game.pickTrump(random.nextInt(4))
                }

                game.phase == TrickPhase.Play -> {
                    game = game.play(game.legal().random(random))
                }

                game.phase == TrickPhase.RoundOver -> {
                    game = game.nextRound(random)
                }
            }
        }
        return game
    }

    @Test
    fun everyTrickGameFinishesAndDealsEveryCardOut() {
        for (variant in TrickVariant.entries) {
            repeat(30) { seed ->
                val game = playOut(variant, seed)
                assertTrue("$variant seed $seed", game.gameOver)
                assertTrue(game.hands.all { it.isEmpty() })
                assertEquals(game.dealt, game.tricks.sum())
            }
        }
    }

    @Test
    fun twoThreeFiveScoresAddUpToZero() {
        repeat(20) { seed -> assertEquals(0.0, playOut(TrickVariant.TwoThreeFive, seed).scores.sum(), 1e-9) }
    }

    @Test
    fun callbreakMakesYouFollowSuitAndTrump() {
        val base = TrickGame.newGame(TrickVariant.Callbreak, NAMES4, Random(3))
        val hand = listOf(c(5, 1), c(12, 1), c(3, 0), c(9, 2))
        val led =
            base.copy(
                hands = listOf(hand, emptyList(), emptyList(), emptyList()),
                trick = listOf(3 to c(8, 1)),
                turn = 0,
                phase = TrickPhase.Play,
            )
        assertEquals(setOf(c(12, 1)), led.legal(0).toSet()) // must beat the 8 of hearts
        val voidHearts = led.copy(hands = listOf(listOf(c(3, 0), c(9, 2)), emptyList(), emptyList(), emptyList()))
        assertEquals(setOf(c(3, 0)), voidHearts.legal(0).toSet()) // no hearts: must play a spade
    }

    @Test
    fun judgementLastBidderCannotMakeTheBidsAddUp() {
        val g = TrickGame.newGame(TrickVariant.Judgement, NAMES4, Random(5))
        val almost = g.copy(bids = listOf(-1, 2, 2, 1), phase = TrickPhase.Bid, turn = 0)
        assertEquals(7, almost.handSize)
        assertFalse(almost.bidAllowed(0, 2))
        assertTrue(almost.bidAllowed(0, 3))
    }

    @Test
    fun mendicotShowsTheTrumpWhenSomeoneCannotFollow() {
        val g = TrickGame.newGame(TrickVariant.Mendicot, NAMES4, Random(2))
        assertNull(g.trump)
        val hands = listOf(listOf(c(3, 0)), listOf(c(4, 1)), listOf(c(5, 1)), listOf(c(6, 1)))
        val started = g.copy(hands = hands, turn = 0, hiddenTrump = 2, phase = TrickPhase.Play).play(c(3, 0))
        assertEquals(2, started.trump)
    }

    // ------------------------------------------------------------ Bluff, Donkey, Gulam Choor

    @Test
    fun bluffGamesFinish() {
        repeat(40) { seed ->
            val random = Random(seed)
            var game = BluffGame.deal(random, NAMES4)
            var guard = 0
            while (!game.over && guard++ < 3000) {
                game =
                    if (game.needsStep) {
                        game.step(random)
                    } else if (game.canCall() && random.nextInt(3) == 0) {
                        game.call()
                    } else {
                        val mine = game.hands[0]
                        val matching = mine.filter { it.rank == game.claim }
                        game.play(if (matching.isNotEmpty()) matching else listOf(mine.first()))
                    }
                assertEquals(52, game.hands.sumOf { it.size } + game.pile.size)
            }
            assertTrue("seed $seed", game.over)
        }
    }

    @Test
    fun bluffCallPunishesTheLiar() {
        val g =
            BluffGame(
                NAMES4,
                listOf(listOf(c(2, 0)), listOf(c(9, 0), c(8, 0)), listOf(c(4, 0)), listOf(c(5, 0))),
                emptyList(),
                claim = 1,
                turn = 0,
            )
        val played = g.play(listOf(c(2, 0)))
        assertEquals(1, played.last!!.third)
        val called = played.copy(turn = 1).call(1)
        assertEquals(1, called.hands[0].size)
        assertTrue(called.pile.isEmpty())
    }

    @Test
    fun donkeyRoundsEndWithALetterAndTheGameWithADonkey() {
        repeat(30) { seed ->
            val random = Random(seed)
            var game = DonkeyGame.deal(random, NAMES4)
            var guard = 0
            while (game.phase != DonkeyGame.Phase.Over && guard++ < 4000) {
                when (game.phase) {
                    DonkeyGame.Phase.Passing -> {
                        game = game.pass(random.nextInt(4), random)
                    }

                    DonkeyGame.Phase.Grabbing -> {
                        if (game.hasFour(0)) game = game.grab(0)
                        game.reactionTimes(random).forEach { game = game.grab(it.first) }
                        game = game.grab(0)
                    }

                    DonkeyGame.Phase.RoundDone -> {
                        game = game.nextRound(random)
                    }

                    else -> {
                        Unit
                    }
                }
            }
            assertNotNull("seed $seed", game.donkey)
        }
    }

    @Test
    fun donkeyGrabbingWithoutFourCostsALetter() {
        val hands = List(4) { p -> List(4) { c(p + 1, it) } }
        val g =
            DonkeyGame(
                NAMES4,
                hands.map {
                    it.mapIndexed { i, card ->
                        if (i == 0 &&
                            it[0].rank == 1
                        ) {
                            card
                        } else {
                            card
                        }
                    }
                },
                List(4) { 0 },
            )
        val swapped =
            g.copy(
                hands =
                    listOf(
                        listOf(c(1, 0), c(1, 1), c(1, 2), c(2, 3)),
                        listOf(c(2, 0), c(2, 1), c(2, 2), c(1, 3)),
                        hands[2],
                        hands[3],
                    ),
                phase = DonkeyGame.Phase.Grabbing,
            )
        assertEquals(1, swapped.grab(0).letters[0])
    }

    @Test
    fun gulamChoorLeavesExactlyOneLoser() {
        repeat(60) { seed ->
            val random = Random(seed)
            var game = GulamChoorGame.deal(random, NAMES4)
            assertEquals(51, game.hands.sumOf { it.size } + game.thrown)
            var guard = 0
            while (!game.over && guard++ < 2000) {
                game =
                    if (game.needsStep) {
                        game.step(
                            random,
                        )
                    } else {
                        game.take(random.nextInt(game.hands[game.victim(0)].size), random)
                    }
            }
            assertTrue("seed $seed", game.over)
            val loser = game.loser!!
            assertEquals(1, game.hands[loser].size)
            assertEquals(11, game.hands[loser].single().rank)
            assertEquals(
                0,
                game.hands.indices
                    .filter { it != loser }
                    .sumOf { game.hands[it].size },
            )
        }
    }

    // ------------------------------------------------------------ board games

    @Test
    fun snakesAndLaddersJumpAndNeedAnExactFinish() {
        val g = SnakesGame.start(listOf("You", "Maria"))
        assertEquals(22, g.move(3).pos[0])
        val onSnake = g.copy(pos = listOf(56, 0)).move(6)
        assertEquals(18, onSnake.pos[0])
        assertEquals(0, onSnake.turn) // a six rolls again
        val close = g.copy(pos = listOf(98, 0))
        assertEquals(98, close.move(3).pos[0])
        assertEquals(100, close.move(2).pos[0])
        assertEquals(0, close.move(2).winner)
    }

    @Test
    fun snakesGamesFinish() {
        repeat(50) { seed ->
            val random = Random(seed)
            var game = SnakesGame.start(NAMES4)
            var guard = 0
            while (!game.over && guard++ < 5000) game = game.roll(random)
            assertTrue(game.over)
        }
    }

    @Test
    fun ludoNeedsASixToLeaveTheYardAndCapturesOffSafeSquares() {
        val g = LudoGame.start(NAMES4)
        assertTrue(g.copy(roll = 5).movable().isEmpty())
        assertEquals(listOf(0, 1, 2, 3), g.copy(roll = 6).movable())
        // A red token on progress 10 stands on square 10; green reaches square 10 at progress 49.
        val setup =
            g.copy(
                tokens =
                    listOf(
                        listOf(10, -1, -1, -1),
                        listOf(48, -1, -1, -1),
                        listOf(-1, -1, -1, -1),
                        listOf(-1, -1, -1, -1),
                    ),
                turn = 1,
                roll = 1,
            )
        assertEquals(LudoGame.square(0, 10), LudoGame.square(1, 49))
        val moved = setup.move(0)
        assertEquals(-1, moved.tokens[0][0])
        assertEquals(49, moved.tokens[1][0])
        assertEquals(1, moved.turn) // a capture rolls again
    }

    @Test
    fun ludoStartAndStarSquaresAreSafe() {
        val g = LudoGame.start(NAMES4)
        val redOn8 = g.copy(tokens = g.tokens.mapIndexed { c, l -> if (c == 0) listOf(8, -1, -1, -1) else l })
        val greenOn8 =
            redOn8.copy(
                tokens =
                    redOn8.tokens.mapIndexed { c, l ->
                        if (c ==
                            1
                        ) {
                            listOf(47, -1, -1, -1)
                        } else {
                            l
                        }
                    },
                turn = 1,
                roll = 1,
            )
        // Green progress 47 + 1 = 48 is square 61 % 52 = 9, not 8, so nothing happens to red.
        assertEquals(8, greenOn8.move(0).tokens[0][0])
        assertTrue(LudoGame.isSafe(8, 0) && LudoGame.isSafe(0, 2) && !LudoGame.isSafe(9, 0))
    }

    @Test
    fun ludoGamesFinish() {
        repeat(20) { seed ->
            val random = Random(seed)
            var game = LudoGame.start(listOf("You", "Maria", "Chen", "Kofi"))
            var guard = 0
            while (!game.over && guard++ < 20000) {
                if (game.turn == 0) {
                    game = if (game.roll == null) game.rollDice(random) else game.move(game.movable().random(random))
                } else {
                    game = game.step(random)
                }
            }
            assertTrue("seed $seed", game.over)
        }
    }

    // ------------------------------------------------------------ keys and the Ludo board

    private fun key(
        keyCode: Int,
        char: Char? = null,
        alt: Boolean = false,
        repeat: Int = 0,
    ) = GameKeymap.command(GameKind.Indian, GameKeyPress(keyCode, char, alt, false, repeat))

    @Test
    fun indianGamesTakeEnterSpaceLettersAndAltDigits() {
        assertEquals(GameCommand.Primary, key(android.view.KeyEvent.KEYCODE_ENTER))
        assertEquals(GameCommand.Select, key(android.view.KeyEvent.KEYCODE_SPACE))
        assertEquals(GameCommand.Erase, key(android.view.KeyEvent.KEYCODE_DEL))
        assertEquals(GameCommand.Letter('Z'), key(android.view.KeyEvent.KEYCODE_Z, 'z'))
        assertEquals(GameCommand.Letter('N'), key(android.view.KeyEvent.KEYCODE_N, 'n'))
        assertEquals(GameCommand.Digit(2), key(android.view.KeyEvent.KEYCODE_E, '2', alt = true))
        assertNull(key(android.view.KeyEvent.KEYCODE_Z, '~', alt = true))
        // A held key does not repeat a command.
        assertNull(key(android.view.KeyEvent.KEYCODE_ENTER, repeat = 2))
    }

    @Test
    fun ludoTrackHasFiftyTwoDistinctSquaresAndHomeColumnsFitTheBoard() {
        assertEquals(52, LudoGeometry.PATH.size)
        assertEquals(52, LudoGeometry.PATH.toSet().size)
        for (color in 0 until 4) {
            for (progress in -1..LudoGame.HOME) {
                val (x, y) = LudoGeometry.centre(color, 0, progress)
                assertTrue("$color $progress", x in 0f..15f && y in 0f..15f)
            }
        }
        // Each colour's first square is its start, 13 squares apart round the board.
        assertEquals(6 to 1, LudoGeometry.PATH[0])
        assertEquals(1 to 8, LudoGeometry.PATH[13])
        assertEquals(8 to 13, LudoGeometry.PATH[26])
        assertEquals(13 to 6, LudoGeometry.PATH[39])
    }
}
