package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.ui.launcher.home.*
import com.katoaapps.openminilaunch.model.*

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HomeLayoutTest {
    @Test
    fun `OpenMink stays the default launcher activity before embedded games`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val launcherEntry = manifest.indexOf(".ui.launcher.LauncherEntryActivity")
        val gamesConsole = manifest.indexOf("org.sableos.games.console.ConsoleActivity")

        assertTrue(launcherEntry >= 0)
        assertTrue(gamesConsole >= 0)
        assertTrue(launcherEntry < gamesConsole)
    }

    @Test fun todoPreviewStepsDownWithAvailableHeight() {
        assertEquals(1, visibleTodoItemsForHeight(220f))
        assertEquals(2, visibleTodoItemsForHeight(270f))
        assertEquals(3, visibleTodoItemsForHeight(280f))
        assertEquals(3, visibleTodoItemsForHeight(350f))
        assertEquals(4, visibleTodoItemsForHeight(410f))
        assertEquals(5, visibleTodoItemsForHeight(480f))
    }

    @Test fun appGridSlotsRemainSquareWithinWidthAndHeightBounds() {
        assertEquals(68f, appGridSlotCellSizeDp(140f, 400f), .01f)
        assertEquals(59.5f, appGridSlotCellSizeDp(140f, 250f), .01f)
        assertEquals(88f, appGridSlotCellSizeDp(240f, 500f), .01f)
    }

    @Test fun homePanelRespectsSpaceReservedForMagicBox() {
        assertEquals(320.dp, resolvedHomeFocusPanelHeight(320.dp, 420.dp, false))
        assertEquals(420.dp, resolvedHomeFocusPanelHeight(500.dp, 420.dp, false))
        assertEquals(500.dp, resolvedHomeFocusPanelHeight(500.dp, 420.dp, true))
    }

    @Test fun constrainedHomeLayoutsReserveSpaceForDockedMinkKeyboard() {
        assertTrue(
            shouldReserveDockedMinkKeyboard(
                layoutReservesKeyboardSpace = true,
                magicExpanded = false,
                launcherKeyboardVisible = true,
                keyboardInputEnabled = true,
                autoOpenKeyboardOnHome = true,
                useMinkKeyboard = true,
            ),
        )
        assertFalse(
            shouldReserveDockedMinkKeyboard(
                layoutReservesKeyboardSpace = true,
                magicExpanded = true,
                launcherKeyboardVisible = true,
                keyboardInputEnabled = true,
                autoOpenKeyboardOnHome = true,
                useMinkKeyboard = true,
            ),
        )
        assertFalse(
            shouldReserveDockedMinkKeyboard(
                layoutReservesKeyboardSpace = false,
                magicExpanded = false,
                launcherKeyboardVisible = true,
                keyboardInputEnabled = true,
                autoOpenKeyboardOnHome = true,
                useMinkKeyboard = true,
            ),
        )
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

    @Test fun configurableAppGridSlotsUseStableGenericSlotLabels() {
        assertEquals(7, configurableAppGridSlots.size)
        assertEquals(R.string.shortcut_note, AppGridSlot.NOTE.labelRes)
        assertEquals(R.string.shortcut_files, AppGridSlot.FILES.labelRes)
        assertEquals(R.string.shortcut_extra, AppGridSlot.EXTRA.labelRes)
    }

    @Test fun freshHomeLayoutLeavesCreateSlotSeventh() {
        assertEquals(6, freshHomeFolderSlots.size)
        assertFalse(AppGridSlot.EXTRA in freshHomeFolderSlots)
        assertEquals(AppGridSlot.EXTRA, freshHomeAppGridOrder[6])
        assertEquals(AppGridSlot.DRAWER, freshHomeAppGridOrder[7])
        assertEquals(AppGridSlot.entries.toSet(), freshHomeAppGridOrder.toSet())
    }

    @Test fun classicHomeWrapsAllSevenSlotsWithFixedDestinations() {
        val pages = classicLauncherPages(configurableAppGridSlots)

        assertEquals(11, pages.size)
        assertEquals(ClassicLauncherPage.Conversations, pages.first())
        assertEquals(ClassicLauncherPage.MinkSpace, pages[1])
        assertEquals(
            configurableAppGridSlots,
            pages.filterIsInstance<ClassicLauncherPage.Folder>().map { it.slot },
        )
        assertEquals(ClassicLauncherPage.QuickApps, pages[pages.lastIndex - 1])
        assertEquals(ClassicLauncherPage.Widgets, pages.last())
    }

    @Test fun classicHomeDefaultsToQuickAppsUntilAFolderIsSelected() {
        val pages = classicLauncherPages(configurableAppGridSlots)

        assertEquals(pages.indexOf(ClassicLauncherPage.QuickApps), classicHomePageIndex(pages, null))
        assertEquals(
            pages.indexOfFirst {
                it is ClassicLauncherPage.Folder && it.slot == AppGridSlot.FILES
            },
            classicHomePageIndex(pages, AppGridSlot.FILES),
        )
    }

    @Test fun classicPageChangesRearmMagicBoxWheneverMagicCanOwnInput() {
        assertTrue(
            shouldRearmMagicBox(
                ClassicLauncherPage.Conversations,
                ClassicLauncherPage.MinkSpace,
            ),
        )
        assertTrue(
            shouldRearmMagicBox(
                ClassicLauncherPage.MinkSpace,
                ClassicLauncherPage.Folder(AppGridSlot.NOTE, 1),
            ),
        )
        assertTrue(
            shouldRearmMagicBox(
                ClassicLauncherPage.MinkSpace,
                ClassicLauncherPage.QuickApps,
            ),
        )
        assertFalse(
            shouldRearmMagicBox(
                ClassicLauncherPage.Conversations,
                ClassicLauncherPage.Conversations,
            ),
        )
        assertFalse(
            shouldRearmMagicBox(
                ClassicLauncherPage.QuickApps,
                ClassicLauncherPage.Conversations,
            ),
        )
        assertFalse(
            shouldRearmMagicBox(
                ClassicLauncherPage.Folder(AppGridSlot.NOTE, 1),
                ClassicLauncherPage.Folder(AppGridSlot.NOTE, 1),
            ),
        )
    }

    @Test fun classicHomePreservesVerticalMotionForEmbeddedTools() {
        assertTrue(ClassicLauncherPage.QuickApps.supportsLauncherVerticalGestures)
        assertTrue(
            ClassicLauncherPage.Folder(AppGridSlot.NOTE, 1)
                .supportsLauncherVerticalGestures,
        )
        assertFalse(ClassicLauncherPage.Conversations.supportsLauncherVerticalGestures)
        assertFalse(ClassicLauncherPage.Widgets.supportsLauncherVerticalGestures)
        assertFalse(ClassicLauncherPage.MinkSpace.supportsLauncherVerticalGestures)
    }

    @Test fun classicUiStateKeepsRootConversationsSeparateFromReplyInput() {
        val root = ClassicLauncherUiState(page = ClassicLauncherPage.Conversations)
        val reply = root.copy(conversationReplyActive = true)

        assertTrue(root.homeKeyboardInputAllowed)
        assertFalse(root.shouldShowHomeMagicBox(magicExpanded = false))
        assertTrue(root.shouldShowHomeMagicBox(magicExpanded = true))
        assertFalse(reply.homeKeyboardInputAllowed)
        assertFalse(reply.shouldShowHomeMagicBox(magicExpanded = true))
    }

    @Test fun classicUiStateSeparatesWidgetAndFolderInputRules() {
        val widgets = ClassicLauncherUiState(page = ClassicLauncherPage.Widgets)
        val folder = ClassicLauncherUiState(
            page = ClassicLauncherPage.Folder(AppGridSlot.NOTE, 1),
        )

        assertFalse(widgets.homeKeyboardInputAllowed)
        assertFalse(widgets.verticalGesturesEnabled)
        assertTrue(folder.homeKeyboardInputAllowed)
        assertTrue(folder.verticalGesturesEnabled)
    }
}
