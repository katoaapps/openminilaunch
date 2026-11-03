package com.katoaapps.openminilaunch.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppGridSlotOrderTest {
    @Test
    fun `quick drawer is always normalized to the final slot`() {
        val restored = normalizedAppGridOrder(
            listOf(AppGridSlot.DRAWER, AppGridSlot.MESSAGE, AppGridSlot.NOTE),
        )

        assertEquals(AppGridSlot.DRAWER, restored.last())
        assertEquals(AppGridSlot.entries.size, restored.size)
        assertEquals(AppGridSlot.entries.toSet(), restored.toSet())
        assertEquals(listOf(AppGridSlot.MESSAGE, AppGridSlot.NOTE), restored.take(2))
    }
}
