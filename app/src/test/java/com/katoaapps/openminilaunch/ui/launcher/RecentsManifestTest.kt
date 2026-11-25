package com.katoaapps.openminilaunch.ui.launcher

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RecentsManifestTest {
    private val manifest by lazy { File("src/main/AndroidManifest.xml").readText() }

    @Test
    fun userWorkspacesHaveStableRecentsLabels() {
        val visibleActivities =
            mapOf(
                ".ui.magic.AssistantActivity" to "@string/mink_assistant",
                ".ui.apps.PrivateAppsActivity" to "@string/private_space",
                ".ui.apps.work.WorkAppsActivity" to "@string/work_space",
                ".ui.minkpad.MinkPadActivity" to "@string/minkpad_lite",
                ".ui.minkspace.media.MediaLabActivity" to "@string/media_lab",
                ".ui.launcher.MinkWorkspaceActivity" to "@string/app_name",
                ".ui.profile.VirtualContactCardActivity" to "@string/profile",
            )

        visibleActivities.forEach { (activityName, label) ->
            val declaration = activityDeclaration(activityName)
            assertTrue(activityName, declaration.contains("android:excludeFromRecents=\"false\""))
            assertTrue(activityName, declaration.contains("android:label=\"$label\""))
        }
    }

    @Test
    fun launcherAndRoutingHostsStayOutOfRecents() {
        listOf(
            ".features.bubbles.AppBubbleHostActivity",
            ".ui.apps.AllAppsActivity",
            ".ui.launcher.LauncherEntryActivity",
            ".ui.shortcuts.PinShortcutActivity",
            ".ui.launcher.MainActivity",
        ).forEach { activityName ->
            val declaration = activityDeclaration(activityName)
            assertTrue(activityName, declaration.contains("android:excludeFromRecents=\"true\""))
        }
    }

    private fun activityDeclaration(activityName: String): String =
        manifest.substringAfter("android:name=\"$activityName\"").substringBefore('>')
}
