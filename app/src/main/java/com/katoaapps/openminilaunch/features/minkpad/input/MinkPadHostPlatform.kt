package com.katoaapps.openminilaunch.features.minkpad.input

import android.bluetooth.BluetoothClass
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

/** Uses the receiving device's advertised identity only to choose the initial controller surface. */
internal fun minkPadRemoteControlsHint(name: String?, deviceClass: Int?): Boolean {
    if (deviceClass in remoteControlDeviceClasses) return true
    val normalized = name?.trim()?.lowercase().orEmpty()
    if (normalized.isEmpty()) return false
    return remoteControlNames.any(normalized::contains) || tvNameToken.containsMatchIn(normalized)
}

private val remoteControlDeviceClasses = setOf(
    BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX,
    BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CONFERENCING,
    BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER,
    BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR,
)
private val remoteControlNames = listOf(
    "android tv", "apple tv", "chromecast", "fire tv", "google tv", "shield tv", "smart tv", "television",
)
private val tvNameToken = Regex("(^|\\W)tv($|\\W)")

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
        MinkPadGestureAction.NEXT_APP, MinkPadGestureAction.PREVIOUS_APP -> when (this) {
            MAC -> {
                val previous = action == MinkPadGestureAction.PREVIOUS_APP
                MinkPadShortcut(
                    usage = if (previous) 80 else 79,
                    modifiers = ctrl,
                    description = (if (previous) "Previous" else "Next") + " Space · Control + " +
                        if (previous) "Left" else "Right",
                )
            }
            ANDROID, WINDOWS, APPLE_MOBILE -> {
                val previous = action == MinkPadGestureAction.PREVIOUS_APP
                val modifier = (if (this == APPLE_MOBILE) meta else alt) or (if (previous) shift else 0)
                MinkPadShortcut(
                    usage = 43,
                    modifiers = modifier,
                    description = "Switch app · " + (if (this == APPLE_MOBILE) "Command" else "Alt") +
                        (if (previous) " + Shift" else "") + " + Tab",
                )
            }
            GENERIC -> null
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
