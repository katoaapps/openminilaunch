package org.sableos.games.console

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameManifestTest {
    @Test
    fun readsAGameToml() {
        val m =
            GameManifest
                .parse(
                    """
                    # comment
                    id = "tilt_racer"
                    name = "Tilt Racer"
                    entry = "main.lua"
                    version = "1.2"
                    shelf = "motion"
                    input = ["tilt", "keys"]
                    best = "high"
                    summary = "Steer by \"tilting\"."
                    """.trimIndent(),
                ).getOrThrow()
        assertEquals("tilt_racer", m.id)
        assertEquals("Tilt Racer", m.name)
        assertEquals(GameShelf.Motion, m.shelf)
        assertTrue(m.usesTilt)
        assertEquals(BestIs.High, m.bestIs)
        assertEquals("Steer by \"tilting\".", m.summary)
    }

    @Test
    fun rustmixManifestsWork() {
        // rustmix-wave's APP.TOM: input "imu" means tilt, and the defaults fill the rest.
        val m =
            GameManifest
                .parse(
                    "id = \"tilt_maze\"\nname = \"Tilt Maze\"\nentry = \"MAIN.lua\"\ninput = [\"imu\", \"select\"]",
                ).getOrThrow()
        assertTrue(m.usesTilt)
        assertEquals(GameShelf.Puzzle, m.shelf)
        assertEquals(BestIs.High, m.bestIs)
    }

    @Test
    fun refusesUnsafeOrBrokenManifests() {
        assertFalse(GameManifest.parse("id = \"x\"").isSuccess)
        assertFalse(GameManifest.parse("id = \"../evil\"\nname = \"E\"").isSuccess)
        assertFalse(GameManifest.parse("id = \"ok\"\nname = \"E\"\nentry = \"../../x.lua\"").isSuccess)
        assertFalse(GameManifest.parse("id = \"ok\"\nname = \"E\"\nentry = \"main.sh\"").isSuccess)
        assertFalse(GameManifest.parse("id = \"ok\"\nname = \"${"n".repeat(41)}\"").isSuccess)
        assertFalse(GameManifest.parse("id = \"ok\"\nname = \"unclosed").isSuccess)
        assertFalse(GameManifest.parse("just words").isSuccess)
    }
}
