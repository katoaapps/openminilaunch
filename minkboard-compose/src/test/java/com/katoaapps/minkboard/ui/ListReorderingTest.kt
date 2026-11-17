package com.katoaapps.minkboard.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListReorderingTest {
    @Test
    fun moveItemUsesImmediateDestinationIndex() {
        val items = mutableListOf("Ideas", "Doing", "Done")

        assertTrue(items.moveItem(0, 2))

        assertEquals(listOf("Doing", "Done", "Ideas"), items)
    }

    @Test
    fun invalidOrSamePositionMovesAreIgnored() {
        val items = mutableListOf("Ideas", "Doing", "Done")

        assertFalse(items.moveItem(-1, 1))
        assertFalse(items.moveItem(1, 3))
        assertFalse(items.moveItem(1, 1))
        assertEquals(listOf("Ideas", "Doing", "Done"), items)
    }
}
