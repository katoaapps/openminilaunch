package org.sableos.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaffoldLevelsTest {
    @Test
    fun fourLevelsNumberedInOrder() {
        assertEquals(4, ScaffoldLevels.all.size)
        ScaffoldLevels.all.forEachIndexed { i, l -> assertEquals(i + 1, l.number) }
    }

    @Test
    fun heroStartsAndJunoStandOnSurfaces() {
        ScaffoldLevels.all.forEach { l ->
            listOf(l.heroStart, l.juno).forEach { p ->
                assertTrue(
                    "level ${l.number} point $p",
                    l.surfaces.any { p.x in it.x0..it.x1 && p.y == it.y },
                )
            }
        }
    }

    @Test
    fun ladderEndsMeetSurfaces() {
        ScaffoldLevels.all.forEach { l ->
            l.ladders.forEach { ld ->
                assertTrue(
                    "level ${l.number} ladder ${ld.x} top",
                    l.surfaces.any {
                        ld.x in it.x0..it.x1 &&
                            it.y == ld.top
                    },
                )
                assertTrue(
                    "level ${l.number} ladder ${ld.x} bottom",
                    l.surfaces.any {
                        ld.x in it.x0..it.x1 &&
                            it.y == ld.bottom
                    },
                )
            }
        }
    }

    @Test
    fun everythingFitsTheField() {
        ScaffoldLevels.all.forEach { l ->
            l.bolts.forEach { assertTrue(it.x in 0f..ScaffoldField.WIDTH && it.y in 0f..ScaffoldField.HEIGHT) }
            l.surfaces.forEach { assertTrue(it.x0 >= 0f && it.x1 <= ScaffoldField.WIDTH) }
            assertTrue(l.boltCount > 0)
        }
    }
}
