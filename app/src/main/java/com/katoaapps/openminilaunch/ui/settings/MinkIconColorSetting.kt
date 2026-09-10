package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore

/** Color controls shown only while Mink's monochrome icon treatment is selected. */
@Composable
internal fun MinkIconColorSetting(store: LauncherStore) {
    val context = LocalContext.current
    val selectedColor = store.iconAppearance.minkIconColorArgb
    val presets = listOf(
        HomePanelColorPreset(
            R.string.color_forest,
            androidx.core.content.ContextCompat.getColor(context, R.color.mink_forest),
        ),
        HomePanelColorPreset(
            R.string.color_mink,
            androidx.core.content.ContextCompat.getColor(context, R.color.home_panel_mink),
        ),
        HomePanelColorPreset(
            R.string.color_navy,
            androidx.core.content.ContextCompat.getColor(context, R.color.home_panel_navy),
        ),
        HomePanelColorPreset(
            R.string.color_plum,
            androidx.core.content.ContextCompat.getColor(context, R.color.home_panel_plum),
        ),
        HomePanelColorPreset(
            R.string.color_rust,
            androidx.core.content.ContextCompat.getColor(context, R.color.rust),
        ),
    )

    AppearanceColorSetting(
        selectedArgb = selectedColor,
        pickerArgb = selectedColor ?: MaterialTheme.colorScheme.primary.toArgb(),
        titleRes = R.string.mink_icon_color,
        descriptionRes = R.string.mink_icon_color_description,
        customTitleRes = R.string.custom_mink_icon_color,
        customDescriptionRes = R.string.custom_mink_icon_color_description,
        presets = presets,
        onColorSelected = store::setMinkIconColor,
        onUseThemeDefault = { store.setMinkIconColor(null) },
        defaultValueLabelRes = R.string.automatic,
        useDefaultLabelRes = R.string.use_automatic_icon_color,
    )
}
