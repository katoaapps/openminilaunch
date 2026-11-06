package com.katoaapps.openminilaunch.ui.appgrid

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.model.AppGridAssignment
import com.katoaapps.openminilaunch.model.AppGridSlot
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.model.MAX_DRAWER_APPS
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.components.moveItem
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

/** Fixed 4 x 2 Home layout editor. Quick Drawer is always the locked eighth slot. */
@Composable
internal fun AppGridOrderEditor(
    store: LauncherStore,
    actions: DeviceActions,
    resolvedTargets: Map<String, LauncherTarget>,
    onSlotClick: (AppGridSlot) -> Unit,
) {
    val storedOrder = store.appGridOrder.toList()
    val visibleOrder = remember {
        mutableStateListOf<AppGridSlot>().apply { addAll(storedOrder) }
    }
    var draggedSlot by remember { mutableStateOf<AppGridSlot?>(null) }
    val gridState = rememberLazyGridState()
    val hapticFeedback = LocalHapticFeedback.current
    val reorderableState = rememberReorderableLazyGridState(gridState) { from, to ->
        val moving = visibleOrder.getOrNull(from.index)
        val destination = visibleOrder.getOrNull(to.index)
        if (
            moving != null && destination != null &&
            moving != AppGridSlot.DRAWER && destination != AppGridSlot.DRAWER &&
            from.index != to.index
        ) {
            visibleOrder.moveItem(from.index, to.index)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    LaunchedEffect(storedOrder, draggedSlot) {
        if (draggedSlot == null && visibleOrder != storedOrder) {
            visibleOrder.clear()
            visibleOrder.addAll(storedOrder)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        state = gridState,
        userScrollEnabled = false,
        modifier = Modifier.fillMaxWidth().height(214.dp),
        contentPadding = PaddingValues(vertical = Dimens.dp4),
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp6),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
    ) {
        items(visibleOrder, key = AppGridSlot::name) { slot ->
            ReorderableItem(reorderableState, key = slot.name) { isDragging ->
                val interactionSource = remember { MutableInteractionSource() }
                val assignment = store.appGridAssignment(slot)
                val folder = (assignment as? AppGridAssignment.Folder)?.let {
                    store.appFolder(it.folderId)
                }
                val targetKey = store.appGridTargetKey(slot)
                val subtitle = when {
                    slot == AppGridSlot.DRAWER -> stringResource(
                        R.string.count_of_max,
                        store.drawerTargets.size,
                        MAX_DRAWER_APPS,
                    )
                    folder != null -> folder.customName ?: pluralStringResource(
                        R.plurals.apps_selected,
                        folder.orderedTargetKeys.size,
                        folder.orderedTargetKeys.size,
                    )
                    else -> targetKey?.let(resolvedTargets::get)?.label
                        ?: targetKey?.let { "…" }
                        ?: stringResource(R.string.shortcut_default, slot.displayLabel())
                }
                val scale by animateFloatAsState(
                    targetValue = if (isDragging) 1.04f else 1f,
                    label = "app grid drag scale",
                )
                val elevation by animateFloatAsState(
                    targetValue = if (isDragging) 12f else 0f,
                    label = "app grid drag elevation",
                )

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
                        .clickable { onSlotClick(slot) }
                        .padding(horizontal = Dimens.dp4, vertical = Dimens.dp6),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = Dimens.dp18),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            when (assignment) {
                                AppGridAssignment.DefaultAction -> Icon(
                                    slot.defaultIcon(),
                                    null,
                                    Modifier.size(Dimens.dp28),
                                )
                                is AppGridAssignment.Target -> {
                                    val target = resolvedTargets[assignment.selectionKey]
                                    if (target != null) {
                                        LauncherTargetIcon(
                                            target = target,
                                            actions = actions,
                                            size = Dimens.dp30,
                                            contentDescription = target.label,
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Apps,
                                            contentDescription = null,
                                            modifier = Modifier.size(Dimens.dp30),
                                            tint = Muted,
                                        )
                                    }
                                }
                                is AppGridAssignment.Folder -> AppFolderIcon(
                                    targetKeys = folder?.orderedTargetKeys.orEmpty(),
                                    resolvedTargets = folder?.orderedTargetKeys.orEmpty()
                                        .mapNotNull(resolvedTargets::get),
                                    actions = actions,
                                    size = Dimens.dp30,
                                    backgroundColor = folder?.backgroundColor(
                                        defaultColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        themeColor = MaterialTheme.colorScheme.primaryContainer,
                                    ) ?: MaterialTheme.colorScheme.surfaceContainerHigh,
                                    indicatorStyle = store.folderIndicatorStyle,
                                )
                            }
                        }
                        Text(
                            slot.displaySlotLabel(),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = Dimens.sp11,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp3),
                        )
                        Text(
                            subtitle,
                            color = Muted,
                            fontSize = Dimens.sp10,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (slot != AppGridSlot.DRAWER) {
                        Icon(
                            Icons.Default.DragHandle,
                            stringResource(R.string.drag_to_reorder),
                            Modifier
                                .align(Alignment.CenterStart)
                                .size(Dimens.dp28)
                                .draggableHandle(
                                    interactionSource = interactionSource,
                                    onDragStarted = {
                                        draggedSlot = slot
                                        hapticFeedback.performHapticFeedback(
                                            HapticFeedbackType.LongPress,
                                        )
                                    },
                                    onDragStopped = {
                                        store.setAppGridOrder(visibleOrder.toList())
                                        draggedSlot = null
                                    },
                                )
                                .padding(Dimens.dp4),
                            tint = if (isDragging) MaterialTheme.colorScheme.primary else Muted,
                        )
                    } else {
                        Icon(
                            Icons.Default.Lock,
                            stringResource(R.string.quick_drawer),
                            Modifier.align(Alignment.CenterEnd).size(Dimens.dp28).padding(Dimens.dp5),
                            tint = Muted,
                        )
                    }
                }
            }
        }
    }
}
