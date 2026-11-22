package org.sableos.design

/*
 * Responsive rules for keyboard-first Sable devices (DESIGN-KF-D part 2).
 *
 * Pure Kotlin (no android.* / Compose imports) so the decisions are unit
 * tested off-device. Device differences come from the window size and font
 * scale the platform reports, never from a device model check.
 */

enum class SableWidthClass {
    Compact,
    Medium,
    Expanded,
}

/**
 * Window metrics in dp plus the user's font scale.
 *
 * [textWidthDp] is the width expressed in "font-scale 1.0 dp": a 360 dp wide
 * square window at font scale 1.3 has the text room of a 277 dp window, which
 * is what label-fitting decisions must use (FONT_SCALE_COMPACT_LAYOUT).
 */
data class SableLayoutMetrics(
    val widthDp: Int,
    val heightDp: Int,
    val fontScale: Float = 1f,
) {
    private val scale: Float get() = if (fontScale.isFinite() && fontScale > 0f) fontScale else 1f

    val textWidthDp: Float get() = widthDp / scale.coerceAtLeast(1f)

    val widthClass: SableWidthClass
        get() =
            when {
                widthDp < SableResponsive.COMPACT_MAX_WIDTH_DP ||
                    textWidthDp < SableResponsive.COMPACT_TEXT_WIDTH_DP -> SableWidthClass.Compact
                widthDp < SableResponsive.MEDIUM_MAX_WIDTH_DP -> SableWidthClass.Medium
                else -> SableWidthClass.Expanded
            }

    /** Square-ish displays (e.g. 720x720) keep compact rules even when wide enough. */
    val squareish: Boolean
        get() {
            if (widthDp <= 0 || heightDp <= 0) return false
            val ratio = widthDp.toFloat() / heightDp.toFloat()
            return ratio in SableResponsive.SQUARE_MIN_RATIO..SableResponsive.SQUARE_MAX_RATIO
        }

    val largeFont: Boolean get() = scale >= SableResponsive.LARGE_FONT_SCALE

    /** MAX_COMPACT_SIMULTANEOUS_TOP_DESTINATIONS=3. */
    val maxTopDestinations: Int
        get() =
            when (widthClass) {
                SableWidthClass.Compact -> SableResponsive.MAX_COMPACT_TOP_DESTINATIONS
                SableWidthClass.Medium -> SableResponsive.MAX_MEDIUM_TOP_DESTINATIONS
                SableWidthClass.Expanded -> SableResponsive.MAX_EXPANDED_TOP_DESTINATIONS
            }

    /** PRIMARY_TRAILING_ACTIONS_MAX=1 on compact layouts. */
    val maxPrimaryTrailingActions: Int
        get() =
            when (widthClass) {
                SableWidthClass.Compact -> 1
                SableWidthClass.Medium -> 2
                SableWidthClass.Expanded -> 3
            }

    /** Secondary actions of a mini-player move to its context menu on constrained layouts. */
    val miniPlayerInlineSecondaryActions: Boolean
        get() = widthClass != SableWidthClass.Compact && !largeFont
}

object SableResponsive {
    const val COMPACT_MAX_WIDTH_DP = 600
    const val MEDIUM_MAX_WIDTH_DP = 840
    const val COMPACT_TEXT_WIDTH_DP = 420f
    const val SQUARE_MIN_RATIO = 0.8f
    const val SQUARE_MAX_RATIO = 1.25f
    const val LARGE_FONT_SCALE = 1.3f

    /**
     * Windows shorter than this (a 720x720 panel at 280 dpi is 411 dp, less the status bar) get the
     * compact type scale, screen padding and headers, so a screen's content starts in the first third.
     */
    const val SHORT_MAX_HEIGHT_DP = 600

    /** True for a window too short for the regular headers; 0 (unknown) is not short. */
    fun isShort(heightDp: Int): Boolean = heightDp in 1 until SHORT_MAX_HEIGHT_DP

    const val MAX_COMPACT_TOP_DESTINATIONS = 3
    const val MAX_MEDIUM_TOP_DESTINATIONS = 5
    const val MAX_EXPANDED_TOP_DESTINATIONS = 7

    const val PRIMARY_TEXT_MAX_LINES = 2
    const val SECONDARY_TEXT_MAX_LINES = 1
    // Same value as SableDesignContract.MIN_TOUCH_TARGET_DP (kept literal so this file stays Compose-free).
    const val TOUCH_TARGET_MIN_DP = 48

    /** Rough average glyph advance as a fraction of the font size, for estimates without a text measurer. */
    const val AVERAGE_GLYPH_WIDTH_EM = 0.58f

    /** Width estimate in dp of a single-line label (used in tests and when no measurer is at hand). */
    fun estimateLabelWidthDp(
        label: String,
        textSizeSp: Float,
        fontScale: Float = 1f,
    ): Float = label.length * textSizeSp * AVERAGE_GLYPH_WIDTH_EM * fontScale.coerceAtLeast(0.5f)
}

/** One top-level destination of an app. */
data class SableDestination(
    val id: String,
    val label: String,
)

/**
 * Result of [SableNavigationPolicy.layout]: what goes in the visible row and
 * what goes behind "More". [visible] never clips a label; [overflow] keeps the
 * original order.
 */
data class SableNavLayout(
    val visible: List<SableDestination>,
    val overflow: List<SableDestination>,
) {
    val hasOverflow: Boolean get() = overflow.isNotEmpty()

    /** Total slots in the row, including "More". */
    val slotCount: Int get() = visible.size + if (hasOverflow) 1 else 0
}

/**
 * ```text
 * MID_WORD_TAB_CLIPPING=FORBIDDEN       OVERFLOWING_FIXED_TAB_ROW=FORBIDDEN
 * MAX_COMPACT_SIMULTANEOUS_TOP_DESTINATIONS=3
 * SECONDARY_DESTINATIONS=OVERFLOW_OR_EXPLICIT_PAGE
 * ```
 * Four or more destinations on a compact window become N visible + an explicit,
 * keyboard-reachable "More" menu instead of shrinking text or scrolling a tab row.
 */
object SableNavigationPolicy {
    const val MORE_LABEL = "more"

    /** Horizontal padding a tab needs around its label. */
    const val TAB_HORIZONTAL_PADDING_DP = 16f

    /**
     * @param availableWidthDp width of the row.
     * @param labelWidthDp measured single-line width of a label at the current font scale.
     */
    fun layout(
        destinations: List<SableDestination>,
        selectedId: String?,
        metrics: SableLayoutMetrics,
        availableWidthDp: Float = metrics.widthDp.toFloat(),
        labelWidthDp: (String) -> Float,
    ): SableNavLayout {
        if (destinations.isEmpty()) return SableNavLayout(emptyList(), emptyList())
        val maxSlots = metrics.maxTopDestinations.coerceAtLeast(2)

        fun fits(labels: List<String>): Boolean {
            val slot = availableWidthDp / labels.size
            return labels.all { labelWidthDp(it) + TAB_HORIZONTAL_PADDING_DP <= slot }
        }

        if (destinations.size <= maxSlots && fits(destinations.map { it.label })) {
            return SableNavLayout(destinations, emptyList())
        }

        var visibleCount = (minOf(maxSlots, destinations.size) - 1).coerceAtLeast(1)
        while (true) {
            val visible = pickVisible(destinations, selectedId, visibleCount)
            if (visibleCount == 1 || fits(visible.map { it.label } + MORE_LABEL)) {
                return SableNavLayout(visible, destinations.filterNot { it in visible })
            }
            visibleCount--
        }
    }

    /** First [count] destinations, but the selected one is always visible (it replaces the last slot). */
    private fun pickVisible(
        destinations: List<SableDestination>,
        selectedId: String?,
        count: Int,
    ): List<SableDestination> {
        val head = destinations.take(count)
        val selected = destinations.firstOrNull { it.id == selectedId }
        if (selected == null || selected in head) return head
        return (head.dropLast(1) + selected).sortedBy { destinations.indexOf(it) }
    }
}

/** One action a dense row may offer. */
data class SableRowAction(
    val id: String,
    val label: String,
    /** Candidate for the single visible trailing action (e.g. play). */
    val primary: Boolean = false,
    val destructive: Boolean = false,
)

data class SableRowActionSplit(
    val trailing: List<SableRowAction>,
    val overflow: List<SableRowAction>,
)

/**
 * ```text
 * PRIMARY_TEXT=1-2_LINES   SECONDARY_TEXT=1_LINE_DEFAULT
 * PRIMARY_TRAILING_ACTIONS_MAX=1   SECONDARY_ACTIONS_TO_MENU=YES
 * TOUCH_TARGET_MIN=48dp   VISIBLE_KEYBOARD_FOCUS=YES
 * ```
 */
object SableDenseRowPolicy {
    fun split(
        actions: List<SableRowAction>,
        metrics: SableLayoutMetrics,
    ): SableRowActionSplit {
        val max = if (metrics.largeFont) 1 else metrics.maxPrimaryTrailingActions
        val compact = metrics.widthClass == SableWidthClass.Compact
        val eligible =
            actions.filter { action -> !(compact && action.destructive) }
        val ordered = eligible.filter { it.primary } + eligible.filterNot { it.primary }
        val trailing = ordered.take(max).filter { it.primary || !compact }
        return SableRowActionSplit(trailing, actions.filterNot { it in trailing })
    }
}

/** Long labels: MID_WORD_CLIP=NO, ELLIPSIS_ALLOWED=YES, full label exposed when truncated. */
object SableLabelPolicy {
    const val ELLIPSIS = "…"

    /**
     * Shortens [label] to at most [maxChars] characters, cutting at a word
     * boundary and adding an ellipsis. A single word longer than the limit is
     * ellipsized (allowed), never silently clipped.
     */
    fun shorten(
        label: String,
        maxChars: Int,
    ): String {
        val text = label.trim()
        if (maxChars <= 0) return ""
        if (text.length <= maxChars) return text
        if (maxChars <= ELLIPSIS.length) return ELLIPSIS
        val room = maxChars - ELLIPSIS.length
        val cut = text.lastIndexOf(' ', room)
        val head = if (cut > 0) text.substring(0, cut) else text.substring(0, room)
        return head.trimEnd(' ', ',', '·', '-') + ELLIPSIS
    }

    /** The label assistive tech / tooltip must carry when the visible text was shortened. */
    fun accessibleLabel(
        full: String,
        shown: String,
    ): String? = if (full.trim() != shown.trim()) full.trim() else null
}

/** Focus restoration: FOCUS_RESTORATION=REQUIRED, BACK_RETURNS_ONE_LOGICAL_LAYER=REQUIRED. */
class SableFocusMemory {
    private data class Entry(
        val key: String,
        val index: Int,
    )

    private val entries = HashMap<String, Entry>()

    /** Record the focused item of [surface] before leaving it (detail, context menu, App info). */
    fun remember(
        surface: String,
        itemKey: String,
        index: Int,
    ) {
        entries[surface] = Entry(itemKey, index)
    }

    fun rememberedKey(surface: String): String? = entries[surface]?.key

    /**
     * Index to focus when [surface] is shown again with [currentKeys]: the same
     * item if it still exists, else the item now at its old position (clamped),
     * else null for an empty list or a surface never focused.
     */
    fun restore(
        surface: String,
        currentKeys: List<String>,
    ): Int? {
        val entry = entries[surface] ?: return null
        if (currentKeys.isEmpty()) return null
        val byKey = currentKeys.indexOf(entry.key)
        return if (byKey >= 0) byKey else entry.index.coerceIn(0, currentKeys.lastIndex)
    }

    fun forget(surface: String) {
        entries.remove(surface)
    }
}

enum class SableListOrder {
    /** Index 0 is the newest item (feeds, reverse-layout conversations). */
    NewestFirst,

    /** The last index is the newest item (chronological history). */
    NewestLast,
}

/**
 * ```text
 * CURRENT_OR_LATEST_STATE_VISIBLE_ON_OPEN=YES   SCROLL_TO_DISCOVER_CURRENT_STATE=NO
 * RETURN_TO_CURRENT_ACTION=REQUIRED_WHERE_HISTORY_EXISTS
 * ```
 */
object SableLatestContentPolicy {
    /** Item to show first when the surface opens. */
    fun initialIndex(
        count: Int,
        order: SableListOrder,
    ): Int =
        when {
            count <= 0 -> 0
            order == SableListOrder.NewestFirst -> 0
            else -> count - 1
        }

    /** Show a "latest" action whenever the newest item is scrolled out of view. */
    fun showReturnToCurrent(
        firstVisibleIndex: Int,
        lastVisibleIndex: Int,
        count: Int,
        order: SableListOrder,
    ): Boolean {
        if (count <= 1) return false
        val newest = initialIndex(count, order)
        return newest !in firstVisibleIndex..lastVisibleIndex
    }
}
