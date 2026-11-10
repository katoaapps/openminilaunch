package com.katoaapps.openminilaunch.ui.theme

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Test

class MinkFeatureColorsTest {
    @Test
    fun magicActionIdentityColorsRemainStable() {
        assertEquals(0xFF2563EB.toInt(), MinkFeatureColors.MagicText.toArgb())
        assertEquals(0xFF198754.toInt(), MinkFeatureColors.MagicCall.toArgb())
        assertEquals(0xFFD6A300.toInt(), MinkFeatureColors.MagicTodo.toArgb())
        assertEquals(0xFF7C3AED.toInt(), MinkFeatureColors.MagicNote.toArgb())
        assertEquals(0xFF8B5A2B.toInt(), MinkFeatureColors.MagicEvent.toArgb())
        assertEquals(0xFFC62828.toInt(), MinkFeatureColors.MagicApp.toArgb())
        assertEquals(0xFFAD4A25.toInt(), MinkFeatureColors.ConversationAttention.toArgb())
        assertEquals(0xFFC62828.toInt(), MinkFeatureColors.ConversationCount.toArgb())
    }

    @Test
    fun blockedAndProviderIdentityColorsRemainStable() {
        assertEquals(0xFFFF0000.toInt(), MinkFeatureColors.AppBlocked.toArgb())
        assertEquals(0xFF4A154B.toInt(), MinkFeatureColors.Slack.toArgb())
        assertEquals(0xFFC5221F.toInt(), MinkFeatureColors.Gmail.toArgb())
        assertEquals(0xFF1A73E8.toInt(), MinkFeatureColors.GoogleMessages.toArgb())
    }
}
