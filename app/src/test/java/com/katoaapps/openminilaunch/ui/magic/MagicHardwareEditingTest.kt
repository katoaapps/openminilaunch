package com.katoaapps.openminilaunch.ui.magic

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class MagicHardwareEditingTest {
    @Test fun selectAllCoversTheWholeDraft() {
        val selected = selectAllMagicText(TextFieldValue("tablet query", TextRange(4)))

        assertEquals(TextRange(0, 12), selected.selection)
    }

    @Test fun arrowsMoveOrCollapseTheSelection() {
        val value = TextFieldValue("query", TextRange(2))
        assertEquals(
            TextRange(1),
            moveMagicCursor(value, HorizontalCursorDirection.LEFT, false).selection,
        )
        assertEquals(
            TextRange(3),
            moveMagicCursor(value, HorizontalCursorDirection.RIGHT, false).selection,
        )

        val selection = TextFieldValue("query", TextRange(4, 1))
        assertEquals(
            TextRange(1),
            moveMagicCursor(selection, HorizontalCursorDirection.LEFT, false).selection,
        )
        assertEquals(
            TextRange(4),
            moveMagicCursor(selection, HorizontalCursorDirection.RIGHT, false).selection,
        )
    }

    @Test fun shiftArrowExtendsSelectionAndUnicodeMovesAsOneCharacter() {
        val value = TextFieldValue("A😀B", TextRange(3))
        val moved = moveMagicCursor(value, HorizontalCursorDirection.LEFT, true)

        assertEquals(TextRange(3, 1), moved.selection)
    }

    @Test fun homeAndEndRespectShiftSelection() {
        val value = TextFieldValue("query", TextRange(2))

        assertEquals(TextRange(0), moveMagicCursorToEdge(value, end = false, extendSelection = false).selection)
        assertEquals(TextRange(2, 5), moveMagicCursorToEdge(value, end = true, extendSelection = true).selection)
    }
}
