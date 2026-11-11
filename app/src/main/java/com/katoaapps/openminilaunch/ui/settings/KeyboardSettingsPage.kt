package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.model.LauncherKeyboardColorSource
import com.katoaapps.openminilaunch.model.LauncherKeyboardHeightPreset
import com.katoaapps.openminilaunch.model.LauncherKeyboardLayout
import com.katoaapps.openminilaunch.model.LauncherKeyboardStyle
import com.katoaapps.openminilaunch.model.launcherKeyboardStyleOptions
import com.katoaapps.openminilaunch.model.resolveLauncherKeyboardHeightDp
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.components.SettingsSwitchRow
import com.katoaapps.openminilaunch.ui.keyboard.resolveLauncherKeyboardColors
import com.katoaapps.openminilaunch.ui.keyboard.KeyboardChoiceGrid
import com.katoaapps.openminilaunch.ui.keyboard.KeyboardDevicePreview
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import kotlin.math.roundToInt

@Composable
internal fun KeyboardSettingsPage(store: LauncherStore, goBack: () -> Unit) {
    val preferences = store.launcherKeyboardPreferences
    val configuration = LocalConfiguration.current
    var previewValue by remember { mutableStateOf(TextFieldValue("Mink makes every screen feel familiar.")) }
    var primaryPicker by remember { mutableStateOf(false) }
    var secondaryPicker by remember { mutableStateOf(false) }
    var keyColorPicker by remember { mutableStateOf(false) }
    var previewHeightPreset by remember(preferences.portraitHeightPreset) {
        mutableStateOf(preferences.portraitHeightPreset)
    }
    val previewPreferences = preferences.copy(portraitHeightPreset = previewHeightPreset)
    val previewHeight = resolveLauncherKeyboardHeightDp(
        preferences = previewPreferences,
        screenWidthDp = configuration.screenWidthDp,
        screenHeightDp = configuration.screenHeightDp,
    )
    val themeSecondary = store.effectiveThemeSecondaryColorArgb
    val resolvedColors = resolveLauncherKeyboardColors(
        preferences = preferences,
        themePrimaryArgb = store.effectiveHomePanelColorArgb,
        themeSecondaryArgb = themeSecondary,
    )

    SettingsPage(stringResource(R.string.launcher_keyboard), goBack) {
        SettingsSwitchRow(
            title = stringResource(R.string.open_keyboard_on_home),
            subtitle = stringResource(R.string.open_keyboard_on_home_description),
            checked = store.openSoftwareKeyboardOnHome,
            onCheckedChange = store::updateOpenSoftwareKeyboardOnHome,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.keyboard_split_wide),
            subtitle = stringResource(R.string.keyboard_split_wide_description),
            checked = preferences.splitOnWideDisplays,
            onCheckedChange = { store.updateLauncherKeyboardPreferences(preferences.copy(splitOnWideDisplays = it)) },
        )
        SettingsSwitchRow(
            title = stringResource(R.string.keyboard_haptic_feedback),
            subtitle = stringResource(R.string.keyboard_haptic_feedback_description),
            checked = preferences.hapticFeedbackEnabled,
            onCheckedChange = { store.updateLauncherKeyboardPreferences(preferences.copy(hapticFeedbackEnabled = it)) },
        )

        SectionLabel(stringResource(R.string.keyboard_live_preview))
        KeyboardDevicePreview(
            value = previewValue,
            onValueChange = { previewValue = it },
            preferences = previewPreferences,
            themePrimaryArgb = store.effectiveHomePanelColorArgb,
            themeSecondaryArgb = themeSecondary,
        )

        SectionLabel(stringResource(R.string.keyboard_layout))
        KeyboardChoiceGrid(
            options = LauncherKeyboardLayout.entries,
            columns = 4,
            selected = preferences.layout,
            label = { layout -> stringResource(layout.labelRes) },
            onSelect = { layout ->
                store.updateLauncherKeyboardPreferences(preferences.copy(layout = layout))
            },
        )

        SectionLabel(stringResource(R.string.keyboard_style))
        Text(stringResource(R.string.keyboard_theme_colors_description), color = Muted, fontSize = Dimens.sp12)
        KeyboardChoiceGrid(
            options = launcherKeyboardStyleOptions,
            columns = 3,
            selected = preferences.style,
            label = { style -> stringResource(style.labelRes) },
            onSelect = { style ->
                store.updateLauncherKeyboardPreferences(preferences.copy(style = style))
            },
        )
        ChoiceStrip {
            LauncherKeyboardColorSource.entries.forEach { source ->
                FilterChip(
                    selected = preferences.colorSource == source,
                    onClick = { store.updateLauncherKeyboardPreferences(preferences.copy(colorSource = source)) },
                    label = { Text(stringResource(source.labelRes)) },
                )
            }
        }
        if (preferences.colorSource == LauncherKeyboardColorSource.CUSTOM) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                SettingsColorPickerButton(
                    label = stringResource(R.string.keyboard_primary_color),
                    color = Color(preferences.customPrimaryArgb),
                    onClick = { primaryPicker = true },
                    modifier = Modifier.weight(1f),
                )
                SettingsColorPickerButton(
                    label = stringResource(R.string.keyboard_secondary_color),
                    color = Color(preferences.customSecondaryArgb),
                    onClick = { secondaryPicker = true },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
        ) {
            SettingsColorPickerButton(
                label = stringResource(R.string.keyboard_key_color),
                color = resolvedColors.key,
                onClick = { keyColorPicker = true },
                modifier = Modifier.weight(1f),
            )
            if (preferences.keyColorOverrideArgb != null) {
                TextButton(
                    onClick = {
                        store.updateLauncherKeyboardPreferences(
                            preferences.copy(keyColorOverrideArgb = null),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.keyboard_use_palette_key_color))
                }
            }
        }

        SectionLabel(stringResource(R.string.keyboard_height))
        Text(
            "${(previewHeightPreset.portraitFraction * 100).roundToInt()}% · " +
                stringResource(R.string.keyboard_height_value, previewHeight.roundToInt()),
            fontWeight = FontWeight.Bold,
        )
        KeyboardHeightSlider(
            selectedPreset = previewHeightPreset,
            onPresetChange = { previewHeightPreset = it },
            onValueChangeFinished = {
                store.updateLauncherKeyboardPreferences(
                    preferences.copy(portraitHeightPreset = previewHeightPreset),
                )
            },
        )

    }

    if (primaryPicker) {
        AppearanceColorDialog(
            initialArgb = preferences.customPrimaryArgb,
            titleRes = R.string.keyboard_primary_color,
            descriptionRes = R.string.keyboard_primary_color_description,
            colorHotphrases = emptyMap(),
            onConfirm = { _, color ->
                store.updateLauncherKeyboardPreferences(preferences.copy(customPrimaryArgb = color))
                primaryPicker = false
            },
            onDismiss = { primaryPicker = false },
        )
    }
    if (secondaryPicker) {
        AppearanceColorDialog(
            initialArgb = preferences.customSecondaryArgb,
            titleRes = R.string.keyboard_secondary_color,
            descriptionRes = R.string.keyboard_secondary_color_description,
            colorHotphrases = emptyMap(),
            onConfirm = { _, color ->
                store.updateLauncherKeyboardPreferences(preferences.copy(customSecondaryArgb = color))
                secondaryPicker = false
            },
            onDismiss = { secondaryPicker = false },
        )
    }
    if (keyColorPicker) {
        AppearanceColorDialog(
            initialArgb = preferences.keyColorOverrideArgb ?: resolvedColors.key.toArgb(),
            titleRes = R.string.keyboard_key_color,
            descriptionRes = R.string.keyboard_key_color_description,
            colorHotphrases = emptyMap(),
            onConfirm = { _, color ->
                store.updateLauncherKeyboardPreferences(
                    preferences.copy(keyColorOverrideArgb = color),
                )
                keyColorPicker = false
            },
            onDismiss = { keyColorPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyboardHeightSlider(
    selectedPreset: LauncherKeyboardHeightPreset,
    onPresetChange: (LauncherKeyboardHeightPreset) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column {
        Slider(
            value = selectedPreset.portraitFraction,
            onValueChange = { fraction ->
                onPresetChange(LauncherKeyboardHeightPreset.nearest(fraction))
            },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = LauncherKeyboardHeightPreset.COMPACT.portraitFraction..
                LauncherKeyboardHeightPreset.TALL.portraitFraction,
            steps = 1,
            interactionSource = interactionSource,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    thumbSize = DpSize(width = 4.dp, height = 22.dp),
                )
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LauncherKeyboardHeightPreset.entries.forEach { preset ->
                Text(
                    text = "${(preset.portraitFraction * 100).roundToInt()}%",
                    color = Muted,
                    fontSize = Dimens.sp12,
                )
            }
        }
    }
}

@Composable
private fun ChoiceStrip(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
        content = content,
    )
}
