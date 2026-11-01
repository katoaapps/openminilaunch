package com.katoaapps.openminilaunch.ui.conversations

import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteContactAvatarTest {
    @Test
    fun blankNameUsesQuestionMark() {
        assertEquals("?", contactInitials("   "))
    }

    @Test
    fun oneWordUsesAtMostTwoCharacters() {
        assertEquals("MA", contactInitials("May"))
        assertEquals("M", contactInitials("M"))
    }

    @Test
    fun multipleWordsUseFirstAndLastInitials() {
        assertEquals("MW", contactInitials("May Ling Wong"))
    }

    @Test
    fun surroundingAndRepeatedSpacesAreIgnored() {
        assertEquals("MW", contactInitials("  May   Wong  "))
    }
}
