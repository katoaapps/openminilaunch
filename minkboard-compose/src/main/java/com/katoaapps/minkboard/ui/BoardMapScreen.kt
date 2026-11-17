package com.katoaapps.minkboard.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.StatusId

/**
 * A spatial projection of the board for navigation. This deliberately renders dedicated
 * miniature columns instead of scaling the editable board and its controls into unusable targets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BoardMapScreen(
    board: BoardSnapshot,
    focusedStatusId: StatusId?,
    listState: LazyListState,
    onDismiss: () -> Unit,
    onSelectStatus: (BoardStatus) -> Unit,
) {
    val statuses = remember(board.statuses) { board.statuses.sortedBy { it.position } }
    val maxCardCount = remember(board.cards, statuses) {
        statuses.maxOfOrNull { status -> board.cardsIn(status.id).size } ?: 0
    }
    val verticalScrollState = rememberScrollState()
    BackHandler(onBack = onDismiss)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back_to_board),
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = board.board.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.board_map),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val columnWidth = boardMapColumnWidth(
                viewportWidth = maxWidth,
                viewportHeight = maxHeight,
                statusCount = statuses.size,
            )
            Column(modifier = Modifier.fillMaxSize()) {
                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val columnHeight = boardMapColumnHeight(maxHeight, maxCardCount)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(verticalScrollState),
                    ) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(columnHeight + BOARD_MAP_VERTICAL_PADDING * 2),
                            state = listState,
                            contentPadding = PaddingValues(
                                horizontal = 12.dp,
                                vertical = BOARD_MAP_VERTICAL_PADDING,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(
                                space = 8.dp,
                                alignment = Alignment.CenterHorizontally,
                            ),
                        ) {
                            items(statuses, key = { it.id.value }) { status ->
                                MiniatureStatusColumn(
                                    status = status,
                                    cards = board.cardsIn(status.id),
                                    isFocused = status.id == focusedStatusId,
                                    onClick = { onSelectStatus(status) },
                                    modifier = Modifier
                                        .width(columnWidth)
                                        .height(columnHeight),
                                )
                            }
                        }
                    }
                }
                StatusScrollDock(
                    state = listState,
                    statuses = statuses,
                    itemSpacing = 8.dp,
                    contentDescription = stringResource(R.string.board_position),
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun MiniatureStatusColumn(
    status: BoardStatus,
    cards: List<BoardCard>,
    isFocused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = Color(status.colorArgb)
    val accentContent = readableContentColor(accent)
    val openStatusDescription = stringResource(R.string.open_status, status.name)
    val countDescription = pluralStringResource(R.plurals.card_count, cards.size, cards.size)

    Surface(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = openStatusDescription },
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = if (isFocused) 0.20f else 0.12f),
        border = if (isFocused) BorderStroke(2.dp, accent) else null,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BOARD_MAP_HEADER_HEIGHT),
                shape = RoundedCornerShape(
                    topStart = 10.dp,
                    topEnd = 10.dp,
                    bottomEnd = 0.dp,
                    bottomStart = 0.dp,
                ),
                color = accent,
                contentColor = accentContent,
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = status.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = cards.size.toString(),
                        modifier = Modifier.semantics { contentDescription = countDescription },
                        style = MaterialTheme.typography.labelSmall,
                        color = accentContent.copy(alpha = 0.78f),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .clipToBounds(),
                verticalArrangement = Arrangement.spacedBy(BOARD_MAP_CARD_SPACING),
            ) {
                cards.forEach { card ->
                    MiniatureCard(
                        card = card,
                        accent = accent,
                        contentColor = accentContent,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniatureCard(
    card: BoardCard,
    accent: Color,
    contentColor: Color,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(BOARD_MAP_CARD_HEIGHT),
        shape = RoundedCornerShape(2.dp),
        color = accent.copy(alpha = 0.82f),
        contentColor = contentColor,
        border = if (card.isMajor) BorderStroke(1.dp, IMPORTANT_COLOR) else null,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (card.isMajor) {
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = IMPORTANT_COLOR,
                )
            }
            Text(
                text = card.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal fun boardMapVisibleColumnTarget(
    viewportWidthDp: Float,
    viewportHeightDp: Float = Float.POSITIVE_INFINITY,
): Int {
    val widthTarget = when {
        viewportWidthDp < 480f -> 4
        viewportWidthDp < 840f -> 6
        else -> 9
    }
    val heightTarget = if (viewportHeightDp < 360f) 5 else 9
    return minOf(widthTarget, heightTarget)
}

internal fun boardMapColumnWidth(
    viewportWidth: Dp,
    viewportHeight: Dp,
    statusCount: Int,
): Dp {
    val visibleTarget = boardMapVisibleColumnTarget(viewportWidth.value, viewportHeight.value)
    val visibleSlots = statusCount.coerceIn(1, visibleTarget)
    val surroundingPadding = 24f
    val interColumnSpacing = 8f * (visibleSlots - 1)
    val naturalWidth = (viewportWidth.value - surroundingPadding - interColumnSpacing) / visibleSlots
    return naturalWidth.coerceIn(72f, 176f).dp
}

internal fun boardMapColumnHeight(
    viewportHeight: Dp,
    maxCardCount: Int,
): Dp {
    val viewportColumnHeight = (viewportHeight - BOARD_MAP_VERTICAL_PADDING * 2)
        .coerceAtLeast(1.dp)
    val cardStackHeight = BOARD_MAP_CARD_HEIGHT * maxCardCount +
        BOARD_MAP_CARD_SPACING * (maxCardCount - 1).coerceAtLeast(0)
    val contentHeight = BOARD_MAP_HEADER_HEIGHT + BOARD_MAP_BODY_PADDING * 2 + cardStackHeight
    return maxOf(viewportColumnHeight, contentHeight)
}

private val BOARD_MAP_CARD_HEIGHT = 24.dp
private val BOARD_MAP_CARD_SPACING = 2.dp
private val BOARD_MAP_HEADER_HEIGHT = 36.dp
private val BOARD_MAP_BODY_PADDING = 4.dp
private val BOARD_MAP_VERTICAL_PADDING = 12.dp
