package com.katoaapps.openminilaunch.features.calendar.language

/**
 * Converts common localized decimal digits to ASCII without changing UTF-16 offsets.
 * This lets regex recognizers keep source spans aligned with the user's original text.
 */
internal fun String.withAsciiCalendarDigits(): String = buildString(length) {
    this@withAsciiCalendarDigits.forEach { character ->
        append(
            when (character) {
                in '\u0660'..'\u0669' -> '0' + (character - '\u0660')
                in '\u06F0'..'\u06F9' -> '0' + (character - '\u06F0')
                in '\u0966'..'\u096F' -> '0' + (character - '\u0966')
                in '\u09E6'..'\u09EF' -> '0' + (character - '\u09E6')
                in '\uFF10'..'\uFF19' -> '0' + (character - '\uFF10')
                else -> character
            },
        )
    }
}
