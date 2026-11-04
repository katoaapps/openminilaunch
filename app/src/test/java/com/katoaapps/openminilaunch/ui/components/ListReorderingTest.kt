package com.katoaapps.openminilaunch.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListReorderingTest {
    @Test
    fun `moves an item forward and backward`() {
        val values = mutableListOf("a", "b", "c", "d")

        assertTrue(values.moveItem(fromIndex = 0, toIndex = 2))
        assertEquals(listOf("b", "c", "a", "d"), values)

        assertTrue(values.moveItem(fromIndex = 3, toIndex = 1))
        assertEquals(listOf("b", "d", "c", "a"), values)
    }

    @Test
    fun `rejects invalid and no-op moves without changing the list`() {
        val values = mutableListOf("a", "b")

        assertFalse(values.moveItem(fromIndex = -1, toIndex = 0))
        assertFalse(values.moveItem(fromIndex = 0, toIndex = 2))
        assertFalse(values.moveItem(fromIndex = 1, toIndex = 1))
        assertEquals(listOf("a", "b"), values)
    }
}
