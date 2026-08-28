@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.katoaapps.openminilaunch.ui.launcher

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.*
import com.katoaapps.openminilaunch.model.*
import com.katoaapps.openminilaunch.platform.*
import com.katoaapps.openminilaunch.features.conversations.*
import com.katoaapps.openminilaunch.features.magic.*
import com.katoaapps.openminilaunch.features.todos.*
import com.katoaapps.openminilaunch.features.wellbeing.*
import com.katoaapps.openminilaunch.ui.components.*
import com.katoaapps.openminilaunch.ui.theme.*

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import kotlin.math.ceil

@Composable
internal fun TodoPager(
    store: LauncherStore,
    openTodos: () -> Unit,
    jumpToken: Int,
    itemsPerPage: Int = 3,
    compact: Boolean = false,
    embedded: Boolean = false,
    contentColor: Color = LightPaper,
    mutedContentColor: Color = Sage,
    insetColor: Color = MinkForestPanel,
    modifier: Modifier = Modifier,
) {
    val safeItemsPerPage = itemsPerPage.coerceIn(1, 5)
    val pages = maxOf(1, ceil(store.todos.size / safeItemsPerPage.toFloat()).toInt())
    val pagerState = rememberPagerState(pageCount = { pages })
    LaunchedEffect(pages) {
        if (pagerState.currentPage >= pages) pagerState.scrollToPage(pages - 1)
    }
    LaunchedEffect(jumpToken, pages) {
        if (jumpToken > 0) {
            val newestUnfinishedPage = store.todos.indexOfLast { !it.completed }
                .coerceAtLeast(0) / safeItemsPerPage
            pagerState.animateScrollToPage(newestUnfinishedPage.coerceAtMost(pages - 1))
        }
    }
    val shape = RoundedCornerShape(if (compact) Dimens.dp18 else Dimens.dp24)
    Column(
        modifier.fillMaxWidth().clip(shape)
            .background(if (embedded) insetColor else MinkForestPanel)
            .then(if (embedded) Modifier else Modifier.border(Dimens.dp1, mutedContentColor.copy(alpha = .42f), shape))
            .clickable(onClick = openTodos)
            .padding(if (compact) Dimens.dp8 else Dimens.dp16),
        verticalArrangement = Arrangement.spacedBy(if (compact) Dimens.dp3 else Dimens.dp8),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.todo_heading), color = contentColor, fontWeight = FontWeight.Black, letterSpacing = Dimens.sp1, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.page_of_pages, pagerState.currentPage + 1, pages), color = mutedContentColor, fontSize = Dimens.sp12)
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().weight(1f)) { page ->
            val pageItems = store.todos
                .drop(page * safeItemsPerPage)
                .take(safeItemsPerPage)
            if (pageItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    Text(stringResource(R.string.tap_to_add_first_todo), color = mutedContentColor)
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(if (compact) Dimens.dp5 else Dimens.dp8)) {
                    pageItems.forEach { item ->
                        Row(
                            Modifier.fillMaxWidth().weight(1f),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Checkbox(
                                checked = item.completed,
                                onCheckedChange = { store.toggleTodo(item.id) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = contentColor,
                                    uncheckedColor = mutedContentColor,
                                    checkmarkColor = if (contentColor == MinkWhite) MinkBlack else MinkWhite,
                                ),
                                modifier = Modifier.size(if (compact) Dimens.dp24 else Dimens.dp26),
                            )
                            Text(
                                item.text,
                                color = if (item.completed) mutedContentColor else contentColor,
                                fontSize = if (compact) Dimens.sp13 else Dimens.sp15,
                                lineHeight = if (compact) Dimens.sp17 else Dimens.sp20,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textDecoration = if (item.completed) TextDecoration.LineThrough else null,
                                modifier = Modifier.padding(start = Dimens.dp7, top = Dimens.dp2).weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ShortcutGrid(
    store: LauncherStore,
    actions: DeviceActions,
    openTodos: () -> Unit,
    compact: Boolean = false,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    itemContainerColor: Color = MinkTransparent,
    modifier: Modifier = Modifier,
    openDrawer: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    val draftOrder = remember { mutableStateListOf<Shortcut>().apply { addAll(store.effectiveShortcutOrder) } }
    val gridState = rememberLazyGridState()
    val hapticFeedback = LocalHapticFeedback.current
    val reorderableState = rememberReorderableLazyGridState(gridState) { from, to ->
        draftOrder[to.index] = draftOrder[from.index].also {
            draftOrder[from.index] = draftOrder[to.index]
        }
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun beginEditing() {
        if (store.demoSearchDataEnabled) return
        draftOrder.clear()
        draftOrder.addAll(store.effectiveShortcutOrder)
        editing = true
    }

    fun cancelEditing() {
        draftOrder.clear()
        draftOrder.addAll(store.effectiveShortcutOrder)
        editing = false
    }

    fun finishEditing() {
        store.setShortcutOrder(draftOrder.toList())
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
                IconButton(onClick = ::cancelEditing) { Icon(Icons.Default.Close, stringResource(R.string.cancel_shortcut_reorder), tint = contentColor) }
                IconButton(onClick = ::finishEditing) { Icon(Icons.Default.Check, stringResource(R.string.save_shortcut_order), tint = contentColor) }
            }
        }
        val visibleOrder: List<Shortcut> = if (editing) draftOrder else store.effectiveShortcutOrder
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val shortcutSize = shortcutCellSizeDp(maxWidth.value, maxHeight.value).dp
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
                items(visibleOrder, key = Shortcut::name) { shortcut ->
                    ReorderableItem(reorderableState, key = shortcut.name) { isDragging ->
                    val assignedPackage = store.shortcutPackages[shortcut]
                    val hasAssignedApp = shortcut in configurableShortcuts && assignedPackage != null
                    val shortcutIndex = Shortcut.entries.indexOf(shortcut)
                    val jiggleAngle = if (editing) {
                        val jiggle = rememberInfiniteTransition(label = "${shortcut.name} jiggle")
                        jiggle.animateFloat(
                            initialValue = if (shortcutIndex % 2 == 0) -1.15f else 1.15f,
                            targetValue = if (shortcutIndex % 2 == 0) 1.15f else -1.15f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(
                                    durationMillis = 125 + (shortcutIndex % 3) * 18,
                                    easing = LinearEasing,
                                ),
                                repeatMode = RepeatMode.Reverse,
                            ),
                            label = "${shortcut.name} rotation",
                        ).value
                    } else {
                        0f
                    }
                    val interactionSource = remember { MutableInteractionSource() }
                    val interactionModifier = if (editing) {
                        Modifier.draggableHandle(
                            interactionSource = interactionSource,
                            onDragStarted = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                        )
                    } else {
                        Modifier.combinedClickable(
                            onClick = {
                                actions.launchShortcut(shortcut, store.shortcutPackages[shortcut], openTodos, openDrawer)
                            },
                            onLongClick = ::beginEditing,
                        )
                    }
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier
                                    .size(shortcutSize)
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
                                    .padding(if (hasAssignedApp) Dimens.dp0 else Dimens.dp6),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (hasAssignedApp) {
                                    AppIcon(
                                        packageName = checkNotNull(assignedPackage),
                                        actions = actions,
                                        size = (shortcutSize * .56f).coerceIn(
                                            if (compact) Dimens.dp28 else Dimens.dp32,
                                            if (compact) Dimens.dp42 else Dimens.dp48,
                                        ),
                                        themedTint = contentColor,
                                        contentDescription = actions.appLabel(assignedPackage),
                                    )
                                } else {
                                    Icon(
                                        shortcut.defaultIcon(),
                                        shortcut.displayLabel(),
                                        Modifier.size(if (compact) Dimens.dp24 else Dimens.dp28),
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
