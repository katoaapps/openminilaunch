package com.katoaapps.openminilaunch.ui.quickdrawer

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.theme.Dimens
import kotlinx.coroutines.delay

internal const val QUICK_DRAWER_FEATURE_TOUR_ID = "quick_drawer_spotlight_tour_v1"

internal enum class QuickDrawerTourTarget {
    ANNOUNCEMENT,
    QUICK_APPS,
    GAMES,
    MINKPAD_LITE,
    ALL_APPS,
    PRIVATE_SPACE,
    WORK_SPACE,
}

internal data class QuickDrawerTourStep(
    val target: QuickDrawerTourTarget,
    val titleRes: Int,
    val descriptionRes: Int,
)

internal fun quickDrawerTourSteps(
    destinations: List<QuickDrawerDestination>,
): List<QuickDrawerTourStep> = buildList {
    add(
        QuickDrawerTourStep(
            QuickDrawerTourTarget.ANNOUNCEMENT,
            R.string.quick_drawer_tour_updates_title,
            R.string.quick_drawer_tour_updates_description,
        ),
    )
    add(
        QuickDrawerTourStep(
            QuickDrawerTourTarget.QUICK_APPS,
            R.string.quick_apps,
            R.string.quick_drawer_tour_quick_apps_description,
        ),
    )
    add(
        QuickDrawerTourStep(
            QuickDrawerTourTarget.GAMES,
            R.string.games_beta,
            R.string.quick_drawer_tour_games_description,
        ),
    )
    if (QuickDrawerDestination.MINKPAD_LITE in destinations) {
        add(
            QuickDrawerTourStep(
                QuickDrawerTourTarget.MINKPAD_LITE,
                R.string.minkpad_lite,
                R.string.quick_drawer_tour_minkpad_description,
            ),
        )
    }
    add(
        QuickDrawerTourStep(
            QuickDrawerTourTarget.ALL_APPS,
            R.string.all_apps,
            R.string.quick_drawer_tour_all_apps_description,
        ),
    )
    if (QuickDrawerDestination.PRIVATE_SPACE in destinations) {
        add(
            QuickDrawerTourStep(
                QuickDrawerTourTarget.PRIVATE_SPACE,
                R.string.private_space,
                R.string.quick_drawer_tour_private_space_description,
            ),
        )
    }
    if (QuickDrawerDestination.WORK_SPACE in destinations) {
        add(
            QuickDrawerTourStep(
                QuickDrawerTourTarget.WORK_SPACE,
                R.string.work_space,
                R.string.quick_drawer_tour_work_space_description,
            ),
        )
    }
}

internal fun shouldStartQuickDrawerFeatureTour(
    onboardingComplete: Boolean,
    currentTourSeen: Boolean,
    replayRequested: Boolean,
): Boolean = onboardingComplete && (!currentTourSeen || replayRequested)

@Stable
internal class QuickDrawerTourTargetRegistry {
    private val targetBounds = mutableStateMapOf<QuickDrawerTourTarget, Rect>()

    operator fun get(target: QuickDrawerTourTarget): Rect? = targetBounds[target]

    fun update(target: QuickDrawerTourTarget, bounds: Rect) {
        targetBounds[target] = bounds
    }

    fun remove(target: QuickDrawerTourTarget) {
        targetBounds.remove(target)
    }
}

@Composable
internal fun rememberQuickDrawerTourTargetRegistry(): QuickDrawerTourTargetRegistry = remember {
    QuickDrawerTourTargetRegistry()
}

internal fun Modifier.quickDrawerTourTarget(
    registry: QuickDrawerTourTargetRegistry,
    target: QuickDrawerTourTarget,
): Modifier = composed {
    DisposableEffect(registry, target) {
        onDispose { registry.remove(target) }
    }
    onGloballyPositioned { coordinates ->
        registry.update(target, coordinates.boundsInRoot())
    }
}

internal fun QuickDrawerDestination.tourTarget(): QuickDrawerTourTarget? = when (this) {
    QuickDrawerDestination.ALL_APPS -> QuickDrawerTourTarget.ALL_APPS
    QuickDrawerDestination.PRIVATE_SPACE -> QuickDrawerTourTarget.PRIVATE_SPACE
    QuickDrawerDestination.WORK_SPACE -> QuickDrawerTourTarget.WORK_SPACE
    QuickDrawerDestination.MINKPAD_LITE -> QuickDrawerTourTarget.MINKPAD_LITE
}

@Composable
internal fun QuickDrawerFeatureTour(
    steps: List<QuickDrawerTourStep>,
    registry: QuickDrawerTourTargetRegistry,
    modifier: Modifier = Modifier,
    onComplete: () -> Unit,
) {
    var currentIndex by remember(steps) { mutableIntStateOf(0) }
    val step = steps[currentIndex]
    val targetBounds = registry[step.target]

    BackHandler { if (currentIndex > 0) currentIndex-- }
    LaunchedEffect(currentIndex, targetBounds) {
        if (targetBounds == null) {
            delay(TARGET_WAIT_MS)
            if (registry[step.target] == null) {
                if (currentIndex < steps.lastIndex) currentIndex++ else onComplete()
            }
        }
    }

    QuickDrawerSpotlightOverlay(
        modifier = modifier,
        step = step,
        index = currentIndex,
        count = steps.size,
        targetBoundsInRoot = targetBounds,
        onPrevious = { if (currentIndex > 0) currentIndex-- },
        onNext = {
            if (currentIndex < steps.lastIndex) currentIndex++ else onComplete()
        },
    )
}

@Composable
private fun QuickDrawerSpotlightOverlay(
    modifier: Modifier,
    step: QuickDrawerTourStep,
    index: Int,
    count: Int,
    targetBoundsInRoot: Rect?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    BoxWithConstraints(
        modifier
            .zIndex(30f)
            .onGloballyPositioned { rootOrigin = it.positionInRoot() },
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val outlineColor = MaterialTheme.colorScheme.primary
        val spotlight = targetBoundsInRoot
            ?.translate(-rootOrigin.x, -rootOrigin.y)
            ?.expandAndClamp(
                padding = with(density) { SPOTLIGHT_PADDING.toPx() },
                maxWidth = widthPx,
                maxHeight = heightPx,
            )

        if (spotlight != null) {
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
            Box(
                Modifier.fillMaxSize().pointerInputGuard(index),
            )
            QuickDrawerTourCard(
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

private fun Modifier.pointerInputGuard(index: Int): Modifier =
    pointerInput(index) {
        detectTapGestures(onPress = { tryAwaitRelease() })
    }

@Composable
private fun BoxScope.QuickDrawerTourCard(
    step: QuickDrawerTourStep,
    index: Int,
    count: Int,
    target: Rect,
    screenHeightPx: Float,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    var horizontalDrag by remember(index) { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val placeAboveTarget = target.center.y > screenHeightPx * .5f
    val alignment = if (placeAboveTarget) Alignment.BottomCenter else Alignment.TopCenter
    val topPadding = if (placeAboveTarget) {
        Dimens.dp18
    } else {
        with(density) { target.bottom.toDp() } + Dimens.dp12
    }
    val bottomPadding = if (placeAboveTarget) {
        with(density) { (screenHeightPx - target.top).toDp() } + Dimens.dp12
    } else {
        Dimens.dp18
    }
    val cardContentColor = MaterialTheme.colorScheme.inverseOnSurface

    Surface(
        modifier = Modifier
            .align(alignment)
            .padding(
                start = Dimens.dp16,
                end = Dimens.dp16,
                top = topPadding,
                bottom = bottomPadding,
            )
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
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = cardContentColor,
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
                color = cardContentColor.copy(alpha = .82f),
                fontSize = Dimens.sp14,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Dimens.dp14))
            Box(Modifier.fillMaxWidth()) {
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
                                        MaterialTheme.colorScheme.inversePrimary
                                    } else {
                                        cardContentColor.copy(alpha = .3f)
                                    },
                                    shape = CircleShape,
                                ),
                        )
                    }
                }
                Button(onClick = onNext, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Text(stringResource(if (index == count - 1) R.string.done else R.string.next))
                }
            }
        }
    }
}

private fun Rect.expandAndClamp(
    padding: Float,
    maxWidth: Float,
    maxHeight: Float,
): Rect = Rect(
    left = (left - padding).coerceAtLeast(0f),
    top = (top - padding).coerceAtLeast(0f),
    right = (right + padding).coerceAtMost(maxWidth),
    bottom = (bottom + padding).coerceAtMost(maxHeight),
)

private const val TARGET_WAIT_MS = 900L
private val SPOTLIGHT_PADDING = 6.dp
private val SPOTLIGHT_CORNER = 18.dp
private val TOUR_SCRIM = Color.Black.copy(alpha = .76f)
