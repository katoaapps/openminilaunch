package org.sableos.games

import kotlin.random.Random

/** A playing card: [rank] 1 (ace) to 13 (king), [suit] 0 spades, 1 hearts, 2 diamonds, 3 clubs. */
internal data class PlayCard(
    val rank: Int,
    val suit: Int,
    val up: Boolean = true,
) {
    val red: Boolean get() = suit == 1 || suit == 2
    val label: String get() = RANKS[rank - 1] + SUITS[suit]

    companion object {
        private val RANKS = listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
        private val SUITS = listOf("♠", "♥", "♦", "♣")
    }
}

/**
 * Klondike, turning one card at a time. Piles are numbered for the keyboard cursor: [STOCK],
 * [WASTE], foundations 2-5 and tableau columns 6-12 ([pile]). A move takes the longest run of
 * face-up cards that fits where it goes; the card under a moved run turns face up.
 */
internal data class SolitaireGame(
    val piles: List<List<PlayCard>>,
    val moves: Int = 0,
) {
    fun pile(index: Int): List<PlayCard> = piles[index]

    val won: Boolean get() = (FOUNDATION_FIRST until TABLEAU_FIRST).all { piles[it].size == RANK_COUNT }

    /** Space: the next stock card to the waste, or the waste back to the stock when the stock is empty. */
    fun draw(): SolitaireGame {
        val stock = piles[STOCK]
        val waste = piles[WASTE]
        return when {
            stock.isNotEmpty() -> with(STOCK to stock.dropLast(1), WASTE to waste + stock.last().copy(up = true))
            waste.isNotEmpty() -> with(STOCK to waste.reversed().map { it.copy(up = false) }, WASTE to emptyList())
            else -> this
        }
    }

    /** Moves cards from [from] to [to], or returns null when no card there may go there. */
    fun move(
        from: Int,
        to: Int,
    ): SolitaireGame? {
        if (from == to || to <= WASTE || from == STOCK) return null
        val source = piles[from]
        val run = movableRun(from)
        if (run.isEmpty()) return null
        val count =
            if (to < TABLEAU_FIRST) {
                1.takeIf { fitsFoundation(run.last(), piles[to]) }
            } else {
                (run.size downTo 1).firstOrNull { fitsTableau(run[run.size - it], piles[to]) }
            } ?: return null
        val left = source.dropLast(count)
        val turned = if (left.isNotEmpty() && !left.last().up) left.dropLast(1) + left.last().copy(up = true) else left
        return with(from to turned, to to piles[to] + source.takeLast(count)).copy(moves = moves + 1)
    }

    /** A: the top card of [from] to the first foundation that takes it. */
    fun toFoundation(from: Int): SolitaireGame? =
        (FOUNDATION_FIRST until TABLEAU_FIRST).firstNotNullOfOrNull { move(from, it) }

    private fun movableRun(from: Int): List<PlayCard> {
        val source = piles[from]
        if (source.isEmpty()) return emptyList()
        if (from < TABLEAU_FIRST) return listOf(source.last())
        return source.takeLastWhile { it.up }
    }

    private fun with(vararg changes: Pair<Int, List<PlayCard>>): SolitaireGame {
        val next = piles.toMutableList()
        changes.forEach { (index, cards) -> next[index] = cards }
        return copy(piles = next)
    }

    companion object {
        const val STOCK = 0
        const val WASTE = 1
        const val FOUNDATION_FIRST = 2
        const val TABLEAU_FIRST = 6
        const val PILE_COUNT = 13
        private const val RANK_COUNT = 13
        private const val COLUMNS = 7

        fun fitsFoundation(
            card: PlayCard,
            foundation: List<PlayCard>,
        ): Boolean {
            val top = foundation.lastOrNull()
            return if (top == null) card.rank == 1 else top.suit == card.suit && card.rank == top.rank + 1
        }

        fun fitsTableau(
            card: PlayCard,
            column: List<PlayCard>,
        ): Boolean {
            val top = column.lastOrNull()
            return if (top ==
                null
            ) {
                card.rank == RANK_COUNT
            } else {
                top.up && top.red != card.red && card.rank == top.rank - 1
            }
        }

        fun deal(random: Random): SolitaireGame {
            val deck =
                (0 until 4)
                    .flatMap { suit ->
                        (1..RANK_COUNT).map { PlayCard(it, suit, up = false) }
                    }.shuffled(random)
            var next = 0
            val tableau =
                (0 until COLUMNS).map { column ->
                    List(column + 1) { row -> deck[next++].copy(up = row == column) }
                }
            val stock = deck.drop(next)
            return SolitaireGame(listOf(stock, emptyList<PlayCard>()) + List(4) { emptyList() } + tableau)
        }
    }
}
