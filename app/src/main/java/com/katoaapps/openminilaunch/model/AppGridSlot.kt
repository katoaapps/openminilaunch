package com.katoaapps.openminilaunch.model

import androidx.annotation.StringRes
import com.katoaapps.openminilaunch.R

enum class AppGridSlot(@param:StringRes val labelRes: Int) {
    NOTE(R.string.shortcut_note),
    EVENT(R.string.shortcut_calendar),
    WEATHER(R.string.shortcut_weather),
    EXTRA(R.string.shortcut_extra),
    CALL(R.string.shortcut_call),
    MESSAGE(R.string.shortcut_messenger),
    FILES(R.string.shortcut_files),
    DRAWER(R.string.quick_drawer),
}

internal const val MAX_DRAWER_APPS = 8

val configurableAppGridSlots: List<AppGridSlot> = AppGridSlot.entries.filterNot { it == AppGridSlot.DRAWER }

/** Keeps the Quick Drawer as the fixed eighth Home slot while preserving app-slot order. */
internal fun normalizedAppGridOrder(order: Iterable<AppGridSlot>): List<AppGridSlot> = buildList {
    order.forEach { slot ->
        if (slot != AppGridSlot.DRAWER && slot !in this) add(slot)
    }
    configurableAppGridSlots.forEach { slot ->
        if (slot !in this) add(slot)
    }
    add(AppGridSlot.DRAWER)
}

internal fun appGridSlotFromStoredName(name: String): AppGridSlot? = when (name) {
    "TODO", "PROFILE" -> AppGridSlot.EXTRA
    else -> runCatching { AppGridSlot.valueOf(name) }.getOrNull()
}
