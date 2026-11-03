@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.katoaapps.openminilaunch.ui.launcher.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.wellbeing.MinkAppAccessState
import com.katoaapps.openminilaunch.model.AppGridSlot
import com.katoaapps.openminilaunch.model.configurableAppGridSlots
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.appgrid.defaultIcon
import com.katoaapps.openminilaunch.ui.appgrid.displayLabel
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.components.minkBuiltInIconTint
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MinkTransparent
import com.katoaapps.openminilaunch.ui.wellbeing.MinkPausedBadge
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

@Composable
internal fun AppGrid(
    store: LauncherStore,
    actions: DeviceActions,
    appAccessState: MinkAppAccessState,
    onPausedApp: (String) -> Unit,
    onUnavailableApp: (String) -> Unit,
    onConfigureExtraApp: () -> Unit,
    compact: Boolean = false,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    itemContainerColor: Color = MinkTransparent,
    modifier: Modifier = Modifier,
    openDrawer: () -> Unit,
) {
    val launcherAppsRevision by actions.launcherAppsRevision.collectAsState()
    val launcherShortcutsRevision by actions.launcherShortcutsRevision.collectAsState()
    val builtInIconColor = store.iconAppearance.minkBuiltInIconTint(contentColor)
    var editing by remember { mutableStateOf(false) }
    val draftOrder = remember { mutableStateListOf<AppGridSlot>().apply { addAll(store.effectiveAppGridOrder) } }
    val gridState = rememberLazyGridState()
    val hapticFeedback = LocalHapticFeedback.current
    val reorderableState = rememberReorderableLazyGridState(gridState) { from, to ->
        if (from.index == draftOrder.lastIndex || to.index == draftOrder.lastIndex) {
            return@rememberReorderableLazyGridState
        }
        draftOrder[to.index] = draftOrder[from.index].also {
            draftOrder[from.index] = draftOrder[to.index]
        }
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun beginEditing() {
        if (store.demoSearchDataEnabled) return
        draftOrder.clear()
        draftOrder.addAll(store.effectiveAppGridOrder)
        editing = true
    }

    fun cancelEditing() {
        draftOrder.clear()
        draftOrder.addAll(store.effectiveAppGridOrder)
        editing = false
    }

    fun finishEditing() {
        store.setAppGridOrder(draftOrder.toList())
        editing = false
    }

    BackHandler(enabled = editing) { cancelEditing() }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.dp4)) {
        if (editing) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = ::cancelEditing) {
                    Icon(
                        Icons.Default.Close,
                        stringResource(R.string.cancel_shortcut_reorder),
                        tint = contentColor,
                    )
                }
                IconButton(onClick = ::finishEditing) {
                    Icon(
                        Icons.Default.Check,
                        stringResource(R.string.save_shortcut_order),
                        tint = contentColor,
                    )
                }
            }
        }
        val visibleOrder: List<AppGridSlot> = if (editing) draftOrder else store.effectiveAppGridOrder
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val slotSize = appGridSlotCellSizeDp(maxWidth.value, maxHeight.value).dp
            val dragShadowElevation = Dimens.dp14
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = Dimens.dp2),
                horizontalArrangement = Arrangement.spacedBy(Dimens.dp4),
                verticalArrangement = Arrangement.SpaceEvenly,
                userScrollEnabled = false,
            ) {
                items(visibleOrder, key = AppGridSlot::name) { slot ->
                    ReorderableItem(reorderableState, key = slot.name) { isDragging ->
                        val assignedTargetKey = store.appGridTargets[slot]
                        val assignedTarget = remember(
                            assignedTargetKey,
                            launcherAppsRevision,
                            launcherShortcutsRevision,
                        ) {
                            assignedTargetKey?.let(actions::resolveLauncherSelection)
                        }
                        val hasAssignedTarget = slot in configurableAppGridSlots && assignedTargetKey != null
                        val defaultTargetPackage = actions.appGridSlotTargetPackage(slot, null)
                        val pausedTargetPackage = assignedTarget?.packageName ?: defaultTargetPackage
                        val appPaused = assignedTarget?.let(appAccessState::isPaused)
                            ?: (defaultTargetPackage?.let(appAccessState::isPaused) == true)
                        val appUnavailable = assignedTarget?.isAvailable == false
                        val slotIndex = AppGridSlot.entries.indexOf(slot)
                        val jiggleAngle = if (editing && slot != AppGridSlot.DRAWER) {
                            val jiggle = rememberInfiniteTransition(label = "${slot.name} jiggle")
                            jiggle.animateFloat(
                                initialValue = if (slotIndex % 2 == 0) -1.15f else 1.15f,
                                targetValue = if (slotIndex % 2 == 0) 1.15f else -1.15f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(
                                        durationMillis = 125 + (slotIndex % 3) * 18,
                                        easing = LinearEasing,
                                    ),
                                    repeatMode = RepeatMode.Reverse,
                                ),
                                label = "${slot.name} rotation",
                            ).value
                        } else {
                            0f
                        }
                        val interactionSource = remember { MutableInteractionSource() }
                        val interactionModifier = if (editing && slot != AppGridSlot.DRAWER) {
                            Modifier.draggableHandle(
                                interactionSource = interactionSource,
                                onDragStarted = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                            )
                        } else if (!editing) {
                            Modifier.combinedClickable(
                                onClick = {
                                    if (appPaused) {
                                        onPausedApp(checkNotNull(pausedTargetPackage))
                                    } else if (appUnavailable) {
                                        onUnavailableApp(checkNotNull(assignedTarget).label)
                                    } else if (slot == AppGridSlot.EXTRA && assignedTargetKey == null) {
                                        onConfigureExtraApp()
                                    } else {
                                        actions.launchAppGridSlot(slot, assignedTargetKey, openDrawer)
                                    }
                                },
                                onLongClick = ::beginEditing,
                            )
                        } else {
                            Modifier
                        }
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier
                                    .size(slotSize)
                                    .animateItem()
                                    .zIndex(if (isDragging) 4f else 0f)
                                    .graphicsLayer {
                                        rotationZ = if (isDragging) 0f else jiggleAngle
                                        if (isDragging) {
                                            scaleX = 1.06f
                                            scaleY = 1.06f
                                            shadowElevation = dragShadowElevation.toPx()
                                        }
                                    }
                                    .clip(RoundedCornerShape(if (compact) Dimens.dp14 else Dimens.dp18))
                                    .background(
                                        if (editing) contentColor.copy(alpha = .17f)
                                        else itemContainerColor
                                    )
                                    .then(interactionModifier)
                                    .padding(if (hasAssignedTarget) Dimens.dp0 else Dimens.dp6),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (hasAssignedTarget) {
                                    LauncherTargetIcon(
                                        target = checkNotNull(assignedTarget),
                                        actions = actions,
                                        size = (slotSize * .56f).coerceIn(
                                            if (compact) Dimens.dp28 else Dimens.dp32,
                                            if (compact) Dimens.dp42 else Dimens.dp48,
                                        ),
                                        themedTint = contentColor,
                                        contentDescription = assignedTarget.label,
                                    )
                                    if (appPaused) {
                                        MinkPausedBadge(Modifier.align(Alignment.Center))
                                    }
                                } else {
                                    Icon(
                                        slot.defaultIcon(),
                                        slot.displayLabel(),
                                        Modifier.size(if (compact) Dimens.dp24 else Dimens.dp28),
                                        tint = builtInIconColor,
                                    )
                                }
                                if (editing && slot == AppGridSlot.DRAWER) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(if (compact) Dimens.dp3 else Dimens.dp4)
                                            .size(if (compact) Dimens.dp12 else Dimens.dp14),
                                        tint = contentColor,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
