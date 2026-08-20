package com.katoaapps.openminilaunch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueryRoutingTest {
    @Test fun preservesExplicitWebUrl() =
        assertEquals("https://google.com/path?q=mink", normalizedWebUrl("https://google.com/path?q=mink"))

    @Test fun upgradesWwwAndBareDomainsToHttps() {
        assertEquals("https://www.google.com", normalizedWebUrl("www.google.com"))
        assertEquals("https://google.com/maps", normalizedWebUrl("google.com/maps"))
    }

    @Test fun plainQueriesAndUnsafeSchemesAreNotUrls() {
        assertNull(normalizedWebUrl("weather tomorrow"))
        assertNull(normalizedWebUrl("javascript:alert(1)"))
        assertNull(normalizedWebUrl("not-a-domain"))
    }
}
