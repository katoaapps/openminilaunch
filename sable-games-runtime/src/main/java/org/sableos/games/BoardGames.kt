package org.sableos.games

import kotlin.random.Random

/**
 * One Snakes & Ladders move, for the screen to replay: [player] rolled [dice] from square [from], walked
 * to [land] and ended on [to] (a ladder or snake moves it from [land] to [to]). [overshot] when the
 * roll was too high to finish, so the token stayed.
 */
internal data class SnakesMove(
    val player: Int,
    val dice: Int,
    val from: Int,
    val land: Int,
    val to: Int,
    val overshot: Boolean,
) {
    /** The squares the token steps on, one at a time, before any ladder or snake. */
    val walk: List<Int> get() = if (land > from) (from + 1..land).toList() else emptyList()
}

/** Snakes & Ladders on a 10 x 10 board, two to four players; you are player 0. Roll a six and roll again. */
internal data class SnakesGame(
    val names: List<String>,
    val pos: List<Int>,
    val turn: Int = 0,
    val roll: Int = 0,
    val winner: Int? = null,
    val note: String = "",
    val last: SnakesMove? = null,
) {
    val over: Boolean get() = winner != null
    val needsStep: Boolean get() = !over && turn != HUMAN

    /** The last roll was a six, so the same player rolls again. */
    val rollsAgain: Boolean get() = !over && last != null && last.dice == 6 && last.player == turn

    /** The players from first to last: the winner, then the others by how far they got. */
    val standings: List<Int>
        get() = pos.indices.sortedWith(compareByDescending<Int> { it == winner }.thenByDescending { pos[it] })

    /** One roll for the player whose turn it is. */
    fun roll(random: Random): SnakesGame = move(random.nextInt(1, 7))

    fun move(dice: Int): SnakesGame {
        if (over) return this
        val p = turn
        val target = pos[p] + dice
        val from = pos[p]
        val overshot = target > LAST
        val land = if (overshot) from else target
        val jump = if (overshot) null else JUMPS[land]
        val end = jump ?: land
        val note =
            when {
                overshot -> "${names[p]} rolled $dice but needed exactly ${LAST - from}"
                jump != null && jump > land -> "${names[p]} rolled $dice and climbed a ladder: $land to $jump"
                jump != null -> "${names[p]} rolled $dice and slid down a snake: $land to $jump"
                end == LAST -> "${names[p]} rolled $dice, reached $LAST and won!"
                else -> "${names[p]} rolled $dice: $from to $end"
            }
        val next = pos.mapIndexed { i, v -> if (i == p) end else v }
        val won = end == LAST
        val again = dice == 6 && !won
        return copy(
            pos = next,
            roll = dice,
            turn = if (again) p else (p + 1) % pos.size,
            winner = if (won) p else null,
            note = note,
            last = SnakesMove(p, dice, from, land, end, overshot),
        )
    }

    companion object {
        const val HUMAN = 0
        const val SIDE = 10
        const val LAST = SIDE * SIDE

        /** Eight ladders and eight snakes on the classic 100-square board (owner, 2026-11-14). */
        val LADDERS = mapOf(3 to 22, 8 to 30, 28 to 55, 36 to 44, 51 to 67, 58 to 77, 71 to 92, 80 to 99)
        val SNAKES = mapOf(97 to 78, 95 to 56, 88 to 24, 74 to 53, 62 to 18, 48 to 26, 32 to 10, 17 to 4)
        private val JUMPS = LADDERS + SNAKES

        fun start(names: List<String>): SnakesGame = SnakesGame(names, List(names.size) { 0 })
    }
}

/**
 * One Ludo move, for the screen to replay: [player]'s [token] went from [from] to [to] (progress, as in
 * [LudoGame.tokens]), and each of [captured] (colour, token, progress) went back to its yard.
 */
internal data class LudoMove(
    val player: Int,
    val token: Int,
    val from: Int,
    val to: Int,
    val captured: List<Triple<Int, Int, Int>> = emptyList(),
) {
    /** The progress steps the token takes, one at a time: out of the yard is one step to its start. */
    val walk: List<Int> get() = if (from < 0) listOf(0) else (from + 1..to).toList()
}

/**
 * Ludo for four colours (you are red, player 0). Roll a six to bring a token out; a six, a capture or
 * a token reaching home gives another roll. Three sixes in a row lose the turn. A token goes round the
 * 52-square track and up its own home column; it needs the exact roll to reach home. Landing on another
 * colour's token sends it back to its yard, except on a start square or a star.
 */
internal data class LudoGame(
    val names: List<String>,
    /** For each colour, the four tokens' progress: -1 in the yard, 0-50 on the track, 51-55 in the home column, 56 home. */
    val tokens: List<List<Int>>,
    val turn: Int = 0,
    val roll: Int? = null,
    val sixes: Int = 0,
    val winner: Int? = null,
    val note: String = "",
    val last: LudoMove? = null,
    /** The player who just lost the turn to three sixes, until the next roll. */
    val threeSixes: Int? = null,
    /** The number the last roll showed, kept after a roll that could not move. */
    val rolled: Int? = null,
) {
    val over: Boolean get() = winner != null
    val needsStep: Boolean get() = !over && turn != HUMAN

    /** How many of [player]'s tokens are home. */
    fun homeCount(player: Int): Int = tokens[player].count { it == HOME }

    /** Where [player]'s [token] would land with the current roll, or null if it cannot move. */
    fun landing(
        token: Int,
        player: Int = turn,
    ): Int? {
        val r = roll ?: return null
        if (token !in movable(player)) return null
        val from = tokens[player][token]
        return if (from < 0) 0 else from + r
    }

    /** Tokens of [player] that can move with the current roll. */
    fun movable(player: Int = turn): List<Int> {
        val r = roll ?: return emptyList()
        return tokens[player].indices.filter { t ->
            val p = tokens[player][t]
            if (p < 0) r == 6 else p + r <= HOME
        }
    }

    fun rollDice(random: Random): LudoGame {
        if (over || roll != null) return this
        val r = random.nextInt(1, 7)
        val six = if (r == 6) sixes + 1 else 0
        if (six >=
            3
        ) {
            return copy(
                roll = null,
                sixes = 0,
                turn = (turn + 1) % PLAYERS,
                note = "${names[turn]} rolled three sixes and lost the turn",
                threeSixes = turn,
                rolled = r,
            )
        }
        val withRoll = copy(roll = r, sixes = six, note = "${names[turn]} rolled $r", threeSixes = null, rolled = r)
        return if (withRoll.movable().isEmpty()) {
            withRoll.copy(
                roll = null,
                turn = (turn + 1) % PLAYERS,
                sixes = 0,
                note = "${names[turn]} rolled $r and could not move",
            )
        } else {
            withRoll
        }
    }

    /** Moves token [token] of the player whose turn it is. */
    fun move(token: Int): LudoGame {
        if (over || token !in movable()) return this
        val r = roll!!
        val p = turn
        val from = tokens[p][token]
        val to = if (from < 0) 0 else from + r
        var bumped =
            tokens.mapIndexed { c, list ->
                if (c ==
                    p
                ) {
                    list.mapIndexed { i, v -> if (i == token) to else v }
                } else {
                    list
                }
            }
        val sent = mutableListOf<Triple<Int, Int, Int>>()
        if (to in 0..LAST_TRACK && !isSafe(to, p)) {
            val square = square(p, to)
            bumped =
                bumped.mapIndexed { c, list ->
                    if (c == p) {
                        list
                    } else {
                        list.mapIndexed { t, v ->
                            if (v in 0..LAST_TRACK && square(c, v) == square) {
                                sent += Triple(c, t, v)
                                -1
                            } else {
                                v
                            }
                        }
                    }
                }
        }
        val captured = sent.isNotEmpty()
        val home = to == HOME
        val won = bumped[p].all { it == HOME }
        val again = (r == 6 || captured || home) && !won
        val text =
            when {
                won -> {
                    "${names[p]} won!"
                }

                captured -> {
                    val whose =
                        sent.map { it.first }.distinct().joinToString(" and ") {
                            if (it ==
                                HUMAN
                            ) {
                                "your"
                            } else {
                                "${names[it]}'s"
                            }
                        }
                    val tokens = if (sent.size == 1) "token" else "tokens"
                    "${names[p]} sent $whose $tokens back to the yard"
                }

                home -> {
                    "${names[p]} got a token home"
                }

                from < 0 -> {
                    "${names[p]} brought a token out"
                }

                else -> {
                    "${names[p]} moved a token $r"
                }
            }
        return copy(
            tokens = bumped,
            roll = null,
            turn = if (again) p else (p + 1) % PLAYERS,
            sixes = if (again && r == 6) sixes else 0,
            winner = if (won) p else null,
            note = text,
            last = LudoMove(p, token, from, to, sent),
        )
    }

    /** One phone move: roll, or choose a token. */
    fun step(random: Random): LudoGame {
        if (!needsStep) return this
        if (roll == null) return rollDice(random)
        val options = movable()
        val p = turn
        val r = roll
        val pick =
            options.maxBy { t ->
                val from = tokens[p][t]
                val to = if (from < 0) 0 else from + r
                var score = to
                if (from < 0) score += 60
                if (to == HOME) score += 80
                if (to in 0..LAST_TRACK && !isSafe(to, p) && captures(p, to)) score += 100
                if (from in 0..LAST_TRACK && endangered(p, from)) score += 25
                if (to in 0..LAST_TRACK && isSafe(to, p)) score += 15
                score
            }
        return move(pick)
    }

    private fun captures(
        player: Int,
        progress: Int,
    ): Boolean {
        val sq = square(player, progress)
        return tokens.withIndex().any { (c, list) ->
            c != player &&
                list.any { it in 0..LAST_TRACK && square(c, it) == sq }
        }
    }

    /** Another colour has a token a roll or two behind this one. */
    private fun endangered(
        player: Int,
        progress: Int,
    ): Boolean {
        if (isSafe(progress, player)) return false
        val sq = square(player, progress)
        return tokens.withIndex().any { (c, list) ->
            c != player && list.any { v -> v in 0..LAST_TRACK && (sq - square(c, v) + TRACK) % TRACK in 1..6 }
        }
    }

    companion object {
        const val HUMAN = 0
        const val PLAYERS = 4
        const val HOME = 56
        const val LAST_TRACK = 50
        const val TRACK = 52

        /** The track square (0-51) a token of [color] stands on at [progress] 0-50. */
        fun square(
            color: Int,
            progress: Int,
        ): Int = (color * 13 + progress) % TRACK

        /** Start squares and the star squares eight steps on cannot be captured on. */
        fun isSafe(
            progress: Int,
            color: Int,
        ): Boolean = progress in 0..LAST_TRACK && square(color, progress) % 13 in listOf(0, 8)

        fun start(names: List<String>): LudoGame = LudoGame(names, List(PLAYERS) { List(4) { -1 } })
    }
}
