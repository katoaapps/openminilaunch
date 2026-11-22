package org.sableos.games.console

import org.junit.Assert.assertEquals
import org.junit.Test

class ConsoleTabTest {
    private fun game(
        id: String,
        shelf: GameShelf,
    ) = GameManifest(id, id, "main.lua", "1.0", shelf, shelf == GameShelf.Motion, BestIs.High, "", "")

    @Test
    fun tabsFilterByShelfAndRecentSortsByLastPlayed() {
        val games = listOf(game("a", GameShelf.Motion), game("b", GameShelf.Keyboard), game("c", GameShelf.Puzzle))
        assertEquals(listOf("a"), ConsoleTab.Motion.pick(games) { 0 }.map { it.id })
        assertEquals(listOf("b"), ConsoleTab.Keyboard.pick(games) { 0 }.map { it.id })
        assertEquals(
            listOf("c", "a"),
            ConsoleTab.Recent.pick(games) { mapOf("a" to 5L, "c" to 9L)[it] ?: 0 }.map { it.id },
        )
        assertEquals(3, ConsoleTab.All.pick(games) { 0 }.size)
    }
}
