package com.katoaapps.openminilaunch.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

/**
 * Shared ordered-target editor used by App Grid folders and Quick Drawer.
 *
 * The caller owns the draft list. Dragging only updates that draft; the enclosing dialog decides
 * whether Done commits it or dismissal discards it.
 */
@Composable
internal fun ReorderableLauncherTargetGrid(
    targets: List<LauncherTarget>,
    actions: DeviceActions,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onRemove: (LauncherTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val hapticFeedback = LocalHapticFeedback.current
    val reorderableState = rememberReorderableLazyGridState(gridState) { from, to ->
        if (from.index in targets.indices && to.index in targets.indices && from.index != to.index) {
            onMove(from.index, to.index)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    val visibleRows = targets.size.coerceAtMost(8).let { (it + 3) / 4 }.coerceAtLeast(1)
    val minimumHeight = (visibleRows * 82).dp

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        state = gridState,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minimumHeight, max = 172.dp),
        contentPadding = PaddingValues(vertical = Dimens.dp4),
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp6),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp6),
    ) {
        items(targets, key = LauncherTarget::selectionKey) { target ->
            ReorderableItem(reorderableState, key = target.selectionKey) { isDragging ->
                val interactionSource = remember { MutableInteractionSource() }
                val scale = animateFloatAsState(
                    targetValue = if (isDragging) 1.04f else 1f,
                    label = "selected target drag scale",
                ).value
                val elevation = animateFloatAsState(
                    targetValue = if (isDragging) 12f else 0f,
                    label = "selected target drag elevation",
                ).value

                Box(
                    Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            shadowElevation = elevation
                        }
                        .clip(RoundedCornerShape(Dimens.dp14))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable { onRemove(target) }
                        .padding(horizontal = Dimens.dp4, vertical = Dimens.dp5),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = Dimens.dp18),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (target.isAvailable) {
                            LauncherTargetIcon(
                                target = target,
                                actions = actions,
                                size = Dimens.dp30,
                                contentDescription = target.label,
                            )
                        } else {
                            UnavailableLauncherTargetIcon(
                                size = Dimens.dp30,
                                contentDescription = stringResource(R.string.app_unavailable),
                            )
                        }
                        Text(
                            if (target.isAvailable) target.label else stringResource(R.string.app_unavailable),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            fontSize = Dimens.sp10,
                            modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp2),
                        )
                    }
                    Icon(
                        Icons.Default.DragHandle,
                        stringResource(R.string.drag_to_reorder),
                        Modifier
                            .align(Alignment.CenterStart)
                            .size(Dimens.dp28)
                            .draggableHandle(
                                interactionSource = interactionSource,
                                onDragStarted = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                            )
                            .padding(Dimens.dp4),
                        tint = if (isDragging) MaterialTheme.colorScheme.primary else Muted,
                    )
                    Icon(
                        Icons.Default.Close,
                        stringResource(R.string.remove_app, target.label),
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(Dimens.dp14),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
