package org.sableos.games

import kotlin.random.Random

/** A joker is a [PlayCard] with rank 0. */
internal val PlayCard.joker: Boolean get() = rank == 0

internal fun jokerCard(): PlayCard = PlayCard(0, 0)

/**
 * Indian Rummy, 13 cards, two decks and two printed jokers. A random rank is the wild joker for the
 * round. A winning hand has two or more sequences (one of them pure, with no joker) and every other
 * card in sets or more sequences. You are player 0, against two phone players.
 */
internal data class RummyGame(
    val names: List<String>,
    val hands: List<List<PlayCard>>,
    val stock: List<PlayCard>,
    val open: List<PlayCard>,
    val wild: Int,
    val turn: Int = 0,
    val drawn: Boolean = false,
    val over: Boolean = false,
    val winner: Int? = null,
    val points: List<Int> = List(hands.size) { 0 },
    val note: String = "",
) {
    val top: PlayCard? get() = open.lastOrNull()

    fun drawFromStock(random: Random): RummyGame {
        if (over || turn != HUMAN || drawn) return this
        val refilled = refill(random)
        val card = refilled.stock.last()
        return refilled.copy(
            hands = hands.withHand(HUMAN, refilled.hands[HUMAN] + card),
            stock = refilled.stock.dropLast(1),
            drawn = true,
            note = "",
        )
    }

    fun drawFromOpen(): RummyGame {
        if (over || turn != HUMAN || drawn || open.isEmpty()) return this
        val card = open.last()
        return copy(
            hands = hands.withHand(HUMAN, hands[HUMAN] + card),
            open = open.dropLast(1),
            drawn = true,
            note = "",
        )
    }

    /** Throws the card at [index] on the open pile; the phone players play until your turn comes back. */
    fun discard(
        index: Int,
        random: Random,
    ): RummyGame {
        if (over || turn != HUMAN || !drawn || index !in hands[HUMAN].indices) return this
        val card = hands[HUMAN][index]
        val next =
            copy(
                hands = hands.withHand(HUMAN, hands[HUMAN].filterIndexed { i, _ -> i != index }),
                open = open + card,
                drawn = false,
                turn = 1,
                note = "",
            )
        return next.runPhone(random)
    }

    /** Whether discarding [index] would leave a winning hand. */
    fun canDeclare(index: Int): Boolean =
        drawn && index in hands[HUMAN].indices && RummyMelds.declarable(hands[HUMAN], index, wild)

    /** Declares with the card at [index] as the finishing discard. A wrong declaration costs 80 points. */
    fun declare(index: Int): RummyGame {
        if (over || turn != HUMAN || !drawn || index !in hands[HUMAN].indices) return this
        return if (canDeclare(index)) finish(HUMAN, index) else wrongDeclaration(HUMAN, index)
    }

    private fun wrongDeclaration(
        player: Int,
        index: Int,
    ): RummyGame {
        val scores = points.toMutableList().also { it[player] = PENALTY }
        val rest = hands[player].filterIndexed { i, _ -> i != index }
        return copy(
            hands = hands.withHand(player, rest),
            over = true,
            winner = null,
            points = scores,
            note = "${names[player]} declared wrongly: $PENALTY points",
        )
    }

    private fun finish(
        player: Int,
        index: Int,
    ): RummyGame {
        val finalHands = hands.withHand(player, hands[player].filterIndexed { i, _ -> i != index })
        val scores = finalHands.mapIndexed { p, hand -> if (p == player) 0 else RummyMelds.penalty(hand, wild) }
        return copy(
            hands = finalHands,
            over = true,
            winner = player,
            points = scores,
            note = "${names[player]} declared and wins",
        )
    }

    private fun runPhone(random: Random): RummyGame {
        var game = this
        var guard = 0
        while (!game.over && game.turn != HUMAN && guard++ < MAX_TURNS) game = game.phoneTurn(random)
        return game
    }

    private fun phoneTurn(random: Random): RummyGame {
        val p = turn
        val refilled = refill(random)
        val mine = refilled.hands[p]
        val before = RummyMelds.analyse(mine, wild).dead((1 shl mine.size) - 1)
        val openTop = refilled.open.lastOrNull()
        val takeOpen =
            openTop != null &&
                RummyMelds.analyse(mine + openTop, wild).let { a -> a.bestDiscard().second < before }
        val drawnCard = if (takeOpen) refilled.open.last() else refilled.stock.last()
        val stock = if (takeOpen) refilled.stock else refilled.stock.dropLast(1)
        val open = if (takeOpen) refilled.open.dropLast(1) else refilled.open
        val hand = mine + drawnCard
        val analysis = RummyMelds.analyse(hand, wild)
        val winning = hand.indices.firstOrNull { analysis.declarable(it) }
        val after = refilled.copy(stock = stock, open = open)
        if (winning != null) return after.copy(hands = after.hands.withHand(p, hand)).finish(p, winning)
        val (discardIndex, _) = analysis.bestDiscard()
        val thrown = hand[discardIndex]
        val left = hand.filterIndexed { i, _ -> i != discardIndex }
        return after.copy(
            hands = after.hands.withHand(p, left),
            open = after.open + thrown,
            turn = (p + 1) % hands.size,
            note = if (takeOpen) "${names[p]} took ${describe(drawnCard)}" else "",
        )
    }

    private fun describe(card: PlayCard): String = if (card.joker) "a joker" else card.label

    private fun refill(random: Random): RummyGame =
        if (stock.isNotEmpty() ||
            open.size < 2
        ) {
            this
        } else {
            copy(stock = open.dropLast(1).shuffled(random), open = listOf(open.last()))
        }

    private fun List<List<PlayCard>>.withHand(
        player: Int,
        hand: List<PlayCard>,
    ): List<List<PlayCard>> = mapIndexed { i, h -> if (i == player) hand else h }

    companion object {
        const val HUMAN = 0
        const val PLAYERS = 3
        const val HAND = 13
        const val PENALTY = 80
        private const val MAX_TURNS = 20

        fun deal(
            random: Random,
            names: List<String>,
        ): RummyGame {
            val deck = (IndianCards.deck(random, decks = 2) + jokerCard() + jokerCard()).shuffled(random)
            val hands = List(PLAYERS) { p -> deck.subList(p * HAND, (p + 1) * HAND).toList() }
            val rest = deck.drop(PLAYERS * HAND)
            val first = rest.last()
            val wild = if (first.joker) random.nextInt(1, 14) else first.rank
            val pool = rest.dropLast(1)
            return RummyGame(
                names = names,
                hands = listOf(IndianCards.sorted(hands[0])) + hands.drop(1),
                stock = pool.dropLast(1),
                open = listOf(pool.last()),
                wild = wild,
            )
        }
    }
}

/** Meld checks and a small search over how a hand splits into groups. */
internal object RummyMelds {
    private fun wildCard(
        card: PlayCard,
        wild: Int,
    ): Boolean = card.joker || card.rank == wild

    /** Three or four cards of one rank in different suits, with wild cards standing in. */
    fun isSet(
        cards: List<PlayCard>,
        wild: Int,
    ): Boolean {
        if (cards.size !in 3..4) return false
        val natural = cards.filterNot { wildCard(it, wild) }
        if (natural.isEmpty()) return false
        return natural.map { it.rank }.distinct().size == 1 && natural.map { it.suit }.distinct().size == natural.size
    }

    /** Three or more cards in order in one suit. A pure sequence has no joker and no stand-in. */
    fun isSequence(
        cards: List<PlayCard>,
        wild: Int,
        pure: Boolean,
    ): Boolean {
        if (cards.size < 3) return false
        val natural =
            if (pure) {
                if (cards.any { it.joker }) return false
                cards
            } else {
                cards.filterNot { wildCard(it, wild) }
            }
        if (natural.isEmpty() || natural.map { it.suit }.distinct().size != 1) return false
        val ranks = natural.map { it.rank }
        if (ranks.distinct().size != ranks.size) return false
        return listOf(false, true).any { aceHigh ->
            val rs = if (aceHigh) ranks.map { if (it == 1) 14 else it } else ranks
            val lo = rs.min()
            val hi = rs.max()
            hi - lo + 1 <= cards.size && maxOf(1, hi - cards.size + 1) <= minOf(lo, 15 - cards.size)
        }
    }

    fun declarable(
        hand: List<PlayCard>,
        discardIndex: Int,
        wild: Int,
    ): Boolean = analyse(hand, wild).declarable(discardIndex)

    /** The hand with its best melds together (sequences first), and where to leave a gap after a meld. */
    fun arrange(
        hand: List<PlayCard>,
        wild: Int,
    ): Pair<List<PlayCard>, Set<Int>> {
        val a = analyse(hand, wild)
        val full = (1 shl hand.size) - 1
        val groups = a.partition(full)
        val sorted =
            groups.sortedBy { g ->
                if (isSequence(a.cardsOf(g), wild, pure = true)) {
                    0
                } else if (isSequence(a.cardsOf(g), wild, pure = false)) {
                    1
                } else {
                    2
                }
            }
        val used = groups.fold(0) { acc, g -> acc or g }
        val left = IndianCards.sorted(a.cardsOf(full and used.inv()))
        val out = mutableListOf<PlayCard>()
        val gaps = mutableSetOf<Int>()
        for (g in sorted) {
            out += a.cardsOf(g)
            gaps += out.size - 1
        }
        out += left
        gaps.remove(out.size - 1)
        return out to gaps
    }

    /** Penalty points for a hand when someone else declares: 80 without a pure and a second sequence. */
    fun penalty(
        hand: List<PlayCard>,
        wild: Int,
    ): Int {
        val a = analyse(hand, wild)
        val best = a.settle((1 shl hand.size) - 1, pure = false, seqs = 0)
        return if (best >= INF) 80 else minOf(80, best)
    }

    fun analyse(
        hand: List<PlayCard>,
        wild: Int,
    ): Analysis = Analysis(hand, wild)

    private const val INF = 1_000_000

    /** Which subsets of a hand are melds, and the cheapest leftover for each. */
    class Analysis(
        private val hand: List<PlayCard>,
        private val wild: Int,
    ) {
        private val n = hand.size
        private val kind = ByteArray(1 shl n)
        private val groupsAt = List(n) { mutableListOf<Int>() }
        private val deadMemo = IntArray(1 shl n) { -1 }
        private val settleMemo = IntArray(1 shl (n + 3)) { -2 }
        private val cost = IntArray(n) { if (hand[it].joker) 0 else IndianCards.points(hand[it]) }

        init {
            for (mask in 1 until (1 shl n)) {
                if (Integer.bitCount(mask) < 3) continue
                val cards = (0 until n).filter { mask shr it and 1 == 1 }.map { hand[it] }
                val k =
                    when {
                        isSequence(cards, wild, pure = true) -> PURE
                        isSequence(cards, wild, pure = false) -> SEQ
                        isSet(cards, wild) -> SET
                        else -> 0
                    }
                if (k != 0) {
                    kind[mask] = k.toByte()
                    groupsAt[Integer.numberOfTrailingZeros(mask)].add(mask)
                }
            }
        }

        /** Fewest points left over when [mask] is split into any melds. */
        fun dead(mask: Int): Int {
            if (mask == 0) return 0
            if (deadMemo[mask] >= 0) return deadMemo[mask]
            val low = Integer.numberOfTrailingZeros(mask)
            var best = cost[low] + dead(mask and (1 shl low).inv())
            for (g in groupsAt[low]) if (g and mask == g) best = minOf(best, dead(mask and g.inv()))
            deadMemo[mask] = best
            return best
        }

        /** The card to throw (index) and the points left over after it, for a hand of 14. */
        fun bestDiscard(): Pair<Int, Int> {
            val full = (1 shl n) - 1
            var bestIndex = 0
            var best = Int.MAX_VALUE
            for (i in 0 until n) {
                if (hand[i].joker) continue
                val d = dead(full and (1 shl i).inv()) * 100 - cost[i]
                if (d < best) {
                    best = d
                    bestIndex = i
                }
            }
            return bestIndex to dead(full and (1 shl bestIndex).inv())
        }

        /** The melds of the cheapest split of [mask]: each is a mask of card indexes; the rest are left over. */
        fun partition(mask: Int): List<Int> {
            val out = mutableListOf<Int>()
            var m = mask
            while (m != 0) {
                val low = Integer.numberOfTrailingZeros(m)
                val target = dead(m)
                val g = groupsAt[low].firstOrNull { it and m == it && dead(m and it.inv()) == target }
                if (g != null) {
                    out += g
                    m = m and g.inv()
                } else {
                    m = m and (1 shl low).inv()
                }
            }
            return out
        }

        fun cardsOf(mask: Int): List<PlayCard> = (0 until n).filter { mask shr it and 1 == 1 }.map { hand[it] }

        fun declarable(discard: Int): Boolean {
            val mask = ((1 shl n) - 1) and (1 shl discard).inv()
            return settle(mask, pure = false, seqs = 0) == 0
        }

        /** Fewest leftover points with at least one pure and two sequences in all, or [INF]. */
        fun settle(
            mask: Int,
            pure: Boolean,
            seqs: Int,
        ): Int {
            val key = mask or ((if (pure) 1 else 0) shl n) or (seqs.coerceAtMost(2) shl (n + 1))
            if (settleMemo[key] != -2) return settleMemo[key]
            val result: Int
            if (mask == 0) {
                result = if (pure && seqs >= 2) 0 else INF
            } else {
                val low = Integer.numberOfTrailingZeros(mask)
                var best = INF
                val skip = settle(mask and (1 shl low).inv(), pure, seqs)
                if (skip < INF) best = cost[low] + skip
                for (g in groupsAt[low]) {
                    if (g and mask != g) continue
                    val k = kind[g].toInt()
                    val rest =
                        settle(
                            mask and g.inv(),
                            pure || k == PURE,
                            if (k == PURE ||
                                k == SEQ
                            ) {
                                seqs + 1
                            } else {
                                seqs
                            },
                        )
                    best = minOf(best, rest)
                }
                result = best
            }
            settleMemo[key] = result
            return result
        }

        private companion object {
            const val PURE = 3
            const val SEQ = 2
            const val SET = 1
        }
    }
}
