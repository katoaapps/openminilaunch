package org.sableos.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SableDesignContractTest {
    @Test
    fun corruptValuesFallBackSafely() {
        assertEquals(AppearanceMode.FollowSystem, AppearanceMode.fromStableValue("bad"))
        assertEquals(AccentPreset.Blue, AccentPreset.fromStableValue(null))
        assertEquals(SableCornerStyle.Compact, SableCornerStyle.fromStableValue(null))
        assertEquals(SableCornerStyle.Compact, SableCornerStyle.fromStableValue("pill"))
        assertEquals(SableCornerStyle.Compact, SableCornerStyle.fromStableValue("ROUNDED"))
    }

    @Test
    fun stableValuesRoundTrip() {
        AppearanceMode.entries.forEach { mode ->
            assertEquals(mode, AppearanceMode.fromStableValue(mode.stableValue))
        }
        AccentPreset.entries.forEach { accent ->
            assertEquals(accent, AccentPreset.fromStableValue(accent.stableValue))
        }
        SableCornerStyle.entries.forEach { style ->
            assertEquals(style, SableCornerStyle.fromStableValue(style.stableValue))
        }
        assertEquals(
            listOf("compact", "rounded"),
            SableCornerStyle.entries.map { it.stableValue },
        )
    }

    @Test
    fun uiContractKeepsTouchTargetsAndCornersIntentional() {
        assertEquals(2, SableDesignContract.SCHEMA_VERSION)
        assertTrue(SableDesignContract.MIN_TOUCH_TARGET_DP >= 48)
        // Two allowed corner styles: Compact (<= 6dp) and Rounded (<= 12dp).
        assertEquals(6, SableDesignContract.MAX_COMPACT_CORNER_RADIUS_DP)
        assertEquals(12, SableDesignContract.MAX_ROUNDED_CORNER_RADIUS_DP)
        assertEquals(
            SableDesignContract.MAX_ROUNDED_CORNER_RADIUS_DP,
            SableDesignContract.MAX_STANDARD_CORNER_RADIUS_DP,
        )
        assertEquals(
            SableDesignContract.MIN_TOUCH_TARGET_DP,
            SableResponsive.TOUCH_TARGET_MIN_DP,
        )
        assertTrue(
            SableDesignContract.SCREEN_HORIZONTAL_PADDING_DP >=
                SableDesignContract.SCREEN_VERTICAL_PADDING_DP,
        )
    }

    @Test
    fun globalAppearanceContractIsStableAndNarrow() {
        assertEquals(
            "org.sableos.appearance",
            SableGlobalAppearanceContract.AUTHORITY,
        )
        assertTrue(
            SableGlobalAppearanceContract.AUTHORITY !=
                "org.sableos.start.appearance",
        )
        assertEquals(
            "appearance",
            SableGlobalAppearanceContract.PATH_APPEARANCE,
        )
        assertEquals(
            "mode",
            SableGlobalAppearanceContract.COLUMN_MODE,
        )
        assertEquals(
            "accent",
            SableGlobalAppearanceContract.COLUMN_ACCENT,
        )
        assertEquals(
            "corner_style",
            SableGlobalAppearanceContract.COLUMN_CORNER_STYLE,
        )
    }

    @Test
    fun compactIsTheDefaultSoNothingChangesUntilTheUserPicks() {
        assertEquals(SableCornerStyle.Compact, SableCornerStyle.DEFAULT)
        assertEquals(SableCornerStyle.Compact, SableAppearance().cornerStyle)
        assertEquals(
            listOf(2, 3, 4, 6, 6),
            SableShapeTable.forStyle(SableCornerStyle.DEFAULT).radiiDp,
        )
    }

    @Test
    fun roundedUsesTheDesignControlAndCardRadii() {
        val rounded = SableShapeTable.forStyle(SableCornerStyle.Rounded)
        // Controls (extraSmall, small) 8dp; cards, sheets and dialogs 12dp.
        assertEquals(listOf(8, 8, 12, 12, 12), rounded.radiiDp)
        assertEquals(SableGeometryTokens.SMALL_CONTROL_RADIUS_DP, rounded.smallDp)
        assertEquals(SableGeometryTokens.CARD_RADIUS_DP, rounded.mediumDp)
    }

    @Test
    fun everyStyleStaysWithinItsAllowedMaximumAndIsMonotonic() {
        val limits =
            mapOf(
                SableCornerStyle.Compact to SableDesignContract.MAX_COMPACT_CORNER_RADIUS_DP,
                SableCornerStyle.Rounded to SableDesignContract.MAX_ROUNDED_CORNER_RADIUS_DP,
            )
        assertEquals(SableCornerStyle.entries.toSet(), limits.keys)
        limits.forEach { (style, max) ->
            val scale = SableShapeTable.forStyle(style)
            assertEquals("$style max", max, scale.maxDp)
            assertTrue("$style radii grow with the role", scale.radiiDp.zipWithNext().all { (a, b) -> a <= b })
            assertTrue("$style radii are positive", scale.radiiDp.all { it > 0 })
        }
    }

    @Test
    fun motionDurationsIncreaseByEmphasis() {
        assertTrue(
            SableDesignContract.FAST_MOTION_MS <
                SableDesignContract.STANDARD_MOTION_MS,
        )
        assertTrue(
            SableDesignContract.STANDARD_MOTION_MS <
                SableDesignContract.EMPHASIZED_MOTION_MS,
        )
    }
}
