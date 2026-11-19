package com.katoaapps.minkboard.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.ZoomOutMap
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.katoaapps.minkboard.model.BoardDraft
import com.katoaapps.minkboard.core.BoardTemplateId
import com.katoaapps.minkboard.core.MinkBoardCapability
import com.katoaapps.minkboard.core.MinkBoardHostAccess
import com.katoaapps.minkboard.core.MinkBoardTemplates
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.BoardSummary
import com.katoaapps.minkboard.model.BoardUpdate
import com.katoaapps.minkboard.model.CardDraft
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.CardPlacement
import com.katoaapps.minkboard.model.CardUpdate
import com.katoaapps.minkboard.model.FocusSession
import com.katoaapps.minkboard.model.FocusSessionState
import com.katoaapps.minkboard.model.FocusTimerId
import com.katoaapps.minkboard.model.StatusDraft
import com.katoaapps.minkboard.model.StatusDeletionPolicy
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.minkboard.model.StatusUpdate
import com.katoaapps.minkboard.model.SubtaskId
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

data class MinkBoardActions(
    val selectBoard: (BoardId) -> Unit,
    /** Returns the new opaque ID so a bounded host can persist its managed editable board. */
    val createBoard: (BoardDraft, (BoardId) -> Unit) -> Unit,
    val createBoardFromTemplate: (BoardDraft, BoardTemplateId, (BoardId) -> Unit) -> Unit,
    val renameBoard: (BoardId, String, () -> Unit) -> Unit,
    val updateBoard: (BoardId, BoardUpdate, () -> Unit) -> Unit,
    val reorderBoards: (List<BoardId>) -> Unit,
    val deleteBoard: (BoardId, () -> Unit) -> Unit,
    val deleteBoards: (Set<BoardId>, () -> Unit) -> Unit,
    val addStatus: (BoardId, StatusDraft, () -> Unit) -> Unit,
    val updateStatus: (StatusId, StatusUpdate, () -> Unit) -> Unit,
    val reorderStatuses: (BoardId, List<StatusId>) -> Unit,
    val deleteStatus: (StatusId, StatusDeletionPolicy, () -> Unit) -> Unit,
    val addCard: (StatusId, CardDraft, (CardPlacement) -> Unit) -> Unit,
    val cloneCard: (CardId, String, (CardPlacement) -> Unit) -> Unit,
    val updateCardAndMove: (CardId, CardUpdate, StatusId, (CardPlacement?) -> Unit) -> Unit,
    val moveCard: (CardId, StatusId, Int) -> Unit,
    val moveCardAndReport: (CardId, StatusId, Int, (CardPlacement) -> Unit) -> Unit,
    val deleteCard: (CardId, () -> Unit) -> Unit,
    val addSubtask: (CardId, String, () -> Unit) -> Unit,
    val updateSubtask: (SubtaskId, String, () -> Unit) -> Unit,
    val setSubtaskCompleted: (SubtaskId, Boolean) -> Unit,
    val deleteSubtask: (SubtaskId) -> Unit,
    val openCard: (BoardId, CardId) -> Unit,
    val closeCard: () -> Unit,
    val unlockBoard: (BoardId) -> Unit,
    val startFocus: (BoardId, CardId, Long, Boolean) -> Unit,
    val stopFocus: (FocusTimerId) -> Unit,
    val clearActionError: () -> Unit,
)

enum class MinkBoardFatalErrorDisplay {
    Decryption,
    Storage,
}
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MinkBoardApp(
    boards: List<BoardSummary>,
    selectedBoard: BoardSnapshot?,
    hostAccess: MinkBoardHostAccess,
    openCardId: CardId?,
    openStatusId: StatusId?,
    isBoardAccessible: (BoardId) -> Boolean,
    activeFocusSession: FocusSession?,
    canUseBoardLock: Boolean,
    isLoading: Boolean,
    fatalError: MinkBoardFatalErrorDisplay?,
    actionErrorMessage: String?,
    editorState: MinkBoardEditorState,
    onEditorStateChange: (MinkBoardEditorState) -> Unit,
    actions: MinkBoardActions,
    onCapabilityUnavailable: (MinkBoardCapability) -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (isLoading && fatalError == null) {
        BoardLoadingScreen(modifier)
        return
    }

    val editableBoards = remember(boards, hostAccess) {
        hostAccess.editableBoards(boards)
    }
    val visibleSelectedBoard = selectedBoard?.takeIf { selected ->
        editableBoards.any { it.board.id == selected.board.id }
    }
    val effectiveSelectedBoardId = visibleSelectedBoard?.board?.id
    val isSelectedBoardAccessible = effectiveSelectedBoardId?.let(isBoardAccessible) ?: true
    val editableBoardIdValues = editableBoards
        .map { it.board.id.value }
        .sorted()
    var boardMenuOpen by rememberSaveable { mutableStateOf(false) }
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    var showBoardMap by rememberSaveable { mutableStateOf(false) }
    var showAddMenu by rememberSaveable { mutableStateOf(false) }
    var showCardStatusPicker by rememberSaveable { mutableStateOf(false) }
    var showManageBoards by rememberSaveable { mutableStateOf(false) }
    var pendingCardReveal by remember(effectiveSelectedBoardId?.value) {
        mutableStateOf<CardPlacement?>(null)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val boardNavigationScope = rememberCoroutineScope()
    val statusListState = rememberLazyListState()
    val boardMapListState = rememberLazyListState()
    val requestNewBoard = {
        if (hostAccess.canCreateBoard(boards)) {
            onEditorStateChange(
                MinkBoardEditorState.NewBoard(
                    BoardEditorDraft(
                        templateIdValue = MinkBoardTemplates.Simple.id.value,
                    ),
                ),
            )
        } else {
            onCapabilityUnavailable(MinkBoardCapability.CreateAdditionalBoard)
        }
    }
    val requestEditBoard: (BoardSummary) -> Unit = { boardSummary ->
        if (isBoardAccessible(boardSummary.board.id)) {
            onEditorStateChange(
                MinkBoardEditorState.EditBoard(
                    boardIdValue = boardSummary.board.id.value,
                    draft = BoardEditorDraft(
                        name = boardSummary.board.name,
                        isLocked = boardSummary.board.isLocked,
                    ),
                ),
            )
        } else {
            actions.selectBoard(boardSummary.board.id)
        }
    }
    val boardMenuScrollState = rememberScrollState()

    var previousSelectedBoardId by remember { mutableStateOf(effectiveSelectedBoardId?.value) }
    LaunchedEffect(effectiveSelectedBoardId?.value, editableBoardIdValues) {
        // Treat the first hydrated board as loading, not user navigation, or a host-restored
        // editor would be discarded.
        val selectedBoardChanged = previousSelectedBoardId != null &&
            previousSelectedBoardId != effectiveSelectedBoardId?.value
        if (selectedBoardChanged) {
            showCalendar = false
            if (editorState.isSelectedBoardEditor()) {
                onEditorStateChange(MinkBoardEditorState.None)
            }
        }
        previousSelectedBoardId = effectiveSelectedBoardId?.value
    }

    val editorTargetIsValid = editorState.isValidFor(
        boards = editableBoards,
        selectedBoard = visibleSelectedBoard,
        canCreateBoard = hostAccess.canCreateBoard(boards),
    )
    LaunchedEffect(isLoading, editorState.targetKey(), editorTargetIsValid) {
        if (!isLoading && !editorTargetIsValid) {
            onEditorStateChange(MinkBoardEditorState.None)
        }
    }

    LaunchedEffect(actionErrorMessage) {
        actionErrorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            actions.clearActionError()
        }
    }

    if (showBoardMap && visibleSelectedBoard != null && isSelectedBoardAccessible) {
        val orderedStatuses = visibleSelectedBoard.statuses.sortedBy { it.position }
        BoardMapScreen(
            board = visibleSelectedBoard,
            focusedStatusId = orderedStatuses.getOrNull(statusListState.firstVisibleItemIndex)?.id,
            listState = boardMapListState,
            onDismiss = { showBoardMap = false },
            onSelectStatus = { status ->
                val statusIndex = orderedStatuses.indexOfFirst { it.id == status.id }
                showBoardMap = false
                if (statusIndex >= 0) {
                    boardNavigationScope.launch {
                        statusListState.animateScrollToItem(statusIndex)
                    }
                }
            },
        )
        return
    }

    if (showCalendar && visibleSelectedBoard != null && isSelectedBoardAccessible) {
        BoardCalendarScreen(
            board = visibleSelectedBoard,
            onBack = { showCalendar = false },
            onOpenCard = { card ->
                actions.openCard(visibleSelectedBoard.board.id, card.id)
                showCalendar = false
            },
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { boardMenuOpen = true },
                                    onLongClickLabel = visibleSelectedBoard?.board?.name?.let {
                                        stringResource(R.string.edit_board_named, it)
                                    },
                                    onLongClick = {
                                        editableBoards.firstOrNull {
                                            it.board.id == visibleSelectedBoard?.board?.id
                                        }?.let(requestEditBoard)
                                    },
                                ),
                            shape = RoundedCornerShape(24.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 40.dp)
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Outlined.Dashboard, contentDescription = null)
                                Text(
                                    text = visibleSelectedBoard?.board?.name
                                        ?: stringResource(R.string.minkboard_title),
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Icon(
                                    Icons.Outlined.ArrowDropDown,
                                    contentDescription = stringResource(R.string.switch_board),
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = boardMenuOpen,
                            onDismissRequest = { boardMenuOpen = false },
                            scrollState = boardMenuScrollState,
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.manage_boards)) },
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                onClick = {
                                    boardMenuOpen = false
                                    showManageBoards = true
                                },
                            )
                            editableBoards.sortedBy { it.board.position }.forEach { board ->
                                DropdownMenuItem(
                                    text = { Text(board.board.name) },
                                    onClick = {
                                        actions.selectBoard(board.board.id)
                                        boardMenuOpen = false
                                    },
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCalendar = true },
                        enabled = visibleSelectedBoard != null && isSelectedBoardAccessible,
                    ) {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = stringResource(R.string.open_calendar),
                        )
                    }
                    IconButton(
                        onClick = {
                            val currentStatusIndex = statusListState.firstVisibleItemIndex
                            boardNavigationScope.launch {
                                boardMapListState.scrollToItem(
                                    (currentStatusIndex - 1).coerceAtLeast(0),
                                )
                            }
                            showBoardMap = true
                        },
                        enabled = visibleSelectedBoard != null && isSelectedBoardAccessible,
                    ) {
                        Icon(
                            Icons.Outlined.ZoomOutMap,
                            contentDescription = stringResource(R.string.open_board_map),
                        )
                    }
                    Box {
                        IconButton(onClick = { showAddMenu = true }) {
                            Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add))
                        }
                        DropdownMenu(
                            expanded = showAddMenu,
                            onDismissRequest = { showAddMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.new_card)) },
                                onClick = {
                                    showAddMenu = false
                                    val statuses = visibleSelectedBoard?.statuses.orEmpty()
                                    if (statuses.size == 1) {
                                        val status = statuses.single()
                                        onEditorStateChange(
                                            MinkBoardEditorState.NewCard(
                                                boardIdValue = checkNotNull(visibleSelectedBoard).board.id.value,
                                                statusIdValue = status.id.value,
                                            ),
                                        )
                                    } else if (statuses.isNotEmpty()) {
                                        showCardStatusPicker = true
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.new_status)) },
                                onClick = {
                                    showAddMenu = false
                                    visibleSelectedBoard?.let { board ->
                                        onEditorStateChange(
                                            MinkBoardEditorState.NewStatus(
                                                boardIdValue = board.board.id.value,
                                            ),
                                        )
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.new_board)) },
                                onClick = {
                                    showAddMenu = false
                                    requestNewBoard()
                                },
                            )
                        }
                    }
                    onOpenSettings?.let { openSettings ->
                        IconButton(onClick = openSettings) {
                            Icon(
                                Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.open_settings),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            fatalError != null -> ErrorPane(fatalError, Modifier.padding(padding))
            visibleSelectedBoard == null -> StatusPane(stringResource(R.string.empty_workspace), Modifier.padding(padding))
            !isSelectedBoardAccessible -> LockedBoardPane(
                onUnlock = { actions.unlockBoard(checkNotNull(effectiveSelectedBoardId)) },
                modifier = Modifier.padding(padding),
            )
            else -> {
                val canonicalStatuses = visibleSelectedBoard.statuses.sortedBy { it.position }
                val visibleStatuses = remember(visibleSelectedBoard.board.id) {
                    mutableStateListOf<com.katoaapps.minkboard.model.BoardStatus>().apply {
                        addAll(canonicalStatuses)
                    }
                }
                LaunchedEffect(
                    pendingCardReveal?.statusId?.value,
                    visibleStatuses.map { it.id.value },
                ) {
                    val reveal = pendingCardReveal ?: return@LaunchedEffect
                    val statusIndex = visibleStatuses.indexOfFirst { it.id == reveal.statusId }
                    if (statusIndex >= 0) {
                        statusListState.animateScrollToItem(statusIndex)
                    }
                }
                LaunchedEffect(openStatusId?.value, visibleSelectedBoard.board.id.value) {
                    val target = openStatusId ?: return@LaunchedEffect
                    val statusIndex = visibleStatuses.indexOfFirst { it.id == target }
                    if (statusIndex >= 0) statusListState.animateScrollToItem(statusIndex)
                }
                LaunchedEffect(openCardId?.value, visibleSelectedBoard.cards) {
                    val target = openCardId ?: return@LaunchedEffect
                    val card = visibleSelectedBoard.cards.firstOrNull { it.id == target }
                        ?: return@LaunchedEffect
                    val position = visibleSelectedBoard.cardsIn(card.statusId)
                        .indexOfFirst { it.id == card.id }
                    if (position >= 0) {
                        pendingCardReveal = CardPlacement(card.id, card.statusId, position)
                    }
                }
                val focusedCardPlacement = activeFocusSession
                    ?.takeIf { session ->
                        session.state == FocusSessionState.Running &&
                            session.boardId == visibleSelectedBoard.board.id
                    }
                    ?.let { session ->
                        visibleSelectedBoard.cards.firstOrNull { it.id == session.cardId }
                    }
                    ?.let { card ->
                        val position = visibleSelectedBoard.cardsIn(card.statusId)
                            .indexOfFirst { it.id == card.id }
                        position.takeIf { it >= 0 }?.let {
                            CardPlacement(card.id, card.statusId, it)
                        }
                    }
                var draggedStatusIdValue by remember(visibleSelectedBoard.board.id) {
                    mutableStateOf<String?>(null)
                }
                var pendingStatusOrder by remember(visibleSelectedBoard.board.id) {
                    mutableStateOf<List<String>?>(null)
                }
                val statusHapticFeedback = LocalHapticFeedback.current
                val statusReorderState = rememberReorderableLazyListState(
                    lazyListState = statusListState,
                    scrollThresholdPadding = WindowInsets.systemBars.asPaddingValues(),
                ) { from, to ->
                    if (visibleStatuses.moveItem(from.index, to.index)) {
                        statusHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
                val canonicalStatusOrder = canonicalStatuses.map { it.id.value }
                LaunchedEffect(canonicalStatuses, draggedStatusIdValue, pendingStatusOrder) {
                    val persistedOrderArrived = pendingStatusOrder == canonicalStatusOrder
                    if (persistedOrderArrived) pendingStatusOrder = null
                    if (
                        draggedStatusIdValue == null &&
                        (pendingStatusOrder == null || persistedOrderArrived) &&
                        visibleStatuses != canonicalStatuses
                    ) {
                        visibleStatuses.clear()
                        visibleStatuses.addAll(canonicalStatuses)
                    }
                }
                val dragDropState = remember(visibleSelectedBoard.board.id) {
                    CardDragDropState().apply {
                        sync(visibleSelectedBoard.statuses, visibleSelectedBoard.cards)
                    }
                }
                LaunchedEffect(visibleSelectedBoard.statuses, visibleSelectedBoard.cards) {
                    dragDropState.sync(visibleSelectedBoard.statuses, visibleSelectedBoard.cards)
                }
                LaunchedEffect(actionErrorMessage) {
                    if (actionErrorMessage != null) {
                        dragDropState.resetToCanonical()
                        pendingStatusOrder = null
                        visibleStatuses.clear()
                        visibleStatuses.addAll(canonicalStatuses)
                    }
                }
                val moveCardAction by rememberUpdatedState(actions.moveCard)
                var boardPositionInRoot by remember { mutableStateOf(Offset.Zero) }
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .onGloballyPositioned { boardPositionInRoot = it.positionInRoot() }
                        .pointerInput(dragDropState) {
                            awaitEachGesture {
                                val down = awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial,
                                )
                                try {
                                    while (true) {
                                        val change = awaitPointerEvent(PointerEventPass.Initial)
                                            .changes
                                            .firstOrNull { it.id == down.id }
                                        if (change == null) {
                                            dragDropState.cancelDrag()
                                            break
                                        }
                                        if (dragDropState.activeDrag != null) {
                                            dragDropState.movePointerTo(
                                                boardPositionInRoot + change.position,
                                            )
                                        }
                                        if (!change.pressed) {
                                            dragDropState.finishDrag(moveCardAction)
                                            break
                                        }
                                    }
                                } finally {
                                    // The board owns gesture completion so a card or column can be
                                    // disposed by edge scrolling without orphaning its preview.
                                    if (dragDropState.activeDrag != null) {
                                        dragDropState.cancelDrag()
                                    }
                                }
                            }
                        },
                ) {
                    val density = LocalDensity.current
                    val targetVisibleColumns = when {
                        maxWidth >= 1_200.dp -> 5.5f
                        maxWidth >= 840.dp -> 4.5f
                        maxWidth >= 600.dp -> 3.5f
                        else -> 2f
                    }
                    val columnSpacing = if (maxWidth < 600.dp) 6.dp else 12.dp
                    val visibleColumnGaps = columnSpacing *
                        (ceil(targetVisibleColumns).toInt() - 1).coerceAtLeast(0)
                    val statusWidth = maxOf(
                        120.dp,
                        (maxWidth - 32.dp - visibleColumnGaps) / targetVisibleColumns,
                    )
                    val useCompactCards = statusWidth < 240.dp
                    val horizontalEdgeThresholdPx = with(density) { 64.dp.toPx() }
                    val dragPointer = dragDropState.activeDrag?.pointerInRoot
                    val pointerXInBoard = dragPointer?.x?.minus(boardPositionInRoot.x)
                    val horizontalScrollDirection = pointerXInBoard?.let { pointerX ->
                        edgeScrollDirection(
                            pointerPosition = pointerX,
                            viewportStart = 0f,
                            viewportEnd = with(density) { maxWidth.toPx() },
                            requestedThreshold = horizontalEdgeThresholdPx,
                        )
                    } ?: 0
                    LaunchedEffect(
                        dragDropState.activeDrag?.card?.id,
                        horizontalScrollDirection,
                    ) {
                        while (
                            isActive &&
                            horizontalScrollDirection != 0 &&
                            dragDropState.activeDrag != null
                        ) {
                            val consumed = statusListState.scrollBy(
                                horizontalScrollDirection * horizontalEdgeThresholdPx * 0.34f,
                            )
                            delay(16)
                            if (consumed != 0f) {
                                dragDropState.refreshDropTarget()
                            }
                        }
                    }
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.completion_count,
                                    visibleSelectedBoard.completedCardCount,
                                    visibleSelectedBoard.cards.size,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            focusedCardPlacement?.let { placement ->
                                IconButton(onClick = { pendingCardReveal = placement }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Timer,
                                        contentDescription = stringResource(
                                            R.string.jump_to_focused_card,
                                        ),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                        LazyRow(
                            modifier = Modifier
                                .weight(1f),
                            state = statusListState,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(columnSpacing),
                        ) {
                            items(
                                visibleStatuses,
                                key = { it.id.value },
                            ) { status ->
                                ReorderableItem(statusReorderState, key = status.id.value) { isDragging ->
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val scale by animateFloatAsState(
                                        targetValue = if (isDragging) 1.025f else 1f,
                                        label = "status drag scale",
                                    )
                                    val elevation by animateFloatAsState(
                                        targetValue = if (isDragging) 12f else 0f,
                                        label = "status drag elevation",
                                    )
                                    val statusIndex = visibleStatuses.indexOf(status)
                                    val previousStatus = visibleStatuses.getOrNull(statusIndex - 1)
                                    val nextStatus = visibleStatuses.getOrNull(statusIndex + 1)
                                    val moveEarlierLabel = stringResource(
                                        R.string.move_status_earlier,
                                        status.name,
                                    )
                                    val moveLaterLabel = stringResource(
                                        R.string.move_status_later,
                                        status.name,
                                    )
                                    val accessibilityActions = buildList {
                                        if (statusIndex > 0) {
                                            add(CustomAccessibilityAction(moveEarlierLabel) {
                                                if (!visibleStatuses.moveItem(statusIndex, statusIndex - 1)) {
                                                    false
                                                } else {
                                                    pendingStatusOrder = visibleStatuses.map { it.id.value }
                                                    actions.reorderStatuses(
                                                        visibleSelectedBoard.board.id,
                                                        visibleStatuses.map { it.id },
                                                    )
                                                    statusHapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.TextHandleMove,
                                                    )
                                                    true
                                                }
                                            })
                                        }
                                        if (statusIndex < visibleStatuses.lastIndex) {
                                            add(CustomAccessibilityAction(moveLaterLabel) {
                                                if (!visibleStatuses.moveItem(statusIndex, statusIndex + 1)) {
                                                    false
                                                } else {
                                                    pendingStatusOrder = visibleStatuses.map { it.id.value }
                                                    actions.reorderStatuses(
                                                        visibleSelectedBoard.board.id,
                                                        visibleStatuses.map { it.id },
                                                    )
                                                    statusHapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.TextHandleMove,
                                                    )
                                                    true
                                                }
                                            })
                                        }
                                    }
                                    BoardStatusPanel(
                                        status = status,
                                        cards = dragDropState.cardOrders[status.id].orEmpty(),
                                        dragDropState = dragDropState,
                                        previousStatusId = previousStatus?.id,
                                        previousStatusCardCount = previousStatus
                                            ?.let { dragDropState.cardOrders[it.id].orEmpty().size }
                                            ?: 0,
                                        nextStatusId = nextStatus?.id,
                                        nextStatusCardCount = nextStatus
                                            ?.let { dragDropState.cardOrders[it.id].orEmpty().size }
                                            ?: 0,
                                        compactLayout = useCompactCards,
                                        focusedCardId = focusedCardPlacement?.cardId,
                                        statusDragHandle = {
                                            Icon(
                                                imageVector = Icons.Default.DragHandle,
                                                contentDescription = stringResource(
                                                    R.string.reorder_status,
                                                    status.name,
                                                ),
                                                modifier = Modifier
                                                    .size(if (useCompactCards) 34.dp else 44.dp)
                                                    .draggableHandle(
                                                        interactionSource = interactionSource,
                                                        onDragStarted = {
                                                            draggedStatusIdValue = status.id.value
                                                            statusHapticFeedback.performHapticFeedback(
                                                                HapticFeedbackType.LongPress,
                                                            )
                                                        },
                                                        onDragStopped = {
                                                            pendingStatusOrder = visibleStatuses.map { it.id.value }
                                                            actions.reorderStatuses(
                                                                visibleSelectedBoard.board.id,
                                                                visibleStatuses.map { it.id },
                                                            )
                                                            draggedStatusIdValue = null
                                                        },
                                                    )
                                                    .padding(if (useCompactCards) 7.dp else 10.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        },
                                        modifier = Modifier
                                            .width(statusWidth)
                                            .fillParentMaxHeight()
                                            .padding(bottom = 12.dp)
                                            .semantics { customActions = accessibilityActions }
                                            .zIndex(if (isDragging) 1f else 0f)
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                                shadowElevation = elevation
                                            },
                                        onAddCard = {
                                            onEditorStateChange(
                                                MinkBoardEditorState.NewCard(
                                                    boardIdValue = visibleSelectedBoard.board.id.value,
                                                    statusIdValue = status.id.value,
                                                ),
                                            )
                                        },
                                        onManageStatus = {
                                            onEditorStateChange(
                                                MinkBoardEditorState.EditStatus(
                                                    boardIdValue = visibleSelectedBoard.board.id.value,
                                                    statusIdValue = status.id.value,
                                                    draft = StatusEditorDraft(
                                                        name = status.name,
                                                        colorArgb = status.colorArgb,
                                                        isDone = status.isDoneStatus,
                                                    ),
                                                ),
                                            )
                                        },
                                        onOpenCard = { card ->
                                            actions.openCard(visibleSelectedBoard.board.id, card.id)
                                        },
                                        revealCard = pendingCardReveal
                                            ?.takeIf { it.statusId == status.id },
                                        onRevealFinished = { revealedCardId ->
                                            if (pendingCardReveal?.cardId == revealedCardId) {
                                                pendingCardReveal = null
                                            }
                                        },
                                        onMoveCard = actions.moveCard,
                                    )
                                }
                            }
                        }
                        StatusScrollDock(
                            state = statusListState,
                            statuses = canonicalStatuses,
                            itemSpacing = columnSpacing,
                            contentDescription = stringResource(R.string.board_position),
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }

                    dragDropState.activeDrag?.let { drag ->
                        val previewStatusColor = visibleSelectedBoard.statuses
                            .firstOrNull { it.id == (drag.targetStatusId ?: drag.card.statusId) }
                            ?.let { Color(it.colorArgb) }
                            ?: MaterialTheme.colorScheme.surface
                        val previewWidth = statusWidth.coerceAtMost(240.dp)
                        val previewHeight = 72.dp
                        val previewWidthPx = with(density) { previewWidth.toPx() }
                        val previewHeightPx = with(density) { previewHeight.toPx() }
                        val maxWidthPx = with(density) { maxWidth.toPx() }
                        val maxHeightPx = with(density) { maxHeight.toPx() }
                        val localPointer = drag.pointerInRoot - boardPositionInRoot
                        val x = (localPointer.x - previewWidthPx / 2f)
                            .coerceIn(0f, (maxWidthPx - previewWidthPx).coerceAtLeast(0f))
                        val y = (localPointer.y - previewHeightPx / 2f)
                            .coerceIn(0f, (maxHeightPx - previewHeightPx).coerceAtLeast(0f))
                        CardDragPreview(
                            card = drag.card,
                            statusColor = previewStatusColor,
                            modifier = Modifier
                                .absoluteOffset { IntOffset(x.roundToInt(), y.roundToInt()) }
                                .zIndex(10f),
                        )
                    }
                }
            }
        }
    }

    MinkBoardEditorDialogHost(
        editableBoards = editableBoards,
        selectedBoard = visibleSelectedBoard,
        selectedBoardAccessible = isSelectedBoardAccessible,
        openCardId = openCardId,
        activeFocusSession = activeFocusSession,
        editorState = editorState,
        onEditorStateChange = onEditorStateChange,
        actions = actions,
        hostAccess = hostAccess,
        canUseBoardLock = canUseBoardLock,
        isBoardAccessible = isBoardAccessible,
        showManageBoards = showManageBoards,
        onManageBoardsDismiss = { showManageBoards = false },
        showCardStatusPicker = showCardStatusPicker,
        onCardStatusPickerDismiss = { showCardStatusPicker = false },
        onRequestEditBoard = requestEditBoard,
        onCardPlacementChanged = { pendingCardReveal = it },
        actionErrorMessage = actionErrorMessage,
    )
}
