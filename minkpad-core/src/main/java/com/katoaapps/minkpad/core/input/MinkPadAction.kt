package com.katoaapps.minkpad.core.input

/** Protocol actions are stable identifiers, never inferred from translated button text. */
enum class MinkPadAction(val usage: Int) {
    UP(82), DOWN(81), LEFT(80), RIGHT(79),
    ENTER(40), ESCAPE(41), TAB(43), BACKSPACE(42), DELETE(76),
}
