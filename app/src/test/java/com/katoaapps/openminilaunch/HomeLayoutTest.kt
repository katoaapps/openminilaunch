package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.ui.launcher.*
import com.katoaapps.openminilaunch.model.*

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeLayoutTest {
    @Test fun todoPreviewStepsDownWithAvailableHeight() {
        assertEquals(1, visibleTodoItemsForHeight(220f))
        assertEquals(2, visibleTodoItemsForHeight(270f))
        assertEquals(3, visibleTodoItemsForHeight(280f))
        assertEquals(3, visibleTodoItemsForHeight(350f))
        assertEquals(4, visibleTodoItemsForHeight(410f))
        assertEquals(5, visibleTodoItemsForHeight(480f))
    }

    @Test fun shortcutCellsRemainSquareWithinWidthAndHeightBounds() {
        assertEquals(68f, shortcutCellSizeDp(140f, 400f), .01f)
        assertEquals(59.5f, shortcutCellSizeDp(140f, 250f), .01f)
        assertEquals(88f, shortcutCellSizeDp(240f, 500f), .01f)
    }

    @Test fun largeDisplayTodoTextUsesAvailableRoomAndFontScale() {
        val normalFont = homeTodoTextMetrics(
            largeDisplay = true,
            compact = true,
            panelHeightDp = 700f,
            itemsPerPage = 5,
            fontScale = 1f,
        )
        val largeFont = homeTodoTextMetrics(
            largeDisplay = true,
            compact = true,
            panelHeightDp = 700f,
            itemsPerPage = 5,
            fontScale = 1.5f,
        )

        assertEquals(20f, normalFont.fontSizeSp, .01f)
        assertEquals(4, normalFont.maxLines)
        assertEquals(3, largeFont.maxLines)
    }

    @Test fun phoneTodoTypographyRemainsCompact() {
        val metrics = homeTodoTextMetrics(
            largeDisplay = false,
            compact = true,
            panelHeightDp = 350f,
            itemsPerPage = 3,
            fontScale = 1f,
        )

        assertEquals(13f, metrics.fontSizeSp, .01f)
        assertEquals(2, metrics.maxLines)
    }

    @Test fun configurableShortcutsUseStableGenericSlotLabels() {
        assertEquals(6, configurableShortcuts.size)
        assertEquals(R.string.shortcut_note, Shortcut.NOTE.labelRes)
        assertEquals(R.string.shortcut_files, Shortcut.FILES.labelRes)
        assertEquals(R.string.shortcut_todo, Shortcut.TODO.labelRes)
    }
}
