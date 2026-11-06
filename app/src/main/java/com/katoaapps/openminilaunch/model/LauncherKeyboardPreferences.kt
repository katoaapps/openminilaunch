package com.katoaapps.openminilaunch.model

import androidx.annotation.StringRes
import com.katoaapps.openminilaunch.R

internal enum class LauncherKeyboardStyle(@param:StringRes val labelRes: Int) {
    FLAT_PRIMARY(R.string.keyboard_style_flat_primary),
    FLAT_SECONDARY(R.string.keyboard_style_flat_secondary),
    RAISED_PRIMARY(R.string.keyboard_style_raised_primary),
    RAISED_SECONDARY(R.string.keyboard_style_raised_secondary),
    OUTLINED_PRIMARY(R.string.keyboard_style_outlined_primary),
    OUTLINED_SECONDARY(R.string.keyboard_style_outlined_secondary),
}

internal enum class LauncherKeyboardLayout(
    @param:StringRes val labelRes: Int,
    val supportsCase: Boolean,
) {
    QWERTY(R.string.keyboard_layout_qwerty, true),
    AZERTY(R.string.keyboard_layout_azerty, true),
    QWERTZ(R.string.keyboard_layout_qwertz, true),
    DVORAK(R.string.keyboard_layout_dvorak, true),
    PINYIN(R.string.keyboard_layout_pinyin, true),
    CANGJIE(R.string.keyboard_layout_cangjie, false),
    HANGUL(R.string.keyboard_layout_hangul, false),
    ABUGIDA(R.string.keyboard_layout_abugida, false),
}

internal enum class LauncherKeyboardColorSource(@param:StringRes val labelRes: Int) {
    THEME(R.string.keyboard_colors_theme),
    CUSTOM(R.string.keyboard_colors_custom),
}

internal data class LauncherKeyboardPreferences(
    val style: LauncherKeyboardStyle = LauncherKeyboardStyle.RAISED_PRIMARY,
    val layout: LauncherKeyboardLayout = LauncherKeyboardLayout.QWERTY,
    val colorSource: LauncherKeyboardColorSource = LauncherKeyboardColorSource.THEME,
    val customPrimaryArgb: Int = DEFAULT_KEYBOARD_PRIMARY_ARGB,
    val customSecondaryArgb: Int = DEFAULT_KEYBOARD_SECONDARY_ARGB,
    val keyColorOverrideArgb: Int? = null,
    val splitOnWideDisplays: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val heightDp: Float = DEFAULT_KEYBOARD_HEIGHT_DP,
)

internal fun normalizeLauncherKeyboardHeight(heightDp: Float): Float =
    heightDp.coerceIn(MIN_KEYBOARD_HEIGHT_DP, MAX_KEYBOARD_HEIGHT_DP)

internal const val MIN_KEYBOARD_HEIGHT_DP = 210f
internal const val MAX_KEYBOARD_HEIGHT_DP = 360f
internal const val DEFAULT_KEYBOARD_HEIGHT_DP = 276f
internal const val DEFAULT_KEYBOARD_PRIMARY_ARGB: Int = 0xFF4B250F.toInt()
internal const val DEFAULT_KEYBOARD_SECONDARY_ARGB: Int = 0xFFFFE9CB.toInt()
