package com.katoaapps.openminilaunch.ui.launcher

import com.katoaapps.minkboard.ui.R as MinkBoardR
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.Screen
import org.junit.Assert.assertEquals
import org.junit.Test

class MinkWorkspaceActivityTest {
    @Test
    fun workspaceScreensHaveSpecificRecentsLabels() {
        assertEquals(R.string.app_name, Screen.HOME.workspaceRecentsLabelRes())
        assertEquals(R.string.settings, Screen.SETTINGS.workspaceRecentsLabelRes())
        assertEquals(MinkBoardR.string.minkboard_title, Screen.TODOS.workspaceRecentsLabelRes())
        assertEquals(R.string.conversations, Screen.CONVERSATIONS.workspaceRecentsLabelRes())
        assertEquals(R.string.mink_day, Screen.MINK_DAY.workspaceRecentsLabelRes())
    }
}
