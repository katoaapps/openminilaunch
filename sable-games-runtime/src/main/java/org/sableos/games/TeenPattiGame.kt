package org.sableos.games

import kotlin.random.Random

/**
 * Teen Patti (three-card brag) for practice chips only. The chips have no value outside the game:
 * they cannot be bought, sold or won as anything else. You are player 0, against three phone
 * players. Everyone puts in the boot; each turn a player packs (folds), plays chaal (matches the
 * stake) or raises (doubles it). A player who has not looked at their cards plays "blind" and pays
 * half. With two players left, either may ask for a show: the better hand wins the pot.
 */
internal data class TeenPattiGame(
    val names: List<String>,
    val hands: List<List<PlayCard>>,
    val chips: List<Int>,
    val seen: List<Boolean>,
    val packed: List<Boolean>,
    val pot: Int,
    val stake: Int,
    val turn: Int,
    val boot: Int,
    val note: String = "",
    /** The winner once the round is over, with how they won; shown hands are revealed. */
    val winner: Int? = null,
    val revealed: Boolean = false,
) {
    enum class Action { Chaal, Raise, Pack, Show }

    val over: Boolean get() = winner != null
    val active: List<Int> get() = hands.indices.filter { !packed[it] }

    fun cost(player: Int): Int = if (seen[player]) stake * 2 else stake

    fun raiseAllowed(player: Int): Boolean = stake * 2 <= boot * MAX_STAKE_BOOTS && chips[player] >= raiseCost(player)

    private fun raiseCost(player: Int): Int = if (seen[player]) stake * 4 else stake * 2

    fun canShow(player: Int): Boolean = active.size == 2 && player in active && chips[player] >= cost(player)

    fun canChaal(player: Int): Boolean = chips[player] >= cost(player)

    /** You look at your cards. */
    fun see(): TeenPattiGame =
        if (over ||
            seen[HUMAN]
        ) {
            this
        } else {
            copy(seen = seen.toMutableList().also { it[HUMAN] = true })
        }

    /** Your action, then the phone players act until it is your turn again or the round ends. */
    fun act(
        action: Action,
        random: Random,
    ): TeenPattiGame {
        if (over || turn != HUMAN) return this
        val next = apply(HUMAN, action) ?: return this
        return next.runPhone(random)
    }

    private fun runPhone(random: Random): TeenPattiGame {
        var game = this
        var guard = 0
        while (!game.over && game.turn != HUMAN && guard++ < MAX_STEPS) {
            val action = game.choose(game.turn, random)
            game = game.apply(game.turn, action) ?: game.apply(game.turn, Action.Pack)!!
        }
        if (!game.over && game.turn != HUMAN) {
            // The phone players kept raising each other: the best hand takes the pot.
            val best = game.active.maxByOrNull { handScore(game.hands[it]) }!!
            return game.finish(best, reveal = true)
        }
        return game
    }

    /** One player's choice, from the strength of the hand and how rich the stake has become. */
    private fun choose(
        player: Int,
        random: Random,
    ): Action {
        val strength = handStrength(hands[player])
        val ratio = stake / boot
        val bluff = random.nextInt(100) < 12
        return when {
            canShow(player) && (strength >= 3 || bluff) -> Action.Show
            canShow(player) && strength == 0 && ratio >= 4 -> Action.Pack
            !canChaal(player) -> Action.Pack
            strength == 0 && ratio >= 2 && !bluff -> Action.Pack
            strength == 1 && ratio >= 8 && !bluff -> Action.Pack
            strength >= 3 && raiseAllowed(player) && random.nextInt(100) < 55 -> Action.Raise
            strength == 2 && raiseAllowed(player) && random.nextInt(100) < 20 -> Action.Raise
            else -> Action.Chaal
        }
    }

    private fun apply(
        player: Int,
        action: Action,
    ): TeenPattiGame? {
        if (over || player != turn || packed[player]) return null
        // A phone player who looks only when the stake grows is simplest: they look at once.
        val lookedAt = if (player != HUMAN) seen.toMutableList().also { it[player] = true } else seen
        val base = copy(seen = lookedAt)
        return when (action) {
            Action.Pack -> {
                base.pack(player)
            }

            Action.Chaal -> {
                if (base.canChaal(player)) base.pay(player, base.cost(player), base.stake) else null
            }

            Action.Raise -> {
                if (!base.raiseAllowed(player)) {
                    null
                } else {
                    val raised = base.copy(stake = base.stake * 2)
                    raised.pay(player, raised.cost(player), raised.stake)
                }
            }

            Action.Show -> {
                base.show(player)
            }
        }
    }

    private fun pay(
        player: Int,
        amount: Int,
        newStake: Int,
    ): TeenPattiGame {
        val nextChips = chips.toMutableList().also { it[player] -= amount }
        return copy(chips = nextChips, pot = pot + amount, stake = newStake, turn = nextActive(player), note = "")
    }

    private fun nextActive(from: Int): Int {
        var p = from
        repeat(hands.size) {
            p = (p + 1) % hands.size
            if (!packed[p]) return p
        }
        return from
    }

    private fun pack(player: Int): TeenPattiGame {
        val nextPacked = packed.toMutableList().also { it[player] = true }
        val left = hands.indices.filter { !nextPacked[it] }
        val moved = copy(packed = nextPacked, note = "${names[player]} packed")
        return if (left.size ==
            1
        ) {
            moved.finish(left.first(), reveal = false)
        } else {
            moved.copy(turn = moved.nextActive(player))
        }
    }

    private fun show(player: Int): TeenPattiGame {
        if (!canShow(player)) return this
        val paid = pay(player, cost(player), stake)
        val other = active.first { it != player }
        val mine = handScore(hands[player])
        val theirs = handScore(hands[other])
        // On a tie the player who asked for the show loses.
        val winner = if (mine > theirs) player else other
        return paid.finish(winner, reveal = true)
    }

    private fun finish(
        winner: Int,
        reveal: Boolean,
    ): TeenPattiGame {
        val nextChips = chips.toMutableList().also { it[winner] += pot }
        val how =
            if (reveal) {
                "${names[winner]} wins with ${handName(
                    hands[winner],
                )}"
            } else {
                "${names[winner]} wins the pot"
            }
        return copy(chips = nextChips, pot = 0, winner = winner, revealed = reveal, note = how)
    }

    companion object {
        const val HUMAN = 0
        const val PLAYERS = 4
        const val BOOT = 20
        const val START_CHIPS = 1000
        private const val MAX_STAKE_BOOTS = 32
        private const val MAX_STEPS = 200

        /** Deals a round. [chips] carries the balances over; a broke phone player gets a fresh stack. */
        fun deal(
            random: Random,
            names: List<String>,
            chips: List<Int>,
            boot: Int = BOOT,
        ): TeenPattiGame {
            val deck = IndianCards.deck(random)
            val stacks = chips.map { if (it < boot * 4) START_CHIPS else it }.toMutableList()
            val hands = List(PLAYERS) { deck.subList(it * 3, it * 3 + 3).toList() }
            for (p in 0 until PLAYERS) stacks[p] -= boot
            return TeenPattiGame(
                names = names,
                hands = hands,
                chips = stacks,
                seen = List(PLAYERS) { false },
                packed = List(PLAYERS) { false },
                pot = boot * PLAYERS,
                stake = boot,
                turn = random.nextInt(PLAYERS),
                boot = boot,
            ).let { if (it.turn == HUMAN) it else it.runPhone(random) }
        }

        /** A rank that sorts the hands: trail, pure sequence, sequence, colour, pair, high card. */
        fun handScore(hand: List<PlayCard>): Long {
            val ranks = hand.map { it.rank }.sorted()
            val flush = hand.map { it.suit }.distinct().size == 1
            val trail = ranks.distinct().size == 1
            val seqHigh = sequenceHigh(ranks)
            val high = hand.map { IndianCards.high(it) }.sortedDescending()
            val tie = high[0] * 10000L + high[1] * 100L + high[2]
            return when {
                trail -> {
                    6_000_000L + high[0]
                }

                seqHigh > 0 && flush -> {
                    5_000_000L + seqHigh
                }

                seqHigh > 0 -> {
                    4_000_000L + seqHigh
                }

                flush -> {
                    3_000_000L + tie
                }

                ranks.distinct().size == 2 -> {
                    val pair =
                        ranks
                            .groupBy { it }
                            .entries
                            .first { it.value.size == 2 }
                            .key
                    val kicker = ranks.first { it != pair }
                    2_000_000L + IndianCards.high(PlayCard(pair, 0)) * 100L + IndianCards.high(PlayCard(kicker, 0))
                }

                else -> {
                    1_000_000L + tie
                }
            }
        }

        /** 0 for no sequence; otherwise the sequence's order (A-K-Q is 15, A-2-3 is 14, then K-Q-J 13 down to 4-3-2 which is 4). */
        private fun sequenceHigh(sortedRanks: List<Int>): Int {
            val r = sortedRanks
            return when {
                r == listOf(1, 12, 13) -> 15
                r == listOf(1, 2, 3) -> 14
                r[1] == r[0] + 1 && r[2] == r[1] + 1 -> r[2]
                else -> 0
            }
        }

        /** 0 high card, 1 pair, 2 colour, 3 sequence or better. */
        fun handStrength(hand: List<PlayCard>): Int =
            when {
                handScore(hand) >= 4_000_000L -> 3
                handScore(hand) >= 3_000_000L -> 2
                handScore(hand) >= 2_000_000L -> 1
                else -> 0
            }

        fun handName(hand: List<PlayCard>): String {
            val score = handScore(hand)
            return when {
                score >= 6_000_000L -> "a trail"
                score >= 5_000_000L -> "a pure sequence"
                score >= 4_000_000L -> "a sequence"
                score >= 3_000_000L -> "a colour"
                score >= 2_000_000L -> "a pair"
                else -> "a high card"
            }
        }
    }
}
