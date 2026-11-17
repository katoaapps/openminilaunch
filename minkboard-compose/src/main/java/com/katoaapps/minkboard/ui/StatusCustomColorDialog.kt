package com.katoaapps.minkboard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
internal fun StatusCustomColorDialog(
    draft: StatusCustomColorDraft,
    onDraftChange: (StatusCustomColorDraft) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val currentArgb = android.graphics.Color.HSVToColor(
        floatArrayOf(draft.hue, draft.saturation, draft.brightness),
    )

    MinkBoardAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = Color(currentArgb),
                ) {}
                Text(stringResource(R.string.color_custom))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                StatusSaturationBrightnessField(
                    hue = draft.hue,
                    saturation = draft.saturation,
                    brightness = draft.brightness,
                    onChange = { newSaturation, newBrightness ->
                        val updated = draft.copy(
                            saturation = newSaturation,
                            brightness = newBrightness,
                        )
                        onDraftChange(
                            updated.copy(
                                hexText = formatStatusColorHex(
                                    android.graphics.Color.HSVToColor(
                                        floatArrayOf(
                                            updated.hue,
                                            updated.saturation,
                                            updated.brightness,
                                        ),
                                    ),
                                ),
                            ),
                        )
                    },
                )
                StatusHueStrip(draft.hue) { newHue ->
                    val updated = draft.copy(hue = newHue)
                    onDraftChange(
                        updated.copy(
                            hexText = formatStatusColorHex(
                                android.graphics.Color.HSVToColor(
                                    floatArrayOf(
                                        updated.hue,
                                        updated.saturation,
                                        updated.brightness,
                                    ),
                                ),
                            ),
                        ),
                    )
                }
                OutlinedTextField(
                    value = draft.hexText,
                    onValueChange = { entered ->
                        val filtered = entered
                            .uppercase(Locale.ROOT)
                            .filterIndexed { index, char ->
                                (index == 0 && char == '#') || char in '0'..'9' || char in 'A'..'F'
                            }
                            .take(7)
                        val parsed = parseStatusColorHex(filtered)
                        if (parsed == null) {
                            onDraftChange(draft.copy(hexText = filtered))
                        } else {
                            val hsv = FloatArray(3)
                            android.graphics.Color.colorToHSV(parsed, hsv)
                            onDraftChange(
                                draft.copy(
                                    hue = hsv[0],
                                    saturation = hsv[1],
                                    brightness = hsv[2],
                                    hexText = filtered,
                                ),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.hex_color)) },
                    supportingText = {
                        if (parseStatusColorHex(draft.hexText) == null) {
                            Text(stringResource(R.string.invalid_hex_color))
                        }
                    },
                    isError = parseStatusColorHex(draft.hexText) == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { parseStatusColorHex(draft.hexText)?.let(onConfirm) },
                enabled = parseStatusColorHex(draft.hexText) != null,
            ) {
                Text(stringResource(R.string.apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

internal fun statusCustomColorDraft(initialArgb: Int): StatusCustomColorDraft {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(initialArgb, hsv)
    return StatusCustomColorDraft(
        hue = hsv[0],
        saturation = hsv[1],
        brightness = hsv[2],
        hexText = formatStatusColorHex(initialArgb),
    )
}

@Composable
private fun StatusSaturationBrightnessField(
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
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .onSizeChanged { fieldSize = it }
            .pointerInput(hue, fieldSize) {
                detectTapGestures(onTap = ::update)
            }
            .pointerInput(hue, fieldSize) {
                detectDragGestures(
                    onDragStart = ::update,
                    onDrag = { change, _ -> update(change.position) },
                )
            },
    ) {
        val cornerRadius = CornerRadius(18.dp.toPx())
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(
                    Color.White,
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))),
                ),
            ),
            cornerRadius = cornerRadius,
        )
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)),
            cornerRadius = cornerRadius,
        )
        drawStatusColorMarker(
            center = Offset(saturation * size.width, (1f - brightness) * size.height),
        )
    }
}

@Composable
private fun StatusHueStrip(
    hue: Float,
    onChange: (Float) -> Unit,
) {
    var stripSize by remember { mutableStateOf(IntSize.Zero) }
    val hueColors = remember {
        (0..6).map { step ->
            Color(android.graphics.Color.HSVToColor(floatArrayOf(step * 60f, 1f, 1f)))
        }
    }

    fun update(position: Offset) {
        if (stripSize.width <= 0) return
        onChange((position.x / stripSize.width * 360f).coerceIn(0f, 359.99f))
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .onSizeChanged { stripSize = it }
            .pointerInput(stripSize) {
                detectTapGestures(onTap = ::update)
            }
            .pointerInput(stripSize) {
                detectDragGestures(
                    onDragStart = ::update,
                    onDrag = { change, _ -> update(change.position) },
                )
            },
    ) {
        drawRoundRect(
            brush = Brush.horizontalGradient(hueColors),
            cornerRadius = CornerRadius(size.height / 2f),
        )
        drawStatusColorMarker(
            center = Offset(hue / 360f * size.width, size.height / 2f),
            innerRadius = 8.dp.toPx(),
            outerRadius = 10.dp.toPx(),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStatusColorMarker(
    center: Offset,
    innerRadius: Float = 9.dp.toPx(),
    outerRadius: Float = 11.dp.toPx(),
) {
    drawCircle(Color.White, radius = innerRadius, center = center, style = Stroke(3.dp.toPx()))
    drawCircle(
        Color.Black.copy(alpha = 0.55f),
        radius = outerRadius,
        center = center,
        style = Stroke(1.dp.toPx()),
    )
}

internal fun formatStatusColorHex(argb: Int): String =
    "#%06X".format(Locale.ROOT, argb and 0x00FFFFFF)

internal fun parseStatusColorHex(value: String): Int? {
    val normalized = value.trim().removePrefix("#")
    if (normalized.length != 6 || normalized.any { it !in "0123456789ABCDEFabcdef" }) return null
    return normalized.toLongOrNull(16)?.let { (0xFF000000L or it).toInt() }
}
