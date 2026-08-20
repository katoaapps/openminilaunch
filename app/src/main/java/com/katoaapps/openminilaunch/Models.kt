package com.katoaapps.openminilaunch

import android.net.Uri
import androidx.annotation.StringRes

data class TodoItem(
    val id: String,
    val text: String,
    val completed: Boolean = false,
)

data class LaunchableApp(
    val label: String,
    val packageName: String,
)

data class ContactResult(
    val contactUri: String,
    val name: String,
    val phone: String,
    val phoneLabel: String,
)

data class SearchFolder(
    val uri: String,
    val label: String,
)

data class WidgetGridSize(
    val columns: Int,
    val rows: Int,
)

data class FileSearchResult(
    val name: String,
    val uri: Uri,
    val mimeType: String,
    val modifiedAt: Long,
)

enum class Shortcut(@StringRes val labelRes: Int) {
    NOTE(R.string.shortcut_note),
    EVENT(R.string.shortcut_calendar),
    WEATHER(R.string.shortcut_weather),
    TODO(R.string.shortcut_todo),
    CALL(R.string.shortcut_call),
    MESSAGE(R.string.shortcut_messenger),
    FILES(R.string.shortcut_files),
    DRAWER(R.string.shortcut_top_eight),
}

internal const val MAX_DRAWER_APPS = 8
val configurableShortcuts: List<Shortcut> = Shortcut.entries.filterNot {
    it == Shortcut.TODO || it == Shortcut.DRAWER
}

enum class Screen { HOME, SETTINGS, TODOS, HUB }

enum class ThemePreference(@StringRes val labelRes: Int) {
    SYSTEM(R.string.theme_system),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark),
}

enum class MessageSendMode(@StringRes val labelRes: Int) {
    ALWAYS_ASK(R.string.message_mode_always_ask),
    DIRECT_SMS(R.string.message_mode_direct_sms),
    MESSAGING_APP(R.string.message_mode_messaging_app),
}

internal val SOCIAL_GOAL_OPTIONS = listOf(30, 60, 90, 120)
