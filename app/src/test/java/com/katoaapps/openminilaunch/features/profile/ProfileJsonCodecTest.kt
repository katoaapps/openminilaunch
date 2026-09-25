package com.katoaapps.openminilaunch.features.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileJsonCodecTest {
    @Test fun encryptedPayloadCodecPreservesUserEnteredText() {
        val card = ProfileCard(
            fullName = "  张伟  ",
            organization = " Mink Labs ",
            note = "First line\n  indented second line",
            selectedLinkIds = listOf("instagram"),
        )
        val link = ProfileLink(
            id = "instagram",
            type = ProfileLinkType.INSTAGRAM,
            label = " Personal account ",
            value = " @openmink ",
        )

        val (restoredCard, restoredLinks) = ProfileJsonCodec.decode(
            ProfileJsonCodec.encode(card, listOf(link)),
        )

        assertEquals(card, restoredCard)
        assertEquals(link, restoredLinks.single())
    }
}
