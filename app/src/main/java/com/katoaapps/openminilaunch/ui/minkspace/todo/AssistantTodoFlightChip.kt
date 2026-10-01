package com.katoaapps.openminilaunch.ui.minkspace.todo

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import com.katoaapps.openminilaunch.ui.theme.Dimens

/** Assistant has no Home widget coordinates, so its saved todo flies out of Magic Mode. */
@Composable
internal fun AssistantTodoFlightChip(
    text: String,
    progress: Animatable<Float, *>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.zIndex(30f)) {
        val density = LocalDensity.current
        val availableWidth = maxWidth
        val availableHeight = maxHeight
        val widthPx = with(density) { availableWidth.toPx() }
        val heightPx = with(density) { availableHeight.toPx() }
        val bottomInsetPx = with(density) { Dimens.dp72.toPx() }
        val topInsetPx = with(density) { Dimens.dp72.toPx() }

        TodoFlightChip(
            text = text,
            progress = progress,
            start = Offset(widthPx / 2f, heightPx - bottomInsetPx),
            destination = Offset(widthPx / 2f, topInsetPx),
        )
    }
}
