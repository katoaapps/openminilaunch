package org.sableos.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay

/** One action for [SableActionBar]. Delete never goes here (BB10): it lives in the row's menu. */
data class SableAction(
    val label: String,
    val onClick: () -> Unit,
)

/** How [SableActionBar] splits its actions between the bar and the overflow menu. */
object SableActionBarLayout {
    /** Actions shown on the bar besides the signature action; more than this go to the overflow. */
    const val MAX_INLINE = 2

    /** How long a [SableUndoBar] stays before the deletion is final. */
    const val UNDO_MILLIS = 5_000L

    /**
     * The actions on the bar and those in the overflow. When they all fit they all show; otherwise the
     * overflow button takes the last slot.
     */
    fun <T> split(
        actions: List<T>,
        inline: Int = MAX_INLINE,
    ): Pair<List<T>, List<T>> =
        if (actions.size <= inline) {
            actions to emptyList()
        } else {
            actions.take((inline - 1).coerceAtLeast(0)) to actions.drop((inline - 1).coerceAtLeast(0))
        }
}

/**
 * The BlackBerry 10 bottom action bar: one signature action in the accent colour on the left, up to
 * [SableActionBarLayout.MAX_INLINE] more beside it, and the rest under "More actions". Flat on the
 * screen's black with a thin line above, like a BB10 list.
 */
@Composable
fun SableActionBar(
    signature: SableAction?,
    actions: List<SableAction>,
    modifier: Modifier = Modifier,
) {
    val (inline, overflow) = SableActionBarLayout.split(actions)
    var menuOpen by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = SableSize.TouchTarget)
                    .padding(horizontal = SableSpacing.Sm),
            horizontalArrangement = Arrangement.spacedBy(SableSpacing.Xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            signature?.let { BarButton(it, accent = true, modifier = Modifier.weight(1f)) }
            inline.forEach { BarButton(it, accent = false, modifier = Modifier.weight(1f)) }
            if (overflow.isNotEmpty()) {
                Box(modifier = Modifier.weight(1f)) {
                    BarButton(SableAction(DesignText.t("sable_more_actions")) { menuOpen = true }, accent = false)
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        overflow.forEach { action ->
                            DropdownMenuItem(
                                text = { Text(action.label) },
                                onClick = {
                                    menuOpen = false
                                    action.onClick()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarButton(
    action: SableAction,
    accent: Boolean,
    modifier: Modifier = Modifier,
) {
    TextButton(onClick = action.onClick, modifier = modifier.fillMaxWidth()) {
        Text(
            text = action.label,
            color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The BB10 undo toast after a Delete: "[message]" with an Undo button, at the bottom of the screen.
 * It goes after [SableActionBarLayout.UNDO_MILLIS], when [onExpire] makes the deletion final; Undo
 * calls [onUndo] instead. A new [message] restarts the time.
 */
@Composable
fun SableUndoBar(
    message: String,
    onUndo: () -> Unit,
    onExpire: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expire by rememberUpdatedState(onExpire)
    LaunchedEffect(message) {
        delay(SableActionBarLayout.UNDO_MILLIS)
        expire()
    }
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = SableSize.TouchTarget)
                    .padding(start = SableSpacing.Lg, end = SableSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = onUndo) {
                Text(
                    text = DesignText.t("sable_undo"),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
