package com.katoaapps.openminilaunch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.Locale

internal data class HomePanelColorPreset(@androidx.annotation.StringRes val labelRes: Int, val argb: Int)

internal fun parseHomePanelHex(value: String): Int? {
    val clean = value.trim().removePrefix("#")
    if (clean.length != 6 || clean.any { it !in "0123456789abcdefABCDEF" }) return null
    return runCatching { clean.toLong(16).toInt() or 0xFF000000.toInt() }.getOrNull()
}

internal fun formatHomePanelHex(argb: Int): String =
    String.format(Locale.US, "#%06X", argb and 0x00FFFFFF)

@Composable
internal fun HomePanelColorSetting(selectedArgb: Int, onColorSelected: (Int) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val presets = listOf(
        HomePanelColorPreset(R.string.color_forest, androidx.core.content.ContextCompat.getColor(context, R.color.mink_forest)),
        HomePanelColorPreset(R.string.color_mink, androidx.core.content.ContextCompat.getColor(context, R.color.home_panel_mink)),
        HomePanelColorPreset(R.string.color_navy, androidx.core.content.ContextCompat.getColor(context, R.color.home_panel_navy)),
        HomePanelColorPreset(R.string.color_plum, androidx.core.content.ContextCompat.getColor(context, R.color.home_panel_plum)),
        HomePanelColorPreset(R.string.color_charcoal, androidx.core.content.ContextCompat.getColor(context, R.color.home_panel_charcoal)),
    )
    AppearanceColorSetting(
        selectedArgb = selectedArgb,
        pickerArgb = selectedArgb,
        titleRes = R.string.home_panel_color,
        descriptionRes = R.string.home_panel_color_description,
        customTitleRes = R.string.custom_panel_color,
        customDescriptionRes = R.string.custom_panel_color_description,
        presets = presets,
        onColorSelected = onColorSelected,
    )
}

@Composable
internal fun AppBackgroundColorSetting(
    selectedArgb: Int?,
    onColorSelected: (Int) -> Unit,
    onUseThemeDefault: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val presets = listOf(
        HomePanelColorPreset(R.string.color_paper, androidx.core.content.ContextCompat.getColor(context, R.color.app_background_paper)),
        HomePanelColorPreset(R.string.color_midnight, androidx.core.content.ContextCompat.getColor(context, R.color.app_background_midnight)),
        HomePanelColorPreset(R.string.color_cream, androidx.core.content.ContextCompat.getColor(context, R.color.app_background_cream)),
        HomePanelColorPreset(R.string.color_soft_sage, androidx.core.content.ContextCompat.getColor(context, R.color.app_background_sage)),
        HomePanelColorPreset(R.string.color_slate, androidx.core.content.ContextCompat.getColor(context, R.color.app_background_slate)),
    )
    AppearanceColorSetting(
        selectedArgb = selectedArgb,
        pickerArgb = selectedArgb ?: MaterialTheme.colorScheme.background.toArgb(),
        titleRes = R.string.app_background_color,
        descriptionRes = R.string.app_background_color_description,
        customTitleRes = R.string.custom_background_color,
        customDescriptionRes = R.string.custom_background_color_description,
        presets = presets,
        onColorSelected = onColorSelected,
        onUseThemeDefault = onUseThemeDefault,
    )
}

@Composable
private fun AppearanceColorSetting(
    selectedArgb: Int?,
    pickerArgb: Int,
    @androidx.annotation.StringRes titleRes: Int,
    @androidx.annotation.StringRes descriptionRes: Int,
    @androidx.annotation.StringRes customTitleRes: Int,
    @androidx.annotation.StringRes customDescriptionRes: Int,
    presets: List<HomePanelColorPreset>,
    onColorSelected: (Int) -> Unit,
    onUseThemeDefault: (() -> Unit)? = null,
) {
    var showCustomPicker by remember { mutableStateOf(false) }
    val selectedPreset = presets.firstOrNull { it.argb == selectedArgb }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.dp16),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(Dimens.dp14), verticalArrangement = Arrangement.spacedBy(Dimens.dp10)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.onSurface)
                Column(Modifier.padding(start = Dimens.dp12).weight(1f)) {
                    Text(stringResource(titleRes), fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(descriptionRes),
                        color = Muted,
                        fontSize = Dimens.sp12,
                    )
                }
                Text(
                    selectedArgb?.let(::formatHomePanelHex) ?: stringResource(R.string.theme_background),
                    color = Muted,
                    fontSize = Dimens.sp12,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                presets.forEach { preset ->
                    val selected = preset.argb == selectedArgb
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            onClick = { onColorSelected(preset.argb) },
                            modifier = Modifier.size(Dimens.dp42)
                                .then(
                                    if (selected) Modifier.border(Dimens.dp3, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier,
                                ),
                            shape = CircleShape,
                            color = Color(preset.argb),
                            shadowElevation = if (selected) Dimens.dp4 else Dimens.dp0,
                        ) {
                            if (selected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Check,
                                        stringResource(preset.labelRes),
                                        tint = readableContentColor(Color(preset.argb)),
                                    )
                                }
                            }
                        }
                        Text(stringResource(preset.labelRes), color = Muted, fontSize = Dimens.sp9, modifier = Modifier.padding(top = Dimens.dp4))
                    }
                }
            }
            OutlinedButton(onClick = { showCustomPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Palette, null, Modifier.size(Dimens.dp18))
                Text(
                    stringResource(if (selectedPreset == null) R.string.edit_custom_color else R.string.choose_custom_color),
                    Modifier.padding(start = Dimens.dp8),
                )
            }
            onUseThemeDefault?.let { useThemeDefault ->
                TextButton(
                    onClick = useThemeDefault,
                    enabled = selectedArgb != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.use_theme_background))
                }
            }
        }
    }

    if (showCustomPicker) {
        AppearanceColorDialog(
            initialArgb = pickerArgb,
            titleRes = customTitleRes,
            descriptionRes = customDescriptionRes,
            onConfirm = {
                onColorSelected(it)
                showCustomPicker = false
            },
            onDismiss = { showCustomPicker = false },
        )
    }
}

@Composable
private fun AppearanceColorDialog(
    initialArgb: Int,
    @androidx.annotation.StringRes titleRes: Int,
    @androidx.annotation.StringRes descriptionRes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialHsv = remember(initialArgb) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialArgb, it) }
    }
    var hue by remember(initialArgb) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(initialArgb) { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember(initialArgb) { mutableFloatStateOf(initialHsv[2]) }
    var hexText by remember(initialArgb) { mutableStateOf(formatHomePanelHex(initialArgb)) }
    val currentArgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness))

    fun syncHex() {
        hexText = formatHomePanelHex(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = Dimens.dp620),
            shape = RoundedCornerShape(Dimens.dp28),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                Modifier.padding(Dimens.dp20).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.dp14),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(Dimens.dp42), shape = CircleShape, color = Color(currentArgb)) {}
                    Column(Modifier.padding(start = Dimens.dp12).weight(1f)) {
                        Text(stringResource(titleRes), fontWeight = FontWeight.Black, fontSize = Dimens.sp20)
                        Text(stringResource(descriptionRes), color = Muted, fontSize = Dimens.sp12)
                    }
                }
                SaturationBrightnessField(
                    hue = hue,
                    saturation = saturation,
                    brightness = brightness,
                    onChange = { newSaturation, newBrightness ->
                        saturation = newSaturation
                        brightness = newBrightness
                        syncHex()
                    },
                )
                HueStrip(hue) { newHue ->
                    hue = newHue
                    syncHex()
                }
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { entered ->
                        val filtered = entered.uppercase().filterIndexed { index, char ->
                            (index == 0 && char == '#') || char in '0'..'9' || char in 'A'..'F'
                        }.take(7)
                        hexText = filtered
                        parseHomePanelHex(filtered)?.let { parsed ->
                            val hsv = FloatArray(3)
                            android.graphics.Color.colorToHSV(parsed, hsv)
                            hue = hsv[0]
                            saturation = hsv[1]
                            brightness = hsv[2]
                        }
                    },
                    label = { Text(stringResource(R.string.hex_color)) },
                    supportingText = {
                        if (parseHomePanelHex(hexText) == null) Text(stringResource(R.string.invalid_hex_color))
                    },
                    isError = parseHomePanelHex(hexText) == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Spacer(Modifier.size(Dimens.dp8))
                    Button(
                        onClick = { onConfirm(currentArgb) },
                        enabled = parseHomePanelHex(hexText) != null,
                    ) { Text(stringResource(R.string.apply)) }
                }
            }
        }
    }
}

@Composable
private fun SaturationBrightnessField(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onChange: (Float, Float) -> Unit,
) {
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }
    val fieldCornerRadius = Dimens.dp18
    val outerMarkerRadius = Dimens.dp11
    val innerMarkerRadius = Dimens.dp9
    val outerMarkerStroke = Dimens.dp1
    val innerMarkerStroke = Dimens.dp3
    val white = MinkWhite
    val black = MinkBlack
    val transparent = MinkTransparent

    fun update(position: Offset) {
        if (fieldSize.width <= 0 || fieldSize.height <= 0) return
        onChange(
            (position.x / fieldSize.width).coerceIn(0f, 1f),
            (1f - position.y / fieldSize.height).coerceIn(0f, 1f),
        )
    }

    Canvas(
        Modifier.fillMaxWidth().height(Dimens.dp180).onSizeChanged { fieldSize = it }
            .pointerInput(hue, fieldSize) {
                detectDragGestures(
                    onDragStart = ::update,
                    onDrag = { change, _ -> update(change.position) },
                )
            },
    ) {
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(white, Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))))),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(fieldCornerRadius.toPx()),
        )
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(transparent, black)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(fieldCornerRadius.toPx()),
        )
        val marker = Offset(saturation * size.width, (1f - brightness) * size.height)
        drawCircle(white, radius = innerMarkerRadius.toPx(), center = marker, style = Stroke(innerMarkerStroke.toPx()))
        drawCircle(black.copy(alpha = .55f), radius = outerMarkerRadius.toPx(), center = marker, style = Stroke(outerMarkerStroke.toPx()))
    }
}

@Composable
private fun HueStrip(hue: Float, onChange: (Float) -> Unit) {
    var stripSize by remember { mutableStateOf(IntSize.Zero) }
    val outerMarkerRadius = Dimens.dp10
    val innerMarkerRadius = Dimens.dp8
    val outerMarkerStroke = Dimens.dp1
    val innerMarkerStroke = Dimens.dp3
    val white = MinkWhite
    val black = MinkBlack
    val hueColors = remember {
        (0..6).map { step -> Color(android.graphics.Color.HSVToColor(floatArrayOf(step * 60f, 1f, 1f))) }
    }

    fun update(position: Offset) {
        if (stripSize.width <= 0) return
        onChange((position.x / stripSize.width * 360f).coerceIn(0f, 359.99f))
    }

    Canvas(
        Modifier.fillMaxWidth().height(Dimens.dp28).onSizeChanged { stripSize = it }
            .pointerInput(stripSize) {
                detectDragGestures(
                    onDragStart = ::update,
                    onDrag = { change, _ -> update(change.position) },
                )
            },
    ) {
        drawRoundRect(
            brush = Brush.horizontalGradient(hueColors),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
        )
        val marker = Offset(hue / 360f * size.width, size.height / 2f)
        drawCircle(white, radius = innerMarkerRadius.toPx(), center = marker, style = Stroke(innerMarkerStroke.toPx()))
        drawCircle(black.copy(alpha = .55f), radius = outerMarkerRadius.toPx(), center = marker, style = Stroke(outerMarkerStroke.toPx()))
    }
}
