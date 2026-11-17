package com.katoaapps.minkboard.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.StatusId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardDragDropTest {
    private val firstStatus = StatusId("first")
    private val secondStatus = StatusId("second")

    @Test
    fun moveCardReordersWithinStatusWithoutChangingOtherStatuses() {
        val first = card("first-card", firstStatus, 0)
        val second = card("second-card", firstStatus, 1)
        val other = card("other-card", secondStatus, 0)
        val orders = mapOf(firstStatus to listOf(first, second), secondStatus to listOf(other))

        val moved = orders.moveCard(first.id, firstStatus, 1)

        assertEquals(listOf(second.id, first.id), moved.getValue(firstStatus).map(BoardCard::id))
        assertEquals(listOf(other.id), moved.getValue(secondStatus).map(BoardCard::id))
    }

    @Test
    fun moveCardInsertsAtExactCrossStatusPosition() {
        val movedCard = card("moving", firstStatus, 0)
        val firstTarget = card("target-one", secondStatus, 0)
        val secondTarget = card("target-two", secondStatus, 1)
        val orders = mapOf(
            firstStatus to listOf(movedCard),
            secondStatus to listOf(firstTarget, secondTarget),
        )

        val moved = orders.moveCard(movedCard.id, secondStatus, 1)

        assertEquals(emptyList<CardId>(), moved.getValue(firstStatus).map(BoardCard::id))
        assertEquals(
            listOf(firstTarget.id, movedCard.id, secondTarget.id),
            moved.getValue(secondStatus).map(BoardCard::id),
        )
    }

    @Test
    fun insertionIndexPreservesFullOrderAroundVisibleCards() {
        val ids = listOf("a", "b", "c", "d").map(::CardId)
        val visibleBounds = mapOf(
            ids[1] to Rect(0f, 80f, 100f, 120f),
            ids[2] to Rect(0f, 180f, 100f, 220f),
        )

        assertEquals(1, insertionIndex(ids, visibleBounds, pointerY = 40f))
        assertEquals(2, insertionIndex(ids, visibleBounds, pointerY = 150f))
        assertEquals(3, insertionIndex(ids, visibleBounds, pointerY = 260f))
    }

    @Test
    fun insertionIndexIgnoresDraggedCard() {
        val ids = listOf("a", "b", "c").map(::CardId)
        val visibleBounds = mapOf(
            ids[0] to Rect(0f, 0f, 100f, 40f),
            ids[1] to Rect(0f, 50f, 100f, 90f),
            ids[2] to Rect(0f, 100f, 100f, 140f),
        )

        assertEquals(
            1,
            insertionIndex(ids, visibleBounds, pointerY = 95f, excludedCardId = ids[1]),
        )
    }

    @Test
    fun edgeScrollDirectionRecognizesBothEdgesAndRestingCenter() {
        assertEquals(-1, edgeScrollDirection(20f, 0f, 400f, 64f))
        assertEquals(0, edgeScrollDirection(200f, 0f, 400f, 64f))
        assertEquals(1, edgeScrollDirection(380f, 0f, 400f, 64f))
    }

    @Test
    fun edgeScrollDirectionClampsThresholdForCompactViewports() {
        assertEquals(-1, edgeScrollDirection(10f, 0f, 80f, 64f))
        assertEquals(0, edgeScrollDirection(40f, 0f, 80f, 64f))
        assertEquals(1, edgeScrollDirection(70f, 0f, 80f, 64f))
        assertEquals(0, edgeScrollDirection(10f, 20f, 20f, 64f))
    }

    @Test
    fun edgeScrollIntensityRampsWithDistanceIntoEachEdge() {
        assertEquals(-1f, edgeScrollIntensity(0f, 0f, 400f, 64f), 0.001f)
        assertEquals(-0.5f, edgeScrollIntensity(32f, 0f, 400f, 64f), 0.001f)
        assertEquals(0f, edgeScrollIntensity(64f, 0f, 400f, 64f), 0.001f)
        assertEquals(0f, edgeScrollIntensity(336f, 0f, 400f, 64f), 0.001f)
        assertEquals(0.5f, edgeScrollIntensity(368f, 0f, 400f, 64f), 0.001f)
        assertEquals(1f, edgeScrollIntensity(400f, 0f, 400f, 64f), 0.001f)
    }

    @Test
    fun edgeScrollIntensityClampsForCompactAndInvalidViewports() {
        assertEquals(-0.5f, edgeScrollIntensity(20f, 0f, 80f, 64f), 0.001f)
        assertEquals(0f, edgeScrollIntensity(40f, 0f, 80f, 64f), 0.001f)
        assertEquals(0.5f, edgeScrollIntensity(60f, 0f, 80f, 64f), 0.001f)
        assertEquals(0f, edgeScrollIntensity(10f, 20f, 20f, 64f), 0.001f)
    }

    @Test
    fun reportingNewCardBoundsDoesNotMutateOrderDuringDrag() {
        val first = card("a", firstStatus, 0)
        val second = card("b", firstStatus, 1)
        val state = CardDragDropState().apply {
            sync(listOf(status(firstStatus, 0)), listOf(first, second))
            registerStatus(firstStatus, Rect(0f, 0f, 100f, 200f))
            registerCard(first.id, Rect(0f, 0f, 100f, 40f))
            registerCard(second.id, Rect(0f, 50f, 100f, 90f))
        }

        state.startDrag(first, Offset(50f, 20f))
        state.registerCard(second.id, Rect(0f, 0f, 100f, 10f))

        assertEquals(listOf(first.id, second.id), state.cardOrders.getValue(firstStatus).map { it.id })

        state.refreshDropTarget()

        assertEquals(listOf(first.id, second.id), state.cardOrders.getValue(firstStatus).map { it.id })
        assertEquals(1, state.activeDrag?.targetIndex)
    }

    @Test
    fun dragTracksWithinStatusPreviewAndPersistsOnlyWhenFinished() {
        val first = card("a", firstStatus, 0)
        val second = card("b", firstStatus, 1)
        val state = CardDragDropState().apply {
            sync(listOf(status(firstStatus, 0)), listOf(first, second))
            registerStatus(firstStatus, Rect(0f, 0f, 100f, 200f))
            registerCard(first.id, Rect(0f, 0f, 100f, 40f))
            registerCard(second.id, Rect(0f, 50f, 100f, 90f))
        }
        var persisted: Triple<CardId, StatusId, Int>? = null

        state.startDrag(first, Offset(50f, 20f))
        state.dragBy(Offset(0f, 70f))

        assertEquals(listOf(first.id, second.id), state.cardOrders.getValue(firstStatus).map(BoardCard::id))
        assertEquals(1, state.activeDrag?.targetIndex)
        assertEquals(null, persisted)

        state.finishDrag { cardId, statusId, position ->
            persisted = Triple(cardId, statusId, position)
        }
        assertEquals(Triple(first.id, firstStatus, 1), persisted)
        assertEquals(listOf(second.id, first.id), state.cardOrders.getValue(firstStatus).map(BoardCard::id))
    }

    @Test
    fun dropPreviewAnchorTracksCalculatedBoundaryWithoutMovingCards() {
        val moving = card("moving", firstStatus, 0)
        val firstTarget = card("target-one", secondStatus, 0)
        val secondTarget = card("target-two", secondStatus, 1)
        val state = CardDragDropState().apply {
            sync(
                listOf(status(firstStatus, 0), status(secondStatus, 1)),
                listOf(moving, firstTarget, secondTarget),
            )
            registerStatus(firstStatus, Rect(0f, 0f, 100f, 200f))
            registerStatus(secondStatus, Rect(110f, 0f, 210f, 200f))
            registerCard(moving.id, Rect(0f, 0f, 100f, 40f))
            registerCard(firstTarget.id, Rect(110f, 20f, 210f, 60f))
            registerCard(secondTarget.id, Rect(110f, 80f, 210f, 120f))
        }

        state.startDrag(moving, Offset(50f, 20f))
        state.movePointerTo(Offset(150f, 70f))

        assertEquals(80f, state.dropPreviewAnchorY(secondStatus))
        assertEquals(listOf(moving.id), state.cardOrders.getValue(firstStatus).map(BoardCard::id))
        assertEquals(
            listOf(firstTarget.id, secondTarget.id),
            state.cardOrders.getValue(secondStatus).map(BoardCard::id),
        )
    }

    @Test
    fun dragDropsIntoExactVisiblePositionInAnotherStatus() {
        val moving = card("moving", firstStatus, 0)
        val firstTarget = card("target-one", secondStatus, 0)
        val secondTarget = card("target-two", secondStatus, 1)
        val state = CardDragDropState().apply {
            sync(
                listOf(status(firstStatus, 0), status(secondStatus, 1)),
                listOf(moving, firstTarget, secondTarget),
            )
            registerStatus(firstStatus, Rect(0f, 0f, 100f, 200f))
            registerStatus(secondStatus, Rect(110f, 0f, 210f, 200f))
            registerCard(moving.id, Rect(0f, 0f, 100f, 40f))
            registerCard(firstTarget.id, Rect(110f, 0f, 210f, 40f))
            registerCard(secondTarget.id, Rect(110f, 50f, 210f, 90f))
        }
        var persisted: Triple<CardId, StatusId, Int>? = null

        state.startDrag(moving, Offset(50f, 20f))
        state.dragBy(Offset(100f, 45f))

        assertEquals(listOf(moving.id), state.cardOrders.getValue(firstStatus).map(BoardCard::id))
        assertEquals(
            listOf(firstTarget.id, secondTarget.id),
            state.cardOrders.getValue(secondStatus).map(BoardCard::id),
        )
        assertEquals(secondStatus, state.activeDrag?.targetStatusId)
        assertEquals(1, state.activeDrag?.targetIndex)
        assertEquals(null, persisted)

        state.finishDrag { cardId, statusId, position ->
            persisted = Triple(cardId, statusId, position)
        }

        assertEquals(Triple(moving.id, secondStatus, 1), persisted)
        assertEquals(
            listOf(firstTarget.id, moving.id, secondTarget.id),
            state.cardOrders.getValue(secondStatus).map(BoardCard::id),
        )
    }

    @Test
    fun dragDropsIntoEmptyVisibleStatus() {
        val moving = card("moving", firstStatus, 0)
        val state = CardDragDropState().apply {
            sync(listOf(status(firstStatus, 0), status(secondStatus, 1)), listOf(moving))
            registerStatus(firstStatus, Rect(0f, 0f, 100f, 200f))
            registerStatus(secondStatus, Rect(110f, 0f, 210f, 200f))
            registerCard(moving.id, Rect(0f, 0f, 100f, 40f))
        }
        var persisted: Triple<CardId, StatusId, Int>? = null

        state.startDrag(moving, Offset(50f, 20f))
        state.dragBy(Offset(100f, 30f))

        assertEquals(listOf(moving.id), state.cardOrders.getValue(firstStatus).map(BoardCard::id))
        assertEquals(emptyList<CardId>(), state.cardOrders.getValue(secondStatus).map(BoardCard::id))
        assertEquals(secondStatus, state.activeDrag?.targetStatusId)
        assertEquals(0, state.activeDrag?.targetIndex)
        assertEquals(null, persisted)

        state.finishDrag { cardId, statusId, position ->
            persisted = Triple(cardId, statusId, position)
        }

        assertEquals(Triple(moving.id, secondStatus, 0), persisted)
        assertEquals(listOf(moving.id), state.cardOrders.getValue(secondStatus).map(BoardCard::id))
    }

    @Test
    fun cancellingDragRestoresCanonicalOrderWithoutPersisting() {
        val first = card("a", firstStatus, 0)
        val second = card("b", firstStatus, 1)
        val state = CardDragDropState().apply {
            sync(listOf(status(firstStatus, 0)), listOf(first, second))
            registerStatus(firstStatus, Rect(0f, 0f, 100f, 200f))
            registerCard(first.id, Rect(0f, 0f, 100f, 40f))
            registerCard(second.id, Rect(0f, 50f, 100f, 90f))
        }

        state.startDrag(first, Offset(50f, 20f))
        state.dragBy(Offset(0f, 70f))
        assertEquals(listOf(first.id, second.id), state.cardOrders.getValue(firstStatus).map { it.id })
        assertEquals(1, state.activeDrag?.targetIndex)

        state.cancelDrag()

        assertEquals(listOf(first.id, second.id), state.cardOrders.getValue(firstStatus).map { it.id })
        assertNull(state.activeDrag)
    }

    @Test
    fun cancellingCrossStatusPreviewRestoresBothCanonicalColumns() {
        val moving = card("moving", firstStatus, 0)
        val target = card("target", secondStatus, 0)
        val state = CardDragDropState().apply {
            sync(
                listOf(status(firstStatus, 0), status(secondStatus, 1)),
                listOf(moving, target),
            )
            registerStatus(firstStatus, Rect(0f, 0f, 100f, 200f))
            registerStatus(secondStatus, Rect(110f, 0f, 210f, 200f))
            registerCard(moving.id, Rect(0f, 0f, 100f, 40f))
            registerCard(target.id, Rect(110f, 0f, 210f, 40f))
        }

        state.startDrag(moving, Offset(50f, 20f))
        state.dragBy(Offset(100f, 30f))
        assertEquals(listOf(moving.id), state.cardOrders.getValue(firstStatus).map(BoardCard::id))
        assertEquals(
            listOf(target.id),
            state.cardOrders.getValue(secondStatus).map(BoardCard::id),
        )
        assertEquals(secondStatus, state.activeDrag?.targetStatusId)
        assertEquals(1, state.activeDrag?.targetIndex)

        state.cancelDrag()

        assertEquals(listOf(moving.id), state.cardOrders.getValue(firstStatus).map(BoardCard::id))
        assertEquals(listOf(target.id), state.cardOrders.getValue(secondStatus).map(BoardCard::id))
        assertNull(state.activeDrag)
    }

    @Test
    fun releasingOutsideVisibleStatusesRollsBackAndDoesNotPersist() {
        val first = card("a", firstStatus, 0)
        val second = card("b", firstStatus, 1)
        val state = CardDragDropState().apply {
            sync(listOf(status(firstStatus, 0)), listOf(first, second))
            registerStatus(firstStatus, Rect(0f, 0f, 100f, 200f))
            registerCard(first.id, Rect(0f, 0f, 100f, 40f))
            registerCard(second.id, Rect(0f, 50f, 100f, 90f))
        }
        var persisted: Triple<CardId, StatusId, Int>? = null

        state.startDrag(first, Offset(50f, 20f))
        state.dragBy(Offset(200f, 0f))
        state.finishDrag { cardId, statusId, position ->
            persisted = Triple(cardId, statusId, position)
        }

        assertNull(persisted)
        assertEquals(listOf(first.id, second.id), state.cardOrders.getValue(firstStatus).map { it.id })
    }

    @Test
    fun resetAfterPersistenceErrorRestoresCanonicalOrder() {
        val first = card("a", firstStatus, 0)
        val second = card("b", firstStatus, 1)
        val state = CardDragDropState().apply {
            sync(listOf(status(firstStatus, 0)), listOf(first, second))
            registerStatus(firstStatus, Rect(0f, 0f, 100f, 200f))
            registerCard(first.id, Rect(0f, 0f, 100f, 40f))
            registerCard(second.id, Rect(0f, 50f, 100f, 90f))
        }

        state.startDrag(first, Offset(50f, 20f))
        state.dragBy(Offset(0f, 70f))
        state.finishDrag { _, _, _ -> Unit }
        assertEquals(listOf(second.id, first.id), state.cardOrders.getValue(firstStatus).map { it.id })

        state.resetToCanonical()

        assertEquals(listOf(first.id, second.id), state.cardOrders.getValue(firstStatus).map { it.id })
    }

    private fun card(id: String, statusId: StatusId, position: Int) = BoardCard(
        id = CardId(id),
        statusId = statusId,
        title = id,
        details = "",
        dueDate = null,
        isMajor = false,
        position = position,
        createdAtEpochMillis = 0L,
        updatedAtEpochMillis = 0L,
    )

    private fun status(id: StatusId, position: Int) = BoardStatus(
        id = id,
        boardId = BoardId("board"),
        name = id.value,
        colorArgb = 0,
        isDoneStatus = false,
        position = position,
    )
}
