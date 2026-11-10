package com.katoaapps.openminilaunch.ui.launcher.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.theme.MinkFeatureColors
import com.katoaapps.openminilaunch.ui.theme.readableContentColor
import kotlinx.coroutines.delay

/** A persistent home action that becomes a software notification light for new activity. */
@Composable
internal fun ConversationAttentionButton(
    conversationCount: Int,
    attentionColors: List<Color>,
    actionSize: Dp,
    iconSize: Dp,
    restingColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = remember(attentionColors) { attentionColors.distinct() }
    var colorIndex by remember(palette) { mutableIntStateOf(0) }
    LaunchedEffect(palette) {
        colorIndex = 0
        while (palette.size > 1) {
            delay(1_100L)
            colorIndex = (colorIndex + 1) % palette.size
        }
    }
    val needsAttention = palette.isNotEmpty()
    val alertColor by animateColorAsState(
        targetValue = palette.getOrNull(colorIndex) ?: Color.Transparent,
        animationSpec = tween(durationMillis = 240),
        label = "favorite attention color",
    )
    val alertContentColor = readableContentColor(alertColor)
    val pulse = if (needsAttention) conversationAttentionPulse() else 0f

    Box(modifier.size(actionSize), contentAlignment = Alignment.Center) {
        if (needsAttention) {
            Canvas(Modifier.fillMaxSize()) {
                val half = size.minDimension / 2f
                drawCircle(
                    color = alertColor.copy(alpha = .3f + pulse * .65f),
                    radius = half * (.66f + pulse * .12f),
                )
                drawCircle(
                    color = alertColor.copy(alpha = (1f - pulse) * .78f),
                    radius = half * (.72f + pulse * .26f),
                )
            }
        }
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            BadgedBox(
                badge = {
                    if (conversationCount > 0) {
                        Badge(
                            containerColor = MinkFeatureColors.ConversationCount,
                            contentColor = readableContentColor(MinkFeatureColors.ConversationCount),
                        ) {
                            Text(
                                if (conversationCount > 99) {
                                    stringResource(R.string.notification_count_overflow)
                                } else {
                                    conversationCount.toString()
                                },
                            )
                        }
                    }
                },
            ) {
                Icon(
                    Icons.Default.Forum,
                    stringResource(R.string.conversations),
                    Modifier.size(iconSize).scale(if (needsAttention) .9f + pulse * .24f else 1f),
                    tint = if (needsAttention) alertContentColor else restingColor,
                )
            }
        }
    }
}

/** Exists only while attention is required, so the idle launcher owns no running animation. */
@Composable
private fun conversationAttentionPulse(): Float {
    val transition = rememberInfiniteTransition(label = "conversation attention")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "conversation attention blink",
    )
    return pulse
}
