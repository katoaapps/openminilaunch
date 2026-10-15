package com.katoaapps.openminilaunch.ui.magic

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
