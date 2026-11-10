package com.katoaapps.openminilaunch.ui.theme

import com.katoaapps.openminilaunch.R

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.colorResource

private val ReadableDarkValue = Color(0xFF172019)
private val MinkWhiteValue = Color(0xFFFAF9F6)

internal val LightInk: Color @Composable get() = colorResource(R.color.light_ink)
internal val LightPaper: Color @Composable get() = colorResource(R.color.light_paper)
internal val Sage: Color @Composable get() = colorResource(R.color.sage)
internal val Rust: Color @Composable get() = colorResource(R.color.rust)
internal val MinkBrown: Color @Composable get() = colorResource(R.color.mink_brown)
internal val MinkForest: Color @Composable get() = colorResource(R.color.mink_forest)
internal val MinkForestPanel: Color @Composable get() = colorResource(R.color.mink_forest_panel)
internal val LocalMutedColor = compositionLocalOf<Color?> { null }
internal val Muted: Color
    @Composable get() = LocalMutedColor.current ?: MaterialTheme.colorScheme.onSurface.copy(alpha = .68f)
internal val DarkPrimary: Color @Composable get() = colorResource(R.color.dark_primary)
internal val DarkOnPrimary: Color @Composable get() = colorResource(R.color.dark_on_primary)
internal val DarkBackground: Color @Composable get() = colorResource(R.color.dark_background)
internal val DarkSurface: Color @Composable get() = colorResource(R.color.dark_surface)
internal val DarkSurfaceContainerLow: Color @Composable get() = colorResource(R.color.dark_surface_container_low)
internal val DarkOnSurface: Color @Composable get() = colorResource(R.color.dark_on_surface)
internal val ReadableDark: Color @Composable get() = colorResource(R.color.readable_dark)
internal val MagicTextColor = MinkFeatureColors.MagicText
internal val MagicCallColor = MinkFeatureColors.MagicCall
internal val MagicTodoColor = MinkFeatureColors.MagicTodo
internal val MagicNoteColor = MinkFeatureColors.MagicNote
internal val MagicEventColor = MinkFeatureColors.MagicEvent
internal val MagicAppColor = MinkFeatureColors.MagicApp
internal val AppBlockedRed = MinkFeatureColors.AppBlocked
internal val MinkWhite: Color @Composable get() = colorResource(R.color.mink_white)
internal val MinkBlack: Color @Composable get() = colorResource(R.color.mink_black)
internal val MinkTransparent: Color @Composable get() = colorResource(R.color.transparent)

internal fun contrastRatio(first: Color, second: Color): Float {
    val lighter = maxOf(first.luminance(), second.luminance())
    val darker = minOf(first.luminance(), second.luminance())
    return (lighter + .05f) / (darker + .05f)
}

/** True when dark foreground content is more legible than light content on [background]. */
internal fun shouldUseDarkForeground(background: Color): Boolean =
    contrastRatio(background, ReadableDarkValue) >= contrastRatio(background, MinkWhiteValue)

/** Picks the higher-contrast member of Mink's standard light/dark content pair. */
internal fun readableContentColor(background: Color): Color =
    if (shouldUseDarkForeground(background)) ReadableDarkValue else MinkWhiteValue

/** Keeps an intentional palette color when legible and falls back only when contrast is unsafe. */
internal fun ensureReadableContentColor(
    background: Color,
    preferred: Color,
    minimumContrast: Float = 4.5f,
): Color = if (contrastRatio(background, preferred) >= minimumContrast) {
    preferred
} else {
    readableContentColor(background)
}

/** Uses a preferred user color when legible and otherwise follows the visible background. */
internal fun resolveContentColor(background: Color, preferred: Color?): Color =
    preferred?.let { ensureReadableContentColor(background, it) }
        ?: readableContentColor(background)

/** Returns the color perceived when [foreground] is drawn over [background]. */
internal fun compositeColor(foreground: Color, background: Color): Color {
    val inverseForegroundAlpha = 1f - foreground.alpha
    val outputAlpha = foreground.alpha + background.alpha * inverseForegroundAlpha
    if (outputAlpha == 0f) return Color.Transparent
    fun channel(foregroundChannel: Float, backgroundChannel: Float): Float =
        (foregroundChannel * foreground.alpha +
            backgroundChannel * background.alpha * inverseForegroundAlpha) / outputAlpha

    return Color(
        red = channel(foreground.red, background.red),
        green = channel(foreground.green, background.green),
        blue = channel(foreground.blue, background.blue),
        alpha = outputAlpha,
    )
}

private fun blendColor(start: Color, end: Color, fraction: Float): Color = Color(
    red = start.red + (end.red - start.red) * fraction,
    green = start.green + (end.green - start.green) * fraction,
    blue = start.blue + (end.blue - start.blue) * fraction,
    alpha = 1f,
)

internal fun ColorScheme.withAppBackground(customArgb: Int?): ColorScheme {
    val background = customArgb?.let(::Color) ?: return this
    val content = readableContentColor(background)
    val darkBackground = !shouldUseDarkForeground(background)
    val surfaceTarget = if (darkBackground) MinkWhiteValue else Color.Black
    fun surfaceTone(fraction: Float) = blendColor(background, surfaceTarget, fraction)
    fun visibleAccent(preferred: Color) = ensureReadableContentColor(
        background = background,
        preferred = preferred,
        minimumContrast = 3f,
    )
    val visiblePrimary = visibleAccent(primary)
    val visibleSecondary = visibleAccent(secondary)
    val visibleTertiary = visibleAccent(tertiary)

    return copy(
        primary = visiblePrimary,
        onPrimary = readableContentColor(visiblePrimary),
        secondary = visibleSecondary,
        onSecondary = readableContentColor(visibleSecondary),
        tertiary = visibleTertiary,
        onTertiary = readableContentColor(visibleTertiary),
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
        surfaceTint = visiblePrimary,
    )
}
