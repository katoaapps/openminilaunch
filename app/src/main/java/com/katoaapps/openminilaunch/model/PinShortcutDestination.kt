package com.katoaapps.openminilaunch.model

/** A Mink location selected for an app-initiated pinned shortcut request. */
internal sealed interface PinShortcutDestination {
    val key: String

    data class HomeSlot(val shortcut: Shortcut) : PinShortcutDestination {
        override val key: String = "home:${shortcut.name}"
    }

    data object AddToDrawer : PinShortcutDestination {
        override val key: String = "drawer:add"
    }

    data class ReplaceDrawerSlot(val index: Int) : PinShortcutDestination {
        override val key: String = "drawer:replace:$index"
    }

    companion object {
        fun fromKey(key: String?): PinShortcutDestination? {
            if (key == AddToDrawer.key) return AddToDrawer
            val parts = key?.split(':') ?: return null
            return when {
                parts.size == 2 && parts[0] == "home" ->
                    runCatching { HomeSlot(Shortcut.valueOf(parts[1])) }.getOrNull()
                parts.size == 3 && parts[0] == "drawer" && parts[1] == "replace" ->
                    parts[2].toIntOrNull()?.takeIf { it >= 0 }?.let(::ReplaceDrawerSlot)
                else -> null
            }
        }
    }
}
