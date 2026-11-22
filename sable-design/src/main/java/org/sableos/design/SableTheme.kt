package org.sableos.design

import android.graphics.Typeface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AppearanceMode(
    val stableValue: String,
) {
    FollowSystem("follow-system"),
    Light("light"),
    Dark("dark"),
    ;

    companion object {
        fun fromStableValue(value: String?): AppearanceMode = entries.firstOrNull { it.stableValue == value } ?: FollowSystem
    }
}

enum class AccentPreset(
    val stableValue: String,
    val displayName: String,
    val color: Color,
    val onColor: Color,
) {
    Blue("blue", "Sable Blue", Color(0xFF2E9BD6), Color(0xFF000000)),
    ;

    companion object {
        fun fromStableValue(value: String?): AccentPreset = entries.firstOrNull { it.stableValue == value } ?: Blue
    }
}

data class SableAppearance(
    val mode: AppearanceMode = AppearanceMode.FollowSystem,
    val accent: AccentPreset = AccentPreset.Blue,
    val cornerStyle: SableCornerStyle = SableCornerStyle.DEFAULT,
)

object SableDesignContract {
    const val SCHEMA_VERSION = 2
    const val KEY_APPEARANCE_MODE = "sable.appearance.mode"
    const val KEY_ACCENT = "sable.appearance.accent"

    const val MIN_TOUCH_TARGET_DP = 48
    const val SCREEN_HORIZONTAL_PADDING_DP = 24
    const val SCREEN_VERTICAL_PADDING_DP = 20
    const val KEY_CORNER_STYLE = "sable.appearance.corner_style"

    /** Largest radius of each allowed corner style (Compact, Rounded); see SableShapeTable. */
    const val MAX_COMPACT_CORNER_RADIUS_DP = 6
    const val MAX_ROUNDED_CORNER_RADIUS_DP = 12

    /** Upper bound across the two allowed styles. */
    const val MAX_STANDARD_CORNER_RADIUS_DP = MAX_ROUNDED_CORNER_RADIUS_DP

    const val FAST_MOTION_MS = 120
    const val STANDARD_MOTION_MS = 180
    const val EMPHASIZED_MOTION_MS = 240
}

object SableSpacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
    val Xxl = 32.dp
    val Xxxl = 48.dp

    val ScreenHorizontal = SableDesignContract.SCREEN_HORIZONTAL_PADDING_DP.dp
    val ScreenVertical = SableDesignContract.SCREEN_VERTICAL_PADDING_DP.dp

    /** Screen padding in a short window ([SableResponsive.isShort]). */
    val ScreenHorizontalShort = 12.dp
    val ScreenVerticalShort = 8.dp
}

/** True while the current window is short ([SableResponsive.isShort]): compact type, padding and headers. */
@Composable
fun isSableShortWindow(): Boolean = SableResponsive.isShort(LocalConfiguration.current.screenHeightDp)

object SableSize {
    val TouchTarget = SableDesignContract.MIN_TOUCH_TARGET_DP.dp
    val ControlHeight = 52.dp
    val ListRowHeight = 58.dp
    val AccentBarWidth = 3.dp
}

object SableColorTokens {
    val DarkBackground = Color(0xFF000000)
    val DarkSurface = Color(0xFF141414)
    val DarkSurfaceRaised = Color(0xFF262626)
    val DarkText = Color(0xFFFFFFFF)
    val DarkTextMuted = Color(0xFFA6A6A6)

    val LightBackground = Color(0xFFF5F7F9)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceRaised = Color(0xFFE9EDF2)
    val LightText = Color(0xFF12171C)
    val LightTextMuted = Color(0xFF515B66)
}

/**
 * Source Sans 3 (Sable 2.0 type, UI direction 2026-11-04): the "source-sans" family the image adds
 * in /product/etc/fonts_customization.xml. Where it is missing, Typeface.create returns the default
 * sans-serif, so nothing breaks.
 */
val SableFontFamily: FontFamily by lazy { FontFamily(Typeface.create("source-sans", Typeface.NORMAL)) }

private val SableTypography by lazy {
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = SableFontFamily,
                fontSize = 44.sp,
                lineHeight = 50.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-0.8).sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = SableFontFamily,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-0.4).sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = SableFontFamily,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Normal,
            ),
        titleLarge =
            TextStyle(
                fontFamily = SableFontFamily,
                fontSize = 21.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Medium,
            ),
        titleMedium =
            TextStyle(
                fontFamily = SableFontFamily,
                fontSize = 17.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Medium,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = SableFontFamily,
                fontSize = 17.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = SableFontFamily,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Normal,
            ),
        labelLarge =
            TextStyle(
                fontFamily = SableFontFamily,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
            ),
        // The roles above were the whole scale; the rest fell back to Material's sizes in Roboto
        // (displayMedium 45 sp, headlineSmall 24 sp, bodySmall 12 sp). Every role is Sable's now.
        displayMedium = sableStyle(36, 42, FontWeight.Light),
        displaySmall = sableStyle(30, 36, FontWeight.Light),
        headlineSmall = sableStyle(22, 28, FontWeight.Normal),
        titleSmall = sableStyle(15, 20, FontWeight.Medium),
        bodySmall = sableStyle(13, 18, FontWeight.Normal),
        labelMedium = sableStyle(12, 16, FontWeight.Medium),
        labelSmall = sableStyle(11, 14, FontWeight.Medium),
    )
}

private fun sableStyle(
    size: Int,
    line: Int,
    weight: FontWeight,
) = TextStyle(fontFamily = SableFontFamily, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)

/**
 * The type scale in a short window (the Q25's 411 dp square screen): the same roles, one step
 * smaller, display and headline the most. Titles follow the Q25 column of the approved TenSeven BB10
 * design review (a 22-24 sp screen title, 15-16 sp rows, 11-12 sp meta lines).
 */
private val SableShortTypography by lazy {
    SableTypography.copy(
        displayLarge = SableTypography.displayLarge.copy(fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
        displayMedium = SableTypography.displayMedium.copy(fontSize = 24.sp, lineHeight = 28.sp),
        displaySmall = SableTypography.displaySmall.copy(fontSize = 22.sp, lineHeight = 26.sp),
        headlineLarge = SableTypography.headlineLarge.copy(fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = 0.sp),
        headlineSmall = SableTypography.headlineSmall.copy(fontSize = 18.sp, lineHeight = 23.sp),
        headlineMedium = SableTypography.headlineMedium.copy(fontSize = 20.sp, lineHeight = 25.sp),
        titleLarge = SableTypography.titleLarge.copy(fontSize = 18.sp, lineHeight = 23.sp),
        titleMedium = SableTypography.titleMedium.copy(fontSize = 16.sp, lineHeight = 21.sp),
        bodyLarge = SableTypography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 21.sp),
        bodyMedium = SableTypography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 19.sp),
        labelLarge = SableTypography.labelLarge.copy(fontSize = 13.sp, lineHeight = 17.sp),
        titleSmall = SableTypography.titleSmall.copy(fontSize = 15.sp, lineHeight = 19.sp),
        bodySmall = SableTypography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
        labelMedium = SableTypography.labelMedium.copy(fontSize = 12.sp, lineHeight = 15.sp),
        labelSmall = SableTypography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
    )
}

/** Material shapes for a corner style, built from [SableShapeTable]. */
fun sableShapes(style: SableCornerStyle): Shapes {
    val scale = SableShapeTable.forStyle(style)
    return Shapes(
        extraSmall = RoundedCornerShape(scale.extraSmallDp.dp),
        small = RoundedCornerShape(scale.smallDp.dp),
        medium = RoundedCornerShape(scale.mediumDp.dp),
        large = RoundedCornerShape(scale.largeDp.dp),
        extraLarge = RoundedCornerShape(scale.extraLargeDp.dp),
    )
}

@Composable
fun SableTheme(
    appearance: SableAppearance = SableAppearance(),
    content: @Composable () -> Unit,
) {
    val dark =
        when (appearance.mode) {
            AppearanceMode.FollowSystem -> isSystemInDarkTheme()
            AppearanceMode.Light -> false
            AppearanceMode.Dark -> true
        }
    val accent = appearance.accent
    val colors =
        if (dark) {
            darkColorScheme(
                primary = accent.color,
                onPrimary = accent.onColor,
                background = SableColorTokens.DarkBackground,
                onBackground = SableColorTokens.DarkText,
                surface = SableColorTokens.DarkSurface,
                onSurface = SableColorTokens.DarkText,
                surfaceVariant = SableColorTokens.DarkSurfaceRaised,
                onSurfaceVariant = SableColorTokens.DarkTextMuted,
                outline = Color(0xFF4D4D4D),
                outlineVariant = Color(0xFF262626),
                secondary = SableColorTokens.DarkTextMuted,
                onSecondary = Color(0xFF000000),
                tertiary = accent.color,
                onTertiary = accent.onColor,
                primaryContainer = SableColorTokens.DarkSurfaceRaised,
                onPrimaryContainer = SableColorTokens.DarkText,
                secondaryContainer = SableColorTokens.DarkSurfaceRaised,
                onSecondaryContainer = SableColorTokens.DarkText,
                tertiaryContainer = SableColorTokens.DarkSurfaceRaised,
                onTertiaryContainer = SableColorTokens.DarkText,
                surfaceTint = Color.Transparent,
                inverseSurface = SableColorTokens.DarkText,
                inverseOnSurface = SableColorTokens.DarkBackground,
                inversePrimary = accent.color,
                surfaceDim = SableColorTokens.DarkBackground,
                surfaceBright = SableColorTokens.DarkSurfaceRaised,
                surfaceContainerLowest = SableColorTokens.DarkBackground,
                surfaceContainerLow = SableColorTokens.DarkSurface,
                surfaceContainer = SableColorTokens.DarkSurface,
                surfaceContainerHigh = SableColorTokens.DarkSurfaceRaised,
                surfaceContainerHighest = SableColorTokens.DarkSurfaceRaised,
                error = Color(0xFFE0463C),
                onError = Color(0xFF000000),
            )
        } else {
            lightColorScheme(
                primary = accent.color,
                onPrimary = accent.onColor,
                background = SableColorTokens.LightBackground,
                onBackground = SableColorTokens.LightText,
                surface = SableColorTokens.LightSurface,
                onSurface = SableColorTokens.LightText,
                surfaceVariant = SableColorTokens.LightSurfaceRaised,
                onSurfaceVariant = SableColorTokens.LightTextMuted,
                outline = Color(0xFF737E8A),
                outlineVariant = Color(0xFFD0D6DD),
                secondary = SableColorTokens.LightTextMuted,
                onSecondary = Color.White,
                tertiary = accent.color,
                onTertiary = accent.onColor,
                primaryContainer = SableColorTokens.LightSurfaceRaised,
                onPrimaryContainer = SableColorTokens.LightText,
                secondaryContainer = SableColorTokens.LightSurfaceRaised,
                onSecondaryContainer = SableColorTokens.LightText,
                tertiaryContainer = SableColorTokens.LightSurfaceRaised,
                onTertiaryContainer = SableColorTokens.LightText,
                surfaceTint = Color.Transparent,
                inverseSurface = SableColorTokens.LightText,
                inverseOnSurface = SableColorTokens.LightBackground,
                inversePrimary = accent.color,
                surfaceDim = SableColorTokens.LightBackground,
                surfaceBright = SableColorTokens.LightSurfaceRaised,
                surfaceContainerLowest = SableColorTokens.LightBackground,
                surfaceContainerLow = SableColorTokens.LightSurface,
                surfaceContainer = SableColorTokens.LightSurface,
                surfaceContainerHigh = SableColorTokens.LightSurfaceRaised,
                surfaceContainerHighest = SableColorTokens.LightSurfaceRaised,
                error = Color(0xFFBA1A1A),
                onError = Color.White,
            )
        }

    val shapes = remember(appearance.cornerStyle) { sableShapes(appearance.cornerStyle) }

    MaterialTheme(
        colorScheme = colors,
        typography = if (isSableShortWindow()) SableShortTypography else SableTypography,
        shapes = shapes,
        content = content,
    )
}
