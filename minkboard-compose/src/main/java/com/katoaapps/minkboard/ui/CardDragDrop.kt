package com.katoaapps.minkboard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.StatusId

/**
 * Owns only the short-lived presentation state for card dragging. Room remains the source of
 * truth; one exact move is persisted when a drag finishes, never for each pointer update.
 */
@Stable
internal class CardDragDropState {
    private var canonicalOrders: Map<StatusId, List<BoardCard>> = emptyMap()
    private var canonicalSignature: List<CardLocation> = emptyList()
    private var pendingMoveBaseSignature: List<CardLocation>? = null
    private val statusBounds = mutableMapOf<StatusId, Rect>()
    private val cardBounds = mutableMapOf<CardId, Rect>()

    var cardOrders: Map<StatusId, List<BoardCard>> by mutableStateOf(emptyMap())
        private set

    var activeDrag: ActiveCardDrag? by mutableStateOf(null)
        private set

    fun sync(statuses: List<BoardStatus>, cards: List<BoardCard>) {
        val newOrders = statuses.sortedBy { it.position }.associate { status ->
            status.id to cards.filter { it.statusId == status.id }.sortedBy { it.position }
        }
        val newSignature = newOrders.toCardLocations()
        val pendingBase = pendingMoveBaseSignature

        canonicalOrders = newOrders
        canonicalSignature = newSignature
        when {
            activeDrag != null -> Unit
            pendingBase == null -> cardOrders = newOrders
            newSignature != pendingBase -> {
                pendingMoveBaseSignature = null
                cardOrders = newOrders
            }
        }
    }

    fun resetToCanonical() {
        activeDrag = null
        pendingMoveBaseSignature = null
        cardOrders = canonicalOrders
    }

    fun registerStatus(statusId: StatusId, bounds: Rect) {
        statusBounds[statusId] = bounds
    }

    fun unregisterStatus(statusId: StatusId) {
        statusBounds.remove(statusId)
    }

    fun registerCard(cardId: CardId, bounds: Rect) {
        cardBounds[cardId] = bounds
    }

    fun unregisterCard(cardId: CardId) {
        cardBounds.remove(cardId)
    }

    fun startDrag(card: BoardCard, pointerInRoot: Offset) {
        if (pendingMoveBaseSignature != null) return
        val sourceCards = cardOrders[card.statusId].orEmpty()
        val sourceIndex = sourceCards.indexOfFirst { it.id == card.id }
        if (sourceIndex < 0) return

        activeDrag = ActiveCardDrag(
            card = card,
            sourceStatusId = card.statusId,
            sourceIndex = sourceIndex,
            pointerInRoot = pointerInRoot,
            targetStatusId = card.statusId,
            targetIndex = sourceIndex,
        )
        refreshTarget()
    }

    fun dragBy(delta: Offset) {
        val drag = activeDrag ?: return
        movePointerTo(drag.pointerInRoot + delta)
    }

    fun movePointerTo(pointerInRoot: Offset) {
        val drag = activeDrag ?: return
        activeDrag = drag.copy(pointerInRoot = pointerInRoot)
        refreshTarget()
    }

    /** Re-evaluates placement after scrolling has moved card bounds under a stationary pointer. */
    fun refreshDropTarget() {
        refreshTarget()
    }

    /** Root-space boundary where the non-mutating destination preview should be drawn. */
    fun dropPreviewAnchorY(statusId: StatusId): Float? {
        val drag = activeDrag?.takeIf { it.targetStatusId == statusId } ?: return null
        val targetIndex = drag.targetIndex ?: return null
        val candidateIds = cardOrders[statusId]
            .orEmpty()
            .map(BoardCard::id)
            .filterNot { it == drag.card.id }
        candidateIds.getOrNull(targetIndex)?.let { nextId ->
            cardBounds[nextId]?.let { return it.top }
        }
        candidateIds.getOrNull(targetIndex - 1)?.let { previousId ->
            cardBounds[previousId]?.let { return it.bottom }
        }
        return null
    }

    fun finishDrag(onMoveCard: (CardId, StatusId, Int) -> Unit) {
        val drag = activeDrag ?: return
        val destination = drag.targetStatusId
        val destinationIndex = drag.targetIndex
        if (destination == null || destinationIndex == null) {
            resetToCanonical()
            return
        }

        val hasMoved = destination != drag.sourceStatusId || destinationIndex != drag.sourceIndex
        activeDrag = null
        if (!hasMoved) {
            cardOrders = canonicalOrders
            return
        }

        pendingMoveBaseSignature = canonicalSignature
        cardOrders = cardOrders.moveCard(drag.card.id, destination, destinationIndex)
        onMoveCard(drag.card.id, destination, destinationIndex)
    }

    fun cancelDrag() {
        resetToCanonical()
    }

    fun isDragging(cardId: CardId): Boolean = activeDrag?.card?.id == cardId

    fun isDropTarget(statusId: StatusId): Boolean = activeDrag?.targetStatusId == statusId

    private fun refreshTarget() {
        val drag = activeDrag ?: return
        val targetStatusId = statusBounds.entries
            .firstOrNull { (_, bounds) -> bounds.contains(drag.pointerInRoot) }
            ?.key

        if (targetStatusId == null) {
            activeDrag = drag.copy(targetStatusId = null, targetIndex = null)
            return
        }

        val targetCards = cardOrders[targetStatusId].orEmpty()
        val targetIndex = insertionIndex(
            orderedCardIds = targetCards.map(BoardCard::id),
            visibleBounds = cardBounds,
            pointerY = drag.pointerInRoot.y,
            excludedCardId = drag.card.id,
        )
        activeDrag = drag.copy(targetStatusId = targetStatusId, targetIndex = targetIndex)
    }
}

internal data class ActiveCardDrag(
    val card: BoardCard,
    val sourceStatusId: StatusId,
    val sourceIndex: Int,
    val pointerInRoot: Offset,
    val targetStatusId: StatusId?,
    val targetIndex: Int?,
)

private data class CardLocation(
    val cardId: CardId,
    val statusId: StatusId,
    val position: Int,
)

private fun Map<StatusId, List<BoardCard>>.toCardLocations(): List<CardLocation> =
    entries.flatMap { (statusId, cards) ->
        cards.mapIndexed { index, card -> CardLocation(card.id, statusId, index) }
    }

internal fun Map<StatusId, List<BoardCard>>.moveCard(
    cardId: CardId,
    destinationStatusId: StatusId,
    destinationIndex: Int,
): Map<StatusId, List<BoardCard>> {
    val movedCard = values.flatten().firstOrNull { it.id == cardId } ?: return this
    val withoutCard = mapValues { (_, cards) -> cards.filterNot { it.id == cardId } }.toMutableMap()
    val destination = withoutCard[destinationStatusId].orEmpty().toMutableList()
    destination.add(destinationIndex.coerceIn(0, destination.size), movedCard)
    withoutCard[destinationStatusId] = destination
    return withoutCard
}

/** Returns an insertion position in the full status order using the currently visible cards. */
internal fun insertionIndex(
    orderedCardIds: List<CardId>,
    visibleBounds: Map<CardId, Rect>,
    pointerY: Float,
    excludedCardId: CardId? = null,
): Int {
    val availableIds = orderedCardIds.filterNot { it == excludedCardId }
    val visible = availableIds.mapIndexedNotNull { index, cardId ->
        visibleBounds[cardId]?.let { bounds -> index to bounds.center.y }
    }
    if (visible.isEmpty()) return 0
    return visible.firstOrNull { (_, centerY) -> pointerY < centerY }?.first
        ?: (visible.last().first + 1).coerceAtMost(availableIds.size)
}

/** Returns the direction a dragged pointer should scroll near a bounded viewport edge. */
internal fun edgeScrollDirection(
    pointerPosition: Float,
    viewportStart: Float,
    viewportEnd: Float,
    requestedThreshold: Float,
): Int {
    if (viewportEnd <= viewportStart || requestedThreshold <= 0f) return 0
    val threshold = minOf(requestedThreshold, (viewportEnd - viewportStart) / 2f)
    return when {
        pointerPosition < viewportStart + threshold -> -1
        pointerPosition > viewportEnd - threshold -> 1
        else -> 0
    }
}

/**
 * Returns a signed edge penetration from -1f at the leading edge to 1f at the trailing edge.
 * A pointer that only barely enters an edge zone therefore produces only a gentle scroll.
 */
internal fun edgeScrollIntensity(
    pointerPosition: Float,
    viewportStart: Float,
    viewportEnd: Float,
    requestedThreshold: Float,
): Float {
    if (viewportEnd <= viewportStart || requestedThreshold <= 0f) return 0f
    val threshold = minOf(requestedThreshold, (viewportEnd - viewportStart) / 2f)
    val leadingBoundary = viewportStart + threshold
    val trailingBoundary = viewportEnd - threshold
    return when {
        pointerPosition < leadingBoundary ->
            -((leadingBoundary - pointerPosition) / threshold).coerceIn(0f, 1f)
        pointerPosition > trailingBoundary ->
            ((pointerPosition - trailingBoundary) / threshold).coerceIn(0f, 1f)
        else -> 0f
    }
}

@Composable
internal fun CardDragPreview(
    card: BoardCard,
    statusColor: Color,
    modifier: Modifier = Modifier,
) {
    val contentColor = readableContentColor(statusColor)
    Surface(
        modifier = modifier.width(240.dp),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 12.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.28f)),
        color = statusColor,
        contentColor = contentColor,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = card.title,
                modifier = Modifier.padding(end = 40.dp),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
            )
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterEnd),
                tint = contentColor.copy(alpha = 0.78f),
            )
        }
    }
}

@Composable
internal fun CardDropPreview(
    card: BoardCard,
    statusColor: Color,
    modifier: Modifier = Modifier,
) {
    val contentColor = readableContentColor(statusColor)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, contentColor.copy(alpha = 0.58f)),
        color = statusColor.copy(alpha = 0.82f),
        contentColor = contentColor,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = card.title,
                modifier = Modifier.padding(end = 40.dp),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
            )
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterEnd),
                tint = contentColor.copy(alpha = 0.78f),
            )
        }
    }
}
