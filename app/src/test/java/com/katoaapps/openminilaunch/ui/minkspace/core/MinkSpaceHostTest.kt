package com.katoaapps.openminilaunch.ui.minkspace.core

import org.junit.Assert.assertEquals
import org.junit.Test

class MinkSpaceHostTest {
    @Test
    fun firstVisitStartsOnManager() {
        assertEquals(
            1,
            initialMinkSpacePage(
                settingsPage = 1,
                shouldStartOnManager = true,
                demoModeEnabled = false,
            ),
        )
    }

    @Test
    fun returningAndDemoVisitsStartOnFirstMiniApp() {
        assertEquals(
            0,
            initialMinkSpacePage(
                settingsPage = 1,
                shouldStartOnManager = false,
                demoModeEnabled = false,
            ),
        )
        assertEquals(
            0,
            initialMinkSpacePage(
                settingsPage = 2,
                shouldStartOnManager = true,
                demoModeEnabled = true,
            ),
        )
    }
}
