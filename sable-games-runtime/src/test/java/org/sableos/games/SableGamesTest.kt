package org.sableos.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SableGamesTest {
    @Test
    fun `top level catalog exposes all stable routes once`() {
        assertEquals(15, SableGames.catalog.size)
        assertEquals(SableGames.catalog.size, SableGames.catalog.map { it.id }.toSet().size)
        listOf("sudoku", "cards", "console").forEach { assertNotNull(SableGames.entry(it)) }
        assertEquals("Tilt Games", SableGames.entry("console")?.title)
        assertEquals(SableGames.catalog.size, SableGames.catalog.map { it.iconRes }.toSet().size)
    }

    @Test
    fun `every standard game has a unique task activity`() {
        val standardGames = SableGames.catalog.filterNot { it.id == "console" }
        val taskActivities = standardGames.map { SableGames.taskActivity(it.id) }

        assertEquals(standardGames.size, taskActivities.toSet().size)
    }

    @Test
    fun `every standard game task supports independent resizable windows`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val affinities = mutableSetOf<String>()

        SableGames.catalog.filterNot { it.id == "console" }.forEach { game ->
            val activityName = SableGames.taskActivity(game.id).name
            val activity = manifest.substringAfter("android:name=\"$activityName\"").substringBefore("</activity>")

            assertTrue(activityName, activity.contains("android:resizeableActivity=\"true\""))
            assertTrue(activityName, activity.contains("android:exported=\"true\""))
            assertTrue(activityName, activity.contains("android:excludeFromRecents=\"false\""))
            assertTrue(activityName, activity.contains("android:documentLaunchMode=\"intoExisting\""))
            assertTrue(activityName, activity.contains("android:label=\"@string/"))
            assertTrue(activityName, activity.contains("android:icon=\"@mipmap/ic_sable_app_"))
            assertTrue(activityName, activity.contains("android.intent.action.MAIN"))
            assertTrue(activityName, activity.contains("android.intent.category.LAUNCHER"))
            val affinity = activity.substringAfter("android:taskAffinity=\"").substringBefore('"')
            assertTrue(activityName, affinity.isNotBlank())
            assertTrue("Duplicate task affinity: $affinity", affinities.add(affinity))
            assertTrue(
                activityName,
                activity.contains("android.window.PROPERTY_SUPPORTS_MULTI_INSTANCE_SYSTEM_UI"),
            )
        }

        val internalHost =
            manifest
                .substringAfter("android:name=\"org.sableos.games.SableGameActivity\"")
                .substringBefore("/>")
        assertTrue(internalHost, internalHost.contains("android:exported=\"false\""))
        assertTrue(internalHost, internalHost.contains("android:excludeFromRecents=\"true\""))
    }

    @Test
    fun `embedding configuration keeps external games disabled by default`() {
        assertFalse(SableGamesConfiguration().allowExternalConsoleGames)
    }
}
