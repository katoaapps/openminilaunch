package org.sableos.games

import kotlin.random.Random

/**
 * Gulam Choor, the "thief Jack" game. One Jack is taken out, so one of the other Jacks has no
 * partner. Pairs of the same rank are thrown away at the start; then, in turn, each player takes a
 * card without looking from the next player's hand and throws any new pair. Whoever is left holding
 * the Jack with no partner is the Gulam Choor. You are player 0 and take from player 1.
 */
internal data class GulamChoorGame(
    val names: List<String>,
    val hands: List<List<PlayCard>>,
    val turn: Int,
    val thrown: Int,
    val loser: Int? = null,
    val note: String = "",
) {
    private val n: Int get() = hands.size

    val over: Boolean get() = loser != null
    val needsStep: Boolean get() = !over && turn != HUMAN

    /** The player [player] takes from: the next one still holding cards. */
    fun victim(player: Int): Int {
        var p = player
        repeat(n) {
            p = (p + 1) % n
            if (hands[p].isNotEmpty() && p != player) return p
        }
        return player
    }

    /** Take the card at [index] of the next player's hand. */
    fun take(
        index: Int,
        random: Random,
    ): GulamChoorGame = if (turn == HUMAN) takeFor(HUMAN, index, random) else this

    fun step(random: Random): GulamChoorGame {
        if (!needsStep) return this
        val v = victim(turn)
        return takeFor(turn, random.nextInt(hands[v].size), random)
    }

    private fun takeFor(
        player: Int,
        index: Int,
        random: Random,
    ): GulamChoorGame {
        if (over || hands[player].isEmpty()) return this
        val from = victim(player)
        if (from == player || index !in hands[from].indices) return this
        val card = hands[from][index]
        val rest = hands[from].filterIndexed { i, _ -> i != index }.shuffled(random)
        val mine = hands[player] + card
        val pair = mine.dropLast(1).firstOrNull { it.rank == card.rank }
        val kept = if (pair != null) mine.filter { it != pair && it != card } else mine
        val thrownNow = if (pair != null) 2 else 0
        val next =
            hands.mapIndexed { i, h ->
                if (i == player) {
                    kept
                } else if (i == from) {
                    rest
                } else {
                    h
                }
            }
        val holders = next.indices.filter { next[it].isNotEmpty() }
        val note =
            if (pair !=
                null
            ) {
                "${names[player]} throws a pair of ${IndianCards.rankName(card.rank)}s"
            } else {
                "${names[player]} takes a card"
            }
        if (holders.size <= 1) {
            val last = holders.firstOrNull() ?: from
            return copy(
                hands = next,
                thrown = thrown + thrownNow,
                loser = last,
                note = "${names[last]} is the Gulam Choor!",
            )
        }
        var t = player
        repeat(n) {
            t = (t + 1) % n
            if (next[t].isNotEmpty()) return copy(hands = next, thrown = thrown + thrownNow, turn = t, note = note)
        }
        return copy(note = note)
    }

    companion object {
        const val HUMAN = 0
        const val PLAYERS = 4

        fun deal(
            random: Random,
            names: List<String>,
        ): GulamChoorGame {
            val deck = IndianCards.deck(random).filterNot { it.rank == 11 && it.suit == IndianCards.CLUBS }
            val dealt = List(PLAYERS) { p -> deck.filterIndexed { i, _ -> i % PLAYERS == p } }
            var thrown = 0
            val hands =
                dealt.map { hand ->
                    val kept = mutableListOf<PlayCard>()
                    hand.groupBy { it.rank }.forEach { (_, same) ->
                        thrown += same.size / 2 * 2
                        if (same.size % 2 == 1) kept += same.last()
                    }
                    kept.shuffled(random)
                }
            val first = hands.indices.firstOrNull { hands[it].isNotEmpty() } ?: 0
            return GulamChoorGame(names, hands, turn = if (hands[HUMAN].isNotEmpty()) HUMAN else first, thrown = thrown)
        }
    }
}
