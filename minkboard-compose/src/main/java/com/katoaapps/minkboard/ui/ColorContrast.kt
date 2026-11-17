package com.katoaapps.minkboard.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private val ReadableDark = Color(0xFF172019)
private val MinkWhite = Color(0xFFFAF9F6)

internal fun contrastRatio(first: Color, second: Color): Float {
    val lighter = maxOf(first.luminance(), second.luminance())
    val darker = minOf(first.luminance(), second.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

internal fun readableContentColor(background: Color): Color =
    if (contrastRatio(background, ReadableDark) >= contrastRatio(background, MinkWhite)) {
        ReadableDark
    } else {
        MinkWhite
    }
