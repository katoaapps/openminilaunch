package com.katoaapps.openminilaunch

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.material3.ColorScheme

internal val LightInk: Color @Composable get() = colorResource(R.color.light_ink)
internal val LightPaper: Color @Composable get() = colorResource(R.color.light_paper)
internal val Sage: Color @Composable get() = colorResource(R.color.sage)
internal val Rust: Color @Composable get() = colorResource(R.color.rust)
internal val MinkForest: Color @Composable get() = colorResource(R.color.mink_forest)
internal val MinkForestPanel: Color @Composable get() = colorResource(R.color.mink_forest_panel)
internal val Muted: Color @Composable get() = colorResource(R.color.muted)
internal val DarkPrimary: Color @Composable get() = colorResource(R.color.dark_primary)
internal val DarkOnPrimary: Color @Composable get() = colorResource(R.color.dark_on_primary)
internal val DarkBackground: Color @Composable get() = colorResource(R.color.dark_background)
internal val DarkSurface: Color @Composable get() = colorResource(R.color.dark_surface)
internal val DarkSurfaceContainerLow: Color @Composable get() = colorResource(R.color.dark_surface_container_low)
internal val DarkOnSurface: Color @Composable get() = colorResource(R.color.dark_on_surface)
internal val ReadableDark: Color @Composable get() = colorResource(R.color.readable_dark)
internal val MagicTextColor: Color @Composable get() = colorResource(R.color.magic_text)
internal val MagicCallColor: Color @Composable get() = colorResource(R.color.magic_call)
internal val MagicTodoColor: Color @Composable get() = colorResource(R.color.magic_todo)
internal val MagicNoteColor: Color @Composable get() = colorResource(R.color.magic_note)
internal val MagicEventColor: Color @Composable get() = colorResource(R.color.magic_event)
internal val MagicAppColor: Color @Composable get() = colorResource(R.color.magic_app)
internal val MinkWhite: Color @Composable get() = colorResource(R.color.mink_white)
internal val MinkBlack: Color @Composable get() = colorResource(R.color.mink_black)
internal val MinkTransparent: Color @Composable get() = colorResource(R.color.transparent)

@Composable
internal fun readableContentColor(background: Color): Color {
    val luminance = .2126f * background.red + .7152f * background.green + .0722f * background.blue
    return if (luminance > .55f) ReadableDark else MinkWhite
}

private fun blendColor(start: Color, end: Color, fraction: Float): Color = Color(
    red = start.red + (end.red - start.red) * fraction,
    green = start.green + (end.green - start.green) * fraction,
    blue = start.blue + (end.blue - start.blue) * fraction,
    alpha = 1f,
)

@Composable
internal fun ColorScheme.withAppBackground(customArgb: Int?): ColorScheme {
    val background = customArgb?.let(::Color) ?: return this
    val content = readableContentColor(background)
    val darkBackground = content == MinkWhite
    val surfaceTarget = if (darkBackground) MinkWhite else MinkBlack
    fun surfaceTone(fraction: Float) = blendColor(background, surfaceTarget, fraction)

    return copy(
        background = background,
        onBackground = content,
        surface = surfaceTone(if (darkBackground) .035f else .015f),
        onSurface = content,
        surfaceVariant = surfaceTone(if (darkBackground) .14f else .08f),
        onSurfaceVariant = content.copy(alpha = .78f),
        surfaceDim = surfaceTone(if (darkBackground) .02f else .06f),
        surfaceBright = surfaceTone(if (darkBackground) .18f else .01f),
        surfaceContainerLowest = background,
        surfaceContainerLow = surfaceTone(.055f),
        surfaceContainer = surfaceTone(.085f),
        surfaceContainerHigh = surfaceTone(.115f),
        surfaceContainerHighest = surfaceTone(.16f),
        outline = content.copy(alpha = .48f),
        outlineVariant = content.copy(alpha = .24f),
    )
}
