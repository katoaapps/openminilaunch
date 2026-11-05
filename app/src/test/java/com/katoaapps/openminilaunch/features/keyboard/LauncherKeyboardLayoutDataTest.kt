package com.katoaapps.openminilaunch.features.keyboard

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.katoaapps.openminilaunch.model.LauncherKeyboardLayout
import org.junit.Assert.assertEquals
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

    @Test fun splitDuplicatesTheMiddleReachKeys() {
        val qwertyTop = launcherKeyboardRows(LauncherKeyboardLayout.QWERTY, symbols = false).first()
        val split = splitLauncherKeyboardRow(qwertyTop)
        assertEquals(listOf("t", "y"), split.left.takeLast(2).map { it.label })
        assertEquals(listOf("t", "y"), split.right.take(2).map { it.label })
    }

    @Test fun everyAdvertisedLayoutHasThreeRows() {
        LauncherKeyboardLayout.entries.forEach { layout ->
            assertEquals(layout.name, 3, launcherKeyboardRows(layout, symbols = false).size)
        }
    }
}
