package org.sableos.games

import kotlin.random.Random

internal enum class TrickVariant(
    val players: Int,
    val rounds: Int,
) {
    Callbreak(4, 5),
    Judgement(4, 7),
    TwoThreeFive(3, 6),
    Mendicot(4, 1),
}

internal enum class TrickPhase { Bid, PickTrump, Play, RoundOver, GameOver }

/**
 * The four trick-taking games share one engine: Callbreak (bid, spades are trump), Judgement
 * (bid exactly, the trump suit changes every round), 2-3-5 (three players with targets of 5, 3 and
 * 2 tricks) and Mendicot (partners, hidden trump, collect the tens). You are player 0; the phone
 * players act one move per [step] so the screen can show each card.
 */
internal data class TrickGame(
    val variant: TrickVariant,
    val names: List<String>,
    val hands: List<List<PlayCard>>,
    val extra: List<List<PlayCard>>,
    val phase: TrickPhase,
    val trump: Int?,
    val hiddenTrump: Int?,
    val trick: List<Pair<Int, PlayCard>>,
    val turn: Int,
    val tricks: List<Int>,
    val tens: List<Int>,
    val bids: List<Int>,
    val scores: List<Double>,
    val round: Int,
    val dealer: Int,
    val note: String = "",
    val summary: String = "",
    val dealt: Int = 0,
    /** The trick collected last, with who played each card, so it stays on view after it is won. */
    val lastTrick: List<Pair<Int, PlayCard>> = emptyList(),
) {
    private val n: Int get() = variant.players

    val roundOver: Boolean get() = phase == TrickPhase.RoundOver || phase == TrickPhase.GameOver
    val gameOver: Boolean get() = phase == TrickPhase.GameOver

    /** Cards each player holds this round (Judgement varies it). */
    val handSize: Int get() = dealt

    /** Your 2-3-5 target, or the bid you have made. */
    fun target(player: Int): Int =
        if (variant ==
            TrickVariant.TwoThreeFive
        ) {
            TARGETS[(player + round) % 3]
        } else {
            bids[player]
        }

    val needsStep: Boolean
        get() =
            when (phase) {
                TrickPhase.Bid, TrickPhase.PickTrump -> turn != HUMAN
                TrickPhase.Play -> trick.size == n || turn != HUMAN
                else -> false
            }

    /** Cards you may play now. */
    fun legal(player: Int = HUMAN): List<PlayCard> {
        val hand = hands[player]
        if (trick.isEmpty()) return hand
        val led = trick.first().second.suit
        val follow = hand.filter { it.suit == led }
        if (variant != TrickVariant.Callbreak) return follow.ifEmpty { hand }
        val spades = trick.map { it.second }.filter { it.suit == IndianCards.SPADES }
        val topSpade = spades.maxOfOrNull { IndianCards.high(it) } ?: 0
        if (follow.isNotEmpty()) {
            val level =
                if (led ==
                    IndianCards.SPADES
                ) {
                    topSpade
                } else {
                    trick.map { it.second }.filter { it.suit == led }.maxOf { IndianCards.high(it) }
                }
            if (led != IndianCards.SPADES && spades.isNotEmpty()) return follow
            return follow.filter { IndianCards.high(it) > level }.ifEmpty { follow }
        }
        val mySpades = hand.filter { it.suit == IndianCards.SPADES }
        if (mySpades.isEmpty()) return hand
        return mySpades.filter { IndianCards.high(it) > topSpade }.ifEmpty { mySpades }
    }

    /** The lowest and highest bid you may place right now. */
    fun bidRange(): IntRange =
        when (variant) {
            TrickVariant.Callbreak -> 1..8
            TrickVariant.Judgement -> 0..handSize
            else -> 0..0
        }

    /** Judgement: the last bidder may not make the bids add up to the number of cards. */
    fun bidAllowed(
        player: Int,
        value: Int,
    ): Boolean {
        if (value !in bidRange()) return false
        if (variant != TrickVariant.Judgement) return true
        val others = bids.filterIndexed { i, b -> i != player && b >= 0 }
        return !(others.size == n - 1 && others.sum() + value == handSize)
    }

    fun bid(value: Int): TrickGame {
        if (phase != TrickPhase.Bid || turn != HUMAN || !bidAllowed(HUMAN, value)) return this
        return placeBid(HUMAN, value)
    }

    private fun placeBid(
        player: Int,
        value: Int,
    ): TrickGame {
        val nextBids = bids.toMutableList().also { it[player] = value }
        val done = nextBids.all { it >= 0 }
        return copy(
            bids = nextBids,
            turn = if (done) (dealer + 1) % n else (player + 1) % n,
            phase = if (done) TrickPhase.Play else TrickPhase.Bid,
            note = "${names[player]} bid $value",
        )
    }

    fun pickTrump(suit: Int): TrickGame {
        if (phase != TrickPhase.PickTrump || turn != HUMAN) return this
        return chooseTrump(suit)
    }

    private fun chooseTrump(suit: Int): TrickGame {
        val full = hands.mapIndexed { i, h -> IndianCards.sorted(h + extra[i]) }
        return copy(
            hands = full,
            extra =
                List(n) {
                    emptyList()
                },
            trump = suit,
            phase = TrickPhase.Play,
            turn = trumper(),
            note = "${names[trumper()]} picks ${IndianCards.suitName(
                suit,
            )}",
        )
    }

    private fun trumper(): Int = (0 until n).first { TARGETS[(it + round) % 3] == 5 }

    /** Plays one of your cards. */
    fun play(card: PlayCard): TrickGame {
        if (phase != TrickPhase.Play || turn != HUMAN || trick.size == n || card !in legal(HUMAN)) return this
        return playCard(HUMAN, card)
    }

    private fun playCard(
        player: Int,
        card: PlayCard,
    ): TrickGame {
        val idx = hands[player].indexOf(card)
        val nextHands = hands.mapIndexed { i, h -> if (i == player) h.filterIndexed { j, _ -> j != idx } else h }
        val next = copy(hands = nextHands, trick = trick + (player to card), turn = (player + 1) % n, note = "")
        return if (next.trick.size == n) next.copy(turn = player) else next.revealIfNeeded()
    }

    /** Mendicot: the hidden trump is shown the first time a player cannot follow suit. */
    private fun revealIfNeeded(): TrickGame {
        if (variant != TrickVariant.Mendicot || trump != null || trick.isEmpty()) return this
        val led = trick.first().second.suit
        if (hands[turn].any { it.suit == led }) return this
        return copy(
            trump = hiddenTrump,
            note = "${names[turn]} cannot follow: trump is ${IndianCards.suitName(hiddenTrump!!)}",
        )
    }

    fun winnerOf(cards: List<Pair<Int, PlayCard>>): Int {
        val led = cards.first().second.suit
        val trumps = if (trump == null) emptyList() else cards.filter { it.second.suit == trump }
        val pool = trumps.ifEmpty { cards.filter { it.second.suit == led } }
        return pool.maxBy { IndianCards.high(it.second) }.first
    }

    /** One phone move: a bid, a trump pick, a card, or collecting a full trick. */
    fun step(random: Random): TrickGame {
        if (!needsStep) return this
        return when (phase) {
            TrickPhase.Bid -> placeBid(turn, botBid(turn))
            TrickPhase.PickTrump -> chooseTrump(botTrump(turn))
            TrickPhase.Play -> if (trick.size == n) collect() else playCard(turn, botCard(turn, random))
            else -> this
        }
    }

    private fun collect(): TrickGame {
        val winner = winnerOf(trick)
        val wonTens = trick.count { it.second.rank == 10 }
        val nextTricks = tricks.toMutableList().also { it[winner]++ }
        val nextTens = tens.toMutableList().also { it[winner] += wonTens }
        val next =
            copy(
                trick = emptyList(),
                lastTrick = trick,
                tricks = nextTricks,
                tens = nextTens,
                turn = winner,
                note = "${names[winner]} wins the trick",
            )
        return if (hands.all { it.isEmpty() }) next.scoreRound() else next
    }

    private fun scoreRound(): TrickGame {
        val delta = DoubleArray(n)
        var text = ""
        when (variant) {
            TrickVariant.Callbreak -> {
                for (p in 0 until n) {
                    delta[p] = if (tricks[p] >= bids[p]) bids[p] + (tricks[p] - bids[p]) / 10.0 else -bids[p].toDouble()
                }
            }

            TrickVariant.Judgement -> {
                for (p in 0 until n) delta[p] = if (tricks[p] == bids[p]) 10.0 + bids[p] else 0.0
            }

            TrickVariant.TwoThreeFive -> {
                for (p in 0 until n) delta[p] = (tricks[p] - target(p)).toDouble()
            }

            TrickVariant.Mendicot -> {
                val a = tens[0] + tens[2]
                val b = tens[1] + tens[3]
                val ta = tricks[0] + tricks[2]
                val tb = tricks[1] + tricks[3]
                val teamA = if (a != b) a > b else ta > tb
                val points =
                    when {
                        maxOf(ta, tb) == 13 -> 3.0
                        maxOf(a, b) == 4 -> 2.0
                        else -> 1.0
                    }
                text = (if (teamA) "${names[0]} and ${names[2]} win" else "${names[1]} and ${names[3]} win") +
                    " · tens $a-$b · tricks $ta-$tb" +
                    (
                        if (points >= 3.0) {
                            " · whitewash"
                        } else if (points >= 2.0) {
                            " · Mendicot!"
                        } else {
                            ""
                        }
                    )
                for (p in 0 until n) delta[p] = if ((p % 2 == 0) == teamA) points else 0.0
            }
        }
        if (text.isEmpty()) {
            text =
                (0 until n).joinToString(" · ") {
                    "${names[it]} ${tricks[it]}${if (variant == TrickVariant.Judgement || variant == TrickVariant.Callbreak) "/${bids[it]}" else ""}"
                }
        }
        val total = scores.mapIndexed { p, s -> s + delta[p] }
        val last = round + 1 >= variant.rounds
        return copy(scores = total, phase = if (last) TrickPhase.GameOver else TrickPhase.RoundOver, summary = text)
    }

    fun nextRound(random: Random): TrickGame =
        if (phase ==
            TrickPhase.RoundOver
        ) {
            deal(variant, names, random, round + 1, scores)
        } else {
            this
        }

    // ---------------------------------------------------------------- the phone players

    private fun botBid(player: Int): Int {
        val hand = hands[player]
        val raw =
            when (variant) {
                TrickVariant.Callbreak -> {
                    val spades = hand.filter { it.suit == IndianCards.SPADES }
                    spades.count { IndianCards.high(it) >= 12 } + maxOf(0, spades.size - 3) * 0.6 +
                        hand.filter { it.suit != IndianCards.SPADES }.count { IndianCards.high(it) == 14 } +
                        hand.filter { it.suit != IndianCards.SPADES }.count { IndianCards.high(it) == 13 } * 0.5
                }

                else -> {
                    val t = trump
                    hand.sumOf {
                        val h = IndianCards.high(it)
                        when {
                            h == 14 -> 0.9
                            t != null && it.suit == t && h >= 11 -> 0.8
                            t != null && it.suit == t -> 0.3
                            h == 13 -> 0.4
                            else -> 0.0
                        }
                    }
                }
            }
        val range = bidRange()
        var value = Math.round(raw).toInt().coerceIn(range.first, range.last)
        if (!bidAllowed(player, value)) value = range.firstOrNull { bidAllowed(player, it) } ?: value
        return value
    }

    private fun botTrump(player: Int): Int =
        (0 until IndianCards.SUITS).maxBy { s ->
            hands[player].filter { it.suit == s }.sumOf { 10 + IndianCards.high(it) }
        }

    private fun botCard(
        player: Int,
        random: Random,
    ): PlayCard {
        val legal = legal(player)

        fun power(c: PlayCard) = IndianCards.high(c) + if (trump != null && c.suit == trump) 20 else 0
        if (trick.isEmpty()) {
            val best = legal.maxBy { power(it) }
            return if (IndianCards.high(best) >= 13 &&
                (trump == null || best.suit != trump || random.nextInt(3) == 0)
            ) {
                best
            } else {
                legal.minBy { power(it) }
            }
        }
        val current = winnerOf(trick)
        val partnerWinning = variant == TrickVariant.Mendicot && current % 2 == player % 2 && current != player
        val winners = legal.filter { winnerOf(trick + (player to it)) == player }
        return when {
            winners.isNotEmpty() && !partnerWinning -> winners.minBy { power(it) }
            partnerWinning && trick.size == n - 1 -> legal.maxBy { IndianCards.points(it) }
            else -> legal.minBy { power(it) }
        }
    }

    companion object {
        const val HUMAN = 0
        private val TARGETS = listOf(5, 3, 2)
        private val JUDGEMENT_CARDS = listOf(7, 6, 5, 4, 3, 2, 1)
        private val JUDGEMENT_TRUMPS =
            listOf(
                IndianCards.SPADES,
                IndianCards.HEARTS,
                IndianCards.DIAMONDS,
                IndianCards.CLUBS,
                -1,
                IndianCards.SPADES,
                IndianCards.HEARTS,
            )

        fun judgementTrump(round: Int): Int? = JUDGEMENT_TRUMPS[round].takeIf { it >= 0 }

        fun newGame(
            variant: TrickVariant,
            names: List<String>,
            random: Random,
        ): TrickGame = deal(variant, names, random, 0, List(variant.players) { 0.0 })

        fun deal(
            variant: TrickVariant,
            names: List<String>,
            random: Random,
            round: Int,
            scores: List<Double>,
        ): TrickGame {
            val n = variant.players
            val dealer = round % n
            val base =
                TrickGame(
                    variant = variant,
                    names = names,
                    hands = List(n) { emptyList() },
                    extra = List(n) { emptyList() },
                    phase = TrickPhase.Play,
                    trump = null,
                    hiddenTrump = null,
                    trick = emptyList(),
                    turn = (dealer + 1) % n,
                    tricks = List(n) { 0 },
                    tens = List(n) { 0 },
                    bids = List(n) { -1 },
                    scores = scores,
                    round = round,
                    dealer = dealer,
                )
            val game =
                when (variant) {
                    TrickVariant.Callbreak -> {
                        val deck = IndianCards.deck(random)
                        base.copy(
                            hands = List(n) { IndianCards.sorted(deck.subList(it * 13, it * 13 + 13)) },
                            phase = TrickPhase.Bid,
                            trump = IndianCards.SPADES,
                        )
                    }

                    TrickVariant.Judgement -> {
                        val deck = IndianCards.deck(random)
                        val size = JUDGEMENT_CARDS[round % JUDGEMENT_CARDS.size]
                        base.copy(
                            hands = List(n) { IndianCards.sorted(deck.subList(it * size, it * size + size)) },
                            phase = TrickPhase.Bid,
                            trump = judgementTrump(round % JUDGEMENT_TRUMPS.size),
                        )
                    }

                    TrickVariant.TwoThreeFive -> {
                        val deck = IndianCards.deck(random).filter { it.rank == 1 || it.rank >= 7 }
                        val first = List(n) { IndianCards.sorted(deck.subList(it * 5, it * 5 + 5)) }
                        val second = List(n) { deck.subList(15 + it * 5, 15 + it * 5 + 5).toList() }
                        val picker = (0 until n).first { TARGETS[(it + round) % 3] == 5 }
                        base.copy(hands = first, extra = second, phase = TrickPhase.PickTrump, turn = picker)
                    }

                    TrickVariant.Mendicot -> {
                        val deck = IndianCards.deck(random)
                        base.copy(
                            hands = List(n) { IndianCards.sorted(deck.subList(it * 13, it * 13 + 13)) },
                            hiddenTrump = random.nextInt(IndianCards.SUITS),
                        )
                    }
                }
            val bidder = (dealer + 1) % n
            val sized = game.copy(dealt = game.hands[0].size + game.extra[0].size)
            return if (sized.phase == TrickPhase.Bid) sized.copy(turn = bidder) else sized
        }
    }
}
