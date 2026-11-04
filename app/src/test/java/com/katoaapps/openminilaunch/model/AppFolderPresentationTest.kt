package com.katoaapps.openminilaunch.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppFolderPresentationTest {
    @Test fun storedIndicatorStyleRestoresKnownValue() {
        assertEquals(
            AppFolderIndicatorStyle.GRID,
            appFolderIndicatorStyle(AppFolderIndicatorStyle.GRID.name),
        )
    }

    @Test fun missingOrUnknownIndicatorStyleUsesStackedIcons() {
        assertEquals(AppFolderIndicatorStyle.STACKED, appFolderIndicatorStyle(null))
        assertEquals(AppFolderIndicatorStyle.STACKED, appFolderIndicatorStyle("future-style"))
    }

    @Test fun storedAnimationStyleRestoresKnownValue() {
        assertEquals(
            AppFolderAnimationStyle.RIPPLE,
            appFolderAnimationStyle(AppFolderAnimationStyle.RIPPLE.name),
        )
    }

    @Test fun missingOrUnknownAnimationStyleUsesRipple() {
        assertEquals(AppFolderAnimationStyle.RIPPLE, appFolderAnimationStyle(null))
        assertEquals(AppFolderAnimationStyle.RIPPLE, appFolderAnimationStyle("future-style"))
    }
}
