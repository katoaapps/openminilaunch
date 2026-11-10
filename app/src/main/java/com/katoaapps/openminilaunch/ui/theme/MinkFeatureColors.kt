package com.katoaapps.openminilaunch.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Identity-bearing colors that must not be derived from Material You, wallpaper, or theme mode.
 * Theme colors own app chrome; these colors tell the user what a feature or provider is.
 */
internal object MinkFeatureColors {
    val MagicText = Color(0xFF2563EB)
    val MagicCall = Color(0xFF198754)
    val MagicTodo = Color(0xFFD6A300)
    val MagicNote = Color(0xFF7C3AED)
    val MagicEvent = Color(0xFF8B5A2B)
    val MagicApp = Color(0xFFC62828)

    /** Default software notification-light color when a favorite has no custom color. */
    val ConversationAttention = Color(0xFFAD4A25)

    /** Numeric unread badge; intentionally independent from favorite and theme colors. */
    val ConversationCount = Color(0xFFC62828)

    val AppBlocked = Color(0xFFFF0000)

    val Slack = Color(0xFF4A154B)
    val Gmail = Color(0xFFC5221F)
    val GoogleMessages = Color(0xFF1A73E8)
}
