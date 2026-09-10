package com.katoaapps.openminilaunch.ui.launcher

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** Makes an intentional return to Home visible after applying an appearance change. */
@Composable
internal fun HomeEntranceTransition(
    animate: Boolean,
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(if (animate) 0f else 1f) }

    LaunchedEffect(animate) {
        if (animate) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = HOME_ENTRANCE_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
            )
            onAnimationFinished()
        } else {
            progress.snapTo(1f)
        }
    }

    Box(
        modifier.graphicsLayer {
            alpha = progress.value
            val entranceScale = HOME_ENTRANCE_START_SCALE +
                ((1f - HOME_ENTRANCE_START_SCALE) * progress.value)
            scaleX = entranceScale
            scaleY = entranceScale
        },
    ) {
        content()
    }
}

private const val HOME_ENTRANCE_MILLIS = 320
private const val HOME_ENTRANCE_START_SCALE = 0.975f
