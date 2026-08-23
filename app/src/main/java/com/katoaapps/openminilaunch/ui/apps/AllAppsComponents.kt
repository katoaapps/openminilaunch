@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.katoaapps.openminilaunch.ui.apps

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.*
import com.katoaapps.openminilaunch.platform.*
import com.katoaapps.openminilaunch.features.apps.*
import com.katoaapps.openminilaunch.ui.components.*
import com.katoaapps.openminilaunch.ui.theme.*

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.util.lerp as lerpFloat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@Composable
internal fun AppCarouselItem(
    app: LaunchableApp,
    actions: DeviceActions,
    iconSize: androidx.compose.ui.unit.Dp,
    scale: Float,
    focused: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = .52f + .48f * scale
        }.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(
            packageName = app.packageName,
            actions = actions,
            size = iconSize,
            contentDescription = stringResource(R.string.open_app, app.label),
        )
        Surface(
            color = Color.Black.copy(alpha = if (focused) .52f else .34f),
            contentColor = Color.White,
            shape = RoundedCornerShape(Dimens.dp18),
            modifier = Modifier.padding(top = Dimens.dp14),
        ) {
            Text(
                app.label,
                modifier = Modifier.padding(horizontal = Dimens.dp14, vertical = Dimens.dp7),
                fontSize = if (focused) Dimens.sp18 else Dimens.sp14,
                fontWeight = if (focused) FontWeight.Black else FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun LetterArc(
    availableLetters: Set<Char>,
    selectedLetter: Char?,
    selectedContentColor: Color,
    compact: Boolean,
    onLetter: (Char) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val railHeight = if (compact) Dimens.dp116 else Dimens.dp200
    var lastTouchedLetter by remember { androidx.compose.runtime.mutableStateOf<Char?>(null) }

    BoxWithConstraints(
        Modifier.fillMaxWidth().height(railHeight)
            .pointerInput(availableLetters) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun selectAt(position: Offset) {
                        val index = nearestLetterIndex(
                            x = position.x,
                            y = position.y,
                            width = size.width.toFloat(),
                            height = size.height.toFloat(),
                        )
                        val letter = ALL_APP_LETTERS[index]
                        if (letter in availableLetters && letter != lastTouchedLetter) {
                            lastTouchedLetter = letter
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onLetter(letter)
                        }
                    }
                    selectAt(down.position)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        selectAt(change.position)
                        change.consume()
                        if (!change.pressed) break
                    }
                    lastTouchedLetter = null
                }
            },
    ) {
        ALL_APP_LETTERS.forEachIndexed { index, letter ->
            val position = letterArcPosition(index, maxWidth.value, maxHeight.value)
            val available = letter in availableLetters
            val selected = letter == selectedLetter
            val center = index == ALL_APPS_CENTER_LETTER_INDEX
            Surface(
                modifier = Modifier.size(if (selected) Dimens.dp28 else Dimens.dp20)
                    .offset(
                        x = position.x.dp - if (selected) Dimens.dp14 else Dimens.dp10,
                        y = position.y.dp - if (selected) Dimens.dp14 else Dimens.dp10,
                    ).clickable(enabled = available) { onLetter(letter) },
                shape = CircleShape,
                color = if (selected) Color.White else Color.Transparent,
                contentColor = if (selected) selectedContentColor else Color.White,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        letter.toString(),
                        fontSize = when {
                            selected -> Dimens.sp17
                            center -> Dimens.sp15
                            else -> Dimens.sp12
                        },
                        fontWeight = if (selected || center) FontWeight.Black else FontWeight.SemiBold,
                        color = when {
                            selected -> selectedContentColor
                            available -> Color.White
                            else -> Color.White.copy(alpha = .28f)
                        },
                    )
                }
            }
        }
    }
}

internal fun dominantAppColor(drawable: Drawable?): Color? {
    val bitmap = runCatching { drawable?.toBitmap(width = 40, height = 40) }.getOrNull() ?: return null
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val hueWeights = FloatArray(24)
    val redTotals = FloatArray(24)
    val greenTotals = FloatArray(24)
    val blueTotals = FloatArray(24)
    var fallbackRed = 0f
    var fallbackGreen = 0f
    var fallbackBlue = 0f
    var fallbackWeight = 0f
    val hsv = FloatArray(3)

    pixels.forEach { pixel ->
        val alpha = AndroidColor.alpha(pixel)
        if (alpha < 96) return@forEach
        val red = AndroidColor.red(pixel).toFloat()
        val green = AndroidColor.green(pixel).toFloat()
        val blue = AndroidColor.blue(pixel).toFloat()
        val alphaWeight = alpha / 255f
        fallbackRed += red * alphaWeight
        fallbackGreen += green * alphaWeight
        fallbackBlue += blue * alphaWeight
        fallbackWeight += alphaWeight

        AndroidColor.colorToHSV(pixel, hsv)
        val saturation = hsv[1]
        val value = hsv[2]
        if (saturation < .16f || value < .12f || value > .98f) return@forEach
        val bin = ((hsv[0] / 360f) * hueWeights.size).toInt().coerceIn(hueWeights.indices)
        val weight = alphaWeight * saturation * (.45f + value)
        hueWeights[bin] += weight
        redTotals[bin] += red * weight
        greenTotals[bin] += green * weight
        blueTotals[bin] += blue * weight
    }

    val strongestBin = hueWeights.indices.maxByOrNull(hueWeights::get)
    val strongestWeight = strongestBin?.let(hueWeights::get) ?: 0f
    return if (strongestBin != null && strongestWeight > 0f) {
        Color(
            red = redTotals[strongestBin] / strongestWeight / 255f,
            green = greenTotals[strongestBin] / strongestWeight / 255f,
            blue = blueTotals[strongestBin] / strongestWeight / 255f,
        )
    } else if (fallbackWeight > 0f) {
        Color(
            red = fallbackRed / fallbackWeight / 255f,
            green = fallbackGreen / fallbackWeight / 255f,
            blue = fallbackBlue / fallbackWeight / 255f,
        )
    } else {
        null
    }
}
