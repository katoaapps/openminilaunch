package com.katoaapps.openminilaunch

import android.net.Uri

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
) {
    val label: String get() = "$columns × $rows"
}

data class FileSearchResult(
    val name: String,
    val uri: Uri,
    val mimeType: String,
    val modifiedAt: Long,
)

enum class Shortcut(val label: String) {
    NOTE("Note"), EVENT("Calendar"), WEATHER("Weather"), TODO("To-do"),
    CALL("Call"), MESSAGE("Messenger"), FILES("Files"), DRAWER("Top 8")
}

internal const val MAX_DRAWER_APPS = 8
internal const val DEFAULT_HOME_PANEL_COLOR_ARGB = 0xFF173529.toInt()

val configurableShortcuts: List<Shortcut> = Shortcut.entries.filterNot {
    it == Shortcut.TODO || it == Shortcut.DRAWER
}

enum class Screen { HOME, SETTINGS, TODOS, HUB }

enum class ThemePreference(val label: String) {
    SYSTEM("System"), LIGHT("Light"), DARK("Dark")
}

enum class MessageSendMode(val label: String) {
    ALWAYS_ASK("Always ask"),
    DIRECT_SMS("Always send as SMS"),
    MESSAGING_APP("Always choose messaging app"),
}

internal val SOCIAL_GOAL_OPTIONS = listOf(30, 60, 90, 120)

internal fun socialGoalLabel(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes}m"
}
