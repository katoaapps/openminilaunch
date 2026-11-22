package org.sableos.games

import kotlin.random.Random

/**
 * Bluff (also called Cheat). The whole deck is dealt out. Turns go round, and each player puts
 * one to four cards face down, saying they are all of the rank whose turn it is (aces, then twos,
 * and so on up to kings, then round again). The next player can call "bluff": if the cards were
 * not all that rank, the player who put them down picks up the whole pile; if they were, the caller
 * does. The first player out of cards wins. You are player 0.
 */
internal data class BluffGame(
    val names: List<String>,
    val hands: List<List<PlayCard>>,
    val pile: List<PlayCard>,
    /** The rank the next player must claim. */
    val claim: Int,
    val turn: Int,
    /** Who put cards down last, how many, and which rank they claimed. */
    val last: Triple<Int, Int, Int>? = null,
    val winner: Int? = null,
    val note: String = "",
    /** Cards turned over by the latest call, with who lied. */
    val reveal: List<PlayCard> = emptyList(),
) {
    private val n: Int get() = hands.size

    val over: Boolean get() = winner != null
    val needsStep: Boolean get() = !over && turn != HUMAN

    /** Someone may call: cards are down and they were not yours. */
    fun canCall(player: Int = HUMAN): Boolean = !over && turn == player && last != null && last.first != player

    /** Put [chosen] down as the claimed rank. */
    fun play(chosen: List<PlayCard>): BluffGame {
        if (over || turn != HUMAN || chosen.isEmpty() || chosen.size > 4 ||
            !hands[HUMAN].containsAll(chosen)
        ) {
            return this
        }
        return put(HUMAN, chosen)
    }

    private fun put(
        player: Int,
        chosen: List<PlayCard>,
    ): BluffGame {
        // Anyone who lets an emptied hand go uncalled has lost to that player.
        val pending = last?.first?.takeIf { hands[it].isEmpty() }
        if (pending !=
            null
        ) {
            return copy(winner = pending, note = "${names[pending]} is out of cards and wins", reveal = emptyList())
        }
        val rest = hands[player].toMutableList().also { h -> chosen.forEach { h.remove(it) } }
        return copy(
            hands = hands.mapIndexed { i, h -> if (i == player) rest else h },
            pile = pile + chosen,
            claim = claim % IndianCards.KING + 1,
            turn = (player + 1) % n,
            last = Triple(player, chosen.size, claim),
            note = "${names[player]} puts down ${chosen.size} ${IndianCards.rankName(
                claim,
            )}${if (chosen.size == 1) "" else "s"}",
            reveal = emptyList(),
        )
    }

    /** Call the last player a bluffer. */
    fun call(caller: Int = HUMAN): BluffGame {
        val l = last
        if (!canCall(caller) || l == null) return this
        val (liar, count, rank) = l
        val shown = pile.takeLast(count)
        val truthful = shown.all { it.rank == rank }
        val loser = if (truthful) caller else liar
        val takes = IndianCards.sorted(hands[loser] + pile).sortedBy { it.rank }
        val next = hands.mapIndexed { i, h -> if (i == loser) takes else h }
        val out = if (truthful && hands[liar].isEmpty()) liar else null
        val text =
            if (truthful) "${names[liar]} told the truth: ${names[caller]} picks up ${pile.size}" else "${names[liar]} was bluffing: picks up ${pile.size}"
        return copy(
            hands = next,
            pile = emptyList(),
            last = null,
            turn = caller,
            winner = out,
            note = text,
            reveal = shown,
        )
    }

    /** One phone move: call a bluff, or put cards down. */
    fun step(random: Random): BluffGame {
        if (!needsStep) return this
        val p = turn
        val l = last
        if (l != null && l.first != p) {
            val (who, count, rank) = l
            val held = hands[p].count { it.rank == rank }
            val surelyLying = held + count > 4
            val winning = hands[who].isEmpty()
            val suspicion = 8 + count * 10 + (if (hands[who].size <= 2) 25 else 0)
            if (winning || surelyLying || random.nextInt(100) < suspicion) return call(p)
        }
        val matching = hands[p].filter { it.rank == claim }
        val chosen =
            when {
                matching.isNotEmpty() -> {
                    matching.take(if (matching.size > 2) 3 else matching.size)
                }

                else -> {
                    val spare = hands[p].sortedBy { IndianCards.points(it) }.filter { it.rank != claim }
                    spare.take(if (spare.size > 3 && random.nextInt(4) == 0) 2 else 1)
                }
            }
        return put(p, chosen)
    }

    companion object {
        const val HUMAN = 0
        const val PLAYERS = 4

        fun deal(
            random: Random,
            names: List<String>,
        ): BluffGame {
            val deck = IndianCards.deck(random)
            val hands = List(PLAYERS) { p -> deck.filterIndexed { i, _ -> i % PLAYERS == p }.sortedBy { it.rank } }
            return BluffGame(names, hands, emptyList(), claim = 1, turn = 0)
        }
    }
}
