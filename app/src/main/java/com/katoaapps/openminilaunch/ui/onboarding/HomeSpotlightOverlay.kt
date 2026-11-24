@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.katoaapps.openminilaunch.ui.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.theme.Dimens
import kotlinx.coroutines.delay

@Composable
internal fun HomeSpotlightOverlay(
    step: HomeTourStep,
    index: Int,
    count: Int,
    targetBounds: Rect?,
    gestureStep: Boolean,
    clearClassicSlabRail: Boolean,
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
            GestureDirectionCues(classic = step.classicGestureCues)
        } else if (spotlight != null) {
            SpotlightScrim(spotlight)
        }

        if (gestureStep || spotlight != null) {
            // The cutout is visual only. Keep every underlying Home control inert so a
            // learner cannot open, drag, long-press, or swipe the highlighted feature.
            TutorialInputGuard(index)
            TourHelpCard(
                step = step,
                index = index,
                count = count,
                target = spotlight,
                screenHeightPx = heightPx,
                clearClassicSlabRail = clearClassicSlabRail,
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
private fun TutorialInputGuard(index: Int) {
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(index) { detectTapGestures(onPress = { tryAwaitRelease() }) },
    )
}

@Composable
private fun BoxScope.TourHelpCard(
    step: HomeTourStep,
    index: Int,
    count: Int,
    target: Rect?,
    screenHeightPx: Float,
    clearClassicSlabRail: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    var horizontalDrag by remember(index) { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val anchorAboveTarget = step.target == HomeTourTarget.MAGIC_BOX ||
        step.target == HomeTourTarget.KEYBOARD_ACTION
    val targetBottomPadding = if (anchorAboveTarget && target != null) {
        with(density) { (screenHeightPx - target.top).toDp() } + Dimens.dp12
    } else {
        Dimens.dp24
    }
    val bottomPadding = if (clearClassicSlabRail) {
        maxOf(targetBottomPadding, CLASSIC_SLAB_RAIL_CLEARANCE)
    } else {
        targetBottomPadding
    }
    Surface(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(start = Dimens.dp16, end = Dimens.dp16, bottom = bottomPadding)
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
            if (step.target == HomeTourTarget.MAGIC_BOX) {
                Spacer(Modifier.height(Dimens.dp10))
                Text(
                    stringResource(R.string.home_spotlight_magic_hotkeys),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = Dimens.sp13,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
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
                            when {
                                step.target == HomeTourTarget.MAGIC_BOX -> R.string.start_typing
                                index == count - 1 -> R.string.done
                                else -> R.string.next
                            },
                        ),
                    )
                }
            }
        }
    }
}

private val CLASSIC_SLAB_RAIL_CLEARANCE = 148.dp

@Composable
private fun BoxScope.GestureDirectionCues(classic: Boolean) {
    val cues = remember(classic) {
        buildList {
            add(GestureCue(R.string.all_apps, GestureDirection.UP))
            add(GestureCue(R.string.notifications, GestureDirection.DOWN))
            if (!classic) {
                add(GestureCue(R.string.widgets, GestureDirection.LEFT))
                add(GestureCue(R.string.mink_day, GestureDirection.RIGHT))
            }
        }
    }
    var cueIndex by remember(classic) { mutableIntStateOf(0) }
    LaunchedEffect(classic) {
        while (true) {
            delay(GESTURE_CUE_DURATION_MS)
            cueIndex = (cueIndex + 1) % cues.size
        }
    }
    Crossfade(
        targetState = cues[cueIndex],
        animationSpec = tween(GESTURE_CUE_FADE_MS),
        label = "homeGestureCue",
        modifier = Modifier.align(Alignment.Center),
    ) { cue ->
        DirectionCue(cue)
    }
}

@Composable
private fun DirectionCue(cue: GestureCue) {
    val density = LocalDensity.current
    val motionTransition = rememberInfiniteTransition(label = "gestureSwipeMotion")
    val motion by motionTransition.animateFloat(
        initialValue = -.5f,
        targetValue = .5f,
        animationSpec = infiniteRepeatable(
            animation = tween(GESTURE_MOTION_MS),
            repeatMode = RepeatMode.Restart,
        ),
        label = "gestureSwipeOffset",
    )
    val travelPx = with(density) { GESTURE_TRAVEL.toPx() }
    val icon = when (cue.direction) {
        GestureDirection.UP -> Icons.Default.SwipeUp
        GestureDirection.DOWN -> Icons.Default.SwipeDown
        GestureDirection.LEFT -> Icons.Default.SwipeLeft
        GestureDirection.RIGHT -> Icons.Default.SwipeRight
    }
    Surface(
        shape = RoundedCornerShape(Dimens.dp20),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = Dimens.dp6,
    ) {
        Column(
            Modifier.padding(horizontal = Dimens.dp28, vertical = Dimens.dp18),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
        ) {
            Icon(
                icon,
                null,
                Modifier.size(Dimens.dp56).graphicsLayer {
                    translationX = when (cue.direction) {
                        GestureDirection.LEFT -> -motion * travelPx
                        GestureDirection.RIGHT -> motion * travelPx
                        else -> 0f
                    }
                    translationY = when (cue.direction) {
                        GestureDirection.UP -> -motion * travelPx
                        GestureDirection.DOWN -> motion * travelPx
                        else -> 0f
                    }
                },
            )
            Text(
                stringResource(cue.labelRes),
                fontSize = Dimens.sp15,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private data class GestureCue(
    val labelRes: Int,
    val direction: GestureDirection,
)

private enum class GestureDirection { UP, DOWN, LEFT, RIGHT }

private fun Rect.expandAndClamp(padding: Float, maxWidth: Float, maxHeight: Float): Rect = Rect(
    left = (left - padding).coerceAtLeast(0f),
    top = (top - padding).coerceAtLeast(0f),
    right = (right + padding).coerceAtMost(maxWidth),
    bottom = (bottom + padding).coerceAtMost(maxHeight),
)

private val SPOTLIGHT_PADDING = 8.dp
private val SPOTLIGHT_CORNER = 22.dp
private val TOUR_SCRIM = Color.Black.copy(alpha = .76f)
private val GESTURE_TRAVEL = 18.dp
private const val GESTURE_CUE_DURATION_MS = 1_350L
private const val GESTURE_CUE_FADE_MS = 220
private const val GESTURE_MOTION_MS = 760
