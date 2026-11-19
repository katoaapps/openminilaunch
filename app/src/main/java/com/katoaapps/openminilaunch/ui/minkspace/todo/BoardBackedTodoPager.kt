@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.katoaapps.openminilaunch.ui.minkspace.todo

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.katoaapps.minkboard.core.LauncherBoardState
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.CardPlacement
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.openminilaunch.R
import com.katoaapps.minklauncher.todos.home.TodoInitializationState
import com.katoaapps.minklauncher.todos.home.TodoController
import com.katoaapps.minklauncher.todos.home.TodoCompactHomePosition
import com.katoaapps.minklauncher.todos.home.canRevealTodoHomeBoard
import com.katoaapps.openminilaunch.ui.launcher.home.HomeTodoTextMetrics
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceTitle
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.LightPaper
import com.katoaapps.openminilaunch.ui.theme.MinkBlack
import com.katoaapps.openminilaunch.ui.theme.MinkWhite
import com.katoaapps.openminilaunch.ui.theme.Sage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class CompactCardRevealRequest(
    val sequence: Int,
    val placement: CardPlacement,
)

@Composable
internal fun BoardBackedTodoPager(
    controller: TodoController,
    openBoard: () -> Unit,
    openStatus: (BoardId, StatusId) -> Unit,
    openCard: (BoardId, CardId) -> Unit,
    cardRevealRequest: CompactCardRevealRequest?,
    initialPosition: TodoCompactHomePosition?,
    onPositionChanged: (TodoCompactHomePosition) -> Unit,
    focusedCardId: CardId?,
    onCardRevealFinished: (Int) -> Unit,
    boardAccessRevision: Long,
    isBoardAccessGranted: (BoardId) -> Boolean,
    jumpToken: Int,
    itemsPerPage: Int = 3,
    textMetrics: HomeTodoTextMetrics = HomeTodoTextMetrics(15f, 20f, 2),
    compact: Boolean = false,
    contentColor: Color = LightPaper,
    mutedContentColor: Color = Sage,
    modifier: Modifier = Modifier,
) {
    val initializationState by controller.initializationState.collectAsState()
    val boardState by controller.boardState.collectAsState()
    val visibility by controller.homePreferences.visibleStatuses.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val ready = boardState as? LauncherBoardState.Ready
    val canRevealBoard = remember(ready, boardAccessRevision) {
        // The revision deliberately participates in this read. Access sessions are intentionally
        // process-local, so Compose needs the host-owned revision to observe grant expiry/change.
        ready?.let { state ->
            canRevealTodoHomeBoard(
                isLocked = state.board.board.isLocked,
                accessGranted = isBoardAccessGranted(state.board.board.id),
            )
        } ?: false
    }

    LaunchedEffect(controller) { controller.initialize() }
    LaunchedEffect(ready, visibility) { ready?.let(controller.homePreferences::reconcile) }

    val pages = remember(ready, visibility, itemsPerPage, canRevealBoard) {
        if (!canRevealBoard) return@remember emptyList()
        ready?.let { state ->
            compactTodoPages(
                board = state.board,
                intakeStatusId = state.designation.intakeStatusId,
                visibility = visibility,
                itemsPerPage = itemsPerPage,
            )
        }.orEmpty()
    }
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
    var highlightedCardId by remember(ready?.board?.board?.id?.value) {
        mutableStateOf<CardId?>(null)
    }
    var statusMenuOpen by remember { mutableStateOf(false) }
    var positionRestored by remember(ready?.board?.board?.id?.value) {
        mutableStateOf(false)
    }

    LaunchedEffect(pages.size) {
        if (pagerState.currentPage >= pages.size.coerceAtLeast(1)) {
            pagerState.scrollToPage((pages.size - 1).coerceAtLeast(0))
        }
    }
    LaunchedEffect(pages, initialPosition, positionRestored) {
        if (positionRestored || pages.isEmpty()) return@LaunchedEffect
        val currentBoardId = ready?.board?.board?.id
        val destination = initialPosition
            ?.takeIf { it.boardId == currentBoardId }
            ?.let { remembered ->
                pages.indexOfFirst { page ->
                    page.status.id == remembered.statusId &&
                        page.statusPage == remembered.statusPage
                }
            }
            ?.takeIf { it >= 0 }
            ?: 0
        pagerState.scrollToPage(destination)
        positionRestored = true
    }
    LaunchedEffect(positionRestored, pages) {
        if (!positionRestored || pages.isEmpty()) return@LaunchedEffect
        snapshotFlow { pagerState.currentPage }.collect { pageIndex ->
            pages.getOrNull(pageIndex)?.let { page ->
                ready?.board?.board?.id?.let { boardId ->
                    onPositionChanged(
                        TodoCompactHomePosition(
                            boardId = boardId,
                            statusId = page.status.id,
                            statusPage = page.statusPage,
                        ),
                    )
                }
            }
        }
    }
    LaunchedEffect(jumpToken, pages) {
        if (jumpToken > 0 && pages.isNotEmpty()) {
            val intakeId = ready?.designation?.intakeStatusId
            // Launcher-created To-Dos are inserted at the front of the intake status, so follow
            // the new card to that status's first compact page rather than its former append page.
            val destination = pages.indexOfFirst { page -> page.status.id == intakeId }
                .coerceAtLeast(0)
            pagerState.animateScrollToPage(destination)
        }
    }
    LaunchedEffect(cardRevealRequest?.sequence, pages) {
        val request = cardRevealRequest ?: return@LaunchedEffect
        val placement = request.placement
        val statusCards = ready?.board?.cardsIn(placement.statusId).orEmpty()
        val currentIndex = statusCards.indexOfFirst { it.id == placement.cardId }
        val expectedIndex = placement.position.coerceIn(0, statusCards.lastIndex.coerceAtLeast(0))
        if (currentIndex != expectedIndex) return@LaunchedEffect

        val pageIndex = pages.indexOfFirst { page ->
            page.cards.any { it.id == placement.cardId }
        }
        if (pageIndex < 0) {
            onCardRevealFinished(request.sequence)
            return@LaunchedEffect
        }

        pagerState.animateScrollToPage(pageIndex)
        delay(80)
        repeat(3) {
            highlightedCardId = placement.cardId
            delay(150)
            highlightedCardId = null
            delay(100)
        }
        onCardRevealFinished(request.sequence)
    }

    when {
        initializationState == TodoInitializationState.CorruptLegacyData -> {
            CompactBoardMessage(
                message = stringResource(R.string.minkboard_legacy_todos_need_attention),
                openBoard = openBoard,
                contentColor = contentColor,
                modifier = modifier,
            )
        }
        initializationState == TodoInitializationState.Failed ||
            boardState is LauncherBoardState.BrokenBinding -> {
            CompactBoardMessage(
                message = stringResource(R.string.minkboard_home_needs_repair),
                openBoard = openBoard,
                contentColor = contentColor,
                modifier = modifier,
            )
        }
        ready != null && !canRevealBoard -> {
            CompactBoardMessage(
                message = stringResource(R.string.minkboard_home_locked),
                openBoard = openBoard,
                contentColor = contentColor,
                modifier = modifier,
            )
        }
        ready == null || pages.isEmpty() -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = contentColor)
            }
        }
        else -> {
            val activePage = pages[pagerState.currentPage.coerceIn(pages.indices)]
            val visibleStatuses = pages.distinctBy { it.status.id }.map { it.status }
            val focusPageIndex = focusedCardId?.let { cardId ->
                pages.indexOfFirst { page -> page.cards.any { it.id == cardId } }
                    .takeIf { it >= 0 }
            }
            Column(
                modifier
                    .fillMaxWidth()
                    .clickable {
                        openStatus(ready.board.board.id, activePage.status.id)
                    }
                    .padding(if (compact) Dimens.dp8 else Dimens.dp16),
                verticalArrangement = Arrangement.spacedBy(if (compact) Dimens.dp3 else Dimens.dp8),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.clickable { statusMenuOpen = true },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MinkSpaceTitle(
                                title = activePage.status.name,
                                icon = Icons.Default.Checklist,
                                contentColor = contentColor,
                            )
                            Icon(
                                imageVector = Icons.Outlined.ArrowDropDown,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(Dimens.dp18),
                            )
                        }
                        DropdownMenu(
                            expanded = statusMenuOpen,
                            onDismissRequest = { statusMenuOpen = false },
                        ) {
                            visibleStatuses.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status.name) },
                                    onClick = {
                                        statusMenuOpen = false
                                        val destination = pages.indexOfFirst {
                                            it.status.id == status.id
                                        }
                                        if (destination >= 0) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(destination)
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                    Text(
                        text = if (activePage.statusPageCount == 1) {
                            stringResource(
                                R.string.minkboard_status_card_count,
                                ready.board.cardsIn(activePage.status.id).size,
                            )
                        } else {
                            stringResource(
                                R.string.page_of_pages,
                                activePage.statusPage + 1,
                                activePage.statusPageCount,
                            )
                        },
                        color = mutedContentColor,
                        fontSize = Dimens.sp12,
                    )
                    focusPageIndex?.let { destination ->
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(destination)
                                    focusedCardId?.let { cardId ->
                                        repeat(3) {
                                            highlightedCardId = cardId
                                            delay(150)
                                            highlightedCardId = null
                                            delay(100)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(Dimens.dp32),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = stringResource(
                                    com.katoaapps.minkboard.ui.R.string.jump_to_focused_card,
                                ),
                                tint = contentColor,
                                modifier = Modifier.size(Dimens.dp18),
                            )
                        }
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) { pageIndex ->
                    val page = pages.getOrNull(pageIndex)
                    if (page == null || page.cards.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                            Text(
                                stringResource(R.string.minkboard_status_empty),
                                color = mutedContentColor,
                            )
                        }
                    } else {
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(
                                if (compact) Dimens.dp5 else Dimens.dp8,
                            ),
                        ) {
                            page.cards.forEach { card ->
                                val completed = page.status.isDoneStatus
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .border(
                                            width = if (highlightedCardId == card.id) {
                                                Dimens.dp3
                                            } else {
                                                Dimens.dp0
                                            },
                                            color = contentColor,
                                            shape = RoundedCornerShape(Dimens.dp8),
                                        )
                                        .clickable {
                                            openCard(ready.board.board.id, card.id)
                                        },
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Checkbox(
                                        checked = completed,
                                        onCheckedChange = {
                                            coroutineScope.launch {
                                                controller.toggleCompletion(card.id)
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = contentColor,
                                            uncheckedColor = mutedContentColor,
                                            checkmarkColor = if (contentColor == MinkWhite) {
                                                MinkBlack
                                            } else {
                                                MinkWhite
                                            },
                                        ),
                                        modifier = Modifier.size(
                                            if (compact) Dimens.dp24 else Dimens.dp26,
                                        ),
                                    )
                                    if (card.isMajor) {
                                        Icon(
                                            imageVector = Icons.Outlined.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFD6A300),
                                            modifier = Modifier
                                                .padding(start = Dimens.dp3, top = Dimens.dp3)
                                                .size(Dimens.dp18),
                                        )
                                    }
                                    if (focusedCardId == card.id) {
                                        Icon(
                                            imageVector = Icons.Outlined.Timer,
                                            contentDescription = null,
                                            tint = contentColor,
                                            modifier = Modifier
                                                .padding(start = Dimens.dp3, top = Dimens.dp3)
                                                .size(Dimens.dp18),
                                        )
                                    }
                                    Text(
                                        text = card.title,
                                        color = if (completed) mutedContentColor else contentColor,
                                        fontSize = textMetrics.fontSizeSp.sp,
                                        lineHeight = textMetrics.lineHeightSp.sp,
                                        maxLines = textMetrics.maxLines,
                                        overflow = TextOverflow.Ellipsis,
                                        textDecoration = if (completed) {
                                            TextDecoration.LineThrough
                                        } else {
                                            null
                                        },
                                        modifier = Modifier
                                            .padding(start = Dimens.dp7, top = Dimens.dp2)
                                            .weight(1f),
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

@Composable
private fun CompactBoardMessage(
    message: String,
    openBoard: () -> Unit,
    contentColor: Color,
    modifier: Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().clickable(onClick = openBoard).padding(Dimens.dp16),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(message, color = contentColor)
    }
}
