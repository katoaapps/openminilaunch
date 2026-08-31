package com.katoaapps.openminilaunch.model

import com.katoaapps.openminilaunch.R

import android.content.ComponentName
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

/** A launcher activity in one Android user profile. Package name alone is not a unique app ID. */
data class LauncherAppTarget(
    val label: String,
    val packageName: String,
    val componentName: ComponentName,
    val userSerial: Long,
    val isWorkProfile: Boolean,
    val isAvailable: Boolean = true,
    val selectionKey: String = launcherAppSelectionKey(userSerial, componentName),
)

data class LauncherAppIdentity(
    val userSerial: Long,
    val componentName: ComponentName,
)

data class LauncherAppKeyParts(
    val userSerial: Long,
    val flattenedComponent: String,
)

private const val LAUNCHER_APP_KEY_PREFIX = "launcher:"

fun launcherAppSelectionKey(userSerial: Long, componentName: ComponentName): String =
    "$LAUNCHER_APP_KEY_PREFIX$userSerial:${componentName.flattenToString()}"

fun launcherAppIdentity(selectionKey: String): LauncherAppIdentity? {
    val parts = launcherAppKeyParts(selectionKey) ?: return null
    val component = ComponentName.unflattenFromString(parts.flattenedComponent) ?: return null
    return LauncherAppIdentity(parts.userSerial, component)
}

fun launcherAppKeyParts(selectionKey: String): LauncherAppKeyParts? {
    if (!selectionKey.startsWith(LAUNCHER_APP_KEY_PREFIX)) return null
    val parts = selectionKey.split(':', limit = 3)
    if (parts.size != 3) return null
    val serial = parts[1].toLongOrNull()?.takeIf { it >= 0 } ?: return null
    if (parts[2].isBlank()) return null
    return LauncherAppKeyParts(serial, parts[2])
}

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

enum class MinkAppPauseMode(
    @param:StringRes val labelRes: Int,
    @param:StringRes val descriptionRes: Int,
) {
    ALWAYS(R.string.pause_mode_always, R.string.pause_mode_always_description),
    AFTER_DAILY_LIMIT(R.string.pause_mode_after_limit, R.string.pause_mode_after_limit_description),
    NEVER(R.string.pause_mode_never, R.string.pause_mode_never_description),
}

internal const val MIN_SOCIAL_GOAL_HOURS = 0
internal const val MAX_SOCIAL_GOAL_HOURS = 23
