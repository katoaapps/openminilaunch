package com.katoaapps.openminilaunch.ui.magic

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.katoaapps.openminilaunch.features.magic.MagicBoxRoute
import com.katoaapps.openminilaunch.features.magic.consumeMagicBoxTrigger
import org.junit.Assert.assertEquals
import org.junit.Test

class InitialMagicInputCorrectionTest {
    @Test fun singleAlternateCharacterReplacesCapturedHardwareCharacter() {
        val correction = InitialMagicInputCorrection(now = { 100L })
        correction.beginHardwareCapture("p", keyCode = 44)

        val corrected = correction.correct(TextFieldValue("p?", TextRange(2)))

        assertEquals("?", corrected.value.text)
        assertEquals(TextRange(1), corrected.value.selection)
        assertEquals(true, corrected.replacedInitialInput)
    }

    @Test fun largerEditorUpdatePreservesCapturedPrefix() {
        val correction = InitialMagicInputCorrection(now = { 100L })
        correction.beginHardwareCapture("p", keyCode = 44)

        val corrected = correction.correct(TextFieldValue("pmaps", TextRange(5)))

        assertEquals("pmaps", corrected.value.text)
        assertEquals(TextRange(5), corrected.value.selection)
        assertEquals(false, corrected.replacedInitialInput)
    }

    @Test fun platformImeCanReplaceItsFirstCharacterWithHeldKeyAlternate() {
        val correction = InitialMagicInputCorrection(now = { 100L })
        correction.beginPlatformInput("p")

        val corrected = correction.correct(TextFieldValue("?", TextRange(1)))

        assertEquals("?", corrected.value.text)
        assertEquals(true, corrected.replacedInitialInput)
    }

    @Test fun heldKeyReplacementCanSelectMagicRouteAsTheRealFirstInput() {
        val correction = InitialMagicInputCorrection(now = { 100L })
        correction.beginHardwareCapture("p", keyCode = 44)

        val corrected = correction.correct(TextFieldValue("p?", TextRange(2)))
        val trigger = consumeMagicBoxTrigger(
            previousText = if (corrected.replacedInitialInput) "" else "p",
            updatedText = corrected.value.text,
        )

        assertEquals(MagicBoxRoute.APP, trigger?.route)
        assertEquals("", trigger?.content)
    }

    @Test fun ordinarySecondPlatformCharacterIsNotTreatedAsReplacement() {
        val correction = InitialMagicInputCorrection(now = { 100L })
        correction.beginPlatformInput("p")

        val corrected = correction.correct(TextFieldValue("pm", TextRange(2)))

        assertEquals("pm", corrected.value.text)
        assertEquals(false, corrected.replacedInitialInput)
    }
}
