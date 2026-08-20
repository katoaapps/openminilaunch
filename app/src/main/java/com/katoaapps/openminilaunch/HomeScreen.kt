@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.katoaapps.openminilaunch

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
internal fun HomeScreen(
    store: LauncherStore,
    actions: DeviceActions,
    openSettings: () -> Unit,
    openTodos: () -> Unit,
    openHub: () -> Unit,
    openMinkDay: () -> Unit,
    minkStatusActive: Boolean,
    onMagicExpandedChange: (Boolean) -> Unit,
    keyboardInputEnabled: Boolean,
) {
    val context = LocalContext.current
    var drawerOpen by remember { mutableStateOf(false) }
    var todoJumpToken by remember { mutableIntStateOf(0) }
    var flyingTodo by remember { mutableStateOf<String?>(null) }
    var flightActive by remember { mutableStateOf(false) }
    var widgetCenter by remember { mutableStateOf(Offset.Zero) }
    var magicCenter by remember { mutableStateOf(Offset.Zero) }
    var magicExpanded by remember { mutableStateOf(false) }
    var showLockDisclosure by remember { mutableStateOf(false) }
    val flightProgress = remember { Animatable(0f) }
    val homePanelColor = Color(store.homePanelColorArgb)
    val homePanelContentColor = readableContentColor(homePanelColor)
    val homePanelMutedColor = homePanelContentColor.copy(alpha = .68f)
    val homePanelInsetColor = if (homePanelContentColor == MinkWhite) {
        MinkBlack.copy(alpha = .16f)
    } else {
        MinkWhite.copy(alpha = .24f)
    }
    val lockServiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (actions.isLockServiceEnabled()) actions.lockDevice()
    }

    fun lockFromHome() {
        if (!actions.supportsLockScreenAction()) {
            Toast.makeText(context, context.getString(R.string.double_tap_lock_requires_android_9), Toast.LENGTH_SHORT).show()
        } else if (!actions.lockDevice()) {
            showLockDisclosure = true
        }
    }

    LaunchedEffect(flyingTodo) {
        if (flyingTodo != null && widgetCenter != Offset.Zero && magicCenter != Offset.Zero) {
            flightProgress.snapTo(0f)
            flightActive = true
            flightProgress.animateTo(1f, tween(1_300, easing = FastOutSlowInEasing))
            flightActive = false
            flyingTodo = null
        }
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().pointerInput(Unit) {
            var distance = 0f
            detectVerticalDragGestures(
                onDragStart = { distance = 0f },
                onVerticalDrag = { _, amount -> if (amount > 0) distance += amount },
                onDragEnd = { if (distance > 140f) actions.expandNotificationShade() },
            )
        }.pointerInput(magicExpanded) {
            if (!magicExpanded) detectTapGestures(onDoubleTap = { lockFromHome() })
        },
    ) {
        val qwertyHome = maxHeight <= maxWidth * 1.55f
        val homeHorizontalPadding = if (qwertyHome) Dimens.dp14 else Dimens.dp22
        val headerActionSize = if (qwertyHome) Dimens.dp40 else Dimens.dp48
        val headerIconSize = if (qwertyHome) Dimens.dp21 else Dimens.dp24
        val focusPanelHeight = (maxWidth * .78f).coerceIn(Dimens.dp310, Dimens.dp350)
        val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val softInputRowHeight = if (magicExpanded) Dimens.dp0 else (imeBottom - navigationBottom).coerceAtLeast(Dimens.dp0)
        Column(
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .blur(if (magicExpanded) Dimens.dp10 else Dimens.dp0),
        ) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides headerActionSize) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding()
                        .padding(horizontal = homeHorizontalPadding)
                        .padding(vertical = if (qwertyHome) Dimens.dp0 else Dimens.dp8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MinkHomeIcon(
                        store = store,
                        isActive = minkStatusActive,
                        onClick = openMinkDay,
                        modifier = Modifier.size(headerActionSize),
                    )
                    Text(
                        LocalDate.now().format(DateTimeFormatter.ofPattern(stringResource(R.string.home_date_pattern))).uppercase(),
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = Dimens.sp1_5,
                        fontSize = Dimens.sp13,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).clickable {
                            if (!actions.openClock()) {
                                Toast.makeText(context, R.string.no_clock_app_found, Toast.LENGTH_SHORT).show()
                            }
                        },
                    )
                    IconButton(onClick = openHub, modifier = Modifier.size(headerActionSize)) {
                        BadgedBox(
                            badge = {
                                val count = NotificationHub.conversations().size
                                if (count > 0) Badge {
                                    Text(if (count > 99) stringResource(R.string.notification_count_overflow) else count.toString())
                                }
                            },
                        ) { Icon(Icons.Default.Forum, stringResource(R.string.conversations), Modifier.size(headerIconSize)) }
                    }
                    IconButton(onClick = openSettings, modifier = Modifier.size(headerActionSize)) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings), Modifier.size(headerIconSize))
                    }
                }
            }
            BoxWithConstraints(
                Modifier.fillMaxWidth().weight(1f)
                    .padding(horizontal = homeHorizontalPadding, vertical = if (qwertyHome) Dimens.dp2 else Dimens.dp10),
            ) {
                val todoPanelHeight = if (qwertyHome) maxHeight else minOf(maxHeight, focusPanelHeight)
                val todoItemsPerPage = visibleTodoItemsForHeight(todoPanelHeight.value)
                val focusModifier = if (qwertyHome) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.fillMaxWidth().height(focusPanelHeight).align(Alignment.TopCenter)
                }
                Surface(
                    modifier = focusModifier.widthIn(max = Dimens.dp620).align(if (qwertyHome) Alignment.Center else Alignment.TopCenter),
                    shape = RoundedCornerShape(if (qwertyHome) Dimens.dp26 else Dimens.dp34),
                    color = homePanelColor,
                    contentColor = homePanelContentColor,
                    shadowElevation = if (isSystemInDarkTheme()) Dimens.dp2 else Dimens.dp8,
                    tonalElevation = Dimens.dp1,
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(if (qwertyHome) Dimens.dp10 else Dimens.dp14),
                        horizontalArrangement = Arrangement.spacedBy(if (qwertyHome) Dimens.dp8 else Dimens.dp12),
                    ) {
                        TodoPager(
                            store,
                            openTodos,
                            todoJumpToken,
                            itemsPerPage = todoItemsPerPage,
                            compact = qwertyHome,
                            embedded = true,
                            contentColor = homePanelContentColor,
                            mutedContentColor = homePanelMutedColor,
                            insetColor = homePanelInsetColor,
                            modifier = Modifier.weight(2f).fillMaxHeight().onGloballyPositioned { coordinates ->
                                val origin = coordinates.positionInRoot()
                                widgetCenter = origin + Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
                            },
                        )
                        ShortcutGrid(
                            store = store,
                            actions = actions,
                            openTodos = openTodos,
                            compact = qwertyHome,
                            contentColor = homePanelContentColor,
                            itemContainerColor = homePanelContentColor.copy(alpha = .09f),
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        ) { drawerOpen = true }
                    }
                }
            }
            Spacer(
                Modifier.navigationBarsPadding()
                    .height(Dimens.dp88 + softInputRowHeight),
            )
        }
        flyingTodo?.takeIf { flightActive }?.let { text ->
            val progress = flightProgress.value
            val position = Offset(
                x = magicCenter.x + (widgetCenter.x - magicCenter.x) * progress,
                y = magicCenter.y + (widgetCenter.y - magicCenter.y) * progress,
            )
            val alpha = if (progress < .72f) 1f else ((1f - progress) / .28f).coerceIn(0f, 1f)
            Surface(
                modifier = Modifier.offset {
                    IntOffset((position.x - 100).roundToInt(), (position.y - 26).roundToInt())
                }.graphicsLayer { this.alpha = alpha }
                    .zIndex(10f).shadow(Dimens.dp8, RoundedCornerShape(Dimens.dp18)),
                color = MagicTodoColor,
                shape = RoundedCornerShape(Dimens.dp18),
            ) {
                Row(Modifier.widthIn(max = Dimens.dp200).padding(horizontal = Dimens.dp14, vertical = Dimens.dp9), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Checklist, null, Modifier.size(Dimens.dp18), tint = LightInk)
                    Text(text, Modifier.padding(start = Dimens.dp7), maxLines = 1, color = LightInk, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        MagicBox(
            store = store,
            actions = actions,
            modifier = Modifier.fillMaxSize().zIndex(if (magicExpanded) 20f else 0f),
            collapsedModifier = Modifier.widthIn(max = Dimens.dp620).fillMaxWidth().navigationBarsPadding().imePadding()
                .padding(horizontal = Dimens.dp22, vertical = Dimens.dp12)
                .onGloballyPositioned { coordinates ->
                    val origin = coordinates.positionInRoot()
                    magicCenter = origin + Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
                },
            keyboardInputEnabled = keyboardInputEnabled,
            onTodoAdded = { text ->
                flyingTodo = text
                todoJumpToken++
            },
            onExpandedChange = { magicExpanded = it; onMagicExpandedChange(it) },
        )
    }

    if (showLockDisclosure) {
        LockAccessibilityDisclosureDialog(
            onContinue = {
                showLockDisclosure = false
                lockServiceLauncher.launch(actions.lockAccessibilitySettingsIntent())
            },
            onDismiss = { showLockDisclosure = false },
        )
    }

    if (drawerOpen) {
        ModalBottomSheet(onDismissRequest = { drawerOpen = false }, containerColor = MaterialTheme.colorScheme.background) {
            Text(stringResource(R.string.your_drawer), Modifier.padding(horizontal = Dimens.dp24), fontWeight = FontWeight.Black, letterSpacing = Dimens.sp1)
            if (store.drawerPackages.isEmpty()) {
                Text(stringResource(R.string.choose_drawer_apps), Modifier.padding(Dimens.dp24), color = Muted)
            } else {
                val drawerRows = ceil(store.drawerPackages.size / 2f).toInt()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().height((drawerRows * 68).dp),
                    contentPadding = PaddingValues(horizontal = Dimens.dp12, vertical = Dimens.dp8),
                ) {
                    items(store.drawerPackages, key = { it }) { packageName ->
                        ListItem(
                            headlineContent = { Text(actions.appLabel(packageName), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingContent = { AppIcon(packageName, actions, Dimens.dp36) },
                            modifier = Modifier.clip(RoundedCornerShape(Dimens.dp16))
                                .clickable { actions.launchPackage(packageName); drawerOpen = false },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                        )
                    }
                }
            }
            Spacer(Modifier.height(Dimens.dp28))
        }
    }
}

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
private fun readableContentColor(background: Color): Color {
    val luminance = .2126f * background.red + .7152f * background.green + .0722f * background.blue
    return if (luminance > .55f) ReadableDark else MinkWhite
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
    val draftOrder = remember { mutableStateListOf<Shortcut>().apply { addAll(store.shortcutOrder) } }
    val gridState = rememberLazyGridState()
    val hapticFeedback = LocalHapticFeedback.current
    val reorderableState = rememberReorderableLazyGridState(gridState) { from, to ->
        draftOrder[to.index] = draftOrder[from.index].also {
            draftOrder[from.index] = draftOrder[to.index]
        }
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun beginEditing() {
        draftOrder.clear()
        draftOrder.addAll(store.shortcutOrder)
        editing = true
    }

    fun cancelEditing() {
        draftOrder.clear()
        draftOrder.addAll(store.shortcutOrder)
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
        val visibleOrder: List<Shortcut> = if (editing) draftOrder else store.shortcutOrder
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
                                    .padding(Dimens.dp6),
                                contentAlignment = Alignment.Center,
                            ) {
                                val assignedPackage = store.shortcutPackages[shortcut]
                                if (shortcut in configurableShortcuts && assignedPackage != null) {
                                    AppIcon(
                                        packageName = assignedPackage,
                                        actions = actions,
                                        size = if (compact) Dimens.dp25 else Dimens.dp29,
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
