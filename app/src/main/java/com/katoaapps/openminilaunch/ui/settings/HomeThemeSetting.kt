package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.appearance.BundledHomeTheme
import com.katoaapps.openminilaunch.features.appearance.WATERMELON_HOTPHRASE
import com.katoaapps.openminilaunch.features.appearance.WATERMELON_PILL_COLOR_ARGB
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun HomeThemeSetting(
    selectedThemeId: String?,
    applyingThemeId: String?,
    themeBackgroundSelected: Boolean,
    deviceWallpaperSelected: Boolean,
    savedDeviceWallpaperAvailable: Boolean,
    savedDeviceWallpaperRevision: Int,
    applyingWallpaperSourceId: String?,
    customBackgroundArgb: Int,
    customPanelArgb: Int,
    panelTransparency: Float,
    onUseThemeBackground: () -> Unit,
    onUseDeviceWallpaper: () -> Unit,
    onChoosePhoto: () -> Unit,
    onSelectTheme: (BundledHomeTheme) -> Unit,
    onCustomBackgroundSelected: (Int) -> Unit,
    onCustomPanelSelected: (String, Int) -> Unit,
    onTransparencyChanged: (Float) -> Unit,
) {
    var editingColor by remember { mutableStateOf<CustomThemeColor?>(null) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.dp16),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.padding(Dimens.dp14),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp10),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.onSurface)
                Column(Modifier.padding(start = Dimens.dp12)) {
                    Text(stringResource(R.string.home_themes), fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.home_themes_description),
                        color = Muted,
                        fontSize = Dimens.sp12,
                    )
                }
            }
            WallpaperSourceChoices(
                themeSelected = themeBackgroundSelected,
                deviceSelected = deviceWallpaperSelected,
                savedDeviceWallpaperAvailable = savedDeviceWallpaperAvailable,
                savedDeviceWallpaperRevision = savedDeviceWallpaperRevision,
                applyingSourceId = applyingWallpaperSourceId,
                enabled = applyingThemeId == null && applyingWallpaperSourceId == null,
                onUseTheme = onUseThemeBackground,
                onUseDevice = onUseDeviceWallpaper,
                onChoosePhoto = onChoosePhoto,
            )
            HorizontalDivider()
            HomeThemeGallery(
                selectedThemeId = selectedThemeId,
                applyingThemeId = applyingThemeId,
                onSelectTheme = onSelectTheme,
            )
            HorizontalDivider()
            CustomThemeEditor(
                customBackgroundArgb = customBackgroundArgb,
                customPanelArgb = customPanelArgb,
                onEditBackground = { editingColor = CustomThemeColor.Background },
                onEditPanel = { editingColor = CustomThemeColor.Panel },
            )
            HorizontalDivider()
            PillTransparencyControl(
                transparency = panelTransparency,
                onTransparencyChanged = onTransparencyChanged,
            )
        }
    }

    editingColor?.let { color ->
        val editingPanel = color == CustomThemeColor.Panel
        AppearanceColorDialog(
            initialArgb = if (editingPanel) customPanelArgb else customBackgroundArgb,
            titleRes = if (editingPanel) R.string.custom_panel_color else R.string.custom_background_color,
            descriptionRes = if (editingPanel) {
                R.string.custom_panel_color_description
            } else {
                R.string.custom_background_color_description
            },
            colorHotphrases = if (editingPanel) {
                mapOf(WATERMELON_HOTPHRASE to WATERMELON_PILL_COLOR_ARGB)
            } else {
                emptyMap()
            },
            onConfirm = { input, argb ->
                if (editingPanel) onCustomPanelSelected(input, argb)
                else onCustomBackgroundSelected(argb)
                editingColor = null
            },
            onDismiss = { editingColor = null },
        )
    }
}

@Composable
private fun CustomThemeEditor(
    customBackgroundArgb: Int,
    customPanelArgb: Int,
    onEditBackground: () -> Unit,
    onEditPanel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp10)) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp2)) {
            Text(stringResource(R.string.custom_theme), fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.custom_theme_description),
                color = Muted,
                fontSize = Dimens.sp12,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
            SettingsColorPickerButton(
                label = stringResource(R.string.app_background_color),
                color = Color(customBackgroundArgb),
                onClick = onEditBackground,
                modifier = Modifier.weight(1f),
            )
            SettingsColorPickerButton(
                label = stringResource(R.string.home_panel_color),
                color = Color(customPanelArgb),
                onClick = onEditPanel,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private enum class CustomThemeColor { Background, Panel }
