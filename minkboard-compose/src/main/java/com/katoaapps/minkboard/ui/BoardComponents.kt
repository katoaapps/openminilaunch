package com.katoaapps.minkboard.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.zIndex
import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.CardPlacement
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.minkboard.core.CardDueState
import com.katoaapps.minkboard.core.dueState
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun BoardStatusPanel(
    status: BoardStatus,
    cards: List<BoardCard>,
    dragDropState: CardDragDropState,
    previousStatusId: StatusId?,
    previousStatusCardCount: Int,
    nextStatusId: StatusId?,
    nextStatusCardCount: Int,
    compactLayout: Boolean,
    focusedCardId: CardId?,
    statusDragHandle: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onAddCard: () -> Unit,
    onManageStatus: () -> Unit,
    onOpenCard: (BoardCard) -> Unit,
    revealCard: CardPlacement?,
    onRevealFinished: (CardId) -> Unit,
    onMoveCard: (CardId, StatusId, Int) -> Unit,
) {
    val accent = Color(status.colorArgb)
    val today by rememberCurrentDate()
    val cardCountDescription = pluralStringResource(R.plurals.card_count, cards.size, cards.size)
    val isDropTarget = dragDropState.isDropTarget(status.id)
    val moveUpLabel = stringResource(R.string.move_card_up)
    val moveDownLabel = stringResource(R.string.move_card_down)
    val moveToPreviousStatusLabel = stringResource(R.string.move_card_to_previous_status)
    val moveToNextStatusLabel = stringResource(R.string.move_card_to_next_status)
    val cardListState = rememberLazyListState()
    var highlightedCardId by remember(status.id) { mutableStateOf<CardId?>(null) }
    var cardListBounds by remember { mutableStateOf<Rect?>(null) }
    val edgeThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }
    val pointer = dragDropState.activeDrag?.pointerInRoot
    val verticalScrollIntensity = if (!isDropTarget || pointer == null || cardListBounds == null) {
        0f
    } else {
        edgeScrollIntensity(
            pointerPosition = pointer.y,
            viewportStart = checkNotNull(cardListBounds).top,
            viewportEnd = checkNotNull(cardListBounds).bottom,
            requestedThreshold = edgeThresholdPx,
        )
    }
    val currentVerticalScrollIntensity by rememberUpdatedState(verticalScrollIntensity)
    val maximumVerticalScrollPxPerSecond = with(LocalDensity.current) { 720.dp.toPx() }
    val activeVerticalScrollTarget = dragDropState.activeDrag
        ?.takeIf { it.targetStatusId == status.id }
        ?.let { it.card.id to status.id }
    val dropPreviewDrag = dragDropState.activeDrag?.takeIf { it.targetStatusId == status.id }
    val dropPreviewAnchorY = dragDropState.dropPreviewAnchorY(status.id)

    LaunchedEffect(
        revealCard?.cardId?.value,
        revealCard?.position,
        cards.map { it.id.value },
    ) {
        val target = revealCard ?: return@LaunchedEffect
        val targetIndex = cards.indexOfFirst { it.id == target.cardId }
        val expectedIndex = target.position.coerceIn(0, cards.lastIndex.coerceAtLeast(0))
        if (targetIndex != expectedIndex) return@LaunchedEffect
        cardListState.animateScrollToItem(targetIndex)
        repeat(3) {
            highlightedCardId = target.cardId
            delay(150)
            highlightedCardId = null
            delay(100)
        }
        onRevealFinished(target.cardId)
    }

    LaunchedEffect(activeVerticalScrollTarget) {
        if (activeVerticalScrollTarget == null) return@LaunchedEffect
        // Let the source or destination preview settle before interpreting a pointer near the top
        // or bottom as vertical-scroll intent. This prevents tiny initial movements from jumping.
        delay(180)
        var previousFrameNanos = withFrameNanos { it }
        var refreshTargetAfterLayout = false
        while (isActive && dragDropState.activeDrag != null) {
            val frameNanos = withFrameNanos { it }
            val elapsedSeconds = ((frameNanos - previousFrameNanos) / 1_000_000_000f)
                .coerceIn(0f, 0.05f)
            previousFrameNanos = frameNanos

            if (refreshTargetAfterLayout) {
                dragDropState.refreshDropTarget()
                refreshTargetAfterLayout = false
            }

            val intensity = currentVerticalScrollIntensity
            if (intensity != 0f) {
                // Squaring the penetration keeps the start of the edge zone deliberately calm.
                val easedIntensity = intensity * abs(intensity)
                val consumed = cardListState.scrollBy(
                    easedIntensity * maximumVerticalScrollPxPerSecond * elapsedSeconds,
                )
                refreshTargetAfterLayout = consumed != 0f
            }
        }
    }

    DisposableEffect(dragDropState, status.id) {
        onDispose { dragDropState.unregisterStatus(status.id) }
    }
    Surface(
        modifier = modifier.onGloballyPositioned { coordinates ->
            dragDropState.registerStatus(status.id, coordinates.boundsInRoot())
        },
        shape = RoundedCornerShape(if (compactLayout) 18.dp else 28.dp),
        color = accent.copy(alpha = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) 0.22f else 0.14f),
        border = BorderStroke(
            width = if (isDropTarget) 3.dp else 1.dp,
            color = accent.copy(alpha = if (isDropTarget) 1f else 0.65f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(if (compactLayout) 8.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactLayout) 6.dp else 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 4.dp else 8.dp),
            ) {
                statusDragHandle()
                Box(
                    Modifier
                        .weight(1f)
                        .then(
                            if (compactLayout) {
                                Modifier.clickable(onClick = onManageStatus)
                            } else {
                                Modifier
                            },
                        )
                        .background(accent, RoundedCornerShape(99.dp))
                        .padding(
                            horizontal = if (compactLayout) 8.dp else 12.dp,
                            vertical = if (compactLayout) 4.dp else 5.dp,
                        ),
                ) {
                    Text(
                        text = status.name,
                        color = readableContentColor(accent),
                        style = if (compactLayout) {
                            MaterialTheme.typography.labelLarge
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (status.isDoneStatus) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = stringResource(R.string.done_status),
                        modifier = Modifier.size(if (compactLayout) 16.dp else 24.dp),
                        tint = accent,
                    )
                }
                Text(
                    text = cards.size.toString(),
                    modifier = Modifier.semantics {
                        contentDescription = cardCountDescription
                    },
                    style = if (compactLayout) {
                        MaterialTheme.typography.labelSmall
                    } else {
                        MaterialTheme.typography.labelLarge
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!compactLayout) {
                    IconButton(onClick = onManageStatus) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.edit_status_named, status.name),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (cards.isEmpty() && dropPreviewDrag == null) {
                    Text(
                        text = stringResource(R.string.no_cards),
                        modifier = Modifier.padding(vertical = 28.dp, horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { cardListBounds = it.boundsInRoot() },
                        state = cardListState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(cards, key = { _, card -> card.id.value }) { index, card ->
                            BoardCardTile(
                                card = card,
                                statusColor = accent,
                                isCompleted = status.isDoneStatus,
                                today = today,
                                dragDropState = dragDropState,
                                compactLayout = compactLayout,
                                isRevealHighlighted = highlightedCardId == card.id,
                                isFocusRunning = focusedCardId == card.id,
                                accessibilityActions = buildList {
                                    if (index > 0) {
                                        add(CustomAccessibilityAction(moveUpLabel) {
                                            onMoveCard(card.id, status.id, index - 1)
                                            true
                                        })
                                    }
                                    if (index < cards.lastIndex) {
                                        add(CustomAccessibilityAction(moveDownLabel) {
                                            onMoveCard(card.id, status.id, index + 1)
                                            true
                                        })
                                    }
                                    previousStatusId?.let { destination ->
                                        add(CustomAccessibilityAction(moveToPreviousStatusLabel) {
                                            onMoveCard(card.id, destination, index.coerceAtMost(previousStatusCardCount))
                                            true
                                        })
                                    }
                                    nextStatusId?.let { destination ->
                                        add(CustomAccessibilityAction(moveToNextStatusLabel) {
                                            onMoveCard(card.id, destination, index.coerceAtMost(nextStatusCardCount))
                                            true
                                        })
                                    }
                                },
                                onClick = { onOpenCard(card) },
                            )
                        }
                    }
                }

                if (dropPreviewDrag != null && cardListBounds != null) {
                    val previewHeight = 64.dp
                    val previewHeightPx = with(LocalDensity.current) { previewHeight.toPx() }
                    val listBounds = checkNotNull(cardListBounds)
                    val anchorY = dropPreviewAnchorY ?: when (dropPreviewDrag.targetIndex) {
                        0 -> listBounds.top
                        else -> listBounds.bottom
                    }
                    val previewY = (anchorY - listBounds.top - previewHeightPx / 2f)
                        .coerceIn(
                            minimumValue = 0f,
                            maximumValue = (listBounds.height - previewHeightPx).coerceAtLeast(0f),
                        )
                    CardDropPreview(
                        card = dropPreviewDrag.card,
                        statusColor = accent,
                        modifier = Modifier
                            .absoluteOffset { IntOffset(0, previewY.roundToInt()) }
                            .zIndex(2f),
                    )
                }
            }

            OutlinedButton(
                onClick = onAddCard,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = if (compactLayout) {
                        stringResource(R.string.add_card)
                    } else {
                        null
                    },
                )
                if (!compactLayout) {
                    Text(
                        text = stringResource(R.string.add_card),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardCardTile(
    card: BoardCard,
    statusColor: Color,
    isCompleted: Boolean,
    today: LocalDate,
    dragDropState: CardDragDropState,
    compactLayout: Boolean,
    isRevealHighlighted: Boolean,
    isFocusRunning: Boolean,
    accessibilityActions: List<CustomAccessibilityAction>,
    onClick: () -> Unit,
) {
    val reorderCardDescription = stringResource(R.string.reorder_card, card.title)
    val dueState = card.dueState(today, isCompleted)
    val cardContentColor = readableContentColor(statusColor)
    val secondaryContentColor = cardContentColor.copy(alpha = 0.78f)
    val cardBorder = when {
        isRevealHighlighted -> BorderStroke(4.dp, cardContentColor)
        card.isMajor -> BorderStroke(2.dp, IMPORTANT_COLOR)
        dueState == CardDueState.Overdue -> BorderStroke(2.dp, MaterialTheme.colorScheme.error)
        else -> null
    }
    val completedSubtasks = card.subtasks.count { it.isCompleted }
    val subtaskProgressDescription = if (card.subtasks.isNotEmpty()) {
        pluralStringResource(
            R.plurals.subtask_progress,
            card.subtasks.size,
            completedSubtasks,
            card.subtasks.size,
        )
    } else {
        null
    }
    val hapticFeedback = LocalHapticFeedback.current
    var dragHandleCoordinates by remember(card.id) { mutableStateOf<LayoutCoordinates?>(null) }
    val focusDashPhase = if (isFocusRunning) {
        val focusTransition = rememberInfiniteTransition(label = "focused card border")
        val phase by focusTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "focused card dash phase",
        )
        phase
    } else {
        0f
    }

    DisposableEffect(dragDropState, card.id) {
        onDispose { dragDropState.unregisterCard(card.id) }
    }
    Card(
        onClick = onClick,
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                dragDropState.registerCard(card.id, coordinates.boundsInRoot())
            }
            .graphicsLayer { alpha = if (dragDropState.isDragging(card.id)) 0.28f else 1f }
            .drawWithContent {
                drawContent()
                if (isFocusRunning) {
                    val strokeWidth = 3.dp.toPx()
                    val inset = strokeWidth / 2f
                    val gap = 7.dp.toPx()
                    drawRoundRect(
                        color = cardContentColor,
                        topLeft = Offset(inset, inset),
                        size = Size(
                            width = size.width - strokeWidth,
                            height = size.height - strokeWidth,
                        ),
                        cornerRadius = CornerRadius(12.dp.toPx()),
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(
                                intervals = floatArrayOf(0.5f, gap),
                                phase = (gap + 0.5f) * focusDashPhase,
                            ),
                        ),
                    )
                }
            }
            .semantics { customActions = accessibilityActions },
        border = cardBorder,
        colors = CardDefaults.cardColors(
            containerColor = statusColor,
            contentColor = cardContentColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compactLayout) 8.dp else 14.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 4.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (card.isMajor || dueState == CardDueState.Overdue) {
                Column(
                    modifier = Modifier.align(Alignment.Top),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (card.isMajor) {
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = stringResource(R.string.important),
                            modifier = Modifier.size(if (compactLayout) 16.dp else 24.dp),
                            tint = IMPORTANT_COLOR,
                        )
                    }
                    if (dueState == CardDueState.Overdue) {
                        Icon(
                            imageVector = Icons.Outlined.EventBusy,
                            contentDescription = stringResource(R.string.overdue),
                            modifier = Modifier.size(if (compactLayout) 16.dp else 24.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (compactLayout) 4.dp else 8.dp),
            ) {
                Text(
                    text = card.title,
                    style = if (compactLayout) {
                        MaterialTheme.typography.labelLarge
                    } else {
                        MaterialTheme.typography.titleSmall
                    },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (compactLayout) 2 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!compactLayout && card.details.isNotBlank()) {
                    Text(
                        text = card.details,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = secondaryContentColor,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (card.dueDate != null || card.subtasks.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(
                            if (compactLayout) 4.dp else 12.dp,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        card.dueDate?.let { date ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(if (compactLayout) 14.dp else 24.dp),
                                    tint = if (dueState == CardDueState.Overdue) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        secondaryContentColor
                                    },
                                )
                                Text(
                                    text = if (dueState == CardDueState.Overdue) {
                                        stringResource(R.string.overdue_due_date, date.toString())
                                    } else {
                                        date.toString()
                                    },
                                    modifier = Modifier.padding(start = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (dueState == CardDueState.Overdue) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        secondaryContentColor
                                    },
                                    fontWeight = if (dueState == CardDueState.Overdue) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Normal
                                    },
                                )
                            }
                        }
                        if (card.subtasks.isNotEmpty()) {
                            Text(
                                text = "$completedSubtasks/${card.subtasks.size}",
                                modifier = Modifier.semantics {
                                    contentDescription = checkNotNull(subtaskProgressDescription)
                                },
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(if (compactLayout) 36.dp else 48.dp)
                    .onGloballyPositioned { dragHandleCoordinates = it }
                    .semantics { contentDescription = reorderCardDescription }
                    .pointerInput(card.id, dragDropState) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { localOffset ->
                                dragHandleCoordinates?.let { coordinates ->
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    dragDropState.startDrag(
                                        card = card,
                                        pointerInRoot = coordinates.localToRoot(localOffset),
                                    )
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                            },
                            // The board-level pointer owner completes or cancels the gesture. It
                            // remains composed when edge scrolling disposes this source card.
                            onDragEnd = {},
                            onDragCancel = {},
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = null,
                    modifier = Modifier.size(if (compactLayout) 18.dp else 24.dp),
                    tint = secondaryContentColor,
                )
            }
        }
    }
}
