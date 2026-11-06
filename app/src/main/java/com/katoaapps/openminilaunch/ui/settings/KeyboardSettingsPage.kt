package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.model.LauncherKeyboardColorSource
import com.katoaapps.openminilaunch.model.LauncherKeyboardLayout
import com.katoaapps.openminilaunch.model.LauncherKeyboardStyle
import com.katoaapps.openminilaunch.model.launcherKeyboardStyleOptions
import com.katoaapps.openminilaunch.model.MAX_KEYBOARD_HEIGHT_DP
import com.katoaapps.openminilaunch.model.MIN_KEYBOARD_HEIGHT_DP
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.components.SettingsSwitchRow
import com.katoaapps.openminilaunch.ui.keyboard.resolveLauncherKeyboardColors
import com.katoaapps.openminilaunch.ui.keyboard.KeyboardChoiceGrid
import com.katoaapps.openminilaunch.ui.keyboard.KeyboardDevicePreview
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.readableContentColor
import kotlin.math.roundToInt

@Composable
internal fun KeyboardSettingsPage(store: LauncherStore, goBack: () -> Unit) {
    val preferences = store.launcherKeyboardPreferences
    var previewValue by remember { mutableStateOf(TextFieldValue("Mink makes every screen feel familiar.")) }
    var primaryPicker by remember { mutableStateOf(false) }
    var secondaryPicker by remember { mutableStateOf(false) }
    var keyColorPicker by remember { mutableStateOf(false) }
    var previewHeight by remember(preferences.heightDp) { mutableFloatStateOf(preferences.heightDp) }
    val themeSecondary = store.visibleAppBackgroundColorArgb
        ?: store.effectiveAppBackgroundColorArgb
        ?: 0xFFFFE9CB.toInt()
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
            preferences = preferences.copy(heightDp = previewHeight),
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
                KeyboardColorButton(
                    stringResource(R.string.keyboard_primary_color),
                    Color(preferences.customPrimaryArgb),
                    { primaryPicker = true },
                    Modifier.weight(1f),
                )
                KeyboardColorButton(
                    stringResource(R.string.keyboard_secondary_color),
                    Color(preferences.customSecondaryArgb),
                    { secondaryPicker = true },
                    Modifier.weight(1f),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
        ) {
            KeyboardColorButton(
                stringResource(R.string.keyboard_key_color),
                resolvedColors.key,
                { keyColorPicker = true },
                Modifier.weight(1f),
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
        Text(stringResource(R.string.keyboard_height_value, previewHeight.roundToInt()), fontWeight = FontWeight.Bold)
        Slider(
            value = previewHeight,
            onValueChange = { previewHeight = it },
            onValueChangeFinished = {
                store.updateLauncherKeyboardPreferences(preferences.copy(heightDp = previewHeight))
            },
            valueRange = MIN_KEYBOARD_HEIGHT_DP..MAX_KEYBOARD_HEIGHT_DP,
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

@Composable
private fun ChoiceStrip(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
        content = content,
    )
}

@Composable
private fun KeyboardColorButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = color,
        contentColor = readableContentColor(color),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(Dimens.dp14),
    ) {
        Text(label, Modifier.padding(Dimens.dp14), fontWeight = FontWeight.Bold)
    }
}
