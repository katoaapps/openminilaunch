package com.katoaapps.openminilaunch.features.keyboard

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.katoaapps.openminilaunch.model.LauncherKeyboardLayout
import com.katoaapps.openminilaunch.model.LauncherKeyboardPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherKeyboardLayoutDataTest {
    @Test fun insertionReplacesSelectionAndMovesCursor() {
        val result = insertLauncherKeyboardText(
            TextFieldValue("mink pad", selection = TextRange(5, 8)),
            "board",
        )
        assertEquals("mink board", result.text)
        assertEquals(TextRange(10), result.selection)
    }

    @Test fun backspaceDeletesWholeUnicodeCodePoint() {
        val result = deleteLauncherKeyboardText(
            TextFieldValue("A😀", selection = TextRange(3)),
        )
        assertEquals("A", result.text)
        assertEquals(TextRange(1), result.selection)
    }

    @Test fun sentenceCapsStartsEmptyAndAfterPunctuation() {
        assertTrue(shouldCapitalizeLauncherKeyboardSentence(TextFieldValue()))
        assertTrue(shouldCapitalizeLauncherKeyboardSentence(TextFieldValue("Hello. ")))
    }

    @Test fun keycapLabelMatchesCapitalizationState() {
        val key = LauncherKeyboardKey("q")
        assertEquals("Q", launcherKeyboardDisplayLabel(key, uppercase = true, supportsCase = true))
        assertEquals("q", launcherKeyboardDisplayLabel(key, uppercase = false, supportsCase = true))
    }

    @Test fun manualShiftOverrideCanEnableOrDisableSentenceCaps() {
        assertTrue(launcherKeyboardUsesUppercase(shiftOverride = null, sentenceCaps = true))
        assertFalse(launcherKeyboardUsesUppercase(shiftOverride = false, sentenceCaps = true))
        assertTrue(launcherKeyboardUsesUppercase(shiftOverride = true, sentenceCaps = false))
    }

    @Test fun numberKeysDoNotAdvertiseSymbolAlternates() {
        assertTrue(launcherKeyboardNumberKeys(symbols = false).all { it.secondaryLabel == null })
        assertTrue(launcherKeyboardNumberKeys(symbols = true).all { it.secondaryLabel == null })
    }

    @Test fun largeScreenSplitIsEnabledByDefault() {
        assertTrue(LauncherKeyboardPreferences().splitOnWideDisplays)
    }

    @Test fun splitKeepsMiddleReachKeysOutOfTheVisibleHalves() {
        val qwertyTop = launcherKeyboardRows(LauncherKeyboardLayout.QWERTY, symbols = false).first()
        val split = splitLauncherKeyboardRow(qwertyTop)
        assertEquals(listOf("q", "w", "e", "r", "t"), split.left.map { it.label })
        assertEquals(listOf("y", "u", "i", "o", "p"), split.right.map { it.label })
        assertEquals("y", split.leftReachKey?.label)
        assertEquals("t", split.rightReachKey?.label)
    }

    @Test fun everyAdvertisedLayoutHasThreeRows() {
        LauncherKeyboardLayout.entries.forEach { layout ->
            assertEquals(layout.name, 3, launcherKeyboardRows(layout, symbols = false).size)
        }
    }
}
