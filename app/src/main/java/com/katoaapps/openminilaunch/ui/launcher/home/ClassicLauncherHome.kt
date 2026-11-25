@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.katoaapps.openminilaunch.ui.launcher.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ViewCarousel
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
import androidx.compose.runtime.key
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.katoaapps.openminilaunch.model.MAX_APP_FOLDER_TARGETS
import com.katoaapps.openminilaunch.model.configurableAppGridSlots
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.appgrid.AppGridSelectionDialog
import com.katoaapps.openminilaunch.ui.appgrid.UnavailableLauncherTargetDialog
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.components.UnavailableLauncherTargetIcon
import com.katoaapps.openminilaunch.ui.conversations.ConversationCenterScreen
import com.katoaapps.openminilaunch.ui.conversations.ConversationCenterUiState
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceHost
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpacePresentationState
import com.katoaapps.openminilaunch.ui.onboarding.HomeTourTarget
import com.katoaapps.openminilaunch.ui.onboarding.homeTourTarget
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
    onOpenWhatsNew: () -> Unit,
    onPausedApp: (String) -> Unit,
    onUiStateChange: (ClassicLauncherUiState) -> Unit,
    onTodoCenterChanged: (Offset) -> Unit,
    onMagicBoxReadinessRequested: () -> Unit,
    onQuickDrawerEntered: () -> Unit,
    launcherKeyboardVisible: Boolean,
    quickDrawerTourRequestToken: Long = 0L,
    onQuickDrawerTourComplete: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val classicSlots = store.effectiveAppGridOrder.filter(configurableAppGridSlots::contains)
    val pages = classicLauncherPages(classicSlots)
    val homePage = classicHomePageIndex(pages, store.classicHomeSlot)
    val minkSpacePage = pages.indexOf(ClassicLauncherPage.MinkSpace)
    val widgetsPage = pages.indexOf(ClassicLauncherPage.Widgets)
    val quickAppsPage = pages.indexOf(ClassicLauncherPage.QuickApps)
    val pagerState = rememberPagerState(
        initialPage = homePage,
        pageCount = pages::size,
    )
    val focusManager = LocalFocusManager.current
    val softwareKeyboard = LocalSoftwareKeyboardController.current
    val widgetHorizontalDragHandler = rememberWidgetPagerDragHandler(pagerState)
    var editingSlot by remember { mutableStateOf<AppGridSlot?>(null) }
    var unavailableTarget by remember { mutableStateOf<LauncherTarget?>(null) }
    var conversationUiState by remember { mutableStateOf(ConversationCenterUiState()) }
    var conversationRootRequestToken by remember { mutableIntStateOf(0) }
    var conversationRevealToken by remember { mutableIntStateOf(0) }
    var widgetsRevealToken by remember { mutableIntStateOf(0) }
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
    LaunchedEffect(quickDrawerTourRequestToken) {
        if (quickDrawerTourRequestToken > 0L && pagerState.currentPage != quickAppsPage) {
            pagerState.scrollToPage(quickAppsPage)
        }
    }
    val currentPage = pages.getOrNull(pagerState.currentPage)
    val settledPage = pages.getOrNull(pagerState.settledPage)
    LaunchedEffect(settledPage) {
        if (settledPage == ClassicLauncherPage.QuickApps) {
            onQuickDrawerEntered()
        }
    }
    val uiState = ClassicLauncherUiState(
        page = currentPage,
        conversationReplyActive = conversationUiState.replyEditorActive,
    )
    LaunchedEffect(uiState) {
        onUiStateChange(uiState)
    }
    LaunchedEffect(currentPage, pages) {
        val leftConversations = previousPage == ClassicLauncherPage.Conversations &&
            currentPage != null &&
            currentPage != ClassicLauncherPage.Conversations
        if (leftConversations) {
            conversationUiState = ConversationCenterUiState()
            softwareKeyboard?.hide()
            focusManager.clearFocus(force = true)
            conversationRootRequestToken++
        }
        if (shouldRearmMagicBox(previousPage, currentPage)) {
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
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .homeTourTarget(HomeTourTarget.CLASSIC_PAGES),
                userScrollEnabled = !minkSpacePresentation.isExpanded,
                key = { pages[it].stableKey },
            ) { page ->
                when (val classicPage = pages[page]) {
                    ClassicLauncherPage.Conversations -> ClassicDestinationReveal(
                        triggerToken = conversationRevealToken,
                    ) {
                        ConversationCenterScreen(
                            store = store,
                            actions = actions,
                            onComposeMessage = onComposeMessage,
                            onComposeConversationMessage = onComposeConversationMessage,
                            goBack = {
                                scope.launch { pagerState.animateScrollToPage(homePage) }
                            },
                            embedded = true,
                            hostActive = pagerState.settledPage == page,
                            rootRequestToken = conversationRootRequestToken,
                            launcherKeyboardVisible = launcherKeyboardVisible,
                            onUiStateChanged = { conversationUiState = it },
                        )
                    }
                    ClassicLauncherPage.QuickApps -> QuickDrawerSurface(
                        store = store,
                        actions = actions,
                        appAccessState = appAccessState,
                        contentColor = contentColor,
                        onHostDismiss = {},
                        onManageQuickApps = onEditQuickApps,
                        onOpenWhatsNew = onOpenWhatsNew,
                        onPausedApp = onPausedApp,
                        modifier = Modifier.fillMaxSize(),
                        tourRequestToken = quickDrawerTourRequestToken,
                        onTourComplete = onQuickDrawerTourComplete,
                    )
                    ClassicLauncherPage.Widgets -> ClassicDestinationReveal(
                        triggerToken = widgetsRevealToken,
                    ) {
                        WidgetPage(
                            store = store,
                            actions = actions,
                            goHome = {
                                scope.launch { pagerState.animateScrollToPage(homePage) }
                            },
                            embedded = true,
                            horizontalDragHandler = widgetHorizontalDragHandler,
                        )
                    }
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
                            targetLimit = MAX_APP_FOLDER_TARGETS,
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
                            publishTourTargets = pagerState.currentPage == page,
                            onUnavailable = { unavailableTarget = it },
                            onPausedApp = onPausedApp,
                        )
                    }
                }
            }
            if (!conversationUiState.keyboardVisible) {
                ClassicPageRail(
                    store = store,
                    currentPage = pagerState.currentPage,
                    pages = pages,
                    contentColor = contentColor,
                    // Rail icons are direct destinations, not a request to visibly travel through
                    // every page between the current one and the selected one.
                    onPage = { page -> scope.launch { pagerState.scrollToPage(page) } },
                    onConversations = {
                        conversationRootRequestToken++
                        conversationRevealToken++
                        scope.launch {
                            pagerState.scrollToPage(
                                pages.indexOf(ClassicLauncherPage.Conversations),
                            )
                        }
                    },
                    // Widgets can take a beat to compose. Request the destination during the next
                    // pager remeasure so the adjacent Quick Apps page is not left on screen first.
                    onWidgets = {
                        widgetsRevealToken++
                        pagerState.requestScrollToPage(widgetsPage)
                    },
                    modifier = Modifier.homeTourTarget(HomeTourTarget.CLASSIC_RAIL),
                )
            }
        }
    }

    editingSlot?.let { slot ->
        val appsRevision by actions.launcherAppsRevision.collectAsState()
        val shortcutsRevision by actions.launcherShortcutsRevision.collectAsState()
        val apps = remember(slot, appsRevision) { actions.selectableApps() }
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
    targetLimit: Int? = null,
    targetKeys: List<String>,
    actions: DeviceActions,
    appAccessState: MinkAppAccessState,
    onEdit: () -> Unit,
    isHome: Boolean = false,
    onHome: (() -> Unit)? = null,
    onTitleCommit: ((String?) -> Unit)? = null,
    publishTourTargets: Boolean = false,
    onUnavailable: (LauncherTarget) -> Unit,
    onPausedApp: (String) -> Unit,
) {
    val appsRevision by actions.launcherAppsRevision.collectAsState()
    val shortcutsRevision by actions.launcherShortcutsRevision.collectAsState()
    val targets = remember(targetKeys, appsRevision, shortcutsRevision) {
        targetKeys.map(actions::resolveLauncherSelection)
    }
    val focusManager = LocalFocusManager.current
    val gridState = rememberLazyGridState()
    var draftTitle by remember(titleEditable, editableTitleValue) {
        mutableStateOf(editableTitleValue)
    }

    fun commitTitle() {
        if (!titleEditable) return
        onTitleCommit?.invoke(draftTitle.trim().ifEmpty { null })
    }
    Column(
        Modifier.fillMaxSize()
            .classicFolderEdgeGestures(
                gridState = gridState,
                itemCount = targets.size,
                onSwipeUp = actions::openAllApps,
                onSwipeDown = actions::expandNotificationShade,
            )
            .padding(horizontal = Dimens.dp12),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = Dimens.dp6),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (titleEditable) {
                Row(
                    Modifier.weight(1f).then(
                        if (publishTourTargets) {
                            Modifier.homeTourTarget(HomeTourTarget.CLASSIC_FOLDER_TITLE)
                        } else {
                            Modifier
                        },
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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
                    targetLimit?.let { limit ->
                        Text(
                            "(${targetKeys.size} / $limit)",
                            modifier = Modifier.padding(start = Dimens.dp8),
                            color = LocalContentColor.current.copy(alpha = .68f),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            } else {
                Text(
                    targetLimit?.let { "$title (${targetKeys.size} / $it)" } ?: title,
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
                    modifier = Modifier
                        .then(
                            if (publishTourTargets) {
                                Modifier.homeTourTarget(HomeTourTarget.CLASSIC_FOLDER_HOME)
                            } else {
                                Modifier
                            },
                        )
                        .semantics { selected = isHome },
                ) {
                    Icon(
                        imageVector = if (isHome) Icons.Filled.Home else Icons.Outlined.Home,
                        contentDescription = stringResource(R.string.settings_section_home),
                    )
                }
            }
            IconButton(
                onClick = onEdit,
                modifier = Modifier.then(
                    if (publishTourTargets) {
                        Modifier.homeTourTarget(HomeTourTarget.CLASSIC_FOLDER_EDIT)
                    } else {
                        Modifier
                    },
                ),
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.add))
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            if (targets.isEmpty()) {
                Column(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.fillMaxWidth().clickable(onClick = onEdit)
                            .padding(vertical = Dimens.dp48),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.classic_empty_folder),
                            color = LocalContentColor.current.copy(alpha = .68f),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.fillMaxWidth().weight(1f))
                }
            } else {
                val rowCount = (targets.size + CLASSIC_TARGET_COLUMNS - 1) /
                    CLASSIC_TARGET_COLUMNS
                val desiredGridHeight = Dimens.dp96 * rowCount +
                    Dimens.dp10 * (rowCount - 1).coerceAtLeast(0)
                val gridHeight = minOf(desiredGridHeight, maxHeight)
                Column(Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(CLASSIC_TARGET_COLUMNS),
                        state = gridState,
                        modifier = Modifier.fillMaxWidth().height(gridHeight),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
                        verticalArrangement = Arrangement.spacedBy(Dimens.dp10),
                    ) {
                        items(targets, key = LauncherTarget::selectionKey) { target ->
                            ClassicTargetCell(
                                target = target,
                                actions = actions,
                                appAccessState = appAccessState,
                                onUnavailable = onUnavailable,
                                onPausedApp = onPausedApp,
                            )
                        }
                    }
                    // When every row fits, the rest of the page remains a reliable gesture zone.
                    Spacer(Modifier.fillMaxWidth().weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ClassicTargetCell(
    target: LauncherTarget,
    actions: DeviceActions,
    appAccessState: MinkAppAccessState,
    onUnavailable: (LauncherTarget) -> Unit,
    onPausedApp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().height(Dimens.dp96).clickable {
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
                LauncherTargetIcon(
                    target = target,
                    actions = actions,
                    size = Dimens.dp48,
                    normalizeTransparentBounds = true,
                )
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

private const val CLASSIC_TARGET_COLUMNS = 4

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
            active = active,
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
    onWidgets: () -> Unit,
    modifier: Modifier = Modifier,
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
        modifier.fillMaxWidth().height(48.dp).padding(horizontal = Dimens.dp8),
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
                    Icons.Default.ViewCarousel,
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
            onClick = onWidgets,
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

/** A lightweight destination reveal that does not animate or recreate the pager itself. */
@Composable
private fun ClassicDestinationReveal(
    triggerToken: Int,
    content: @Composable () -> Unit,
) {
    if (triggerToken <= 0) {
        Box(Modifier.fillMaxSize()) { content() }
        return
    }

    val rippleDistancePx = with(LocalDensity.current) { 7.dp.toPx() }
    key(triggerToken) {
        var revealed by remember { mutableStateOf(false) }
        val progress by animateFloatAsState(
            targetValue = if (revealed) 1f else 0f,
            animationSpec = tween(
                durationMillis = 170,
                easing = FastOutSlowInEasing,
            ),
            label = "classic destination reveal",
        )
        LaunchedEffect(Unit) { revealed = true }

        Box(
            Modifier.fillMaxSize().graphicsLayer {
                alpha = progress
                scaleX = .96f + (.04f * progress)
                scaleY = scaleX
                translationY = rippleDistancePx * (1f - progress)
            },
        ) {
            content()
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
