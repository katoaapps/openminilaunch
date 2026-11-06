package com.katoaapps.openminilaunch.ui.quickdrawer

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickDrawerDestinationTest {
    @Test
    fun `available navigation follows the requested two by two order`() {
        assertEquals(
            listOf(
                QuickDrawerDestination.MINKPAD_LITE,
                QuickDrawerDestination.ALL_APPS,
                QuickDrawerDestination.PRIVATE_SPACE,
                QuickDrawerDestination.WORK_SPACE,
            ),
            quickDrawerDestinations(
                showMinkPad = true,
                showPrivateSpace = true,
                privateSpaceAvailable = true,
                showWorkSpace = true,
                workSpaceAvailable = true,
            ),
        )
    }

    @Test
    fun `optional destinations require visibility and availability`() {
        assertEquals(
            listOf(QuickDrawerDestination.ALL_APPS),
            quickDrawerDestinations(
                showMinkPad = false,
                showPrivateSpace = true,
                privateSpaceAvailable = false,
                showWorkSpace = false,
                workSpaceAvailable = true,
            ),
        )
    }
}
