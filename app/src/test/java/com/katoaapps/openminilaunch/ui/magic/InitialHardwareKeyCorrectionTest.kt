package com.katoaapps.openminilaunch.ui.magic

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.katoaapps.openminilaunch.features.magic.MAGIC_COMMAND_PREFIXES
import org.junit.Assert.assertEquals
import org.junit.Test

class InitialHardwareKeyCorrectionTest {
    @Test fun imeAutoSpaceDoesNotReplaceCapturedPunctuationHotkey() {
        val correction = InitialHardwareKeyCorrection(now = { 100L })
        correction.begin("?", keyCode = 76)

        val corrected = correction.correct(TextFieldValue("? ", TextRange(2)))

        assertEquals("?", corrected.text)
        assertEquals(TextRange(1), corrected.selection)
    }

    @Test fun commandPrefixesAreNotReplacedByTheNextTypedCharacter() {
        MAGIC_COMMAND_PREFIXES.forEach { prefix ->
            val correction = InitialHardwareKeyCorrection(now = { 100L })
            correction.begin(prefix.toString(), keyCode = 76)

            val input = "$prefix" + "text"
            val corrected = correction.correct(TextFieldValue(input, TextRange(input.length)))

            assertEquals(input, corrected.text)
            assertEquals(TextRange(input.length), corrected.selection)
        }
    }

    @Test fun commandPrefixAutoSpaceIsRemovedWithoutLosingFollowingText() {
        val correction = InitialHardwareKeyCorrection(now = { 100L })
        correction.begin("-", keyCode = 69)

        val corrected = correction.correct(TextFieldValue("- todo", TextRange(6)))

        assertEquals("-todo", corrected.text)
        assertEquals(TextRange(5), corrected.selection)
    }

    @Test fun singleAlternateCharacterReplacesCapturedHardwareCharacter() {
        val correction = InitialHardwareKeyCorrection(now = { 100L })
        correction.begin("p", keyCode = 44)

        val corrected = correction.correct(TextFieldValue("p?", TextRange(2)))

        assertEquals("?", corrected.text)
        assertEquals(TextRange(1), corrected.selection)
    }

    @Test fun largerEditorUpdatePreservesCapturedPrefix() {
        val correction = InitialHardwareKeyCorrection(now = { 100L })
        correction.begin("?", keyCode = 76)

        val corrected = correction.correct(TextFieldValue("?maps", TextRange(5)))

        assertEquals("?maps", corrected.text)
        assertEquals(TextRange(5), corrected.selection)
    }
}
