package com.katoaapps.minkboard.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class BoardMapScreenTest {
    @Test
    fun visibleColumnTargetAdaptsAcrossCompactMediumAndExpandedWidths() {
        assertEquals(4, boardMapVisibleColumnTarget(360f))
        assertEquals(6, boardMapVisibleColumnTarget(600f))
        assertEquals(9, boardMapVisibleColumnTarget(1_000f))
        assertEquals(5, boardMapVisibleColumnTarget(800f, 300f))
        assertEquals(9, boardMapVisibleColumnTarget(1_000f, 700f))
    }

    @Test
    fun columnWidthFitsSmallBoardsWithoutMakingLargeBoardsUnreadable() {
        assertEquals(
            106.67f,
            boardMapColumnWidth(360.dp, 800.dp, statusCount = 3).value,
            0.01f,
        )
        assertEquals(78f, boardMapColumnWidth(360.dp, 800.dp, statusCount = 8).value, 0.01f)
        assertEquals(176.dp, boardMapColumnWidth(360.dp, 800.dp, statusCount = 1))
        assertEquals(148.8f, boardMapColumnWidth(800.dp, 300.dp, statusCount = 8).value, 0.01f)
    }

    @Test
    fun columnHeightLeavesFixedCardsAloneAndScrollsWhenNeeded() {
        assertEquals(336.dp, boardMapColumnHeight(360.dp, maxCardCount = 3))
        assertEquals(562.dp, boardMapColumnHeight(360.dp, maxCardCount = 20))
        assertEquals(776.dp, boardMapColumnHeight(800.dp, maxCardCount = 28))
    }
}
