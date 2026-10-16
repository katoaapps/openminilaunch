package com.katoaapps.openminilaunch.features.minkpad.input

import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadHostPlatform.ANDROID
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadHostPlatform.APPLE_MOBILE
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadHostPlatform.GENERIC
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadHostPlatform.MAC
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadHostPlatform.WINDOWS

/** User-selected per-host preset. Standard HID does not expose the receiving operating system. */
internal enum class MinkPadHostPlatform(val label: String) {
    GENERIC("Choose Operating System"), ANDROID("Android / Google TV"), MAC("Mac"),
    WINDOWS("Windows"), APPLE_MOBILE("iPhone / iPad"),
}

/** Uses only explicit host-name signals. The Bluetooth phone class is handled by the repository. */
internal fun minkPadHostPlatformHint(name: String?): MinkPadHostPlatform? {
    val normalized = name?.trim()?.lowercase().orEmpty()
    if (normalized.isEmpty()) return null
    return when {
        listOf("iphone", "ipad").any(normalized::contains) -> APPLE_MOBILE
        listOf("macbook", "imac", "mac mini", "mac studio", "mac pro").any(normalized::contains) -> MAC
        listOf("windows", "surface pro", "surface laptop").any(normalized::contains) -> WINDOWS
        listOf("android", "google tv", "chromecast", "pixel", "galaxy", "shield tv")
            .any(normalized::contains) -> ANDROID
        else -> null
    }
}

internal enum class MinkPadGestureAction {
    BACK, OVERVIEW, DISMISS_OVERVIEW, PREVIOUS_APP, NEXT_APP, APP_LIST, ZOOM_IN, ZOOM_OUT,
}

internal data class MinkPadShortcut(val usage: Int, val modifiers: Int, val description: String)

/** HID modifier bits, not Android KeyEvent masks. Missing mappings are intentional no-ops. */
internal fun MinkPadHostPlatform.shortcut(action: MinkPadGestureAction): MinkPadShortcut? {
    val ctrl = 1
    val shift = 2
    val alt = 4
    val meta = 8
    return when (action) {
        MinkPadGestureAction.BACK -> when (this) {
            ANDROID -> MinkPadShortcut(41, meta, "Back · Meta + Esc")
            MAC -> MinkPadShortcut(47, meta, "Back in compatible apps · Command + [")
            WINDOWS -> MinkPadShortcut(80, alt, "Back in compatible apps · Alt + Left")
            else -> null
        }
        MinkPadGestureAction.OVERVIEW -> when (this) {
            ANDROID, WINDOWS -> MinkPadShortcut(43, meta, if (this == ANDROID) "Recent apps · Meta + Tab" else "Task View · Windows + Tab")
            MAC -> MinkPadShortcut(82, ctrl, "Mission Control · Control + Up")
            else -> null // Apple's Globe/Fn key needs a different report, not a guessed modifier.
        }
        MinkPadGestureAction.DISMISS_OVERVIEW -> when (this) {
            ANDROID -> MinkPadShortcut(41, meta, "Dismiss overview · Meta + Esc")
            MAC -> MinkPadShortcut(81, ctrl, "App Exposé · Control + Down")
            WINDOWS -> MinkPadShortcut(41, 0, "Dismiss Task View · Esc")
            else -> null
        }
        MinkPadGestureAction.NEXT_APP, MinkPadGestureAction.PREVIOUS_APP -> {
            val modifier = when (this) {
                ANDROID, WINDOWS -> alt
                MAC, APPLE_MOBILE -> meta
                GENERIC -> return null
            } or (if (action == MinkPadGestureAction.PREVIOUS_APP) shift else 0)
            MinkPadShortcut(43, modifier, "Switch app · " +
                (if (this == MAC || this == APPLE_MOBILE) "Command" else "Alt") +
                (if (action == MinkPadGestureAction.PREVIOUS_APP) " + Shift" else "") + " + Tab")
        }
        MinkPadGestureAction.APP_LIST -> when (this) {
            ANDROID -> MinkPadShortcut(0, meta, "App list / search · Meta (host-dependent)")
            WINDOWS -> MinkPadShortcut(0, meta, "Start menu · Windows")
            else -> null // No universal app-drawer shortcut on Mac or iOS.
        }
        MinkPadGestureAction.ZOOM_IN, MinkPadGestureAction.ZOOM_OUT -> when (this) {
            MAC, WINDOWS, ANDROID -> {
                val zoomIn = action == MinkPadGestureAction.ZOOM_IN
                MinkPadShortcut(if (zoomIn) 46 else 45,
                    (if (this == MAC) meta else ctrl) or (if (zoomIn) shift else 0),
                    "App zoom · " + (if (this == MAC) "Command" else "Ctrl") + (if (zoomIn) " + Plus" else " + Minus"))
            }
            else -> null
        }
    }
}
