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
import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.core.BoardTemplateId
import com.katoaapps.minkboard.core.MinkBoardCapability
import com.katoaapps.minkboard.core.MinkBoardHostAccess
import com.katoaapps.minkboard.core.MinkBoardTemplates
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardUpdate
import com.katoaapps.minkboard.model.BoardSubtask
import com.katoaapps.minkboard.model.CardDraft
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.CardUpdate
import com.katoaapps.minkboard.model.FocusSession
import com.katoaapps.minkboard.model.FocusSessionState
import com.katoaapps.minkboard.model.FocusTimerId
import com.katoaapps.minkboard.model.MinkBoardWorkspace
import com.katoaapps.minkboard.model.StatusDraft
import com.katoaapps.minkboard.model.StatusDeletionPolicy
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.minkboard.model.StatusUpdate
import com.katoaapps.minkboard.model.SubtaskId
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
    val addCard: (StatusId, CardDraft, () -> Unit) -> Unit,
    val cloneCard: (CardId, String, () -> Unit) -> Unit,
    val updateCardAndMove: (CardId, CardUpdate, StatusId, () -> Unit) -> Unit,
    val moveCard: (CardId, StatusId, Int) -> Unit,
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
    workspace: MinkBoardWorkspace,
    hostAccess: MinkBoardHostAccess,
    selectedBoardId: BoardId?,
    openCardId: CardId?,
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

    val editableWorkspace = remember(workspace, hostAccess) {
        hostAccess.editableWorkspace(workspace)
    }
    val selectedBoard = editableWorkspace.board(selectedBoardId) ?: editableWorkspace.firstBoard
    val effectiveSelectedBoardId = selectedBoard?.board?.id
    val isSelectedBoardAccessible = effectiveSelectedBoardId?.let(isBoardAccessible) ?: true
    val editableBoardIdValues = editableWorkspace.boards
        .map { it.board.id.value }
        .sorted()
    var boardMenuOpen by rememberSaveable { mutableStateOf(false) }
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    var showBoardMap by rememberSaveable { mutableStateOf(false) }
    var showAddMenu by rememberSaveable { mutableStateOf(false) }
    var showCardStatusPicker by rememberSaveable { mutableStateOf(false) }
    var showManageBoards by rememberSaveable { mutableStateOf(false) }
    // Confirmations are deliberately process-memory only: restoring an editor must never restore
    // a pending destructive decision or replay its callback.
    var pendingDeleteBoardIdValue by remember { mutableStateOf<String?>(null) }
    var pendingDeleteBoardIdValues by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingDeleteStatusIdValue by remember { mutableStateOf<String?>(null) }
    var pendingDeleteCardIdValue by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val boardNavigationScope = rememberCoroutineScope()
    val statusListState = rememberLazyListState()
    val boardMapListState = rememberLazyListState()
    val openCard = selectedBoard?.cards?.firstOrNull { it.id == openCardId }
    val pendingDeleteBoard = pendingDeleteBoardIdValue
        ?.let { id -> editableWorkspace.boards.firstOrNull { it.board.id.value == id } }
    val pendingDeleteBoards = editableWorkspace.boards.filter {
        it.board.id.value in pendingDeleteBoardIdValues
    }
    val pendingDeleteStatus = pendingDeleteStatusIdValue
        ?.let { id -> selectedBoard?.statuses?.firstOrNull { it.id.value == id } }
    val requestNewBoard = {
        if (hostAccess.canCreateBoard(workspace)) {
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
    val requestEditBoard: (com.katoaapps.minkboard.model.BoardSnapshot) -> Unit = { boardSnapshot ->
        if (isBoardAccessible(boardSnapshot.board.id)) {
            onEditorStateChange(
                MinkBoardEditorState.EditBoard(
                    boardIdValue = boardSnapshot.board.id.value,
                    draft = BoardEditorDraft(
                        name = boardSnapshot.board.name,
                        isLocked = boardSnapshot.board.isLocked,
                    ),
                ),
            )
        } else {
            actions.selectBoard(boardSnapshot.board.id)
        }
    }
    val boardMenuScrollState = rememberScrollState()

    var previousSelectedBoardId by remember { mutableStateOf(effectiveSelectedBoardId?.value) }
    LaunchedEffect(effectiveSelectedBoardId?.value, editableBoardIdValues) {
        // The repository initially emits an empty workspace. Treat the first resolved board as
        // hydration, not as user navigation, or a host-restored editor would be discarded.
        val selectedBoardChanged = previousSelectedBoardId != null &&
            previousSelectedBoardId != effectiveSelectedBoardId?.value
        if (selectedBoardChanged) {
            showCalendar = false
            pendingDeleteStatusIdValue = null
            pendingDeleteCardIdValue = null
            if (editorState.isSelectedBoardEditor()) {
                onEditorStateChange(MinkBoardEditorState.None)
            }
        }
        previousSelectedBoardId = effectiveSelectedBoardId?.value
    }

    val editorTargetIsValid = editorState.isValidFor(
        workspace = editableWorkspace,
        canCreateBoard = hostAccess.canCreateBoard(workspace),
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

    if (showBoardMap && selectedBoard != null && isSelectedBoardAccessible) {
        val orderedStatuses = selectedBoard.statuses.sortedBy { it.position }
        BoardMapScreen(
            board = selectedBoard,
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

    if (showCalendar && selectedBoard != null && isSelectedBoardAccessible) {
        BoardCalendarScreen(
            board = selectedBoard,
            onBack = { showCalendar = false },
            onOpenCard = { card ->
                actions.openCard(selectedBoard.board.id, card.id)
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
                                    onLongClickLabel = selectedBoard?.board?.name?.let {
                                        stringResource(R.string.edit_board_named, it)
                                    },
                                    onLongClick = {
                                        selectedBoard?.let(requestEditBoard)
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
                                    text = selectedBoard?.board?.name
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
                            editableWorkspace.boards.sortedBy { it.board.position }.forEach { board ->
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
                        enabled = selectedBoard != null && isSelectedBoardAccessible,
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
                        enabled = selectedBoard != null && isSelectedBoardAccessible,
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
                                    val statuses = selectedBoard?.statuses.orEmpty()
                                    if (statuses.size == 1) {
                                        val status = statuses.single()
                                        onEditorStateChange(
                                            MinkBoardEditorState.NewCard(
                                                boardIdValue = checkNotNull(selectedBoard).board.id.value,
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
                                    selectedBoard?.let { board ->
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
            selectedBoard == null -> StatusPane(stringResource(R.string.empty_workspace), Modifier.padding(padding))
            !isSelectedBoardAccessible -> LockedBoardPane(
                onUnlock = { actions.unlockBoard(checkNotNull(effectiveSelectedBoardId)) },
                modifier = Modifier.padding(padding),
            )
            else -> {
                val canonicalStatuses = selectedBoard.statuses.sortedBy { it.position }
                val visibleStatuses = remember(selectedBoard.board.id) {
                    mutableStateListOf<com.katoaapps.minkboard.model.BoardStatus>().apply {
                        addAll(canonicalStatuses)
                    }
                }
                var draggedStatusIdValue by remember(selectedBoard.board.id) {
                    mutableStateOf<String?>(null)
                }
                var pendingStatusOrder by remember(selectedBoard.board.id) {
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
                val dragDropState = remember(selectedBoard.board.id) {
                    CardDragDropState().apply {
                        sync(selectedBoard.statuses, selectedBoard.cards)
                    }
                }
                LaunchedEffect(selectedBoard.statuses, selectedBoard.cards) {
                    dragDropState.sync(selectedBoard.statuses, selectedBoard.cards)
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
                    val statusWidth = if (maxWidth < 600.dp) maxWidth * 0.86f else 360.dp
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
                        Text(
                            text = stringResource(
                                R.string.completion_count,
                                selectedBoard.completedCardCount,
                                selectedBoard.cards.size,
                            ),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyRow(
                            modifier = Modifier
                                .weight(1f),
                            state = statusListState,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                                                        selectedBoard.board.id,
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
                                                        selectedBoard.board.id,
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
                                        statusDragHandle = {
                                            Icon(
                                                imageVector = Icons.Default.DragHandle,
                                                contentDescription = stringResource(
                                                    R.string.reorder_status,
                                                    status.name,
                                                ),
                                                modifier = Modifier
                                                    .size(44.dp)
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
                                                                selectedBoard.board.id,
                                                                visibleStatuses.map { it.id },
                                                            )
                                                            draggedStatusIdValue = null
                                                        },
                                                    )
                                                    .padding(10.dp),
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
                                                    boardIdValue = selectedBoard.board.id.value,
                                                    statusIdValue = status.id.value,
                                                ),
                                            )
                                        },
                                        onManageStatus = {
                                            onEditorStateChange(
                                                MinkBoardEditorState.EditStatus(
                                                    boardIdValue = selectedBoard.board.id.value,
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
                                            actions.openCard(selectedBoard.board.id, card.id)
                                        },
                                        onMoveCard = actions.moveCard,
                                    )
                                }
                            }
                        }
                        StatusScrollDock(
                            state = statusListState,
                            statuses = canonicalStatuses,
                            itemSpacing = 12.dp,
                            contentDescription = stringResource(R.string.board_position),
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }

                    dragDropState.activeDrag?.let { drag ->
                        val previewStatusColor = selectedBoard.statuses
                            .firstOrNull { it.id == (drag.targetStatusId ?: drag.card.statusId) }
                            ?.let { Color(it.colorArgb) }
                            ?: MaterialTheme.colorScheme.surface
                        val previewWidth = 240.dp
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

    if (showManageBoards) {
        ManageBoardsDialog(
            boards = editableWorkspace.boards.map { it.board },
            actionErrorMessage = actionErrorMessage,
            onDismiss = { showManageBoards = false },
            onSelect = { boardId ->
                actions.selectBoard(boardId)
                showManageBoards = false
            },
            onEdit = { boardId ->
                showManageBoards = false
                editableWorkspace.board(boardId)?.let(requestEditBoard)
            },
            onReorder = actions.reorderBoards,
            onDeleteMany = { boardIds ->
                showManageBoards = false
                pendingDeleteBoardIdValues = boardIds.map(BoardId::value).toSet()
            },
        )
    }

    if (showCardStatusPicker && selectedBoard != null && isSelectedBoardAccessible) {
        ChooseCardStatusDialog(
            statuses = selectedBoard.statuses,
            onDismiss = { showCardStatusPicker = false },
            onSelect = { status ->
                showCardStatusPicker = false
                onEditorStateChange(
                    MinkBoardEditorState.NewCard(
                        boardIdValue = selectedBoard.board.id.value,
                        statusIdValue = status.id.value,
                    ),
                )
            },
        )
    }

    if (
        editorState == MinkBoardEditorState.None &&
        openCard != null &&
        isSelectedBoardAccessible
    ) {
        val cardStatus = selectedBoard.statuses.firstOrNull { it.id == openCard.statusId }
        val cardDuplicateSuffix = stringResource(R.string.duplicate_title_suffix)
        if (cardStatus != null) {
            val statusCards = selectedBoard.cardsIn(cardStatus.id)
            val cardIndex = statusCards.indexOfFirst { it.id == openCard.id }
            CardViewDialog(
                card = openCard,
                status = cardStatus,
                isFocusRunningForCard = activeFocusSession?.let {
                    it.state == FocusSessionState.Running && it.cardId == openCard.id
                } == true,
                onDismiss = actions.closeCard,
                onEdit = {
                    onEditorStateChange(openCard.toEditorState(selectedBoard.board.id.value))
                },
                onManageSubtasks = {
                    onEditorStateChange(
                        MinkBoardEditorState.ManageCardSubtasks(
                            boardIdValue = selectedBoard.board.id.value,
                            cardIdValue = openCard.id.value,
                        ),
                    )
                },
                onFocus = {
                    val active = activeFocusSession
                    if (
                        active?.state == FocusSessionState.Running &&
                        active.cardId == openCard.id
                    ) {
                        actions.stopFocus(active.id)
                    } else {
                        onEditorStateChange(
                            MinkBoardEditorState.FocusCard(
                                boardIdValue = selectedBoard.board.id.value,
                                cardIdValue = openCard.id.value,
                            ),
                        )
                    }
                },
                canSendToTop = cardIndex > 0,
                canSendToBottom = cardIndex in 0 until statusCards.lastIndex,
                onSendToTop = {
                    actions.moveCard(openCard.id, cardStatus.id, 0)
                },
                onSendToBottom = {
                    actions.moveCard(openCard.id, cardStatus.id, statusCards.lastIndex)
                },
                onDuplicate = {
                    actions.cloneCard(
                        openCard.id,
                        cardDuplicateSuffix,
                    ) {}
                },
                onDelete = { pendingDeleteCardIdValue = openCard.id.value },
            )
        }
    }

    when (val editor = editorState) {
        MinkBoardEditorState.None -> Unit

        is MinkBoardEditorState.NewBoard -> BoardNameDialog(
            draft = editor.draft,
            canLockBoard = canUseBoardLock,
            supportsTemplates = hostAccess.capabilities.supportsTemplates,
            onDraftChange = { onEditorStateChange(editor.copy(draft = it)) },
            onDismiss = { onEditorStateChange(MinkBoardEditorState.None) },
            onCreate = { name, templateId, isLocked ->
                val boardDraft = BoardDraft(name, isLocked)
                val onCreated: (BoardId) -> Unit = {
                    onEditorStateChange(MinkBoardEditorState.None)
                }
                if (templateId == null) {
                    actions.createBoard(boardDraft, onCreated)
                } else {
                    actions.createBoardFromTemplate(boardDraft, templateId, onCreated)
                }
            },
        )

        is MinkBoardEditorState.EditBoard -> {
            val boardSnapshot = editableWorkspace.boards.firstOrNull {
                it.board.id.value == editor.boardIdValue
            }
            if (
                boardSnapshot != null &&
                isBoardAccessible(boardSnapshot.board.id)
            ) {
                BoardSettingsDialog(
                    board = boardSnapshot.board,
                    draft = editor.draft,
                    canDelete = editableWorkspace.boards.size > 1,
                    canLockBoard = canUseBoardLock,
                    onDraftChange = { onEditorStateChange(editor.copy(draft = it)) },
                    onDismiss = { onEditorStateChange(MinkBoardEditorState.None) },
                    onSave = { update ->
                        actions.updateBoard(boardSnapshot.board.id, update) {
                            onEditorStateChange(MinkBoardEditorState.None)
                        }
                    },
                    onRequestDelete = {
                        pendingDeleteBoardIdValue = boardSnapshot.board.id.value
                        onEditorStateChange(MinkBoardEditorState.None)
                    },
                )
            }
        }

        is MinkBoardEditorState.NewStatus -> {
            val boardSnapshot = editableWorkspace.boards.firstOrNull {
                it.board.id.value == editor.boardIdValue
            }
            if (boardSnapshot != null && isBoardAccessible(boardSnapshot.board.id)) {
                StatusDialog(
                    draft = editor.draft,
                    replacesDoneStatus = boardSnapshot.statuses.any { it.isDoneStatus },
                    onDraftChange = { onEditorStateChange(editor.copy(draft = it)) },
                    onDismiss = { onEditorStateChange(MinkBoardEditorState.None) },
                    onCreate = { statusDraft ->
                        actions.addStatus(boardSnapshot.board.id, statusDraft) {
                            onEditorStateChange(MinkBoardEditorState.None)
                        }
                    },
                )
            }
        }

        is MinkBoardEditorState.EditStatus -> {
            val boardSnapshot = editableWorkspace.boards.firstOrNull {
                it.board.id.value == editor.boardIdValue
            }
            val status = boardSnapshot?.statuses?.firstOrNull {
                it.id.value == editor.statusIdValue
            }
            if (
                boardSnapshot != null &&
                status != null &&
                isBoardAccessible(boardSnapshot.board.id)
            ) {
                val statuses = boardSnapshot.statuses.sortedBy { it.position }
                StatusSettingsDialog(
                    status = status,
                    draft = editor.draft,
                    statusCount = statuses.size,
                    replacesDoneStatus = !status.isDoneStatus &&
                        statuses.any { it.id != status.id && it.isDoneStatus },
                    onDraftChange = { onEditorStateChange(editor.copy(draft = it)) },
                    onDismiss = { onEditorStateChange(MinkBoardEditorState.None) },
                    onSave = { update ->
                        actions.updateStatus(status.id, update) {
                            onEditorStateChange(MinkBoardEditorState.None)
                        }
                    },
                    onRequestDelete = {
                        pendingDeleteStatusIdValue = status.id.value
                        onEditorStateChange(MinkBoardEditorState.None)
                    },
                )
            }
        }

        is MinkBoardEditorState.NewCard -> {
            val boardSnapshot = editableWorkspace.boards.firstOrNull {
                it.board.id.value == editor.boardIdValue
            }
            val status = boardSnapshot?.statuses?.firstOrNull {
                it.id.value == editor.statusIdValue
            }
            if (
                boardSnapshot != null &&
                status != null &&
                isBoardAccessible(boardSnapshot.board.id)
            ) {
                NewCardDialog(
                    draft = editor.draft,
                    onDraftChange = { onEditorStateChange(editor.copy(draft = it)) },
                    onDismiss = { onEditorStateChange(MinkBoardEditorState.None) },
                    onCreate = { cardDraft ->
                        actions.addCard(status.id, cardDraft) {
                            onEditorStateChange(MinkBoardEditorState.None)
                        }
                    },
                )
            }
        }

        is MinkBoardEditorState.EditCard -> {
            val boardSnapshot = editableWorkspace.boards.firstOrNull {
                it.board.id.value == editor.boardIdValue
            }
            val card = boardSnapshot?.cards?.firstOrNull { it.id.value == editor.cardIdValue }
            if (
                boardSnapshot != null &&
                card != null &&
                isBoardAccessible(boardSnapshot.board.id)
            ) {
                val statuses = boardSnapshot.statuses.sortedBy { it.position }
                val currentStatus = statuses.first { it.id == card.statusId }
                EditCardDialog(
                    card = card,
                    draft = editor.draft,
                    statuses = statuses,
                    currentStatus = currentStatus,
                    onDraftChange = { onEditorStateChange(editor.copy(draft = it)) },
                    onDismiss = {
                        onEditorStateChange(MinkBoardEditorState.None)
                    },
                    onSave = { update, destination ->
                        actions.updateCardAndMove(card.id, update, destination.id) {
                            onEditorStateChange(MinkBoardEditorState.None)
                        }
                    },
                )
            }
        }

        is MinkBoardEditorState.ManageCardSubtasks -> {
            val boardSnapshot = editableWorkspace.boards.firstOrNull {
                it.board.id.value == editor.boardIdValue
            }
            val card = boardSnapshot?.cards?.firstOrNull { it.id.value == editor.cardIdValue }
            if (
                boardSnapshot != null &&
                card != null &&
                isBoardAccessible(boardSnapshot.board.id)
            ) {
                SubtaskManagerDialog(
                    card = card,
                    draft = editor.draft,
                    onDraftChange = { onEditorStateChange(editor.copy(draft = it)) },
                    onDismiss = { onEditorStateChange(MinkBoardEditorState.None) },
                    onAddSubtask = { text, onSuccess ->
                        actions.addSubtask(card.id, text, onSuccess)
                    },
                    onUpdateSubtask = { subtask, text, onSuccess ->
                        actions.updateSubtask(subtask.id, text, onSuccess)
                    },
                    onToggleSubtask = { subtask: BoardSubtask, checked ->
                        actions.setSubtaskCompleted(subtask.id, checked)
                    },
                    onDeleteSubtask = { subtask -> actions.deleteSubtask(subtask.id) },
                )
            }
        }

        is MinkBoardEditorState.FocusCard -> {
            val boardSnapshot = editableWorkspace.boards.firstOrNull {
                it.board.id.value == editor.boardIdValue
            }
            val card = boardSnapshot?.cards?.firstOrNull { it.id.value == editor.cardIdValue }
            if (
                boardSnapshot != null &&
                card != null &&
                isBoardAccessible(boardSnapshot.board.id)
            ) {
                val returnToCard = {
                    val returnDraft = editor.returnCardDraft
                    if (returnDraft == null) {
                        onEditorStateChange(MinkBoardEditorState.None)
                    } else {
                        onEditorStateChange(
                            MinkBoardEditorState.EditCard(
                                boardIdValue = boardSnapshot.board.id.value,
                                cardIdValue = card.id.value,
                                draft = returnDraft,
                            ),
                        )
                    }
                }
                FocusTimerDialog(
                    draft = editor.draft,
                    replacesRunningTimer = activeFocusSession?.state == FocusSessionState.Running,
                    onDraftChange = { onEditorStateChange(editor.copy(draft = it)) },
                    onDismiss = returnToCard,
                    onStart = { durationMillis, replace ->
                        actions.startFocus(
                            boardSnapshot.board.id,
                            card.id,
                            durationMillis,
                            replace,
                        )
                        onEditorStateChange(MinkBoardEditorState.None)
                    },
                )
            }
        }
    }

    pendingDeleteBoard
        ?.takeIf { boardSnapshot -> isBoardAccessible(boardSnapshot.board.id) }
        ?.let { boardSnapshot ->
            DeleteBoardConfirmationDialog(
                boardName = boardSnapshot.board.name,
                cardCount = boardSnapshot.cards.size,
                onDismiss = { pendingDeleteBoardIdValue = null },
                onConfirm = {
                    actions.deleteBoard(boardSnapshot.board.id) {
                        pendingDeleteBoardIdValue = null
                    }
                },
            )
        }
    if (pendingDeleteBoards.isNotEmpty()) {
        DeleteBoardsConfirmationDialog(
            boardCount = pendingDeleteBoards.size,
            cardCount = pendingDeleteBoards.sumOf { it.cards.size },
            onDismiss = { pendingDeleteBoardIdValues = emptySet() },
            onConfirm = {
                val boardIds = pendingDeleteBoards.map { it.board.id }.toSet()
                actions.deleteBoards(boardIds) {
                    pendingDeleteBoardIdValues = emptySet()
                }
            },
        )
    }
    if (
        pendingDeleteStatus != null &&
        selectedBoard != null &&
        isBoardAccessible(selectedBoard.board.id)
    ) {
        DeleteStatusDialog(
            status = pendingDeleteStatus,
            cardCount = selectedBoard.cardsIn(pendingDeleteStatus.id).size,
            destinations = selectedBoard.statuses.filter { it.id != pendingDeleteStatus.id },
            onDismiss = { pendingDeleteStatusIdValue = null },
            onConfirm = { policy ->
                actions.deleteStatus(pendingDeleteStatus.id, policy) {
                    pendingDeleteStatusIdValue = null
                }
            },
        )
    }
    val pendingDeleteCard = pendingDeleteCardIdValue?.let { cardIdValue ->
        editableWorkspace.boards.firstNotNullOfOrNull { board ->
            board.cards.firstOrNull { it.id.value == cardIdValue }?.let { board to it }
        }
    }
    if (pendingDeleteCard != null) {
        val (board, card) = pendingDeleteCard
        if (isBoardAccessible(board.board.id)) {
            DeleteCardConfirmationDialog(
                cardTitle = card.title,
                onDismiss = { pendingDeleteCardIdValue = null },
                onConfirm = {
                    actions.deleteCard(card.id) {
                        pendingDeleteCardIdValue = null
                        actions.closeCard()
                        onEditorStateChange(MinkBoardEditorState.None)
                    }
                },
            )
        }
    }
}

private fun MinkBoardEditorState.isSelectedBoardEditor(): Boolean = when (this) {
    MinkBoardEditorState.None,
    is MinkBoardEditorState.NewBoard,
    is MinkBoardEditorState.EditBoard,
    -> false

    is MinkBoardEditorState.NewStatus,
    is MinkBoardEditorState.EditStatus,
    is MinkBoardEditorState.NewCard,
    is MinkBoardEditorState.EditCard,
    is MinkBoardEditorState.ManageCardSubtasks,
    is MinkBoardEditorState.FocusCard,
    -> true
}

private fun MinkBoardEditorState.targetKey(): String = when (this) {
    MinkBoardEditorState.None -> "none"
    is MinkBoardEditorState.NewBoard -> "new-board"
    is MinkBoardEditorState.EditBoard -> "edit-board:$boardIdValue"
    is MinkBoardEditorState.NewStatus -> "new-status:$boardIdValue"
    is MinkBoardEditorState.EditStatus -> "edit-status:$boardIdValue:$statusIdValue"
    is MinkBoardEditorState.NewCard -> "new-card:$boardIdValue:$statusIdValue"
    is MinkBoardEditorState.EditCard -> "edit-card:$boardIdValue:$cardIdValue"
    is MinkBoardEditorState.ManageCardSubtasks -> "manage-subtasks:$boardIdValue:$cardIdValue"
    is MinkBoardEditorState.FocusCard -> "focus-card:$boardIdValue:$cardIdValue"
}

private fun MinkBoardEditorState.isValidFor(
    workspace: MinkBoardWorkspace,
    canCreateBoard: Boolean,
): Boolean {
    fun board(idValue: String) = workspace.boards.firstOrNull { it.board.id.value == idValue }

    return when (this) {
        MinkBoardEditorState.None -> true
        is MinkBoardEditorState.NewBoard -> canCreateBoard

        is MinkBoardEditorState.EditBoard -> board(boardIdValue) != null
        is MinkBoardEditorState.NewStatus -> board(boardIdValue) != null
        is MinkBoardEditorState.EditStatus -> board(boardIdValue)
            ?.statuses
            ?.any { it.id.value == statusIdValue } == true
        is MinkBoardEditorState.NewCard -> board(boardIdValue)
            ?.statuses
            ?.any { it.id.value == statusIdValue } == true
        is MinkBoardEditorState.EditCard -> board(boardIdValue)
            ?.cards
            ?.any { it.id.value == cardIdValue } == true
        is MinkBoardEditorState.ManageCardSubtasks -> board(boardIdValue)
            ?.cards
            ?.any { it.id.value == cardIdValue } == true
        is MinkBoardEditorState.FocusCard -> board(boardIdValue)
            ?.cards
            ?.any { it.id.value == cardIdValue } == true
    }
}

private fun BoardCard.toEditorDraft(): CardEditorDraft = CardEditorDraft(
    title = title,
    details = details,
    dueDateText = dueDate?.toString().orEmpty(),
    isMajor = isMajor,
    destinationStatusIdValue = statusId.value,
)

private fun BoardCard.toEditorState(boardIdValue: String): MinkBoardEditorState.EditCard =
    MinkBoardEditorState.EditCard(
        boardIdValue = boardIdValue,
        cardIdValue = id.value,
        draft = toEditorDraft(),
    )


@Composable
private fun StatusPane(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LockedBoardPane(onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text(stringResource(R.string.locked_board_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.locked_board_body), textAlign = TextAlign.Center)
            androidx.compose.material3.Button(onClick = onUnlock) {
                Text(stringResource(R.string.unlock_board))
            }
        }
    }
}

@Composable
private fun ErrorPane(
    error: MinkBoardFatalErrorDisplay,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(
                    if (error == MinkBoardFatalErrorDisplay.Decryption) {
                        R.string.data_locked_title
                    } else {
                        R.string.data_storage_error_title
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(
                    if (error == MinkBoardFatalErrorDisplay.Decryption) {
                        R.string.data_locked_body
                    } else {
                        R.string.data_storage_error_body
                    },
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
