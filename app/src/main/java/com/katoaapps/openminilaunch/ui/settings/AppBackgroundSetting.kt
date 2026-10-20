package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.katoaapps.openminilaunch.R

@Composable
internal fun AppBackgroundSetting(
    selectedArgb: Int?,
    activeWallpaperLabel: String?,
    onColorSelected: (Int) -> Unit,
    onUseThemeDefault: () -> Unit,
) {
    val context = LocalContext.current
    val presets = listOf(
        HomePanelColorPreset(R.string.color_paper, androidx.core.content.ContextCompat.getColor(context, R.color.app_background_paper)),
        HomePanelColorPreset(R.string.color_midnight, androidx.core.content.ContextCompat.getColor(context, R.color.app_background_midnight)),
        HomePanelColorPreset(R.string.color_cream, androidx.core.content.ContextCompat.getColor(context, R.color.app_background_cream)),
        HomePanelColorPreset(R.string.color_slate, androidx.core.content.ContextCompat.getColor(context, R.color.app_background_slate)),
    )
    AppearanceColorSetting(
        selectedArgb = selectedArgb.takeUnless { activeWallpaperLabel != null },
        pickerArgb = selectedArgb ?: MaterialTheme.colorScheme.background.toArgb(),
        titleRes = R.string.app_background_color,
        descriptionRes = R.string.app_background_color_description,
        customTitleRes = R.string.custom_background_color,
        customDescriptionRes = R.string.custom_background_color_description,
        presets = presets,
        onColorSelected = onColorSelected,
        selectedValueLabel = activeWallpaperLabel,
        onUseThemeDefault = onUseThemeDefault,
    )
}
