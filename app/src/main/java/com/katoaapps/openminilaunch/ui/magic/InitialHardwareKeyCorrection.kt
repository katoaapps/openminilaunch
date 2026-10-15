package com.katoaapps.openminilaunch.ui.magic

import android.os.SystemClock
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Reconciles the first key captured by collapsed Home with an IME's long-press replacement.
 *
 * The collapsed key target inserts the ordinary character before the real editor is focused.
 * Some physical-keyboard IMEs then append their alternate character for that same held key.
 * In that one case, replace the captured character instead of leaving both characters behind.
 */
internal class InitialHardwareKeyCorrection(
    private val now: () -> Long = SystemClock::uptimeMillis,
) {
    private data class Candidate(
        val text: String,
        val keyCode: Int,
        val startedAt: Long,
    )

    private var candidate: Candidate? = null

    fun begin(text: String, keyCode: Int) {
        candidate = Candidate(text, keyCode, now())
    }

    fun onHardwareKeyUp(keyCode: Int) {
        if (candidate?.keyCode == keyCode) clear()
    }

    fun correct(value: TextFieldValue): TextFieldValue {
        val pending = candidate ?: return value
        if (now() - pending.startedAt > REPLACEMENT_WINDOW_MILLIS) {
            clear()
            return value
        }
        if (!value.text.startsWith(pending.text)) {
            clear()
            return value
        }

        val appendedText = value.text.removePrefix(pending.text)
        if (appendedText.isEmpty() || appendedText.startsWith(pending.text)) return value

        // A few physical-keyboard IMEs automatically commit a separator after punctuation.
        // The captured punctuation is already the complete Magic Box command prefix, so do not
        // mistake that separator for the IME replacing a long-pressed key.
        if (appendedText.all(Char::isWhitespace)) {
            clear()
            return TextFieldValue(
                text = pending.text,
                selection = TextRange(pending.text.length),
            )
        }

        // Long-press replacement is a single character. Preserve any larger edit rather than
        // deleting the captured prefix based on a coincidental first update from the real editor.
        if (appendedText.codePointCount(0, appendedText.length) != 1) {
            clear()
            return value
        }

        clear()
        return TextFieldValue(
            text = appendedText,
            selection = TextRange(appendedText.length),
        )
    }

    fun clear() {
        candidate = null
    }

    private companion object {
        const val REPLACEMENT_WINDOW_MILLIS = 1_500L
    }
}
