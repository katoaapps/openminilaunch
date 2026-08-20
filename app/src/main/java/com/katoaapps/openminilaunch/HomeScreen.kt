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

private const val TODO_ITEMS_PER_PAGE = 3

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
    val homePanelInsetColor = if (homePanelContentColor == Color.White) {
        Color.Black.copy(alpha = .16f)
    } else {
        Color.White.copy(alpha = .24f)
    }
    val lockServiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (actions.isLockServiceEnabled()) actions.lockDevice()
    }

    fun lockFromHome() {
        if (!actions.supportsLockScreenAction()) {
            Toast.makeText(context, "Double-tap lock requires Android 9 or newer", Toast.LENGTH_SHORT).show()
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
        val homeHorizontalPadding = if (qwertyHome) 14.dp else 22.dp
        val headerActionSize = if (qwertyHome) 40.dp else 48.dp
        val headerIconSize = if (qwertyHome) 21.dp else 24.dp
        val focusPanelHeight = (maxWidth * .78f).coerceIn(310.dp, 350.dp)
        Column(
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .blur(if (magicExpanded) 10.dp else 0.dp),
        ) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides headerActionSize) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding()
                        .padding(horizontal = homeHorizontalPadding)
                        .padding(vertical = if (qwertyHome) 0.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MinkHomeIcon(
                        store = store,
                        isActive = minkStatusActive,
                        onClick = openMinkDay,
                        modifier = Modifier.size(headerActionSize),
                    )
                    Text(
                        LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d")).uppercase(),
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.5.sp,
                        fontSize = 13.sp,
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
                                if (count > 0) Badge { Text(if (count > 99) "99+" else count.toString()) }
                            },
                        ) { Icon(Icons.Default.Forum, "Conversations", Modifier.size(headerIconSize)) }
                    }
                    IconButton(onClick = openSettings, modifier = Modifier.size(headerActionSize)) {
                        Icon(Icons.Default.Settings, "Settings", Modifier.size(headerIconSize))
                    }
                }
            }
            Box(
                Modifier.fillMaxWidth().weight(1f)
                    .padding(horizontal = homeHorizontalPadding, vertical = if (qwertyHome) 2.dp else 10.dp),
            ) {
                val focusModifier = if (qwertyHome) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.fillMaxWidth().height(focusPanelHeight).align(Alignment.TopCenter)
                }
                Surface(
                    modifier = focusModifier.widthIn(max = 620.dp).align(if (qwertyHome) Alignment.Center else Alignment.TopCenter),
                    shape = RoundedCornerShape(if (qwertyHome) 26.dp else 34.dp),
                    color = homePanelColor,
                    contentColor = homePanelContentColor,
                    shadowElevation = if (isSystemInDarkTheme()) 2.dp else 8.dp,
                    tonalElevation = 1.dp,
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(if (qwertyHome) 10.dp else 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (qwertyHome) 8.dp else 12.dp),
                    ) {
                        TodoPager(
                            store,
                            openTodos,
                            todoJumpToken,
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
                    .height(if (qwertyHome) 64.dp else 52.dp),
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
                    .zIndex(10f).shadow(8.dp, RoundedCornerShape(18.dp)),
                color = Color(0xFFD6A300),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(Modifier.widthIn(max = 200.dp).padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Checklist, null, Modifier.size(18.dp), tint = LightInk)
                    Text(text, Modifier.padding(start = 7.dp), maxLines = 1, color = LightInk, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        MagicBox(
            store = store,
            actions = actions,
            modifier = Modifier.fillMaxSize().zIndex(if (magicExpanded) 20f else 0f),
            collapsedModifier = Modifier.widthIn(max = 620.dp).fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 12.dp)
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
            Text(stringResource(R.string.your_drawer), Modifier.padding(horizontal = 24.dp), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            if (store.drawerPackages.isEmpty()) {
                Text(stringResource(R.string.choose_drawer_apps), Modifier.padding(24.dp), color = Muted)
            } else {
                val drawerRows = ceil(store.drawerPackages.size / 2f).toInt()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().height((drawerRows * 68).dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    items(store.drawerPackages, key = { it }) { packageName ->
                        ListItem(
                            headlineContent = { Text(actions.appLabel(packageName), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingContent = { AppIcon(packageName, actions, 36.dp) },
                            modifier = Modifier.clip(RoundedCornerShape(16.dp))
                                .clickable { actions.launchPackage(packageName); drawerOpen = false },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
internal fun TodoPager(
    store: LauncherStore,
    openTodos: () -> Unit,
    jumpToken: Int,
    compact: Boolean = false,
    embedded: Boolean = false,
    contentColor: Color = LightPaper,
    mutedContentColor: Color = Sage,
    insetColor: Color = MinkForestPanel,
    modifier: Modifier = Modifier,
) {
    val pages = maxOf(1, ceil(store.todos.size / TODO_ITEMS_PER_PAGE.toFloat()).toInt())
    val pagerState = rememberPagerState(pageCount = { pages })
    LaunchedEffect(jumpToken, pages) {
        if (jumpToken > 0) {
            val newestUnfinishedPage = store.todos.indexOfLast { !it.completed }
                .coerceAtLeast(0) / TODO_ITEMS_PER_PAGE
            pagerState.animateScrollToPage(newestUnfinishedPage.coerceAtMost(pages - 1))
        }
    }
    val shape = RoundedCornerShape(if (compact) 18.dp else 24.dp)
    Column(
        modifier.fillMaxWidth().clip(shape)
            .background(if (embedded) insetColor else MinkForestPanel)
            .then(if (embedded) Modifier else Modifier.border(1.dp, mutedContentColor.copy(alpha = .42f), shape))
            .clickable(onClick = openTodos)
            .padding(if (compact) 8.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.todo_heading), color = contentColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.page_of_pages, pagerState.currentPage + 1, pages), color = mutedContentColor, fontSize = 12.sp)
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().weight(1f)) { page ->
            val pageItems = store.todos
                .drop(page * TODO_ITEMS_PER_PAGE)
                .take(TODO_ITEMS_PER_PAGE)
            if (pageItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    Text(stringResource(R.string.tap_to_add_first_todo), color = mutedContentColor)
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp)) {
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
                                    checkmarkColor = if (contentColor == Color.White) Color.Black else Color.White,
                                ),
                                modifier = Modifier.size(if (compact) 24.dp else 26.dp),
                            )
                            Text(
                                item.text,
                                color = if (item.completed) mutedContentColor else contentColor,
                                fontSize = if (compact) 13.sp else 15.sp,
                                lineHeight = if (compact) 17.sp else 20.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textDecoration = if (item.completed) TextDecoration.LineThrough else null,
                                modifier = Modifier.padding(start = 7.dp, top = 2.dp).weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun readableContentColor(background: Color): Color {
    val luminance = .2126f * background.red + .7152f * background.green + .0722f * background.blue
    return if (luminance > .55f) Color(0xFF172019) else Color.White
}

@Composable
internal fun ShortcutGrid(
    store: LauncherStore,
    actions: DeviceActions,
    openTodos: () -> Unit,
    compact: Boolean = false,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    itemContainerColor: Color = Color.Transparent,
    modifier: Modifier = Modifier,
    openDrawer: () -> Unit,
) {
    val icons = mapOf(
        Shortcut.NOTE to Icons.Default.EditNote, Shortcut.EVENT to Icons.Default.Event,
        Shortcut.WEATHER to Icons.Default.Cloud, Shortcut.TODO to Icons.Default.CheckCircle,
        Shortcut.CALL to Icons.Default.Call, Shortcut.MESSAGE to Icons.AutoMirrored.Filled.Message,
        Shortcut.FILES to Icons.Default.FolderOpen, Shortcut.DRAWER to Icons.Default.GridView,
    )
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

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (editing) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = ::cancelEditing) { Icon(Icons.Default.Close, "Cancel shortcut reorder", tint = contentColor) }
                IconButton(onClick = ::finishEditing) { Icon(Icons.Default.Check, "Save shortcut order", tint = contentColor) }
            }
        }
        val visibleOrder: List<Shortcut> = if (editing) draftOrder else store.shortcutOrder
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
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
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(if (compact) 42.dp else 50.dp)
                            .animateItem()
                            .zIndex(if (isDragging) 4f else 0f)
                            .graphicsLayer {
                                rotationZ = if (isDragging) 0f else jiggleAngle
                                if (isDragging) {
                                    scaleX = 1.06f
                                    scaleY = 1.06f
                                    shadowElevation = 14.dp.toPx()
                                }
                            }
                            .clip(RoundedCornerShape(if (compact) 14.dp else 18.dp))
                            .background(
                                if (editing) contentColor.copy(alpha = .17f)
                                else itemContainerColor
                            )
                            .then(interactionModifier)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            icons.getValue(shortcut),
                            shortcut.label,
                            Modifier.size(if (compact) 24.dp else 28.dp),
                            tint = contentColor,
                        )
                    }
                }
            }
        }
    }
}
