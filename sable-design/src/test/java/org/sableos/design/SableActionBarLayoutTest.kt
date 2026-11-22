package org.sableos.design

import org.junit.Assert.assertEquals
import org.junit.Test

class SableActionBarLayoutTest {
    @Test
    fun actionsThatFitAllShow() {
        assertEquals(listOf("a", "b") to emptyList<String>(), SableActionBarLayout.split(listOf("a", "b")))
        assertEquals(emptyList<String>() to emptyList<String>(), SableActionBarLayout.split(emptyList<String>()))
    }

    @Test
    fun theOverflowTakesTheLastSlot() {
        assertEquals(listOf("a") to listOf("b", "c"), SableActionBarLayout.split(listOf("a", "b", "c")))
        assertEquals(emptyList<String>() to listOf("a", "b"), SableActionBarLayout.split(listOf("a", "b"), inline = 1))
    }
}
