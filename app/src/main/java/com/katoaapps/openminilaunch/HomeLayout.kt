package com.katoaapps.openminilaunch

internal fun visibleTodoItemsForHeight(availableHeightDp: Float): Int = when {
    availableHeightDp < 240f -> 1
    availableHeightDp < 310f -> 2
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
