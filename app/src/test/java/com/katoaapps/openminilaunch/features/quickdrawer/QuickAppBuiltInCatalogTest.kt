package com.katoaapps.openminilaunch.features.quickdrawer

import com.katoaapps.openminilaunch.model.AppGridSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickAppBuiltInCatalogTest {
    @Test
    fun freshInstallOrderMirrorsMinkActionsAndReplacesExtraWithTodos() {
        assertEquals(
            listOf(
                AppGridSlot.NOTE,
                AppGridSlot.EVENT,
                AppGridSlot.WEATHER,
                null,
                AppGridSlot.CALL,
                AppGridSlot.MESSAGE,
                AppGridSlot.FILES,
            ),
            QuickAppBuiltIn.defaultOrder.map(QuickAppBuiltIn::appGridSlot),
        )
    }

    @Test
    fun keysResolveOnlyKnownBuiltInActions() {
        QuickAppBuiltIn.entries.forEach { action ->
            assertEquals(action, QuickAppBuiltIn.fromSelectionKey(action.selectionKey))
        }
        assertNull(QuickAppBuiltIn.fromSelectionKey("internal:quick-app:unknown"))
    }
}
