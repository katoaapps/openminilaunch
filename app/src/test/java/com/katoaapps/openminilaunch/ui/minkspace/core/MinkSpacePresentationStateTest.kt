package com.katoaapps.openminilaunch.ui.minkspace.core

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MinkSpacePresentationStateTest {
    @Test fun everyPersistedIdentityHasOneMatchingManifest() {
        val resolved = MinkSpaceMiniApp.entries.map { miniApp ->
            MinkSpaceMiniAppCatalog.get(miniApp).miniApp
        }

        assertEquals(MinkSpaceMiniApp.entries, resolved)
    }

    @Test fun catalogDeclaresExpansionCapability() {
        assertFalse(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.TODO).supportsExpansion)
        assertFalse(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.CALCULATOR).supportsExpansion)
        assertFalse(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.MEDIA).supportsExpansion)
        assertTrue(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.MUSIC).supportsExpansion)
        assertTrue(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.WIDGET).supportsExpansion)
        assertTrue(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.VCARD).supportsExpansion)
        assertTrue(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.WIDGET).showExpansionButton)
        assertFalse(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.MUSIC).showExpansionButton)
        assertFalse(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.VCARD).showExpansionButton)
    }

    @Test fun standardExpansionButtonRequiresExpansionSupport() {
        val widget = MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.WIDGET)

        assertThrows(IllegalArgumentException::class.java) {
            widget.copy(supportsExpansion = false)
        }
    }

    @Test fun customExpansionLabelRequiresStandardButton() {
        val widget = MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.WIDGET)

        assertThrows(IllegalArgumentException::class.java) {
            widget.copy(
                showExpansionButton = false,
                expansionButtonLabelRes = R.string.expand_view,
            )
        }
    }

    @Test fun disabledControllerCannotExpand() {
        val state = MinkSpacePresentationState()
        val todo = state.expansionController(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.TODO))

        todo.expand()

        assertFalse(todo.enabled)
        assertFalse(todo.expanded)
        assertNull(state.expandedMiniApp)
    }

    @Test fun disabledMiniAppCannotRestoreAsExpanded() {
        val state = MinkSpacePresentationState(MinkSpaceMiniApp.TODO)

        assertNull(state.expandedMiniApp)
        assertFalse(state.isExpanded)
    }

    @Test fun miniAppControlsItsOwnExpansionState() {
        val state = MinkSpacePresentationState()
        val music = state.expansionController(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.MUSIC))

        music.expand()
        assertTrue(music.expanded)
        assertEquals(MinkSpaceMiniApp.MUSIC, state.expandedMiniApp)

        music.collapse()
        assertFalse(music.expanded)
        assertNull(state.expandedMiniApp)
    }

    @Test fun expandingAnotherMiniAppTransfersShellOwnership() {
        val state = MinkSpacePresentationState()
        val music = state.expansionController(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.MUSIC))
        val widget = state.expansionController(MinkSpaceMiniAppCatalog.get(MinkSpaceMiniApp.WIDGET))

        music.expand()
        widget.expand()

        assertFalse(music.expanded)
        assertTrue(widget.expanded)
        assertEquals(MinkSpaceMiniApp.WIDGET, state.expandedMiniApp)
    }

    @Test fun leavingExpandedPageRestoresCollapsedShell() {
        val state = MinkSpacePresentationState(MinkSpaceMiniApp.VCARD)

        state.retainOnly(MinkSpaceMiniApp.MUSIC)

        assertNull(state.expandedMiniApp)
    }
}
