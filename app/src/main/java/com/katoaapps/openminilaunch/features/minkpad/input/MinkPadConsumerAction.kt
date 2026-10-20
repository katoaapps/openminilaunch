package com.katoaapps.openminilaunch.features.minkpad.input

/** USB HID Consumer page usages. All commands are momentary press/release pairs. */
internal enum class MinkPadConsumerAction(val usage: Int) {
    BACK(0x224),
    VOLUME_DOWN(0xEA), VOLUME_UP(0xE9),
    CHANNEL_DOWN(0x9D), CHANNEL_UP(0x9C),
    MUTE(0xE2), PLAY_PAUSE(0xCD),
    PREVIOUS(0xB6), NEXT(0xB5),
}
