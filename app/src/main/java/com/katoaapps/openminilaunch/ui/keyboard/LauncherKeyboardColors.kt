package com.katoaapps.openminilaunch.ui.keyboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.katoaapps.openminilaunch.model.LauncherKeyboardColorSource
import com.katoaapps.openminilaunch.model.LauncherKeyboardPreferences
import com.katoaapps.openminilaunch.model.LauncherKeyboardStyle
import com.katoaapps.openminilaunch.ui.theme.MinkWhite
import com.katoaapps.openminilaunch.ui.theme.ensureReadableContentColor
import com.katoaapps.openminilaunch.ui.theme.readableContentColor

internal data class LauncherKeyboardColors(
    val board: Color,
    val key: Color,
    val content: Color,
    val outline: Color,
)

internal fun resolveLauncherKeyboardColors(
    preferences: LauncherKeyboardPreferences,
    themePrimaryArgb: Int,
    themeSecondaryArgb: Int,
): LauncherKeyboardColors {
    val primary = Color(
        if (preferences.colorSource == LauncherKeyboardColorSource.CUSTOM) {
            preferences.customPrimaryArgb
        } else {
            themePrimaryArgb
        },
    )
    val secondary = Color(
        if (preferences.colorSource == LauncherKeyboardColorSource.CUSTOM) {
            preferences.customSecondaryArgb
        } else {
            themeSecondaryArgb
        },
    )
    val primaryKeys = preferences.style in setOf(
        LauncherKeyboardStyle.FLAT_PRIMARY,
        LauncherKeyboardStyle.RAISED_PRIMARY,
        LauncherKeyboardStyle.OUTLINED_PRIMARY,
    )
    val paletteDominant = if (primaryKeys) primary else secondary
    val dominant = preferences.keyColorOverrideArgb?.let(::Color) ?: paletteDominant
    val contrast = if (primaryKeys) secondary else primary
    val outlined = preferences.style == LauncherKeyboardStyle.OUTLINED_PRIMARY ||
        preferences.style == LauncherKeyboardStyle.OUTLINED_SECONDARY
    return LauncherKeyboardColors(
        board = if (outlined || preferences.style.isFlatKeyboardStyle) dominant else contrast,
        key = dominant,
        content = ensureReadableContentColor(dominant, contrast),
        outline = ensureReadableContentColor(dominant, contrast, minimumContrast = 3f),
    )
}

@Composable
internal fun LauncherKeyboardColors.forAction(
    actionColor: Color,
    contentColor: Color? = null,
): LauncherKeyboardColors {
    val resolvedContentColor = contentColor ?: readableContentColor(actionColor)
    return copy(
        key = actionColor,
        content = resolvedContentColor,
        outline = MinkWhite,
    )
}
