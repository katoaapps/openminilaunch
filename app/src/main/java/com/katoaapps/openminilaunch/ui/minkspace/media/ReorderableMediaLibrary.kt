package com.katoaapps.openminilaunch.ui.minkspace.media

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkspace.media.MediaItem
import com.katoaapps.openminilaunch.ui.components.moveItem
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
internal fun ReorderableMediaLibrary(
    items: List<MediaItem>,
    focusedItemKey: String?,
    onFocus: (MediaItem) -> Unit,
    onOrderChange: (List<Uri>) -> Unit,
    onRemove: (MediaItem) -> Unit,
) {
    val visibleItems = remember { mutableStateListOf<MediaItem>().apply { addAll(items) } }
    var draggedItemKey by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val hapticFeedback = LocalHapticFeedback.current
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        if (visibleItems.moveItem(from.index, to.index)) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    LaunchedEffect(items.toList(), draggedItemKey) {
        if (draggedItemKey == null && visibleItems != items) {
            visibleItems.clear()
            visibleItems.addAll(items)
        }
    }
    LaunchedEffect(focusedItemKey, draggedItemKey) {
        if (draggedItemKey == null) {
            val index = visibleItems.indexOfFirst { it.uri.toString() == focusedItemKey }
            if (index >= 0) listState.animateScrollToItem(index)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = Dimens.dp240)
            .padding(horizontal = Dimens.dp16, vertical = Dimens.dp8),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(Dimens.dp6),
    ) {
        items(visibleItems, key = { item -> item.uri.toString() }) { item ->
            ReorderableItem(reorderableState, key = item.uri.toString()) { isDragging ->
                val interactionSource = remember { MutableInteractionSource() }
                val scale by animateFloatAsState(
                    targetValue = if (isDragging) 1.025f else 1f,
                    label = "media drag scale",
                )
                val elevation by animateFloatAsState(
                    targetValue = if (isDragging) 12f else 0f,
                    label = "media drag elevation",
                )
                MediaLibraryRow(
                    item = item,
                    selected = focusedItemKey == item.uri.toString(),
                    onFocus = { onFocus(item) },
                    onRemove = { onRemove(item) },
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            shadowElevation = elevation
                        },
                    dragHandle = {
                        Icon(
                            Icons.Default.DragHandle,
                            stringResource(R.string.drag_to_reorder),
                            Modifier
                                .size(Dimens.dp44)
                                .draggableHandle(
                                    interactionSource = interactionSource,
                                    onDragStarted = {
                                        draggedItemKey = item.uri.toString()
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragStopped = {
                                        onOrderChange(visibleItems.map(MediaItem::uri))
                                        draggedItemKey = null
                                    },
                                )
                                .padding(Dimens.dp10),
                            tint = if (isDragging) MaterialTheme.colorScheme.primary else Muted,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun MediaLibraryRow(
    item: MediaItem,
    selected: Boolean,
    onFocus: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandle: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.dp14),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.dp14))
                .clickable(onClick = onFocus)
                .padding(end = Dimens.dp4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dragHandle()
            Column(Modifier.weight(1f).padding(vertical = Dimens.dp8)) {
                Text(
                    text = item.displayName,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (item.isVideo) {
                        stringResource(R.string.video)
                    } else {
                        stringResource(R.string.image_or_gif)
                    },
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, stringResource(R.string.remove))
            }
        }
    }
}
