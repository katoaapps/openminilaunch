package com.katoaapps.openminilaunch

import org.junit.Assert.assertEquals
import org.junit.Test

class MagicBoxInputTest {
    @Test
    fun plainTextBecomesSearchQuery() {
        val input = parseMagicBoxInput("  quarterly budget  ")

        assertEquals(' ', input.prefix)
        assertEquals("quarterly budget", input.plainQuery)
    }

    @Test
    fun commandPrefixProducesSuggestionTermWithoutPlainSearch() {
        val input = parseMagicBoxInput("?maps Home Depot")

        assertEquals('?', input.prefix)
        assertEquals("maps", input.searchTerm)
        assertEquals("", input.plainQuery)
    }

    @Test
    fun slashPrefixProducesNoteCommandWithoutPlainSearch() {
        val input = parseMagicBoxInput("/Draft the release announcement")

        assertEquals(MAGIC_NOTE_PREFIX, input.prefix)
        assertEquals("", input.plainQuery)
    }

    @Test
    fun dollarPrefixIsNowPlainSearchText() {
        val input = parseMagicBoxInput("\$quarterly revenue")

        assertEquals('$', input.prefix)
        assertEquals("\$quarterly revenue", input.plainQuery)
    }

    @Test
    fun selectedContactLocksCommandAndStopsContactFiltering() {
        val input = parseMagicBoxInput("message body", lockedPrefix = '@')

        assertEquals('@', input.prefix)
        assertEquals("", input.searchTerm)
        assertEquals("", input.plainQuery)
    }
}
