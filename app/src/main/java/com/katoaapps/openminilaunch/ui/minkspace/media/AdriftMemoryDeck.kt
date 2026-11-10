package com.katoaapps.openminilaunch.ui.minkspace.media

import kotlin.random.Random

/**
 * Coordinates independently animated frames. With more memories than slots, every visible item
 * is unique and every memory is dealt before a new shuffled cycle begins.
 */
internal class AdriftMemoryDeck<T>(
    sourceItems: List<T>,
    private val slotCount: Int,
    private val seed: Int,
) {
    private val items = sourceItems.distinct()
    private val visible = mutableListOf<T>()
    private var remaining = mutableListOf<T>()
    private var cycle = 0

    init {
        require(slotCount > 0) { "Adrift requires at least one frame slot" }
        if (items.isNotEmpty()) {
            if (items.size <= slotCount) {
                val shuffled = items.shuffled(Random(seed))
                repeat(slotCount) { slot -> visible += shuffled[slot % shuffled.size] }
            } else {
                refill(excludeVisible = false)
                repeat(slotCount) { visible += remaining.removeAt(0) }
            }
        }
    }

    fun currentItems(): List<T> = visible.toList()

    fun advance(slotIndex: Int): T {
        require(slotIndex in visible.indices) { "Unknown Adrift frame slot $slotIndex" }
        // When every unique memory already fits on screen, keep those memories visible and let
        // the frame's next generation provide a fresh placement rather than adding duplicates.
        if (items.size <= slotCount) return visible[slotIndex]
        if (remaining.isEmpty()) refill(excludeVisible = true)
        return remaining.removeAt(0).also { next -> visible[slotIndex] = next }
    }

    private fun refill(excludeVisible: Boolean) {
        cycle++
        val excluded = if (excludeVisible) visible.toSet() else emptySet()
        remaining = items
            .shuffled(Random(seed xor (cycle * 0x45D9F3B)))
            .filterNot(excluded::contains)
            .toMutableList()
    }
}
