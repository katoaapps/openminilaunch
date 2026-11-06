package com.katoaapps.openminilaunch.ui.appgrid

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.model.AppFolderAnimationStyle
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.theme.Dimens

private const val GROW_AND_SPIN_DURATION_MILLIS = 260
private const val APPEAR_DURATION_MILLIS = 90
private const val RIPPLE_DURATION_MILLIS = 170
private const val RIPPLE_STEP_DELAY_MILLIS = 34
private const val MAX_RIPPLE_DELAY_STEPS = 6
private const val RISE_DURATION_MILLIS = 280

/** Applies one of the shared folder-entry animations to a single item. */
@Composable
internal fun AnimatedFolderEntry(
    style: AppFolderAnimationStyle,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var started by remember(style) { mutableStateOf(false) }
    val density = LocalDensity.current
    val rippleDistancePx = with(density) { 7.dp.toPx() }
    val riseDistancePx = with(density) { 22.dp.toPx() }
    val durationMillis = when (style) {
        AppFolderAnimationStyle.GROW_AND_SPIN -> GROW_AND_SPIN_DURATION_MILLIS
        AppFolderAnimationStyle.APPEAR -> APPEAR_DURATION_MILLIS
        AppFolderAnimationStyle.RIPPLE -> RIPPLE_DURATION_MILLIS
        AppFolderAnimationStyle.RISE -> RISE_DURATION_MILLIS
    }
    val delayMillis = if (style == AppFolderAnimationStyle.RIPPLE) {
        index.coerceAtMost(MAX_RIPPLE_DELAY_STEPS) * RIPPLE_STEP_DELAY_MILLIS
    } else {
        0
    }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(
            durationMillis = durationMillis,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "folder entry",
    )
    LaunchedEffect(style) { started = true }

    Box(
        modifier.graphicsLayer {
            alpha = progress
            when (style) {
                AppFolderAnimationStyle.GROW_AND_SPIN -> {
                    scaleX = .55f + (.45f * progress)
                    scaleY = scaleX
                    rotationZ = -22f * (1f - progress)
                }
                AppFolderAnimationStyle.APPEAR -> Unit
                AppFolderAnimationStyle.RIPPLE -> {
                    scaleX = .82f + (.18f * progress)
                    scaleY = scaleX
                    translationY = rippleDistancePx * (1f - progress)
                }
                AppFolderAnimationStyle.RISE -> {
                    scaleX = .93f + (.07f * progress)
                    scaleY = scaleX
                    translationY = riseDistancePx * (1f - progress)
                }
            }
        },
    ) {
        content()
    }
}

/** Compact preview that replays inside the animation dropdown whenever its value changes. */
@Composable
internal fun FolderAnimationInlinePreview(
    style: AppFolderAnimationStyle,
    targets: List<LauncherTarget>,
    actions: DeviceActions,
) {
    val availableTargets = remember(targets) {
        targets.filter(LauncherTarget::isAvailable).take(3)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        key(style) {
            repeat(3) { index ->
                AnimatedFolderEntry(style = style, index = index) {
                    val target = availableTargets.getOrNull(index)
                    if (target != null) {
                        LauncherTargetIcon(
                            target = target,
                            actions = actions,
                            size = Dimens.dp12,
                            contentDescription = null,
                        )
                    } else {
                        val color = if (index % 2 == 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        }
                        Box(
                            Modifier
                                .size(Dimens.dp12)
                                .clip(RoundedCornerShape(Dimens.dp3))
                                .background(color),
                        )
                    }
                }
            }
        }
    }
}
