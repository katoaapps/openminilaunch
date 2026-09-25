package com.katoaapps.openminilaunch.ui.launcher

internal data class HomeTodoTextMetrics(
    val fontSizeSp: Float,
    val lineHeightSp: Float,
    val maxLines: Int,
)

internal fun visibleTodoItemsForHeight(availableHeightDp: Float): Int = when {
    availableHeightDp < 240f -> 1
    availableHeightDp < 280f -> 2
    availableHeightDp < 380f -> 3
    availableHeightDp < 450f -> 4
    else -> 5
}

internal fun shortcutCellSizeDp(availableWidthDp: Float, availableHeightDp: Float): Float {
    val horizontalGap = 4f
    val verticalGaps = 12f
    val widthBound = (availableWidthDp - horizontalGap) / 2f
    val heightBound = (availableHeightDp - verticalGaps) / 4f
    return minOf(widthBound, heightBound, 88f).coerceAtLeast(36f)
}

/** Uses dp for available room and fontScale for the rendered line height. */
internal fun homeTodoTextMetrics(
    largeDisplay: Boolean,
    compact: Boolean,
    panelHeightDp: Float,
    itemsPerPage: Int,
    fontScale: Float,
): HomeTodoTextMetrics {
    val fontSizeSp = when {
        largeDisplay -> 20f
        compact -> 13f
        else -> 15f
    }
    val lineHeightSp = when {
        largeDisplay -> 26f
        compact -> 17f
        else -> 20f
    }
    val maximumLines = if (largeDisplay) 4 else 2
    val safeItemCount = itemsPerPage.coerceAtLeast(1)
    val rowHeightDp = (panelHeightDp - 72f).coerceAtLeast(1f) / safeItemCount
    val renderedLineHeightDp = lineHeightSp * fontScale.coerceAtLeast(.5f)
    val linesThatFit = (rowHeightDp / renderedLineHeightDp).toInt()

    return HomeTodoTextMetrics(
        fontSizeSp = fontSizeSp,
        lineHeightSp = lineHeightSp,
        maxLines = linesThatFit.coerceIn(1, maximumLines),
    )
}
