package com.katoaapps.openminilaunch.ui.magic

import android.os.SystemClock
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Reconciles the first character entered on collapsed Home with a held-key replacement.
 *
 * Physical-keyboard IMEs can append an alternate after Home captures the ordinary character.
 * Software IMEs can instead replace their first character outright. Both are reported as a
 * replacement so Magic Box can route the alternate as the session's real first character.
 */
internal class InitialMagicInputCorrection(
    private val now: () -> Long = SystemClock::uptimeMillis,
) {
    internal data class Result(
        val value: TextFieldValue,
        val replacedInitialInput: Boolean = false,
    )

    private enum class Source {
        HARDWARE_CAPTURE,
        PLATFORM_IME,
    }

    private data class Candidate(
        val text: String,
        val source: Source,
        val keyCode: Int? = null,
        val startedAt: Long,
    )

    private var candidate: Candidate? = null

    fun beginHardwareCapture(text: String, keyCode: Int) {
        candidate = Candidate(text, Source.HARDWARE_CAPTURE, keyCode, now())
    }

    fun beginPlatformInput(text: String) {
        candidate = Candidate(text, Source.PLATFORM_IME, startedAt = now())
    }

    fun onHardwareKeyUp(keyCode: Int) {
        if (candidate?.source == Source.HARDWARE_CAPTURE && candidate?.keyCode == keyCode) clear()
    }

    fun correct(value: TextFieldValue): Result {
        val pending = candidate ?: return Result(value)
        if (now() - pending.startedAt > REPLACEMENT_WINDOW_MILLIS) {
            clear()
            return Result(value)
        }

        if (pending.source == Source.PLATFORM_IME) {
            val isSingleReplacement = value.text != pending.text &&
                value.text.codePointCount(0, value.text.length) == 1
            clear()
            return Result(value, replacedInitialInput = isSingleReplacement)
        }

        if (!value.text.startsWith(pending.text)) {
            clear()
            return Result(value)
        }

        val appendedText = value.text.removePrefix(pending.text)
        if (appendedText.isEmpty()) return Result(value)

        if (appendedText.startsWith(pending.text)) return Result(value)

        // A few physical-keyboard IMEs automatically commit a separator after punctuation.
        // Do not mistake that separator for the IME replacing a long-pressed key.
        if (appendedText.all(Char::isWhitespace)) {
            clear()
            return Result(
                TextFieldValue(
                    text = pending.text,
                    selection = TextRange(pending.text.length),
                ),
            )
        }

        // Long-press replacement is a single character. Preserve any larger edit rather than
        // deleting the captured character based on a coincidental first update from the real editor.
        if (appendedText.codePointCount(0, appendedText.length) != 1) {
            clear()
            return Result(value)
        }

        clear()
        return Result(
            value = TextFieldValue(
                text = appendedText,
                selection = TextRange(appendedText.length),
            ),
            replacedInitialInput = true,
        )
    }

    fun clear() {
        candidate = null
    }

    private companion object {
        const val REPLACEMENT_WINDOW_MILLIS = 1_500L
    }
}
