package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.features.demo.DemoSearchData
import com.katoaapps.openminilaunch.features.demo.DemoHomeData
import com.katoaapps.openminilaunch.features.demo.DemoHomeProfile
import com.katoaapps.openminilaunch.model.Shortcut
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoSearchDataTest {
    @Test
    fun demoHomeProvidesFiveSafeTodosWithKaraCompleted() {
        val todos = DemoHomeData.todos()

        assertEquals(5, todos.size)
        assertEquals(todos.size, todos.map { it.id }.distinct().size)
        assertEquals(1, todos.count { it.completed })
        assertEquals("Call Kara after work", todos.single { it.completed }.text)
    }

    @Test
    fun everyDemoHomeProfileProvidesACompleteRepeatableScene() {
        DemoHomeProfile.entries.forEach { profile ->
            val todos = DemoHomeData.todos(profile)
            val shortcuts = DemoHomeData.shortcutOrder(profile)

            assertEquals(5, todos.size)
            assertEquals(todos.size, todos.map { it.id }.distinct().size)
            assertEquals(1, todos.count { it.completed })
            assertEquals(Shortcut.entries.toSet(), shortcuts.toSet())
            assertEquals(Shortcut.entries.size, shortcuts.size)
        }
    }

    @Test
    fun contactSearchProvidesTwoToFourNamedResultsForEveryLetter() {
        var totalResults = 0
        ('a'..'z').forEach { letter ->
            val results = DemoSearchData.searchContacts(letter.toString())
            assertTrue(results.size in 2..4)
            assertTrue(results.all { it.name.startsWith(letter, ignoreCase = true) })
            totalResults += results.size
        }
        assertEquals(78, totalResults)
    }

    @Test
    fun contactSearchGeneratesOnlyTheRequestedResultsAndStaysDeterministic() {
        val firstSearch = DemoSearchData.searchContacts("gab")
        val repeatedSearch = DemoSearchData.searchContacts("gab")

        assertEquals(1, firstSearch.size)
        assertEquals(firstSearch, repeatedSearch)
        assertTrue(firstSearch.all { it.name.startsWith("Gabriela") })
    }

    @Test
    fun contactNumbersStayInsideReservedFictionalRange() {
        val results = ('a'..'z').flatMap { DemoSearchData.searchContacts(it.toString()) }
        assertTrue(results.all { it.phone.startsWith("+1 202-555-01") })
    }
}
