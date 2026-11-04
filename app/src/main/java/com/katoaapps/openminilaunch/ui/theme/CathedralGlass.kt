package com.katoaapps.openminilaunch.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** A shared translucent tint used by the Home pill and user-created folders. */
internal fun cathedralGlassBrush(baseColor: Color, transparency: Float): Brush {
    val opacity = 1f - normalizedGlassTransparency(transparency)
    val opaqueBase = baseColor.copy(alpha = 1f)
    return Brush.linearGradient(
        listOf(
            lerp(opaqueBase, Color.White, GLASS_LIGHT_PANE_MIX).copy(alpha = opacity),
            opaqueBase.copy(alpha = opacity),
            lerp(opaqueBase, Color.Black, GLASS_DARK_PANE_MIX).copy(alpha = opacity),
        ),
    )
}

internal fun cathedralGlassCompositeColor(
    baseColor: Color,
    transparency: Float,
    backdropColor: Color,
): Color = compositeColor(
    baseColor.copy(alpha = 1f - normalizedGlassTransparency(transparency)),
    backdropColor,
)

private fun normalizedGlassTransparency(value: Float): Float =
    if (value.isFinite()) value.coerceIn(0f, 1f) else 0f

private const val GLASS_LIGHT_PANE_MIX = .08f
private const val GLASS_DARK_PANE_MIX = .05f
