@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.katoaapps.openminilaunch.ui.launcher.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Home
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.conversations.ConversationNotificationStore
import com.katoaapps.openminilaunch.features.wellbeing.MinkAppAccessState
import com.katoaapps.openminilaunch.model.AppGridAssignment
import com.katoaapps.openminilaunch.model.AppGridSlot
import com.katoaapps.openminilaunch.model.CommunicationRecipient
import com.katoaapps.openminilaunch.model.ContactAppRoute
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.model.MAX_APP_FOLDER_NAME_LENGTH
import com.katoaapps.openminilaunch.model.configurableAppGridSlots
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.appgrid.AppGridSelectionDialog
import com.katoaapps.openminilaunch.ui.appgrid.UnavailableLauncherTargetDialog
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.components.UnavailableLauncherTargetIcon
import com.katoaapps.openminilaunch.ui.conversations.ConversationCenterScreen
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceHost
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpacePresentationState
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MinkFeatureColors
import com.katoaapps.openminilaunch.ui.theme.resolveContentColor
import com.katoaapps.openminilaunch.ui.widgets.WidgetPage
import com.katoaapps.openminilaunch.ui.widgets.rememberWidgetPagerDragHandler
import com.katoaapps.openminilaunch.ui.wellbeing.MinkPausedBadge
import kotlinx.coroutines.launch

/** The BlackBerry-inspired Home arrangement. All content still comes from shared launcher stores. */
@Composable
internal fun ClassicLauncherHome(
    store: LauncherStore,
    actions: DeviceActions,
    appAccessState: MinkAppAccessState,
    homeRequestToken: Int,
    todoJumpToken: Int,
    minkSpacePresentation: MinkSpacePresentationState,
    onComposeMessage: (CommunicationRecipient, ContactAppRoute) -> Unit,
    onComposeConversationMessage: (LauncherShortcutTarget) -> Unit,
    openTodos: () -> Unit,
    openVCardSettings: () -> Unit,
    openMinkSpaceSettings: () -> Unit,
    onEditQuickApps: () -> Unit,
    onPausedApp: (String) -> Unit,
    onVerticalGesturesEnabledChange: (Boolean) -> Unit,
    onTodoCenterChanged: (Offset) -> Unit,
    onMagicBoxReadinessRequested: () -> Unit,
    launcherKeyboardVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val classicSlots = store.effectiveAppGridOrder.filter(configurableAppGridSlots::contains)
    val pages = classicLauncherPages(classicSlots)
    val homePage = classicHomePageIndex(pages, store.classicHomeSlot)
    val minkSpacePage = pages.indexOf(ClassicLauncherPage.MinkSpace)
    val pagerState = rememberPagerState(
        initialPage = homePage,
        pageCount = pages::size,
    )
    val focusManager = LocalFocusManager.current
    val softwareKeyboard = LocalSoftwareKeyboardController.current
    val widgetHorizontalDragHandler = rememberWidgetPagerDragHandler(pagerState)
    var editingSlot by remember { mutableStateOf<AppGridSlot?>(null) }
    var unavailableTarget by remember { mutableStateOf<LauncherTarget?>(null) }
    var conversationKeyboardVisible by remember { mutableStateOf(false) }
    var conversationRootRequestToken by remember { mutableIntStateOf(0) }
    var previousPage by remember(pages) {
        mutableStateOf(pages.getOrNull(pagerState.currentPage))
    }

    // Pager state may be restored after switching away from Classic UI. Re-apply the persisted
    // Classic Home destination instead of treating the previously viewed page as Home.
    LaunchedEffect(homePage) {
        if (pagerState.currentPage != homePage) {
            pagerState.scrollToPage(homePage)
        }
    }
    LaunchedEffect(homeRequestToken) {
        if (homeRequestToken > 0 && pagerState.currentPage != homePage) {
            pagerState.animateScrollToPage(homePage)
        }
    }
    LaunchedEffect(todoJumpToken) {
        if (todoJumpToken > 0 && pagerState.currentPage != minkSpacePage) {
            pagerState.animateScrollToPage(minkSpacePage)
        }
    }
    LaunchedEffect(pagerState.currentPage, pages) {
        val currentPage = pages.getOrNull(pagerState.currentPage)
        val leftConversations = shouldRearmMagicBox(previousPage, currentPage)
        if (currentPage != ClassicLauncherPage.Conversations) {
            conversationKeyboardVisible = false
            softwareKeyboard?.hide()
            focusManager.clearFocus(force = true)
        }
        onVerticalGesturesEnabledChange(
            currentPage?.supportsLauncherVerticalGestures == true,
        )
        if (leftConversations) {
            conversationRootRequestToken++
            onMagicBoxReadinessRequested()
        }
        previousPage = currentPage
    }

    val contentColor = launcherBackgroundContentColor(store)
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent,
        contentColor = contentColor,
    ) {
        Column(Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                userScrollEnabled = !minkSpacePresentation.isExpanded,
                key = { pages[it].stableKey },
            ) { page ->
                when (val classicPage = pages[page]) {
                    ClassicLauncherPage.Conversations -> ConversationCenterScreen(
                        store = store,
                        actions = actions,
                        onComposeMessage = onComposeMessage,
                        onComposeConversationMessage = onComposeConversationMessage,
                        goBack = {
                            scope.launch { pagerState.animateScrollToPage(homePage) }
                        },
                        embedded = true,
                        rootRequestToken = conversationRootRequestToken,
                        launcherKeyboardVisible = launcherKeyboardVisible,
                        onKeyboardVisibilityChanged = { conversationKeyboardVisible = it },
                    )
                    ClassicLauncherPage.QuickApps -> ClassicTargetPage(
                        title = stringResource(R.string.quick_apps),
                        targetKeys = store.drawerTargets,
                        actions = actions,
                        appAccessState = appAccessState,
                        onEdit = onEditQuickApps,
                        onUnavailable = { unavailableTarget = it },
                        onPausedApp = onPausedApp,
                    )
                    ClassicLauncherPage.Widgets -> WidgetPage(
                        store = store,
                        actions = actions,
                        goHome = {
                            scope.launch { pagerState.animateScrollToPage(homePage) }
                        },
                        embedded = true,
                        horizontalDragHandler = widgetHorizontalDragHandler,
                    )
                    ClassicLauncherPage.MinkSpace -> ClassicMinkSpacePage(
                        store = store,
                        actions = actions,
                        todoJumpToken = todoJumpToken,
                        presentationState = minkSpacePresentation,
                        openTodos = openTodos,
                        openVCardSettings = openVCardSettings,
                        openMinkSpaceSettings = openMinkSpaceSettings,
                        onTodoCenterChanged = onTodoCenterChanged,
                        active = pagerState.currentPage == page,
                    )
                    is ClassicLauncherPage.Folder -> {
                        val slot = classicPage.slot
                        val assignment = store.appGridAssignment(slot)
                        val folder = (assignment as? AppGridAssignment.Folder)
                            ?.let { store.appFolder(it.folderId) }
                        ClassicTargetPage(
                            title = folder?.customName ?: stringResource(
                                R.string.classic_folder_number,
                                classicPage.position,
                            ),
                            titlePlaceholder = stringResource(
                                R.string.classic_folder_number,
                                classicPage.position,
                            ),
                            titleEditable = folder != null,
                            editableTitleValue = folder?.customName.orEmpty(),
                            targetKeys = store.appGridSelectionKeys(slot),
                            actions = actions,
                            appAccessState = appAccessState,
                            onEdit = { editingSlot = slot },
                            isHome = store.classicHomeSlot == slot,
                            onHome = {
                                store.updateClassicHomeSlot(
                                    slot.takeUnless { store.classicHomeSlot == slot },
                                )
                            },
                            onTitleCommit = { customName ->
                                folder?.let {
                                    store.updateAppFolderAppearance(
                                        folderId = it.id,
                                        customName = customName,
                                        colorMode = it.colorMode,
                                        colorArgb = it.colorArgb,
                                    )
                                }
                            },
                            onUnavailable = { unavailableTarget = it },
                            onPausedApp = onPausedApp,
                        )
                    }
                }
            }
            if (!conversationKeyboardVisible) {
                ClassicPageRail(
                    store = store,
                    currentPage = pagerState.currentPage,
                    pages = pages,
                    contentColor = contentColor,
                    onPage = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
                    onConversations = {
                        conversationRootRequestToken++
                        scope.launch {
                            pagerState.animateScrollToPage(
                                pages.indexOf(ClassicLauncherPage.Conversations),
                            )
                        }
                    },
                )
            }
        }
    }

    editingSlot?.let { slot ->
        val appsRevision by actions.launcherAppsRevision.collectAsState()
        val shortcutsRevision by actions.launcherShortcutsRevision.collectAsState()
        val apps = remember(slot, appsRevision) { actions.installedApps() }
        val shortcuts = remember(slot, shortcutsRevision) { actions.installedShortcuts() }
        AppGridSelectionDialog(
            slot = slot,
            initialSelectionKeys = store.appGridSelectionKeys(slot),
            actions = actions,
            apps = apps,
            shortcuts = shortcuts,
            onSave = { selected ->
                store.saveAppGridSelection(slot, selected)
                actions.syncPinnedLauncherShortcuts(store.pinnedLauncherSelectionKeys)
            },
            onDismiss = { editingSlot = null },
        )
    }

    unavailableTarget?.let { target ->
        UnavailableLauncherTargetDialog(
            target = target,
            actions = actions,
            preferredWebPackage = store.preferredWebPackage,
            onRemove = {
                if (store.pruneLauncherTargets { it == target.selectionKey }) {
                    actions.syncPinnedLauncherShortcuts(store.pinnedLauncherSelectionKeys)
                }
                unavailableTarget = null
            },
            onDismiss = { unavailableTarget = null },
        )
    }
}

@Composable
private fun ClassicTargetPage(
    title: String,
    titlePlaceholder: String = title,
    titleEditable: Boolean = false,
    editableTitleValue: String = title,
    targetKeys: List<String>,
    actions: DeviceActions,
    appAccessState: MinkAppAccessState,
    onEdit: () -> Unit,
    isHome: Boolean = false,
    onHome: (() -> Unit)? = null,
    onTitleCommit: ((String?) -> Unit)? = null,
    onUnavailable: (LauncherTarget) -> Unit,
    onPausedApp: (String) -> Unit,
) {
    val appsRevision by actions.launcherAppsRevision.collectAsState()
    val shortcutsRevision by actions.launcherShortcutsRevision.collectAsState()
    val targets = remember(targetKeys, appsRevision, shortcutsRevision) {
        targetKeys.map(actions::resolveLauncherSelection)
    }
    val focusManager = LocalFocusManager.current
    var draftTitle by remember(titleEditable, editableTitleValue) {
        mutableStateOf(editableTitleValue)
    }

    fun commitTitle() {
        if (!titleEditable) return
        onTitleCommit?.invoke(draftTitle.trim().ifEmpty { null })
    }
    Column(Modifier.fillMaxSize().padding(horizontal = Dimens.dp12)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = Dimens.dp6),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (titleEditable) {
                BasicTextField(
                    value = draftTitle,
                    onValueChange = { draftTitle = it.take(MAX_APP_FOLDER_NAME_LENGTH) },
                    modifier = Modifier.weight(1f).onFocusChanged { state ->
                        if (!state.isFocused) commitTitle()
                    },
                    singleLine = true,
                    textStyle = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                        color = LocalContentColor.current,
                        fontWeight = FontWeight.Black,
                    ),
                    cursorBrush = SolidColor(LocalContentColor.current),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        commitTitle()
                        focusManager.clearFocus()
                    }),
                    decorationBox = { field ->
                        Box {
                            if (draftTitle.isBlank()) {
                                Text(
                                    titlePlaceholder,
                                    color = LocalContentColor.current.copy(alpha = .68f),
                                    fontWeight = FontWeight.Black,
                                    fontSize = Dimens.sp20,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            field()
                        }
                    },
                )
            } else {
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Black,
                    fontSize = Dimens.sp20,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            onHome?.let { selectHome ->
                IconButton(
                    onClick = selectHome,
                    modifier = Modifier.semantics { selected = isHome },
                ) {
                    Icon(
                        imageVector = if (isHome) Icons.Filled.Home else Icons.Outlined.Home,
                        contentDescription = stringResource(R.string.settings_section_home),
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Add, stringResource(R.string.add))
            }
        }
        if (targets.isEmpty()) {
            Box(
                Modifier.fillMaxSize().clickable(onClick = onEdit),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.classic_empty_folder),
                    color = LocalContentColor.current.copy(alpha = .68f),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
                verticalArrangement = Arrangement.spacedBy(Dimens.dp10),
            ) {
                items(targets, key = LauncherTarget::selectionKey) { target ->
                    Column(
                        Modifier.fillMaxWidth().clickable {
                            when {
                                !target.isAvailable -> onUnavailable(target)
                                appAccessState.isPaused(target) -> onPausedApp(target.packageName)
                                else -> actions.launchLauncherTarget(target)
                            }
                        }.padding(vertical = Dimens.dp6),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.size(Dimens.dp48), contentAlignment = Alignment.Center) {
                            if (target.isAvailable) {
                                LauncherTargetIcon(target, actions, Dimens.dp48)
                            } else {
                                UnavailableLauncherTargetIcon(
                                    size = Dimens.dp48,
                                    contentDescription = stringResource(R.string.app_unavailable),
                                )
                            }
                            if (target.isAvailable && appAccessState.isPaused(target)) {
                                MinkPausedBadge(Modifier.align(Alignment.Center))
                            }
                        }
                        Text(
                            if (target.isAvailable) target.label else stringResource(R.string.app_unavailable),
                            modifier = Modifier.padding(top = Dimens.dp5),
                            fontSize = Dimens.sp11,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassicMinkSpacePage(
    store: LauncherStore,
    actions: DeviceActions,
    todoJumpToken: Int,
    presentationState: MinkSpacePresentationState,
    openTodos: () -> Unit,
    openVCardSettings: () -> Unit,
    openMinkSpaceSettings: () -> Unit,
    onTodoCenterChanged: (Offset) -> Unit,
    active: Boolean,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val contentColor = launcherBackgroundContentColor(store)
    var center by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(active, center) {
        onTodoCenterChanged(if (active) center else Offset.Zero)
    }
    BoxWithConstraints(
        Modifier.fillMaxSize().padding(horizontal = Dimens.dp12).onGloballyPositioned { coordinates ->
            val origin = coordinates.positionInRoot()
            center = origin + Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
        },
    ) {
        val itemsPerPage = visibleTodoItemsForHeight(maxHeight.value)
        val metrics = homeTodoTextMetrics(
            largeDisplay = configuration.smallestScreenWidthDp >= 600,
            compact = maxHeight <= maxWidth * 1.55f,
            panelHeightDp = maxHeight.value,
            itemsPerPage = itemsPerPage,
            fontScale = density.fontScale,
        )
        MinkSpaceHost(
            store = store,
            actions = actions,
            openTodos = openTodos,
            todoJumpToken = todoJumpToken,
            todoItemsPerPage = itemsPerPage,
            todoTextMetrics = metrics,
            compact = false,
            contentColor = contentColor,
            mutedContentColor = contentColor.copy(alpha = .68f),
            insetColor = contentColor.copy(alpha = .12f),
            openMediaLab = actions::openMediaLab,
            openVCardSettings = openVCardSettings,
            openMinkSpaceSettings = openMinkSpaceSettings,
            presentationState = presentationState,
            expandedPresentation = true,
            modifier = Modifier.fillMaxSize().padding(Dimens.dp12),
        )
    }
}

@Composable
private fun ClassicPageRail(
    store: LauncherStore,
    currentPage: Int,
    pages: List<ClassicLauncherPage>,
    contentColor: Color,
    onPage: (Int) -> Unit,
    onConversations: () -> Unit,
) {
    val conversations = ConversationNotificationStore.currentConversations(
        useDemoData = store.demoModeEnabled,
        demoScenario = store.demoScenario,
        demoConversationState = store.demoConversationState,
        demoSessionStartedAt = store.demoSessionStartedAt,
    )
    val attention = rememberPendingFavoriteAttention(
        store = store,
        conversations = conversations,
        defaultColorArgb = MinkFeatureColors.ConversationAttention.toArgb(),
    )
    Box(
        Modifier.fillMaxWidth().height(48.dp).padding(horizontal = Dimens.dp8),
    ) {
        ConversationAttentionButton(
            conversationCount = conversations.size,
            attentionColors = attention.map { Color(it.colorArgb) },
            actionSize = 40.dp,
            iconSize = 21.dp,
            restingColor = contentColor,
            onClick = onConversations,
            modifier = Modifier.classicRailSelection(
                currentPage == pages.indexOf(ClassicLauncherPage.Conversations),
                contentColor,
            ).align(Alignment.CenterStart),
        )
        Row(
            Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ClassicRailIcon(
                selected = currentPage == pages.indexOf(ClassicLauncherPage.MinkSpace),
                selectionColor = contentColor,
                size = 30.dp,
                onClick = { onPage(pages.indexOf(ClassicLauncherPage.MinkSpace)) },
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    stringResource(R.string.settings_tab_mini_apps),
                    Modifier.size(18.dp),
                )
            }
            pages.filterIsInstance<ClassicLauncherPage.Folder>().forEach { folderPage ->
                val page = pages.indexOf(folderPage)
                val folderName = (store.appGridAssignment(folderPage.slot) as? AppGridAssignment.Folder)
                    ?.let { store.appFolder(it.folderId) }
                    ?.customName
                val label = folderName
                    ?: stringResource(R.string.classic_folder_number, folderPage.position)
                ClassicFolderPageIndicator(
                    pageNumber = folderPage.position,
                    label = label,
                    color = contentColor,
                    selected = currentPage == page,
                    onClick = { onPage(page) },
                )
            }
            ClassicRailIcon(
                selected = currentPage == pages.indexOf(ClassicLauncherPage.QuickApps),
                selectionColor = contentColor,
                size = 30.dp,
                onClick = { onPage(pages.indexOf(ClassicLauncherPage.QuickApps)) },
            ) {
                Icon(
                    Icons.Default.GridView,
                    stringResource(R.string.quick_apps),
                    Modifier.size(18.dp),
                )
            }
        }
        ClassicRailIcon(
            selected = currentPage == pages.indexOf(ClassicLauncherPage.Widgets),
            selectionColor = contentColor,
            onClick = { onPage(pages.indexOf(ClassicLauncherPage.Widgets)) },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Icon(
                Icons.Default.Widgets,
                stringResource(R.string.widgets),
                Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ClassicFolderPageIndicator(
    pageNumber: Int,
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val indicatorSize by animateDpAsState(
        targetValue = if (selected) 22.dp else 7.dp,
        label = "classicFolderPageIndicator",
    )
    Box(
        Modifier.size(24.dp)
            .semantics {
                contentDescription = label
                this.selected = selected
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(indicatorSize).clip(CircleShape).background(
                if (selected) color else color.copy(alpha = .68f),
            ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Text(
                    text = pageNumber.toString(),
                    color = resolveContentColor(background = color, preferred = null),
                    fontSize = Dimens.sp10,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ClassicRailIcon(
    selected: Boolean,
    selectionColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier.size(size).classicRailSelection(selected, selectionColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

private fun Modifier.classicRailSelection(selected: Boolean, color: Color): Modifier =
    clip(CircleShape).then(
        if (selected) background(color.copy(alpha = .16f)) else Modifier,
    )
