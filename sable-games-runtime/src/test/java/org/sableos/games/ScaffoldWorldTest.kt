package org.sableos.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaffoldWorldTest {
    private fun world(
        n: Int = 1,
        run: ScaffoldRun = ScaffoldRun(),
    ) = ScaffoldWorld(ScaffoldLevels.level(n), run)

    private fun ScaffoldWorld.hold(
        seconds: Float,
        input: ScaffoldInput,
    ) {
        var t = 0f
        while (t < seconds && state == WorldState.Playing) {
            step(1f / 60f, input)
            t += 1f / 60f
        }
    }

    @Test
    fun startsOnTheGroundWithTheRun() {
        val w = world(run = ScaffoldRun(hearts = 2, hats = 1, bolts = 5, score = 300))
        assertTrue(w.onGround)
        assertEquals(2, w.hearts)
        assertEquals(1, w.hats)
        assertEquals(5, w.boltTally)
        assertEquals(WorldState.Playing, w.state)
    }

    @Test
    fun walkingMovesTheHero() {
        val w = world()
        val x0 = w.heroX
        w.hold(0.3f, ScaffoldInput(right = true))
        assertTrue(w.heroX > x0)
        w.hold(0.6f, ScaffoldInput(left = true))
        assertTrue(w.heroX < x0 + 20f)
    }

    @Test
    fun jumpLeavesTheGroundAndComesBack() {
        val w = world()
        val y0 = w.heroY
        w.step(1f / 60f, ScaffoldInput(jump = true))
        w.hold(0.1f, ScaffoldInput())
        assertFalse(w.onGround)
        assertTrue(w.heroY < y0)
        w.hold(2f, ScaffoldInput())
        assertTrue(w.onGround)
        assertEquals(y0, w.heroY, 0.5f)
    }

    @Test
    fun climbingALadderRaisesTheHero() {
        val w = world()
        val l = w.level.ladders.first { it.bottom == ScaffoldField.GROUND_Y }
        // walk to it
        var guard = 0
        while (kotlin.math.abs(w.heroX - l.x) > 3f && guard++ < 2000) {
            w.step(1f / 60f, ScaffoldInput(right = w.heroX < l.x, left = w.heroX > l.x))
        }
        val y0 = w.heroY
        w.hold(0.5f, ScaffoldInput(up = true))
        assertTrue(w.heroY < y0)
    }

    @Test
    fun hitsTakeAHatBeforeAHeart() {
        val w = world(run = ScaffoldRun(hats = 1))
        injectHit(w)
        assertEquals(0, w.hats)
        assertEquals(ScaffoldWorld.START_HEARTS, w.hearts)
    }

    @Test
    fun hitsWithoutAHatCostAHeartAndThreeEndTheRun() {
        val w = world()
        injectHit(w)
        assertEquals(ScaffoldWorld.START_HEARTS - 1, w.hearts)
        val dead = world(run = ScaffoldRun(hearts = 1))
        injectHit(dead)
        assertEquals(WorldState.Dead, dead.state)
    }

    @Test
    fun hatRulesAreEightBoltsUpToThree() {
        assertEquals(8, ScaffoldWorld.BOLTS_PER_HAT)
        assertEquals(3, ScaffoldWorld.MAX_HATS)
    }

    /** Puts a hazard on the hero and lets one frame run. */
    private fun injectHit(w: ScaffoldWorld) {
        val h = Hazard(HazardKind.Pipe, w.heroX, w.heroY - 10f, 1, 1)
        w.hazards.add(h)
        w.step(1f / 60f, ScaffoldInput())
    }
}
