@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.katoaapps.openminilaunch.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwipeDown
import androidx.compose.material.icons.filled.SwipeLeft
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.SwipeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.theme.Dimens
import kotlin.math.roundToInt

@Composable
internal fun HomeSpotlightOverlay(
    step: HomeTourStep,
    index: Int,
    count: Int,
    targetBounds: Rect?,
    gestureStep: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val spotlightPadding = with(density) { SPOTLIGHT_PADDING.toPx() }
        val spotlight = targetBounds?.expandAndClamp(
            padding = spotlightPadding,
            maxWidth = widthPx,
            maxHeight = heightPx,
        )

        if (gestureStep) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(TOUR_SCRIM.copy(alpha = .64f))
            }
            GestureDirectionCues()
        } else if (spotlight != null) {
            SpotlightScrim(spotlight)
            SpotlightInputGuard(spotlight, widthPx, heightPx)
        }

        if (gestureStep || spotlight != null) {
            TourHelpCard(
                step = step,
                index = index,
                count = count,
                target = spotlight,
                screenHeightPx = heightPx,
                onPrevious = onPrevious,
                onNext = onNext,
            )
        }
    }
}

@Composable
private fun SpotlightScrim(spotlight: Rect) {
    val outlineColor = MaterialTheme.colorScheme.primary
    Canvas(
        Modifier.fillMaxSize().graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        },
    ) {
        drawRect(TOUR_SCRIM)
        drawRoundRect(
            color = Color.Transparent,
            topLeft = spotlight.topLeft,
            size = spotlight.size,
            cornerRadius = CornerRadius(SPOTLIGHT_CORNER.toPx()),
            blendMode = BlendMode.Clear,
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = spotlight.topLeft,
            size = spotlight.size,
            cornerRadius = CornerRadius(SPOTLIGHT_CORNER.toPx()),
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

@Composable
private fun SpotlightInputGuard(spotlight: Rect, screenWidthPx: Float, screenHeightPx: Float) {
    BlockingRegion(0f, 0f, screenWidthPx, spotlight.top)
    BlockingRegion(0f, spotlight.bottom, screenWidthPx, screenHeightPx)
    BlockingRegion(0f, spotlight.top, spotlight.left, spotlight.bottom)
    BlockingRegion(spotlight.right, spotlight.top, screenWidthPx, spotlight.bottom)
}

@Composable
private fun BlockingRegion(left: Float, top: Float, right: Float, bottom: Float) {
    if (right <= left || bottom <= top) return
    val density = LocalDensity.current
    Box(
        Modifier
            .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .size(
                width = with(density) { (right - left).toDp() },
                height = with(density) { (bottom - top).toDp() },
            )
            .pointerInput(Unit) { detectTapGestures(onPress = { tryAwaitRelease() }) },
    )
}

@Composable
private fun BoxScope.TourHelpCard(
    step: HomeTourStep,
    index: Int,
    count: Int,
    target: Rect?,
    screenHeightPx: Float,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    var horizontalDrag by remember(index) { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val placeAtTop = target?.center?.y?.let { it > screenHeightPx * .5f } == true
    Surface(
        modifier = Modifier
            .align(if (placeAtTop) Alignment.TopCenter else Alignment.BottomCenter)
            .padding(horizontal = Dimens.dp16, vertical = Dimens.dp24)
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .pointerInput(index) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, amount -> horizontalDrag += amount },
                    onDragEnd = {
                        val threshold = with(density) { 48.dp.toPx() }
                        when {
                            horizontalDrag < -threshold -> onNext()
                            horizontalDrag > threshold -> onPrevious()
                        }
                        horizontalDrag = 0f
                    },
                    onDragCancel = { horizontalDrag = 0f },
                )
            },
        shape = RoundedCornerShape(Dimens.dp28),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = Dimens.dp16,
    ) {
        Column(
            Modifier.padding(horizontal = Dimens.dp20, vertical = Dimens.dp18),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(step.titleRes),
                fontSize = Dimens.sp19,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Dimens.dp6))
            Text(
                stringResource(step.descriptionRes),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f),
                fontSize = Dimens.sp14,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Dimens.dp14))
            Box(
                Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(count) { dotIndex ->
                        Box(
                            Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (dotIndex == index) 10.dp else 7.dp)
                                .background(
                                    color = if (dotIndex == index) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = .24f)
                                    },
                                    shape = CircleShape,
                                ),
                        )
                    }
                }
                Button(
                    onClick = onNext,
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Text(
                        stringResource(
                            if (index == count - 1) R.string.done else R.string.next,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.GestureDirectionCues() {
    DirectionCue(
        label = stringResource(R.string.all_apps),
        icon = { Icon(Icons.Default.SwipeUp, null) },
        modifier = Modifier.align(Alignment.TopCenter).padding(top = Dimens.dp24),
    )
    DirectionCue(
        label = stringResource(R.string.notifications),
        icon = { Icon(Icons.Default.SwipeDown, null) },
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 220.dp),
    )
    DirectionCue(
        label = stringResource(R.string.widgets),
        icon = { Icon(Icons.Default.SwipeLeft, null) },
        modifier = Modifier.align(Alignment.CenterStart).padding(start = Dimens.dp8),
        vertical = true,
    )
    DirectionCue(
        label = stringResource(R.string.mink_day),
        icon = { Icon(Icons.Default.SwipeRight, null) },
        modifier = Modifier.align(Alignment.CenterEnd).padding(end = Dimens.dp8),
        vertical = true,
    )
}

@Composable
private fun DirectionCue(
    label: String,
    icon: @Composable () -> Unit,
    modifier: Modifier,
    vertical: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.dp20),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = Dimens.dp6,
    ) {
        if (vertical) {
            Column(
                Modifier.padding(horizontal = Dimens.dp8, vertical = Dimens.dp10),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                icon()
                Text(label, fontSize = Dimens.sp11, fontWeight = FontWeight.Bold)
            }
        } else {
            Row(
                Modifier.padding(horizontal = Dimens.dp12, vertical = Dimens.dp6),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon()
                Text(label, fontSize = Dimens.sp12, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun Rect.expandAndClamp(padding: Float, maxWidth: Float, maxHeight: Float): Rect = Rect(
    left = (left - padding).coerceAtLeast(0f),
    top = (top - padding).coerceAtLeast(0f),
    right = (right + padding).coerceAtMost(maxWidth),
    bottom = (bottom + padding).coerceAtMost(maxHeight),
)

private val SPOTLIGHT_PADDING = 8.dp
private val SPOTLIGHT_CORNER = 22.dp
private val TOUR_SCRIM = Color.Black.copy(alpha = .76f)
