package com.katoaapps.openminilaunch.ui.magic.tutorial

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.zIndex
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.magic.MagicBoxRoute
import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.theme.Dimens

internal const val MAGIC_BOX_FEATURE_TOUR_ID = "magic_box_spotlight_tour_v1"
internal const val MAGIC_BOX_TOUR_SEARCH_QUERY = "hello world"

internal enum class MagicBoxTourScene {
    HOTKEYS,
    APP_SEARCH,
    SEARCH_DESTINATIONS,
}

internal data class MagicBoxTourStep(
    val target: MagicBoxTourTarget,
    val scene: MagicBoxTourScene,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    val hotkey: Char? = null,
    val showAppOptions: Boolean = false,
)

internal val magicBoxTourSteps = listOf(
    MagicBoxTourStep(
        MagicBoxTourTarget.MESSAGE_HOTKEY,
        MagicBoxTourScene.HOTKEYS,
        R.string.command_text,
        R.string.magic_tour_message_description,
        hotkey = '@',
    ),
    MagicBoxTourStep(
        MagicBoxTourTarget.CALL_HOTKEY,
        MagicBoxTourScene.HOTKEYS,
        R.string.command_call,
        R.string.magic_tour_call_description,
        hotkey = '#',
    ),
    MagicBoxTourStep(
        MagicBoxTourTarget.TODO_HOTKEY,
        MagicBoxTourScene.HOTKEYS,
        R.string.command_todo,
        R.string.magic_tour_todo_description,
        hotkey = '-',
    ),
    MagicBoxTourStep(
        MagicBoxTourTarget.NOTE_HOTKEY,
        MagicBoxTourScene.HOTKEYS,
        R.string.command_note,
        R.string.magic_tour_note_description,
        hotkey = '/',
    ),
    MagicBoxTourStep(
        MagicBoxTourTarget.EVENT_HOTKEY,
        MagicBoxTourScene.HOTKEYS,
        R.string.command_event,
        R.string.magic_tour_event_description,
        hotkey = '+',
    ),
    MagicBoxTourStep(
        MagicBoxTourTarget.APP_HOTKEY,
        MagicBoxTourScene.HOTKEYS,
        R.string.command_app,
        R.string.magic_tour_app_description,
        hotkey = '?',
    ),
    MagicBoxTourStep(
        MagicBoxTourTarget.APP_RESULT,
        MagicBoxTourScene.APP_SEARCH,
        R.string.magic_tour_tap_to_open,
        R.string.magic_tour_tap_to_open_description,
    ),
    MagicBoxTourStep(
        MagicBoxTourTarget.APP_RESULT,
        MagicBoxTourScene.APP_SEARCH,
        R.string.magic_tour_long_press,
        R.string.magic_tour_long_press_description,
        showAppOptions = true,
    ),
    MagicBoxTourStep(
        MagicBoxTourTarget.DOCUMENT_SEARCH,
        MagicBoxTourScene.SEARCH_DESTINATIONS,
        R.string.document_search_not_set_up,
        R.string.magic_tour_document_description,
    ),
    MagicBoxTourStep(
        MagicBoxTourTarget.WEB_SEARCH,
        MagicBoxTourScene.SEARCH_DESTINATIONS,
        R.string.magic_tour_search_web,
        R.string.magic_tour_search_web_description,
    ),
    MagicBoxTourStep(
        MagicBoxTourTarget.AI_SEARCH,
        MagicBoxTourScene.SEARCH_DESTINATIONS,
        R.string.magic_tour_search_ai,
        R.string.magic_tour_search_ai_description,
    ),
)

internal fun shouldStartMagicBoxFeatureTour(
    enabled: Boolean,
    expanded: Boolean,
    currentTourSeen: Boolean,
    alreadyStarted: Boolean,
): Boolean = enabled && expanded && !currentTourSeen && !alreadyStarted

internal fun MagicBoxTourStep.route(): MagicBoxRoute = when (scene) {
    MagicBoxTourScene.HOTKEYS -> MagicBoxRoute.SEARCH
    MagicBoxTourScene.APP_SEARCH -> MagicBoxRoute.APP
    MagicBoxTourScene.SEARCH_DESTINATIONS -> MagicBoxRoute.SEARCH
}

@Composable
internal fun MagicBoxFeatureTour(
    currentIndex: Int,
    registry: MagicBoxTourTargetRegistry,
    settingsTarget: LauncherAppTarget?,
    actions: DeviceActions,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val step = magicBoxTourSteps[currentIndex]
    BackHandler(onBack = onPrevious)
    var retainedSpotlight by remember { mutableStateOf<Rect?>(null) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .zIndex(20f),
    ) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val spotlight = registry[step.target]?.expandAndClamp(
            padding = with(density) { SPOTLIGHT_PADDING.toPx() },
            maxWidth = screenWidthPx,
            maxHeight = screenHeightPx,
        )
        LaunchedEffect(spotlight) {
            if (spotlight != null) retainedSpotlight = spotlight
        }
        // A programmatic query recomposes results before their new bounds are published.
        // Keep the prior measured target for that brief frame instead of sending the card
        // to a distant fallback position.
        val displayedSpotlight = spotlight ?: retainedSpotlight

        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            drawRect(TOUR_SCRIM)
            displayedSpotlight?.let { bounds ->
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = bounds.topLeft,
                    size = bounds.size,
                    cornerRadius = CornerRadius(SPOTLIGHT_CORNER.toPx()),
                    blendMode = BlendMode.Clear,
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = bounds.topLeft,
                    size = bounds.size,
                    cornerRadius = CornerRadius(SPOTLIGHT_CORNER.toPx()),
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
        }

        // The highlighted control is illustrative during this lesson. Consume the whole surface
        // so Settings, Web, AI, and bubble actions cannot launch behind the tutorial.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(currentIndex) {
                    detectTapGestures(onPress = { tryAwaitRelease() })
                },
        )

        displayedSpotlight?.let { target ->
            MagicTourHelpCard(
                step = step,
                index = currentIndex,
                count = magicBoxTourSteps.size,
                target = target,
                screenHeightPx = screenHeightPx,
                settingsTarget = settingsTarget,
                actions = actions,
                onPrevious = onPrevious,
                onNext = onNext,
            )
        }
    }
}

@Composable
private fun BoxScope.MagicTourHelpCard(
    step: MagicBoxTourStep,
    index: Int,
    count: Int,
    target: Rect,
    screenHeightPx: Float,
    settingsTarget: LauncherAppTarget?,
    actions: DeviceActions,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    var horizontalDrag by remember(index) { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val targetIsLow = target.center.y > screenHeightPx * .56f
    val alignment = if (targetIsLow) Alignment.BottomCenter else Alignment.TopCenter
    val targetClearance = if (targetIsLow) {
        with(density) { (screenHeightPx - target.top).toDp() }
    } else {
        with(density) { target.bottom.toDp() }
    }
    val outsideTargetPadding = (targetClearance + Dimens.dp12).coerceAtLeast(Dimens.dp24)

    Surface(
        modifier = Modifier
            .align(alignment)
            .padding(
                start = Dimens.dp16,
                end = Dimens.dp16,
                top = if (targetIsLow) Dimens.dp24 else outsideTargetPadding,
                bottom = if (targetIsLow) outsideTargetPadding else Dimens.dp24,
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
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = Dimens.dp16,
    ) {
        Column(
            Modifier.padding(horizontal = Dimens.dp20, vertical = Dimens.dp18),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = step.hotkey?.let { key ->
                    stringResource(R.string.magic_tour_hotkey_title, key.toString(), stringResource(step.titleRes))
                } ?: stringResource(step.titleRes),
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
            if (step.showAppOptions) {
                Spacer(Modifier.height(Dimens.dp12))
                AppOptionsPreview(settingsTarget, actions)
            }
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
                                    if (dotIndex == index) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = .24f)
                                    },
                                    CircleShape,
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

@Composable
private fun AppOptionsPreview(target: LauncherAppTarget?, actions: DeviceActions) {
    Surface(
        shape = RoundedCornerShape(Dimens.dp18),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(Dimens.dp12),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                target?.let { LauncherTargetIcon(it, actions, Dimens.dp28) }
                Text(
                    target?.label ?: stringResource(R.string.settings),
                    modifier = Modifier.padding(start = if (target == null) Dimens.dp0 else Dimens.dp10),
                    fontWeight = FontWeight.Bold,
                )
            }
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.BubbleChart, null)
                Text(stringResource(R.string.open_in_app_bubble), Modifier.padding(start = Dimens.dp8))
            }
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null)
                Text(stringResource(R.string.open_normally), Modifier.padding(start = Dimens.dp8))
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

private val TOUR_SCRIM = Color.Black.copy(alpha = .78f)
private val SPOTLIGHT_PADDING = 6.dp
private val SPOTLIGHT_CORNER = 18.dp
