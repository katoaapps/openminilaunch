package com.katoaapps.openminilaunch.model

import androidx.annotation.StringRes
import com.katoaapps.openminilaunch.R

enum class Shortcut(@param:StringRes val labelRes: Int) {
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
