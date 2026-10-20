package com.katoaapps.openminilaunch.features.minkpad.input

/** USB HID Consumer page usages. All commands are momentary press/release pairs. */
internal enum class MinkPadConsumerAction(val usage: Int, val label: String) {
    BACK(0x224, "Back"),
    VOLUME_DOWN(0xEA, "Volume −"), VOLUME_UP(0xE9, "Volume +"),
    CHANNEL_DOWN(0x9D, "Channel −"), CHANNEL_UP(0x9C, "Channel +"),
    MUTE(0xE2, "Mute"), PLAY_PAUSE(0xCD, "Play / pause"),
    PREVIOUS(0xB6, "Previous"), NEXT(0xB5, "Next"),
}
