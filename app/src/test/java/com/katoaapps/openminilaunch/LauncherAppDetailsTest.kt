package com.katoaapps.openminilaunch

import android.content.ComponentName
import com.katoaapps.openminilaunch.features.apps.appDetailsComponent
import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class LauncherAppDetailsTest {
    private val personalComponent = ComponentName("com.example.notes", "com.example.notes.MainActivity")
    private val workComponent = ComponentName("com.example.notes", "com.example.notes.WorkActivity")
    private val apps = listOf(
        app(personalComponent, userSerial = 0),
        app(workComponent, userSerial = 10),
    )

    @Test fun appUsesItsExactLauncherActivity() {
        assertSame(workComponent, appDetailsComponent(apps[1], apps))
    }

    @Test fun shortcutUsesOwningAppInTheSameProfile() {
        val shortcut = LauncherShortcutTarget(
            label = "New note",
            packageName = "com.example.notes",
            shortcutId = "new_note",
            userSerial = 10,
            isWorkProfile = true,
        )

        assertSame(workComponent, appDetailsComponent(shortcut, apps))
    }

    @Test fun shortcutWithoutAnInstalledOwningAppCannotOpenProfileDetails() {
        val shortcut = LauncherShortcutTarget(
            label = "Missing",
            packageName = "com.example.missing",
            shortcutId = "missing",
            userSerial = 0,
            isWorkProfile = false,
        )

        assertNull(appDetailsComponent(shortcut, apps))
    }

    private fun app(component: ComponentName, userSerial: Long) = LauncherAppTarget(
        label = "Notes",
        packageName = "com.example.notes",
        componentName = component,
        userSerial = userSerial,
        isWorkProfile = userSerial != 0L,
        selectionKey = "app:$userSerial",
    )
}
