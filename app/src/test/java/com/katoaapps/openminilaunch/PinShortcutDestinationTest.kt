package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.model.PinShortcutDestination
import com.katoaapps.openminilaunch.model.Shortcut
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PinShortcutDestinationTest {
    @Test fun restoresEachDestinationFromItsSavedKey() {
        val destinations = listOf(
            PinShortcutDestination.HomeSlot(Shortcut.NOTE),
            PinShortcutDestination.AddToDrawer,
            PinShortcutDestination.AddToLibrary,
            PinShortcutDestination.ReplaceDrawerSlot(4),
        )

        destinations.forEach { destination ->
            assertEquals(destination, PinShortcutDestination.fromKey(destination.key))
        }
    }

    @Test fun rejectsMalformedAndNegativeDrawerDestinations() {
        assertNull(PinShortcutDestination.fromKey("drawer:replace:-1"))
        assertNull(PinShortcutDestination.fromKey("home:NOT_A_SLOT"))
        assertNull(PinShortcutDestination.fromKey("something-else"))
    }
}
