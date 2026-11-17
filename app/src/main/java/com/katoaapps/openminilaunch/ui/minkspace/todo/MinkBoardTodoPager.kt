@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.katoaapps.openminilaunch.ui.minkspace.todo

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.katoaapps.minkboard.core.LauncherBoardState
import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkboard.LauncherBoardInitializationState
import com.katoaapps.openminilaunch.features.minkboard.OpenMinkLauncherBoardController
import com.katoaapps.openminilaunch.features.minkboard.canRevealLauncherHomeBoard
import com.katoaapps.openminilaunch.features.minkboard.visibleStatusIds
import com.katoaapps.openminilaunch.ui.launcher.home.HomeTodoTextMetrics
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceTitle
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.LightPaper
import com.katoaapps.openminilaunch.ui.theme.MinkBlack
import com.katoaapps.openminilaunch.ui.theme.MinkWhite
import com.katoaapps.openminilaunch.ui.theme.Sage
import kotlinx.coroutines.launch

private data class CompactBoardPage(
    val status: BoardStatus,
    val cards: List<BoardCard>,
    val statusPage: Int,
    val statusPageCount: Int,
)

@Composable
internal fun MinkBoardTodoPager(
    controller: OpenMinkLauncherBoardController,
    openBoard: () -> Unit,
    openCard: (BoardId, CardId) -> Unit,
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
            canRevealLauncherHomeBoard(
                isLocked = state.board.board.isLocked,
                accessGranted = isBoardAccessGranted(state.board.board.id),
            )
        } ?: false
    }

    LaunchedEffect(controller) { controller.initialize() }
    LaunchedEffect(ready, visibility) { ready?.let(controller.homePreferences::reconcile) }

    val safeItemsPerPage = itemsPerPage.coerceIn(1, 5)
    val pages = remember(ready, visibility, safeItemsPerPage, canRevealBoard) {
        if (!canRevealBoard) return@remember emptyList()
        ready?.let { state ->
            val visibleIds = visibleStatusIds(
                board = state.board,
                intakeStatusId = state.designation.intakeStatusId,
                preference = visibility,
            ).toSet()
            state.board.statuses
                .sortedBy { it.position }
                .filter { it.id in visibleIds }
                .flatMap { status ->
                    val chunks = state.board.cardsIn(status.id).chunked(safeItemsPerPage)
                        .ifEmpty { listOf(emptyList()) }
                    chunks.mapIndexed { index, cards ->
                        CompactBoardPage(
                            status = status,
                            cards = cards,
                            statusPage = index,
                            statusPageCount = chunks.size,
                        )
                    }
                }
        }.orEmpty()
    }
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })

    LaunchedEffect(pages.size) {
        if (pagerState.currentPage >= pages.size.coerceAtLeast(1)) {
            pagerState.scrollToPage((pages.size - 1).coerceAtLeast(0))
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

    when {
        initializationState == LauncherBoardInitializationState.CorruptLegacyData -> {
            CompactBoardMessage(
                message = stringResource(R.string.minkboard_legacy_todos_need_attention),
                openBoard = openBoard,
                contentColor = contentColor,
                modifier = modifier,
            )
        }
        initializationState == LauncherBoardInitializationState.Failed ||
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
            Column(
                modifier
                    .fillMaxWidth()
                    .clickable(onClick = openBoard)
                    .padding(if (compact) Dimens.dp8 else Dimens.dp16),
                verticalArrangement = Arrangement.spacedBy(if (compact) Dimens.dp3 else Dimens.dp8),
            ) {
                val activePage = pages[pagerState.currentPage.coerceIn(pages.indices)]
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    MinkSpaceTitle(
                        title = activePage.status.name,
                        icon = Icons.Default.Checklist,
                        contentColor = contentColor,
                        modifier = Modifier.weight(1f),
                    )
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
