package com.katoaapps.openminilaunch.features.minkpad.input

/** USB HID Consumer page usages. All commands are momentary press/release pairs. */
internal enum class MinkPadConsumerAction(val usage: Int, val label: String) {
    VOLUME_DOWN(0xEA, "Volume −"), VOLUME_UP(0xE9, "Volume +"),
    MUTE(0xE2, "Mute"), PLAY_PAUSE(0xCD, "Play / pause"),
    PREVIOUS(0xB6, "Previous"), NEXT(0xB5, "Next"),
}
