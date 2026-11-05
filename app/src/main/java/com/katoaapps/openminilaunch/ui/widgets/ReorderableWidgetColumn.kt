package com.katoaapps.openminilaunch.ui.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.WidgetEntry
import com.katoaapps.openminilaunch.model.WidgetGridSize
import com.katoaapps.openminilaunch.model.WidgetProviderIdentity
import com.katoaapps.openminilaunch.model.WidgetRestoreRecipe
import com.katoaapps.openminilaunch.ui.components.moveItem
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
internal fun ReorderableWidgetColumn(
    entries: List<WidgetEntry>,
    host: AppWidgetHost,
    manager: AppWidgetManager,
    availableProviders: List<AppWidgetProviderInfo>,
    installedPackages: Set<String>,
    widgetInfoRevision: Int,
    launcherAppsRevision: Long,
    onRetryUnavailableWidget: () -> Unit,
    onOrderChange: (List<String>) -> Unit,
    onRemove: (WidgetEntry) -> Unit,
    onResize: (Int, WidgetGridSize) -> Unit,
    onBindReplacement: (String, AppWidgetProviderInfo, WidgetGridSize) -> Unit,
    onChooseReplacement: (WidgetEntry.RestorePlaceholder) -> Unit,
    onReinstall: (WidgetProviderIdentity) -> Unit,
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val visibleEntries = remember {
        mutableStateListOf<WidgetEntry>().apply { addAll(entries) }
    }
    var draggedEntryId by remember { mutableStateOf<String?>(null) }
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = listState,
        scrollThresholdPadding = WindowInsets.systemBars.asPaddingValues(),
    ) { from, to ->
        if (visibleEntries.moveItem(from.index, to.index)) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    val moveUpLabel = stringResource(R.string.move_up)
    val moveDownLabel = stringResource(R.string.move_down)

    fun moveWidgetForAccessibility(entryId: String, direction: Int): Boolean {
        val fromIndex = visibleEntries.indexOfFirst { it.entryId == entryId }
        val toIndex = fromIndex + direction
        if (fromIndex < 0 || toIndex !in visibleEntries.indices) return false
        if (!visibleEntries.moveItem(fromIndex, toIndex)) return false
        onOrderChange(visibleEntries.map(WidgetEntry::entryId))
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        return true
    }

    LaunchedEffect(entries.toList(), draggedEntryId) {
        if (draggedEntryId == null && visibleEntries != entries) {
            visibleEntries.clear()
            visibleEntries.addAll(entries)
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Dimens.dp20, end = Dimens.dp48),
        ) {
            itemsIndexed(visibleEntries, key = { _, entry -> entry.entryId }) { index, entry ->
                ReorderableItem(reorderableState, key = entry.entryId) { isDragging ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val scale by animateFloatAsState(
                        targetValue = if (isDragging) 1.025f else 1f,
                        label = "widget drag scale",
                    )
                    val elevation by animateFloatAsState(
                        targetValue = if (isDragging) 12f else 0f,
                        label = "widget drag elevation",
                    )
                    val accessibilityActions = buildList {
                        if (index > 0) {
                            add(
                                CustomAccessibilityAction(moveUpLabel) {
                                    moveWidgetForAccessibility(entry.entryId, -1)
                                },
                            )
                        }
                        if (index < visibleEntries.lastIndex) {
                            add(
                                CustomAccessibilityAction(moveDownLabel) {
                                    moveWidgetForAccessibility(entry.entryId, 1)
                                },
                            )
                        }
                    }
                    val itemModifier = Modifier
                        .padding(bottom = Dimens.dp20)
                        .semantics { customActions = accessibilityActions }
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            shadowElevation = elevation
                        }
                    val dragHandle: @Composable () -> Unit = {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = stringResource(R.string.drag_to_reorder),
                            tint = if (isDragging) MaterialTheme.colorScheme.primary else Muted,
                            modifier = Modifier
                                .size(Dimens.dp44)
                                .draggableHandle(
                                    interactionSource = interactionSource,
                                    onDragStarted = {
                                        draggedEntryId = entry.entryId
                                        hapticFeedback.performHapticFeedback(
                                            HapticFeedbackType.LongPress,
                                        )
                                    },
                                    onDragStopped = {
                                        onOrderChange(visibleEntries.map(WidgetEntry::entryId))
                                        draggedEntryId = null
                                    },
                                )
                                .padding(Dimens.dp10),
                        )
                    }
                    when (entry) {
                        is WidgetEntry.Bound -> {
                            val info = remember(
                                entry.appWidgetId,
                                widgetInfoRevision,
                                launcherAppsRevision,
                            ) { manager.getAppWidgetInfo(entry.appWidgetId) }
                            if (info != null) {
                                WidgetPanel(
                                    host = host,
                                    id = entry.appWidgetId,
                                    info = info,
                                    gridSize = entry.gridSize
                                        ?: widgetSizeRange(
                                            info,
                                            context.resources.displayMetrics.density,
                                        ).preferred,
                                    onRemove = { onRemove(entry) },
                                    onResize = { onResize(entry.appWidgetId, it) },
                                    modifier = itemModifier,
                                    dragHandle = dragHandle,
                                )
                            } else if (entry.provider != null && entry.gridSize != null) {
                                RestorableWidgetPanel(
                                    entryId = entry.entryId,
                                    provider = entry.provider,
                                    gridSize = entry.gridSize,
                                    availableProviders = availableProviders,
                                    packageInstalled = entry.provider.packageName in installedPackages,
                                    onBindReplacement = onBindReplacement,
                                    onChooseReplacement = {
                                        onChooseReplacement(
                                            WidgetEntry.RestorePlaceholder(
                                                WidgetRestoreRecipe(
                                                    entry.entryId,
                                                    entry.provider,
                                                    entry.gridSize,
                                                ),
                                            ),
                                        )
                                    },
                                    onReinstall = onReinstall,
                                    onRemove = { onRemove(entry) },
                                    modifier = itemModifier,
                                    dragHandle = dragHandle,
                                )
                            } else {
                                UnavailableWidgetPanel(
                                    onRetry = onRetryUnavailableWidget,
                                    onRemove = { onRemove(entry) },
                                    modifier = itemModifier,
                                    dragHandle = dragHandle,
                                )
                            }
                        }
                        is WidgetEntry.RestorePlaceholder -> RestorableWidgetPanel(
                            entryId = entry.entryId,
                            provider = entry.recipe.provider,
                            gridSize = entry.gridSize,
                            availableProviders = availableProviders,
                            packageInstalled = entry.recipe.provider.packageName in installedPackages,
                            onBindReplacement = onBindReplacement,
                            onChooseReplacement = { onChooseReplacement(entry) },
                            onReinstall = onReinstall,
                            onRemove = { onRemove(entry) },
                            modifier = itemModifier,
                            dragHandle = dragHandle,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(Dimens.dp12)) }
        }
        if (listState.canScrollBackward || listState.canScrollForward) {
            WidgetScrollIndicator(
                state = listState,
                widgetCount = visibleEntries.size,
                contentDescription = stringResource(R.string.scroll_widgets),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = Dimens.dp10),
            )
        }
    }
}
