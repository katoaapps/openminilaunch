package com.katoaapps.openminilaunch

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeLayoutTest {
    @Test fun todoPreviewStepsDownWithAvailableHeight() {
        assertEquals(1, visibleTodoItemsForHeight(220f))
        assertEquals(2, visibleTodoItemsForHeight(280f))
        assertEquals(3, visibleTodoItemsForHeight(350f))
        assertEquals(4, visibleTodoItemsForHeight(410f))
        assertEquals(5, visibleTodoItemsForHeight(480f))
    }

    @Test fun shortcutCellsRemainSquareWithinWidthAndHeightBounds() {
        assertEquals(68f, shortcutCellSizeDp(140f, 400f), .01f)
        assertEquals(59.5f, shortcutCellSizeDp(140f, 250f), .01f)
        assertEquals(88f, shortcutCellSizeDp(240f, 500f), .01f)
    }

    @Test fun configurableShortcutsUseStableGenericSlotLabels() {
        assertEquals(6, configurableShortcuts.size)
        assertEquals(R.string.shortcut_note, Shortcut.NOTE.labelRes)
        assertEquals(R.string.shortcut_files, Shortcut.FILES.labelRes)
        assertEquals(R.string.shortcut_todo, Shortcut.TODO.labelRes)
    }
}
