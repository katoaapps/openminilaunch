package com.katoaapps.openminilaunch.ui.launcher

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.LightInk
import com.katoaapps.openminilaunch.ui.theme.MagicTodoColor
import kotlin.math.roundToInt

@Composable
internal fun TodoFlightChip(
    text: String,
    progress: Animatable<Float, *>,
    start: Offset,
    destination: Offset,
) {
    val value = progress.value
    val position = Offset(
        x = start.x + (destination.x - start.x) * value,
        y = start.y + (destination.y - start.y) * value,
    )
    val alpha = if (value < .72f) 1f else ((1f - value) / .28f).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier
            .offset { IntOffset((position.x - 100).roundToInt(), (position.y - 26).roundToInt()) }
            .graphicsLayer { this.alpha = alpha }
            .zIndex(10f)
            .shadow(Dimens.dp8, RoundedCornerShape(Dimens.dp18)),
        color = MagicTodoColor,
        shape = RoundedCornerShape(Dimens.dp18),
    ) {
        Row(
            Modifier.widthIn(max = Dimens.dp200)
                .padding(horizontal = Dimens.dp14, vertical = Dimens.dp9),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Checklist, null, Modifier.size(Dimens.dp18), tint = LightInk)
            Text(
                text,
                Modifier.padding(start = Dimens.dp7),
                maxLines = 1,
                color = LightInk,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            )
        }
    }
}
