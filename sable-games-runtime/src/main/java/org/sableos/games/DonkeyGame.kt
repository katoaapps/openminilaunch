package org.sableos.games

import kotlin.random.Random

/**
 * Donkey. Four players, sixteen cards: four ranks, four suits each. Everyone passes a card to the
 * player on their left at the same time, over and over, trying to hold four of a kind. The first to
 * get four grabs a spoon, and everyone else must grab too: with one spoon fewer than players, the one
 * left without gets a letter of D-O-N-K-E-Y. Grab without four of a kind and no one grabbing yet, and
 * you get a letter yourself. Spell DONKEY and you are the donkey. You are player 0.
 */
internal data class DonkeyGame(
    val names: List<String>,
    val hands: List<List<PlayCard>>,
    val letters: List<Int>,
    val grabbed: List<Int> = emptyList(),
    val round: Int = 1,
    val phase: Phase = Phase.Passing,
    val note: String = "",
) {
    enum class Phase { Passing, Grabbing, RoundDone, Over }

    private val n: Int get() = hands.size

    fun hasFour(player: Int): Boolean = hands[player].groupBy { it.rank }.any { it.value.size == 4 }

    val donkey: Int? get() = letters.indices.firstOrNull { letters[it] >= WORD.length }

    fun lettersOf(player: Int): String = WORD.take(letters[player])

    /** You pass the card at [index]; the phone players choose theirs; all pass at once. */
    fun pass(
        index: Int,
        random: Random,
    ): DonkeyGame {
        if (phase != Phase.Passing || index !in hands[HUMAN].indices) return this
        val given =
            List(n) { p ->
                if (p == HUMAN) index else botPass(hands[p], random)
            }
        val passed = List(n) { p -> hands[p][given[p]] }
        val next =
            List(n) { p ->
                val from = (p + n - 1) % n
                hands[p].filterIndexed { i, _ -> i != given[p] } + passed[from]
            }
        val moved = copy(hands = next.map { IndianCards.sorted(it) }, note = "")
        val holder = (0 until n).firstOrNull { moved.hasFour(it) }
        return if (holder ==
            null
        ) {
            moved
        } else {
            moved.copy(
                phase = Phase.Grabbing,
                grabbed = emptyList(),
                note =
                    if (holder ==
                        HUMAN
                    ) {
                        "You have four! Grab a spoon (Space)"
                    } else {
                        ""
                    },
            )
        }
    }

    private fun botPass(
        hand: List<PlayCard>,
        random: Random,
    ): Int {
        val counts = hand.groupBy { it.rank }.mapValues { it.value.size }
        val best = counts.maxByOrNull { it.value }!!.key
        val choices = hand.indices.filter { hand[it].rank != best }
        val pool = if (choices.isEmpty()) hand.indices.toList() else choices
        val minCount = pool.minOf { counts.getValue(hand[it].rank) }
        return pool.filter { counts.getValue(hand[it].rank) == minCount }.random(random)
    }

    /** [player] grabs a spoon. */
    fun grab(player: Int): DonkeyGame {
        if (phase != Phase.Grabbing || player in grabbed) return this
        val legit = hasFour(player) || grabbed.isNotEmpty()
        if (!legit) {
            val bumped = letters.toMutableList().also { it[player]++ }
            val over = bumped[player] >= WORD.length
            return copy(
                letters = bumped,
                phase = if (over) Phase.Over else phase,
                note = "${names[player]} grabbed too soon and gets a letter",
            )
        }
        val now = grabbed + player
        if (now.size < n - 1) return copy(grabbed = now, note = "${names[player]} grabs a spoon")
        val loser = (0 until n).first { it !in now }
        val bumped = letters.toMutableList().also { it[loser]++ }
        val over = bumped[loser] >= WORD.length
        return copy(
            grabbed = now,
            letters = bumped,
            phase = if (over) Phase.Over else Phase.RoundDone,
            note =
                if (over) {
                    "${names[loser]} is the DONKEY!"
                } else {
                    "${names[loser]} has no spoon: ${WORD.take(
                        bumped[loser],
                    )}"
                },
        )
    }

    /** How long each phone player takes to notice, in milliseconds, fastest first. */
    fun reactionTimes(random: Random): List<Pair<Int, Int>> =
        (0 until n)
            .filter { it != HUMAN }
            .map { it to (if (hasFour(it)) 500 + random.nextInt(500) else 1200 + random.nextInt(1400)) }
            .sortedBy { it.second }

    fun nextRound(random: Random): DonkeyGame =
        if (phase ==
            Phase.RoundDone
        ) {
            deal(random, names, letters, round + 1)
        } else {
            this
        }

    companion object {
        const val HUMAN = 0
        const val PLAYERS = 4
        const val WORD = "DONKEY"

        fun deal(
            random: Random,
            names: List<String>,
            letters: List<Int> = List(PLAYERS) { 0 },
            round: Int = 1,
        ): DonkeyGame {
            // Four ranks of four suits. Redeal in the rare case someone is dealt four of a kind.
            while (true) {
                val ranks = (1..IndianCards.KING).shuffled(random).take(PLAYERS)
                val deck = ranks.flatMap { r -> (0 until 4).map { PlayCard(r, it) } }.shuffled(random)
                val hands = List(PLAYERS) { IndianCards.sorted(deck.subList(it * 4, it * 4 + 4)) }
                val game = DonkeyGame(names, hands, letters, round = round)
                if ((0 until PLAYERS).none { game.hasFour(it) }) return game
            }
        }
    }
}
