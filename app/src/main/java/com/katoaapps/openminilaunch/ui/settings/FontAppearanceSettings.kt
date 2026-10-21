package com.katoaapps.openminilaunch.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.model.FONT_SIZE_SCALE_STEPS
import com.katoaapps.openminilaunch.model.normalizeFontSizeScale
import com.katoaapps.openminilaunch.ui.components.SettingsRow
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MinkBlack
import com.katoaapps.openminilaunch.ui.theme.MinkWhite
import com.katoaapps.openminilaunch.ui.theme.Muted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
internal fun FontAppearanceSettings(store: LauncherStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showFontPicker by remember { mutableStateOf(false) }
    var sliderScale by remember(store.fontAppearance.sizeScale) {
        mutableFloatStateOf(store.fontAppearance.sizeScale)
    }
    val importFailed = stringResource(R.string.custom_font_import_failed)
    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) { store.importCustomFont(uri) }
            if (result.isSuccess) {
                showFontPicker = true
            } else {
                Toast.makeText(context, importFailed, Toast.LENGTH_LONG).show()
            }
        }
    }

    SettingsRow(
        title = stringResource(R.string.font_style),
        subtitle = fontSourceLabel(store.fontAppearance.source, store.fontAppearance.customFontName),
        icon = Icons.Default.FontDownload,
        onClick = { showFontPicker = true },
    )

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
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.font_size), fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.font_size_description),
                        color = Muted,
                        fontSize = Dimens.sp12,
                    )
                }
                Text(
                    stringResource(R.string.font_size_percent, (sliderScale * 100).roundToInt()),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                )
            }
            Slider(
                value = FONT_SIZE_SCALE_STEPS.indexOf(normalizeFontSizeScale(sliderScale)).toFloat(),
                onValueChange = { index ->
                    val nextScale = FONT_SIZE_SCALE_STEPS[index.roundToInt()]
                    if (sliderScale != nextScale) {
                        sliderScale = nextScale
                        store.setFontSizeScale(nextScale)
                    }
                },
                valueRange = 0f..FONT_SIZE_SCALE_STEPS.lastIndex.toFloat(),
                steps = FONT_SIZE_SCALE_STEPS.size - 2,
            )
        }
    }

    FontColorSetting(store)

    if (showFontPicker) {
        FontPickerDialog(
            store = store,
            onImport = {
                fontPicker.launch(
                    arrayOf(
                        "font/ttf",
                        "font/otf",
                        "application/x-font-ttf",
                        "application/x-font-opentype",
                        "application/octet-stream",
                    ),
                )
            },
            onDismiss = { showFontPicker = false },
        )
    }
}

@Composable
private fun FontColorSetting(store: LauncherStore) {
    val context = LocalContext.current
    val selected = store.fontAppearance.colorArgb
    val presets = listOf(
        HomePanelColorPreset(R.string.color_white, MinkWhite.toArgb()),
        HomePanelColorPreset(R.string.color_black, MinkBlack.toArgb()),
        HomePanelColorPreset(R.string.color_forest, androidx.core.content.ContextCompat.getColor(context, R.color.mink_forest)),
        HomePanelColorPreset(R.string.color_mink, androidx.core.content.ContextCompat.getColor(context, R.color.home_panel_mink)),
        HomePanelColorPreset(R.string.color_rust, androidx.core.content.ContextCompat.getColor(context, R.color.rust)),
    )
    AppearanceColorSetting(
        selectedArgb = selected,
        pickerArgb = selected ?: MaterialTheme.colorScheme.onBackground.toArgb(),
        titleRes = R.string.font_color,
        descriptionRes = R.string.font_color_description,
        customTitleRes = R.string.custom_font_color,
        customDescriptionRes = R.string.custom_font_color_description,
        presets = presets,
        onColorSelected = store::setFontColor,
        onUseThemeDefault = { store.setFontColor(null) },
        defaultValueLabelRes = R.string.automatic,
        useDefaultLabelRes = R.string.use_automatic_font_color,
    )
}

