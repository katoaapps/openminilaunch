package com.katoaapps.openminilaunch.model

enum class FontSource {
    DEFAULT,
    SERIF,
    MONOSPACE,
    CUSTOM,
}

data class FontAppearance(
    val source: FontSource = FontSource.DEFAULT,
    val sizeScale: Float = DEFAULT_FONT_SIZE_SCALE,
    val colorArgb: Int? = null,
    val customFontName: String? = null,
)

const val DEFAULT_FONT_SIZE_SCALE = 1f

val FONT_SIZE_SCALE_STEPS = listOf(.85f, 1f, 1.15f, 1.3f, 1.45f)

fun normalizeFontSizeScale(value: Float): Float {
    if (!value.isFinite()) return DEFAULT_FONT_SIZE_SCALE
    return FONT_SIZE_SCALE_STEPS.minByOrNull { kotlin.math.abs(it - value) }
        ?: DEFAULT_FONT_SIZE_SCALE
}
