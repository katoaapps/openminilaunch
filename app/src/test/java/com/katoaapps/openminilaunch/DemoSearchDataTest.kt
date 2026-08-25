package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.features.demo.DemoSearchData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoSearchDataTest {
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
