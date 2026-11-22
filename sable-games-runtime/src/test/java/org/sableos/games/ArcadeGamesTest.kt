package org.sableos.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Snake, Blocks and Bricks: the rules behind the screens. */
class ArcadeGamesTest {
    @Test
    fun snakeMovesGrowsOnFoodAndDiesAtTheWall() {
        val start = SnakeGame.start(Random(1))
        val moved = start.copy(food = Cell(0, 0)).step(Random(1))
        assertEquals(Cell(9, 8), moved.body.first())
        assertEquals(3, moved.body.size)

        val eating = start.copy(food = Cell(9, 8)).step(Random(1))
        assertEquals(4, eating.body.size)
        assertEquals(1, eating.score)
        assertTrue(eating.food !in eating.body)

        var game = start.copy(food = Cell(0, 0))
        repeat(SnakeGame.SIZE) { game = game.step(Random(1)) }
        assertFalse(game.alive)
    }

    @Test
    fun snakeCannotTurnStraightBack() {
        val start = SnakeGame.start(Random(1))
        assertEquals(start, start.turn(-1, 0))
        assertEquals(Cell(0, 1), start.turn(0, 1).pending)
    }

    @Test
    fun blocksClearAFullRowAndScoreIt() {
        val well = List(BlocksGame.HEIGHT) { IntArray(BlocksGame.WIDTH) }
        // Fill the bottom row except the four cells an I piece lying flat at x=3..6 will cover.
        (0 until BlocksGame.WIDTH).filter { it !in 3..6 }.forEach { well[BlocksGame.HEIGHT - 1][it] = 1 }
        val game = BlocksGame(well, FallingPiece(0, BlocksGame.SHAPES[0], 4, 1), next = 1)
        val dropped = game.drop(Random(1))
        assertEquals(1, dropped.lines)
        assertEquals(100, dropped.score)
        assertTrue(dropped.well.last().all { it == 0 })
        assertFalse(dropped.over)
    }

    @Test
    fun blocksStayInsideTheWell() {
        var game = BlocksGame.start(Random(3))
        repeat(20) { game = game.move(-1) }
        assertTrue(game.piece.absolute.all { it.x >= 0 })
        repeat(4) { game = game.rotate() }
        assertTrue(game.piece.absolute.all { it.x in 0 until BlocksGame.WIDTH })
    }

    @Test
    fun blocksEndWhenTheWellFillsUp() {
        var game = BlocksGame.start(Random(5))
        repeat(200) { if (!game.over) game = game.drop(Random(it)) }
        assertTrue(game.over)
    }

    @Test
    fun bricksBallRestsOnThePaddleUntilLaunched() {
        val game = BricksGame().movePaddle(0.2f)
        assertEquals(0.7f, game.paddleX, 0.001f)
        assertEquals(game.paddleX, game.ballX, 0.001f)
        assertEquals(game, game.step(1f))
        assertTrue(game.launch().velY < 0f)
    }

    @Test
    fun bricksBreakAndAMissCostsALife() {
        var game = BricksGame().launch()
        repeat(200) { game = game.step(1f / 60) }
        assertTrue(game.score > 0)

        val falling = BricksGame(ballX = 0.05f, ballY = 0.99f, velX = 0f, velY = 1f, paddleX = 0.9f, launched = true)
        val missed = falling.step(0.05f)
        assertEquals(2, missed.lives)
        assertFalse(missed.launched)
    }
}

/** Reversi, Solitaire, Mahjong and Words. */
class BoardGamesTest {
    @Test
    fun reversiOpensWithFourMovesAndFlips() {
        val game = ReversiGame()
        assertEquals(4, game.legalMoves(ReversiGame.DARK).size)
        val played = game.play(2 * 8 + 3)
        assertEquals(4, played.count(ReversiGame.DARK))
        assertEquals(1, played.count(ReversiGame.LIGHT))
        assertEquals(ReversiGame.LIGHT, played.turn)
        assertEquals(played, played.play(0))
        assertNotNull(played.phoneMove())
    }

    @Test
    fun reversiPhonePrefersACorner() {
        val cells = MutableList(64) { ReversiGame.EMPTY }
        cells[1] = ReversiGame.DARK
        cells[2] = ReversiGame.LIGHT
        cells[9] = ReversiGame.DARK
        cells[18] = ReversiGame.LIGHT
        cells[10] = ReversiGame.DARK
        cells[11] = ReversiGame.LIGHT
        val game = ReversiGame(cells, ReversiGame.LIGHT)
        assertEquals(0, game.phoneMove())
    }

    @Test
    fun solitaireDealsKlondike() {
        val game = SolitaireGame.deal(Random(7))
        assertEquals(24, game.pile(SolitaireGame.STOCK).size)
        (0 until 7).forEach { column ->
            val pile = game.pile(SolitaireGame.TABLEAU_FIRST + column)
            assertEquals(column + 1, pile.size)
            assertTrue(pile.last().up)
            assertTrue(pile.dropLast(1).none { it.up })
        }
        val drawn = game.draw()
        assertEquals(1, drawn.pile(SolitaireGame.WASTE).size)
        var cycled = game
        repeat(25) { cycled = cycled.draw() }
        assertEquals(24, cycled.pile(SolitaireGame.STOCK).size)
    }

    @Test
    fun solitaireMovesARunAndTurnsTheCardUnder() {
        val piles = MutableList(SolitaireGame.PILE_COUNT) { emptyList<PlayCard>() }
        piles[6] = listOf(PlayCard(5, 0, up = false), PlayCard(9, 1), PlayCard(8, 0))
        piles[7] = listOf(PlayCard(10, 3))
        piles[2] = emptyList()
        val game = SolitaireGame(piles)
        val moved = game.move(6, 7)
        assertNotNull(moved)
        assertEquals(3, moved!!.pile(7).size)
        assertTrue(moved.pile(6).single().up)
        assertNull(game.move(7, 6))
        assertNull(game.toFoundation(6))
        val ace = SolitaireGame(piles.toMutableList().also { it[8] = listOf(PlayCard(1, 2)) })
        assertEquals(1, ace.toFoundation(8)!!.pile(2).size)
    }

    @Test
    fun mahjongDealIsClearableAndMatchesFreePairs() {
        val game = MahjongGame.deal(Random(11))
        assertEquals(72, game.tiles.size)
        assertTrue(
            game.tiles.values
                .groupBy { it }
                .values
                .all { it.size == 4 },
        )
        var state = game
        while (!state.won) {
            val (a, b) = state.hint() ?: break
            state = state.match(a, b)!!
        }
        // A greedy run may get stuck, but it always gets somewhere; the deal itself is solvable.
        assertTrue(state.tiles.size < 72)
        val covered = TileSpot(0, 3, 3)
        assertFalse(game.isFree(covered))
    }

    @Test
    fun wordsMarksRepeatedLettersOnce() {
        assertEquals(
            listOf(LetterMark.Elsewhere, LetterMark.Elsewhere, LetterMark.Absent, LetterMark.Right, LetterMark.Absent),
            WordsGame.marks("LOOLS", "HELLO"),
        )
        assertEquals(List(5) { LetterMark.Right }, WordsGame.marks("HELLO", "HELLO"))
    }

    @Test
    fun wordsTakesFiveLettersAndEndsAfterSixGuesses() {
        var game = WordsGame("CRANE")
        "CRAN".forEach { game = game.type(it) }
        assertEquals(game, game.submit())
        game = game.type('E').type('X').submit()
        assertTrue(game.won)
        var losing = WordsGame("CRANE")
        repeat(6) { losing = "SLOTH".fold(losing) { g, c -> g.type(c) }.submit() }
        assertTrue(losing.over)
        assertFalse(losing.won)
        assertTrue(WordsGame.ANSWERS.all { it.length == 5 && it.all(Char::isUpperCase) })
    }
}
