package com.katoaapps.openminilaunch.features.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileVCardTest {
    @Test fun createsCompleteEscapedUtf8VCardInSelectedOrder() {
        val email = ProfileLink("email", ProfileLinkType.EMAIL, "Work", "mink@example.com")
        val site = ProfileLink("site", ProfileLinkType.WEBSITE, "Portfolio", "example.com/a,b")
        val card = ProfileCard(
            fullName = "José Mink",
            organization = "Katoa; Apps",
            note = "First line\nSecond line",
            selectedLinkIds = listOf(site.id, email.id),
            hasPortrait = true,
        )

        val result = ProfileVCard.create(card, listOf(email, site))

        assertTrue(result.startsWith("BEGIN:VCARD\r\nVERSION:3.0\r\n"))
        assertTrue(result.endsWith("END:VCARD\r\n"))
        assertTrue(result.contains("FN:José Mink"))
        assertTrue(result.contains("ORG:Katoa\\; Apps"))
        assertTrue(result.contains("NOTE:First line\\nSecond line"))
        assertTrue(result.contains("EMAIL;TYPE=INTERNET:mink@example.com"))
        assertTrue(result.contains("URL:https://example.com/a\\,b"))
        assertFalse(result.contains("item1."))
        assertTrue(result.indexOf("example.com") < result.indexOf("mink@example.com"))
        assertFalse(result.contains("PHOTO"))
    }

    @Test fun emitsSamsungCompatibleUngroupedPhoneAndWebsiteProperties() {
        val phone = ProfileLink("phone", ProfileLinkType.PHONE, "Mobile", "+1 (555) 010-0200")
        val site = ProfileLink("site", ProfileLinkType.WEBSITE, "Website", "minklauncher.com")
        val result = ProfileVCard.create(
            ProfileCard(fullName = "Mink Launcher", selectedLinkIds = listOf(phone.id, site.id)),
            listOf(phone, site),
        )

        assertTrue(result.contains("TEL;TYPE=CELL:+15550100200\r\n"))
        assertTrue(result.contains("URL:https://minklauncher.com\r\n"))
        assertFalse(result.contains("item1.TEL"))
        assertFalse(result.contains("X-ABLabel"))
    }

    @Test fun normalizesProviderHandlesWithoutChangingSavedValue() {
        val link = ProfileLink("id", ProfileLinkType.INSTAGRAM, "Instagram", "@openmink")

        assertEquals("https://instagram.com/openmink", ProfileVCard.normalizedValue(link))
        assertEquals("@openmink", link.value)
    }

    @Test fun signalRequiresCompleteSignalLink() {
        assertFalse(ProfileVCard.isValid(ProfileLink("1", ProfileLinkType.SIGNAL, "Signal", "mink")))
        assertTrue(
            ProfileVCard.isValid(
                ProfileLink("1", ProfileLinkType.SIGNAL, "Signal", "https://signal.me/#eu/test"),
            ),
        )
    }

    @Test fun foldedUnicodeLinesStayWithinVCardByteLimit() {
        val result = ProfileVCard.create(ProfileCard(fullName = "张".repeat(80)), emptyList())

        result.lineSequence().filter(String::isNotEmpty).forEach { line ->
            assertTrue(line.toByteArray(Charsets.UTF_8).size <= 75)
        }
    }
}
