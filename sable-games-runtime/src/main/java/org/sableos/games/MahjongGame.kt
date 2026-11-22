package org.sableos.games

import kotlin.random.Random

/** A tile spot: [layer] 0 at the bottom, then its [col] and [row] on the board grid. */
internal data class TileSpot(
    val layer: Int,
    val col: Int,
    val row: Int,
)

/**
 * Mahjong solitaire on a square two-layer pile of 72 tiles (18 faces, four of each). A tile is free
 * when nothing lies on it and its left or right side is open; two free tiles with the same face
 * are removed together. Every deal is built backwards from the full pile, so it can be cleared.
 * [tiles] maps each spot still on the board to its face (0-17).
 */
internal data class MahjongGame(
    val tiles: Map<TileSpot, Int>,
) {
    val won: Boolean get() = tiles.isEmpty()

    fun isFree(spot: TileSpot): Boolean = spot in tiles && isFree(spot, tiles.keys)

    /** The top tile at a board cell, if any: what the cursor points at. */
    fun topAt(
        col: Int,
        row: Int,
    ): TileSpot? = tiles.keys.filter { it.col == col && it.row == row }.maxByOrNull { it.layer }

    /** Removes [a] and [b] when both are free, different and the same face; else null. */
    fun match(
        a: TileSpot,
        b: TileSpot,
    ): MahjongGame? {
        val ok = a != b && isFree(a) && isFree(b) && tiles[a] == tiles[b]
        return if (ok) MahjongGame(tiles - a - b) else null
    }

    /** A pair that can be removed now, or null when the board is stuck. */
    fun hint(): Pair<TileSpot, TileSpot>? =
        tiles.keys
            .filter(::isFree)
            .groupBy { tiles.getValue(it) }
            .values
            .firstOrNull { it.size >= 2 }
            ?.let { it[0] to it[1] }

    companion object {
        const val COLUMNS = 8
        const val ROWS = 6
        const val FACES = 18
        private const val MAX_TRIES = 50

        /** 8 x 6 on the bottom and 6 x 4 centred on top. */
        val LAYOUT: List<TileSpot> =
            (0 until ROWS).flatMap { row -> (0 until COLUMNS).map { TileSpot(0, it, row) } } +
                (1 until ROWS - 1).flatMap { row -> (1 until COLUMNS - 1).map { TileSpot(1, it, row) } }

        fun isFree(
            spot: TileSpot,
            present: Set<TileSpot>,
        ): Boolean {
            val covered = TileSpot(spot.layer + 1, spot.col, spot.row) in present
            val left = TileSpot(spot.layer, spot.col - 1, spot.row) in present
            val right = TileSpot(spot.layer, spot.col + 1, spot.row) in present
            return !covered && (!left || !right)
        }

        fun deal(random: Random): MahjongGame {
            repeat(MAX_TRIES) {
                tryDeal(random)?.let { return MahjongGame(it) }
            }
            error("no deal")
        }

        /** Removes free pairs from the full pile at random, giving each pair the next face. */
        private fun tryDeal(random: Random): Map<TileSpot, Int>? {
            val faces = (0 until FACES).flatMap { listOf(it, it) }.shuffled(random)
            val left = LAYOUT.toMutableSet()
            val dealt = mutableMapOf<TileSpot, Int>()
            for (face in faces) {
                val free = left.filter { isFree(it, left) }.shuffled(random)
                if (free.size < 2) return null
                val a = free[0]
                val b = free[1]
                dealt[a] = face
                dealt[b] = face
                left -= a
                left -= b
            }
            return dealt
        }
    }
}
