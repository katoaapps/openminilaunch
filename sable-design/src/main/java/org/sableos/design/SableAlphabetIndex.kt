package org.sableos.design

import java.text.Normalizer
import java.util.Locale

/**
 * The single alphabet-index concept for indexed lists (DESIGN-KF-D part 2).
 *
 * ```text
 * DUPLICATE_LEFT_RIGHT_ALPHABET_RAILS=FORBIDDEN
 * KEYBOARD_A_TO_Z_JUMP=PRIMARY_ON_KEYBOARD_DEVICES
 * TOUCH_ALPHABET_RAIL=OPTIONAL_SECONDARY
 * ALPHABET_RAIL_MUST_FUNCTION_IF_VISIBLE=YES
 * ```
 *
 * Built once (off the UI thread for big lists) from labels in display order.
 * [railKeys] contains only sections that exist, so every visible rail letter
 * jumps somewhere; keyboard jumps to a missing letter land on the next section.
 */
class SableAlphabetIndex private constructor(
    val sections: List<Section>,
    private val itemSections: IntArray,
) {
    data class Section(
        val key: String,
        val firstIndex: Int,
    )

    val isEmpty: Boolean get() = sections.isEmpty()

    /** Rail letters: only sections with items (no dead, decorative letters). */
    val railKeys: List<String> get() = sections.map { it.key }

    /** First item of section [key], or null when the section does not exist. */
    fun indexOf(key: String): Int? = sections.firstOrNull { it.key == key }?.firstIndex

    /** Section key of the item at [itemIndex] (for highlighting the active letter). */
    fun sectionKeyAt(itemIndex: Int): String? {
        if (itemSections.isEmpty()) return null
        val i = itemIndex.coerceIn(0, itemSections.lastIndex)
        return sections.getOrNull(itemSections[i])?.key
    }

    /**
     * Keyboard A–Z jump: the section of [char], else the next section after it
     * in rail order, else the last section. Digits and symbols go to "#".
     */
    fun indexForChar(char: Char): Int? {
        if (sections.isEmpty()) return null
        val key = keyFor(char.toString())
        indexOf(key)?.let { return it }
        if (key == OTHER) return null
        val lettered = sections.filter { it.key != OTHER && it.key != PINNED }
        if (lettered.isEmpty()) return null
        val next = lettered.firstOrNull { it.key > key }
        return (next ?: lettered.last()).firstIndex
    }

    /** Touch rail: section under a pointer at [fraction] (0..1) of the rail's height. */
    fun sectionAt(fraction: Float): Section? {
        if (sections.isEmpty() || !fraction.isFinite()) return null
        val slot = (fraction * sections.size).toInt().coerceIn(0, sections.lastIndex)
        return sections[slot]
    }

    companion object {
        const val OTHER = "#"

        /** Section of items pinned above the alphabetical list (e.g. starred contacts). */
        const val PINNED = "★"

        /** Section key of a label: its first letter without accents, upper case; anything else is "#". */
        fun keyFor(
            label: String,
            locale: Locale = Locale.ROOT,
        ): String {
            val first = label.trim().firstOrNull() ?: return OTHER
            if (!first.isLetter()) return OTHER
            val base =
                Normalizer
                    .normalize(first.toString(), Normalizer.Form.NFD)
                    .firstOrNull { it.isLetter() } ?: first
            return base.toString().uppercase(locale)
        }

        /**
         * [labels] must already be in display order; sections follow that order.
         * The first [pinnedCount] items (favourites listed before the A–Z list)
         * form one [PINNED] section so letters never point into them.
         */
        fun build(
            labels: List<String>,
            locale: Locale = Locale.ROOT,
            pinnedCount: Int = 0,
        ): SableAlphabetIndex {
            val sections = mutableListOf<Section>()
            val sectionByKey = HashMap<String, Int>()
            val itemSections = IntArray(labels.size)
            labels.forEachIndexed { index, label ->
                val key = if (index < pinnedCount) PINNED else keyFor(label, locale)
                val sectionIndex =
                    sectionByKey.getOrPut(key) {
                        sections += Section(key, index)
                        sections.lastIndex
                    }
                itemSections[index] = sectionIndex
            }
            return SableAlphabetIndex(sections, itemSections)
        }
    }
}

/**
 * Multi-letter type-to-jump for focused lists on keyboard devices.
 *
 * Letters typed within [timeoutMs] refine a prefix ("ca" → Calendar/Camera);
 * repeating the same single letter cycles through items starting with it.
 */
class SableTypeToJump(
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
) {
    private var prefix = ""
    private var lastAtMs = Long.MIN_VALUE

    val currentPrefix: String get() = prefix

    /**
     * Feeds one typed character and returns the item index to focus, or null.
     * @param currentIndex the focused index (for cycling).
     */
    fun onChar(
        char: Char,
        nowMs: Long,
        labels: List<String>,
        currentIndex: Int = -1,
    ): Int? {
        val c = char.lowercaseChar()
        val expired = lastAtMs == Long.MIN_VALUE || nowMs - lastAtMs > timeoutMs || nowMs < lastAtMs
        lastAtMs = nowMs
        val cycling = !expired && prefix.length == 1 && prefix[0] == c
        prefix = if (expired || cycling) c.toString() else prefix + c
        if (labels.isEmpty()) return null
        if (cycling) {
            return firstMatch(labels, prefix, from = currentIndex + 1)
                ?: firstMatch(labels, prefix, from = 0)
        }
        return firstMatch(labels, prefix, from = 0)
    }

    fun reset() {
        prefix = ""
        lastAtMs = Long.MIN_VALUE
    }

    private fun firstMatch(
        labels: List<String>,
        prefix: String,
        from: Int,
    ): Int? {
        for (i in from.coerceAtLeast(0) until labels.size) {
            if (normalize(labels[i]).startsWith(prefix)) return i
        }
        return null
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 1_000L

        internal fun normalize(label: String): String =
            Normalizer
                .normalize(label.trim(), Normalizer.Form.NFD)
                .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }
                .lowercase(Locale.ROOT)
    }
}
