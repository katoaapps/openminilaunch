package org.sableos.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SableSystemTokensTest {
    @Test
    fun paletteSatisfiesEveryContrastRule() {
        val violations = SableTokenRules.violations()
        assertTrue("token violations: $violations", violations.isEmpty())
    }

    @Test
    fun contrastMatchesWcagReferenceValues() {
        assertEquals(21.0, SableContrast.ratio(0xFF000000, 0xFFFFFFFF), 0.01)
        assertEquals(1.0, SableContrast.ratio(0xFF777777, 0xFF777777), 0.0001)
        // The 2.0 blue on white is why the light theme needs its own, darker tone.
        assertEquals(3.09, SableContrast.ratio(0xFF2E9BD6, 0xFFFFFFFF), 0.01)
    }

    @Test
    fun seedAloneWouldFailOnTheLightBase() {
        // Guards the reason for the dark/light accent split: the seed blue is under 3:1 on the light base.
        assertTrue(SableContrast.ratio(SableAccentTokens.Blue.seed, SableSystemTokens.Light.surfaceBase) < 3.0)
    }

    @Test
    fun rulesCoverBothThemesEverySurfaceAndEveryAccent() {
        val rules = SableTokenRules.contrastRules()
        // 2 themes x 3 surfaces x (7 roles + 1 accent x 2) + 2 themes x 1 onAccent
        assertEquals(2 * 3 * (7 + 1 * 2) + 2 * 1, rules.size)
        assertTrue(rules.any { it.name == "light.surface1 blue focus" })
        assertTrue(rules.any { it.name == "dark.blue onAccent" })
    }

    @Test
    fun oneBlueAccentFor2point0() {
        // AccentPreset (SableTheme.kt) and this seed must stay identical; Sable apps and
        // the system palette are seeded from the same value. 1.0's presets read as it.
        assertEquals(listOf(SableAccentTokens.Blue), SableAccentTokens.entries.toList())
        assertEquals("2E9BD6", SableAccentTokens.Blue.seedHex)
        assertEquals(SableAccentTokens.Blue, SableAccentTokens.fromStableValue(null))
        assertEquals(SableAccentTokens.Blue, SableAccentTokens.fromStableValue("slate"))
    }

    @Test
    fun surfacesKeepExistingSableColorTokenValues() {
        assertEquals(0xFF000000L, SableSystemTokens.Dark.surfaceBase)
        assertEquals(0xFF141414L, SableSystemTokens.Dark.surfaceRaised)
        assertEquals(0xFF262626L, SableSystemTokens.Dark.surfaceOverlay)
        assertEquals(0xFFF5F7F9L, SableSystemTokens.Light.surfaceBase)
        assertEquals(0xFFFFFFFFL, SableSystemTokens.Light.surfaceRaised)
        assertEquals(0xFFE9EDF2L, SableSystemTokens.Light.surfaceOverlay)
    }

    @Test
    fun privacyIsNeverStyledAsDanger() {
        for (dark in listOf(true, false)) {
            val roles = SableSystemTokens.roles(dark)
            assertFalse(roles.privacy == roles.danger)
            // Distinct hue family: privacy is a cool neutral, danger is red-dominant.
            val r = (roles.danger shr 16) and 0xFF
            val b = roles.danger and 0xFF
            assertTrue(r > b)
            val pr = (roles.privacy shr 16) and 0xFF
            val pb = roles.privacy and 0xFF
            assertTrue(pb >= pr)
        }
    }

    @Test
    fun geometryFollowsTheDesignBaseline() {
        assertEquals(12, SableGeometryTokens.CARD_RADIUS_DP)
        assertEquals(8, SableGeometryTokens.SMALL_CONTROL_RADIUS_DP)
        assertEquals(2, SableGeometryTokens.FOCUS_STROKE_DP)
        assertEquals(2, SableGeometryTokens.FOCUS_OUTER_GAP_DP)
        assertEquals(48, SableGeometryTokens.ROW_MIN_HEIGHT_DP)
        assertTrue(SableGeometryTokens.spaces.all { it % 4 == 0 })
        assertEquals(listOf(28, 22, 18, 16, 14, 12), SableTypeRole.entries.map { it.sizeSp })
    }

    @Test
    fun motionIsClampedAndReduceMotionRemovesIt() {
        assertEquals(120, SableMotionTokens.transitionMs(40, 1f))
        assertEquals(180, SableMotionTokens.transitionMs(400, 1f))
        assertEquals(150, SableMotionTokens.transitionMs(150, 1f))
        assertEquals(0, SableMotionTokens.transitionMs(150, 0f))
        assertEquals(75, SableMotionTokens.transitionMs(150, 0.5f))
    }

    @Test
    fun displayClassComesFromGeometryNotDeviceModel() {
        // Q25 3.5" 720x720 at the LineageOS density and at the candidate Sable densities.
        assertEquals(SableDisplayClass.SquareKeyboard, SableDisplayClass.of(720, 720, 193))
        assertEquals(SableDisplayClass.SquareKeyboard, SableDisplayClass.of(720, 720, 280))
        // Titan 2 1440x1440 is the same class.
        assertEquals(SableDisplayClass.SquareKeyboard, SableDisplayClass.of(1440, 1440, 480))
        // Pixel 7 1080x2400 at 420 dpi is a tall touch phone.
        assertEquals(SableDisplayClass.Tall, SableDisplayClass.of(1080, 2400, 420))
        // A narrow tall keyboard phone.
        assertEquals(SableDisplayClass.CompactPortrait, SableDisplayClass.of(720, 1600, 320))
        // 4:3 is still "notlong", like Android's resource qualifier.
        assertEquals(SableDisplayClass.SquareKeyboard, SableDisplayClass.of(768, 1024, 160))
    }

    @Test
    fun smallestWidthMatchesDisplayProfileArithmetic() {
        assertEquals(596, SableDisplayClass.smallestWidthDp(720, 720, 193))
        assertEquals(480, SableDisplayClass.smallestWidthDp(720, 720, 240))
        assertEquals(411, SableDisplayClass.smallestWidthDp(720, 720, 280))
    }

    @Test
    fun squareDisplaysGetTheOneColumnListAndFourTileCompactRow() {
        val square = SableQuickSettingsLayout.forDisplayClass(SableDisplayClass.SquareKeyboard)
        assertEquals(2, square.gridColumns)
        assertEquals(1, square.labeledTilesPerRow)
        assertTrue(square.smallTileLabels)
        assertTrue(square.allDefaultTilesLarge)
        assertEquals(1, square.compactRows)
        assertEquals(4, square.compactColumns)
        val tall = SableQuickSettingsLayout.forDisplayClass(SableDisplayClass.Tall)
        assertFalse(tall.allDefaultTilesLarge)
    }

    @Test
    fun defaultTilesPutHighValueControlsFirstAndExcludeGatedHardware() {
        val tiles = SableQuickSettingsTiles.DEFAULT
        assertEquals(listOf("internet", "bt", "dark", "dnd"), tiles.take(4))
        assertTrue(tiles.contains("battery"))
        assertTrue(tiles.contains("flashlight"))
        assertTrue(tiles.none { it in SableQuickSettingsTiles.PROFILE_GATED })
        assertEquals(tiles.size, tiles.toSet().size)
    }

    @Test
    fun roundedCornerStyleEqualsTheSystemShapeTokens() {
        // Sable apps in the Rounded style match SystemUI cards and controls (DESIGN-KF-B).
        val rounded = SableShapeTable.forStyle(SableCornerStyle.Rounded)
        assertEquals(SableGeometryTokens.SMALL_CONTROL_RADIUS_DP, rounded.extraSmallDp)
        assertEquals(SableGeometryTokens.SMALL_CONTROL_RADIUS_DP, rounded.smallDp)
        assertEquals(SableGeometryTokens.CARD_RADIUS_DP, rounded.mediumDp)
        assertEquals(SableGeometryTokens.CARD_RADIUS_DP, rounded.largeDp)
        assertEquals(SableGeometryTokens.CARD_RADIUS_DP, rounded.extraLargeDp)
        assertTrue(SableShapeTable.Compact.maxDp < SableGeometryTokens.SMALL_CONTROL_RADIUS_DP)
    }
}
