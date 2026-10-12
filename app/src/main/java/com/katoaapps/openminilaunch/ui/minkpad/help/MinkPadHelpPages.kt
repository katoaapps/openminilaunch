package com.katoaapps.openminilaunch.ui.minkpad.help

import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadGestureAction
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadHostPlatform
import com.katoaapps.openminilaunch.features.minkpad.input.shortcut

internal data class MinkPadHelpPage(
    val title: String,
    val description: String,
    val examples: List<MinkPadGestureExample> = emptyList(),
)

/** Visual examples have no input-report behavior. Their captions explain the same host mappings. */
internal fun minkPadHelpPages(platform: MinkPadHostPlatform): List<MinkPadHelpPage> {
    fun action(value: MinkPadGestureAction) =
        platform.shortcut(value)?.description ?: "Not mapped for this host. No command is sent."
    fun example(label: String, fingers: Int, motion: MinkPadGestureMotion, caption: String) =
        MinkPadGestureExample(label, fingers, motion, caption)

    return listOf(
        MinkPadHelpPage("Point, select and open",
            "Move one finger to move the pointer. Tap to left-click. Double-tap to double-click or select text, depending on the host app. Tap to click can be disabled in settings.",
            listOf(
                example("Move", 1, MinkPadGestureMotion.MOVE, "Slide one finger to move the pointer."),
                example("Tap", 1, MinkPadGestureMotion.TAP, "Tap once to select."),
                example("Double tap", 1, MinkPadGestureMotion.DOUBLE_TAP, "Two quick taps to open or highlight."),
            )),
        MinkPadHelpPage("Right-click and scroll",
            "Tap with two fingers to right-click. Slide two fingers together to scroll vertically or horizontally. Invert two-finger scrolling in controller settings if the direction feels wrong.",
            listOf(
                example("Right-click", 2, MinkPadGestureMotion.TAP, "Tap two fingers together."),
                example("Scroll up/down", 2, MinkPadGestureMotion.VERTICAL, "Slide two fingers vertically."),
                example("Scroll left/right", 2, MinkPadGestureMotion.HORIZONTAL, "Slide two fingers horizontally."),
            )),
        MinkPadHelpPage("Pinch to zoom",
            "Spread or pinch two fingers for discrete app zoom steps. This sends a keyboard shortcut, not a native trackpad pinch. Apps that do not support the shortcut will not zoom.",
            listOf(
                example("Zoom in", 2, MinkPadGestureMotion.SPREAD, action(MinkPadGestureAction.ZOOM_IN)),
                example("Zoom out", 2, MinkPadGestureMotion.PINCH, action(MinkPadGestureAction.ZOOM_OUT)),
            )),
        MinkPadHelpPage("Hold, grab and drag",
            "Hold one finger still until the small haptic tick, then move to drag. Lift to release. Double-tap and keep the second tap down also holds the left button. A host app decides what can be dragged.",
            listOf(
                example("Hold", 1, MinkPadGestureMotion.HOLD, "Keep one finger still to hold the left button."),
                example("Drag", 1, MinkPadGestureMotion.DRAG, "After holding, move the finger. Lift to drop."),
            )),
        MinkPadHelpPage("Back and overview",
            "Use three fingers for Back and overview shortcuts. A downward swipe dismisses an overview; it does not close an app.",
            listOf(
                example("Back", 3, MinkPadGestureMotion.TAP, action(MinkPadGestureAction.BACK)),
                example("Swipe up", 3, MinkPadGestureMotion.UP, action(MinkPadGestureAction.OVERVIEW)),
                example("Swipe down", 3, MinkPadGestureMotion.DOWN, action(MinkPadGestureAction.DISMISS_OVERVIEW)),
            )),
        MinkPadHelpPage("Switch apps and app list",
            "Swipe three fingers left or right to switch apps. Tap four fingers for the available app-list equivalent. Some host systems do not have a mapping.",
            listOf(
                example("Swipe left", 3, MinkPadGestureMotion.LEFT, action(MinkPadGestureAction.NEXT_APP)),
                example("Swipe right", 3, MinkPadGestureMotion.RIGHT, action(MinkPadGestureAction.PREVIOUS_APP)),
                example("App list", 4, MinkPadGestureMotion.TAP, action(MinkPadGestureAction.APP_LIST)),
            )),
        MinkPadHelpPage("Host setup and keyboard",
            "Choose your receiving device's system in controller settings. The preset is saved for that paired host; HID does not reliably reveal its operating system. Meta is Command on Apple devices and Windows on Windows. Software text uses a US layout. On a hardware-keyboard phone the live editor stays hidden; tap Type on a touch-only phone."),
        MinkPadHelpPage("Pairing and privacy",
            "Start 2-minute pairing opens Android's system discoverability prompt. While the countdown is running, select Pro Controller - MinkPad in the receiving device's settings. MinkPad registers one native keyboard-and-mouse profile; it does not broadcast a second custom BLE identity. Pair new device releases the active connection before pairing another host. Android's pairings are never removed automatically.\n\nOpen the top controls and tap the current host name to switch between saved devices. Close MinkPad releases all input, closes the HID session, restores this phone's Bluetooth name, and returns to the landing screen. Start MinkPad asks the most recently used receiving device to reconnect. Manage saved devices contains a troubleshooting Disconnect action.\n\nKeyboard, mouse, and media commands go directly to the receiving device. Mink does not save or upload typed text or pointer coordinates. Nearby devices access is for Bluetooth connection and discoverability, not location. Notifications provide Open, Reconnect and Stop; denying them does not prevent input."),
    )
}
