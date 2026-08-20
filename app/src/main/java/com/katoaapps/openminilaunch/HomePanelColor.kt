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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.Locale

internal data class HomePanelColorPreset(val label: String, val argb: Int)

internal val HOME_PANEL_COLOR_PRESETS = listOf(
    HomePanelColorPreset("Forest", DEFAULT_HOME_PANEL_COLOR_ARGB),
    HomePanelColorPreset("Mink", 0xFF602C00.toInt()),
    HomePanelColorPreset("Navy", 0xFF183A5A.toInt()),
    HomePanelColorPreset("Plum", 0xFF512B58.toInt()),
    HomePanelColorPreset("Charcoal", 0xFF34383B.toInt()),
)

internal fun parseHomePanelHex(value: String): Int? {
    val clean = value.trim().removePrefix("#")
    if (clean.length != 6 || clean.any { it !in "0123456789abcdefABCDEF" }) return null
    return runCatching { clean.toLong(16).toInt() or 0xFF000000.toInt() }.getOrNull()
}

internal fun formatHomePanelHex(argb: Int): String =
    String.format(Locale.US, "#%06X", argb and 0x00FFFFFF)

@Composable
internal fun HomePanelColorSetting(selectedArgb: Int, onColorSelected: (Int) -> Unit) {
    var showCustomPicker by remember { mutableStateOf(false) }
    val selectedPreset = HOME_PANEL_COLOR_PRESETS.firstOrNull { it.argb == selectedArgb }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.onSurface)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text("Home panel color", fontWeight = FontWeight.Bold)
                    Text(
                        "Changes the large To-do and shortcuts pill.",
                        color = Muted,
                        fontSize = 12.sp,
                    )
                }
                Text(formatHomePanelHex(selectedArgb), color = Muted, fontSize = 12.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HOME_PANEL_COLOR_PRESETS.forEach { preset ->
                    val selected = preset.argb == selectedArgb
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            onClick = { onColorSelected(preset.argb) },
                            modifier = Modifier.size(42.dp)
                                .then(
                                    if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier,
                                ),
                            shape = CircleShape,
                            color = Color(preset.argb),
                            shadowElevation = if (selected) 4.dp else 0.dp,
                        ) {
                            if (selected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Check, preset.label, tint = Color.White)
                                }
                            }
                        }
                        Text(preset.label, color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            OutlinedButton(onClick = { showCustomPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Palette, null, Modifier.size(18.dp))
                Text(
                    if (selectedPreset == null) "Edit custom color" else "Choose a custom color",
                    Modifier.padding(start = 8.dp),
                )
            }
        }
    }

    if (showCustomPicker) {
        HomePanelColorDialog(
            initialArgb = selectedArgb,
            onConfirm = {
                onColorSelected(it)
                showCustomPicker = false
            },
            onDismiss = { showCustomPicker = false },
        )
    }
}

@Composable
private fun HomePanelColorDialog(
    initialArgb: Int,
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
            modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(42.dp), shape = CircleShape, color = Color(currentArgb)) {}
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("Custom panel color", fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Text("Swipe the field and hue strip, or enter a hex color.", color = Muted, fontSize = 12.sp)
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
                    label = { Text("Hex color") },
                    supportingText = {
                        if (parseHomePanelHex(hexText) == null) Text("Use six digits, for example #602C00")
                    },
                    isError = parseHomePanelHex(hexText) == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.size(8.dp))
                    Button(
                        onClick = { onConfirm(currentArgb) },
                        enabled = parseHomePanelHex(hexText) != null,
                    ) { Text("Apply") }
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

    fun update(position: Offset) {
        if (fieldSize.width <= 0 || fieldSize.height <= 0) return
        onChange(
            (position.x / fieldSize.width).coerceIn(0f, 1f),
            (1f - position.y / fieldSize.height).coerceIn(0f, 1f),
        )
    }

    Canvas(
        Modifier.fillMaxWidth().height(180.dp).onSizeChanged { fieldSize = it }
            .pointerInput(hue, fieldSize) {
                detectDragGestures(
                    onDragStart = ::update,
                    onDrag = { change, _ -> update(change.position) },
                )
            },
    ) {
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Color.White, Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))))),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
        )
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
        )
        val marker = Offset(saturation * size.width, (1f - brightness) * size.height)
        drawCircle(Color.White, radius = 9.dp.toPx(), center = marker, style = Stroke(3.dp.toPx()))
        drawCircle(Color.Black.copy(alpha = .55f), radius = 11.dp.toPx(), center = marker, style = Stroke(1.dp.toPx()))
    }
}

@Composable
private fun HueStrip(hue: Float, onChange: (Float) -> Unit) {
    var stripSize by remember { mutableStateOf(IntSize.Zero) }
    val hueColors = remember {
        (0..6).map { step -> Color(android.graphics.Color.HSVToColor(floatArrayOf(step * 60f, 1f, 1f))) }
    }

    fun update(position: Offset) {
        if (stripSize.width <= 0) return
        onChange((position.x / stripSize.width * 360f).coerceIn(0f, 359.99f))
    }

    Canvas(
        Modifier.fillMaxWidth().height(28.dp).onSizeChanged { stripSize = it }
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
        drawCircle(Color.White, radius = 8.dp.toPx(), center = marker, style = Stroke(3.dp.toPx()))
        drawCircle(Color.Black.copy(alpha = .55f), radius = 10.dp.toPx(), center = marker, style = Stroke(1.dp.toPx()))
    }
}
