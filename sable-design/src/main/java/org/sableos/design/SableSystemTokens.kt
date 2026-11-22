package org.sableos.design

/*
 * DESIGN-KF-B semantic token table for Sable-owned and Sable-themed system
 * surfaces (status bar, Quick Settings, shade, Keyguard presentation,
 * Settings, SetupWizard and Sable apps).
 *
 * Pure Kotlin: no android.* or Compose imports, so the table and its contrast
 * rules are unit-tested on the JVM (src/test) and mirrored, value for value,
 * by the runtime resource overlays in the Q25 repository
 * (product/common/overlay), which tests/check-sable-design.py cross-checks.
 *
 * Colors are 0xAARRGGBB Longs so they can be passed straight to Compose's
 * Color(Long) or written as Android #AARRGGBB resources.
 *
 * Nothing here branches on a device model. Display class decides layout
 * (see SableDisplayClass); the device profile only supplies the measured
 * display size and density.
 */

/** The semantic color roles every Sable surface consumes (DESIGN-KF-B "Core token model"). */
data class SableColorRoles(
    val surfaceBase: Long,
    val surfaceRaised: Long,
    val surfaceOverlay: Long,
    val textPrimary: Long,
    val textSecondary: Long,
    val textDisabled: Long,
    val success: Long,
    val warning: Long,
    val danger: Long,
    val privacy: Long,
) {
    val surfaces: List<Long>
        get() = listOf(surfaceBase, surfaceRaised, surfaceOverlay)
}

/**
 * The Sable accent. SableOS 2.0 is black and white with one blue (#2E9BD6) for focus and
 * selection, as agreed for the 2.0 UI direction on 2026-11-04; the colour presets of 1.0 are gone.
 * A stored value of an old preset reads as this one.
 *
 * A Sable accent preset. [seed] is the stable preset color (the value Sable
 * apps have always used and the seed handed to the system palette). [onDark]
 * and [onLight] are the accent tones used on dark and light surfaces so the
 * accent and the focus ring keep 3:1 non-text contrast in both themes.
 */
enum class SableAccentTokens(
    val stableValue: String,
    val seed: Long,
    val dark: Long,
    val onDark: Long,
    val light: Long,
    val onLight: Long,
) {
    Blue("blue", 0xFF2E9BD6, 0xFF2E9BD6, 0xFF000000, 0xFF1A6FA0, 0xFFFFFFFF),
    ;

    fun accent(dark: Boolean): Long = if (dark) this.dark else light

    fun onAccent(dark: Boolean): Long = if (dark) onDark else onLight

    /** Focus uses the accent tone for the theme, drawn as an outline with a gap, never a fill. */
    fun focus(dark: Boolean): Long = accent(dark)

    /** "RRGGBB" seed for the system palette (Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES). */
    val seedHex: String
        get() = rgbHex(seed)

    companion object {
        val DEFAULT = Blue

        fun fromStableValue(value: String?): SableAccentTokens = entries.firstOrNull { it.stableValue == value } ?: DEFAULT
    }
}

object SableSystemTokens {
    const val SCHEMA_VERSION = 1

    /** Values equal SableColorTokens (base = background, raised = surface, overlay = raised surface). */
    val Dark =
        SableColorRoles(
            surfaceBase = 0xFF000000,
            surfaceRaised = 0xFF141414,
            surfaceOverlay = 0xFF262626,
            textPrimary = 0xFFFFFFFF,
            textSecondary = 0xFFA6A6A6,
            textDisabled = 0xFF808080,
            success = 0xFF4ADE80,
            warning = 0xFFFBBF24,
            danger = 0xFFE0463C,
            privacy = 0xFFBDBDBD,
        )

    val Light =
        SableColorRoles(
            surfaceBase = 0xFFF5F7F9,
            surfaceRaised = 0xFFFFFFFF,
            surfaceOverlay = 0xFFE9EDF2,
            textPrimary = 0xFF12171C,
            textSecondary = 0xFF515B66,
            textDisabled = 0xFF6B7682,
            success = 0xFF1A7F45,
            warning = 0xFF8A5A00,
            danger = 0xFFBA1A1A,
            privacy = 0xFF4A5D73,
        )

    fun roles(dark: Boolean): SableColorRoles = if (dark) Dark else Light
}

/** Geometry in dp (DESIGN-KF-B "Recommended baseline geometry"). */
object SableGeometryTokens {
    const val SPACE_GRID_DP = 4
    const val SPACE_1_DP = 4
    const val SPACE_2_DP = 8
    const val SPACE_3_DP = 12
    const val SPACE_4_DP = 16
    const val SPACE_6_DP = 24
    const val SPACE_8_DP = 32

    const val ROW_MIN_HEIGHT_DP = 48
    const val COMPACT_ROW_MIN_DP = 48
    const val COMPACT_ROW_MAX_DP = 56

    const val SHAPE_SMALL_DP = 8
    const val SHAPE_MEDIUM_DP = 12
    const val SHAPE_LARGE_DP = 16
    const val CARD_RADIUS_DP = SHAPE_MEDIUM_DP
    const val SMALL_CONTROL_RADIUS_DP = SHAPE_SMALL_DP

    const val FOCUS_STROKE_DP = 2
    const val FOCUS_OUTER_GAP_DP = 2

    val spaces: List<Int>
        get() = listOf(SPACE_1_DP, SPACE_2_DP, SPACE_3_DP, SPACE_4_DP, SPACE_6_DP, SPACE_8_DP)
}

/** Typography roles in sp (DESIGN-KF-B "Typography"). */
enum class SableTypeRole(
    val sizeSp: Int,
) {
    Display(28),
    ScreenTitle(22),
    SectionTitle(18),
    PrimaryRow(16),
    SecondaryRow(14),
    Metadata(12),
}

/** Motion in ms (DESIGN-KF-B "Motion"). */
object SableMotionTokens {
    const val DEFAULT_TRANSITION_MIN_MS = 120
    const val DEFAULT_TRANSITION_MAX_MS = 180
    const val FAST_MS = 120
    const val STANDARD_MS = 180

    /** Reduce-motion (animator scale 0) removes the transition rather than shortening it. */
    fun transitionMs(
        requestedMs: Int,
        animatorScale: Float,
    ): Int {
        if (animatorScale <= 0f) return 0
        val clamped = requestedMs.coerceIn(DEFAULT_TRANSITION_MIN_MS, DEFAULT_TRANSITION_MAX_MS)
        return (clamped * animatorScale).toInt()
    }
}

/**
 * Display class, decided from the measured logical display only (no device
 * model). Mirrors the Android resource qualifiers the overlays use:
 * "notlong" for square-ish screens (aspect below 5:3).
 */
enum class SableDisplayClass {
    SquareKeyboard,
    CompactPortrait,
    Tall,
    ;

    companion object {
        fun of(
            widthPx: Int,
            heightPx: Int,
            densityDpi: Int,
        ): SableDisplayClass {
            require(widthPx > 0 && heightPx > 0 && densityDpi > 0)
            val longSide = maxOf(widthPx, heightPx)
            val shortSide = minOf(widthPx, heightPx)
            // Same rule as Configuration.SCREENLAYOUT_LONG: long when longSide * 3 / 5 >= shortSide - 1.
            val isLong = longSide * 3 / 5 >= shortSide - 1
            val shortDp = shortSide * 160 / densityDpi
            return when {
                !isLong -> SquareKeyboard
                shortDp < 400 -> CompactPortrait
                else -> Tall
            }
        }

        fun smallestWidthDp(
            widthPx: Int,
            heightPx: Int,
            densityDpi: Int,
        ): Int = minOf(widthPx, heightPx) * 160 / densityDpi
    }
}

/** Quick Settings layout derived from the display class (DESIGN-KF-B "Quick Settings and shade"). */
data class SableQuickSettingsLayout(
    val gridColumns: Int,
    val largeTileSpan: Int,
    val compactRows: Int,
    val allDefaultTilesLarge: Boolean,
    /** Tiles in one compact-shade row (SystemUI config_sableQuickQsNumColumns). */
    val compactColumns: Int = gridColumns,
    /** Small tiles show their label under the icon (SystemUI config_sableQsSmallTileLabels). */
    val smallTileLabels: Boolean = false,
) {
    /**
     * Visible labeled tiles per row in the expanded grid: the large tiles that fit when every
     * default tile is large (the Q25's one-column list), else every column when small tiles are
     * labeled, else the large tiles that fit.
     */
    val labeledTilesPerRow: Int
        get() =
            when {
                allDefaultTilesLarge -> gridColumns / largeTileSpan
                smallTileLabels -> gridColumns
                else -> gridColumns / largeTileSpan
            }

    companion object {
        fun forDisplayClass(displayClass: SableDisplayClass): SableQuickSettingsLayout =
            when (displayClass) {
                // Owner decision 2026-11-12 (BB7-style, trackpad first): the expanded grid is a
                // one-column list, every default tile large (icon, name and state on one row,
                // spanning both columns); the compact shade keeps one row of four labeled tiles.
                SableDisplayClass.SquareKeyboard ->
                    SableQuickSettingsLayout(
                        gridColumns = 2,
                        largeTileSpan = 2,
                        compactRows = 1,
                        allDefaultTilesLarge = true,
                        compactColumns = 4,
                        smallTileLabels = true,
                    )
                SableDisplayClass.CompactPortrait -> SableQuickSettingsLayout(4, 2, 2, allDefaultTilesLarge = false)
                SableDisplayClass.Tall -> SableQuickSettingsLayout(4, 2, 2, allDefaultTilesLarge = false)
            }
    }
}

/**
 * Default Quick Settings tiles, high-value controls first; the first four
 * are the compact shade row of sable-quick-settings-titan2. Only stock
 * SystemUI tile specs; profile-gated tiles (SubScreen, AOD) are added by a
 * validated device profile, never by default. There is no keyboard light
 * tile: the Q25 keyboard firmware ignores host writes.
 */
object SableQuickSettingsTiles {
    val DEFAULT: List<String> =
        listOf("internet", "bt", "dark", "dnd", "battery", "flashlight", "hotspot", "location", "airplane")

    val PROFILE_GATED: Set<String> = setOf("aod", "ambient_display")
}

/** WCAG 2.x relative luminance and contrast ratio. */
object SableContrast {
    fun relativeLuminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF).toDouble() / 255.0
            return if (c <= 0.04045) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    fun ratio(
        a: Long,
        b: Long,
    ): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }
}

/** One contrast rule of the token contract. */
data class SableContrastRule(
    val name: String,
    val foreground: Long,
    val background: Long,
    val minimum: Double,
) {
    val actual: Double
        get() = SableContrast.ratio(foreground, background)

    val passes: Boolean
        get() = actual + 1e-9 >= minimum
}

/**
 * The palette rules from DESIGN-KF-B: readable text (WCAG AA, primary AAA),
 * readable disabled labels, 3:1 non-text contrast for focus, accent and
 * status colors, readable text on accent fills, and a privacy color that is
 * never the danger color.
 */
object SableTokenRules {
    const val TEXT_PRIMARY_MIN = 7.0
    const val TEXT_SECONDARY_MIN = 4.5
    const val TEXT_DISABLED_MIN = 3.0
    const val NON_TEXT_MIN = 3.0
    const val ON_ACCENT_MIN = 4.5

    fun contrastRules(): List<SableContrastRule> {
        val rules = mutableListOf<SableContrastRule>()
        for (dark in listOf(true, false)) {
            val theme = if (dark) "dark" else "light"
            val roles = SableSystemTokens.roles(dark)
            roles.surfaces.forEachIndexed { index, surface ->
                val s = "$theme.surface$index"
                rules += SableContrastRule("$s text.primary", roles.textPrimary, surface, TEXT_PRIMARY_MIN)
                rules += SableContrastRule("$s text.secondary", roles.textSecondary, surface, TEXT_SECONDARY_MIN)
                rules += SableContrastRule("$s text.disabled", roles.textDisabled, surface, TEXT_DISABLED_MIN)
                rules += SableContrastRule("$s success", roles.success, surface, NON_TEXT_MIN)
                rules += SableContrastRule("$s warning", roles.warning, surface, NON_TEXT_MIN)
                rules += SableContrastRule("$s danger", roles.danger, surface, NON_TEXT_MIN)
                rules += SableContrastRule("$s privacy", roles.privacy, surface, NON_TEXT_MIN)
                for (accent in SableAccentTokens.entries) {
                    val a = "$s ${accent.stableValue}"
                    rules += SableContrastRule("$a accent", accent.accent(dark), surface, NON_TEXT_MIN)
                    rules += SableContrastRule("$a focus", accent.focus(dark), surface, NON_TEXT_MIN)
                }
            }
            for (accent in SableAccentTokens.entries) {
                rules +=
                    SableContrastRule(
                        "$theme.${accent.stableValue} onAccent",
                        accent.onAccent(dark),
                        accent.accent(dark),
                        ON_ACCENT_MIN,
                    )
            }
        }
        return rules
    }

    /** Human-readable violations; empty when the palette satisfies the contract. */
    fun violations(): List<String> {
        val out = mutableListOf<String>()
        for (rule in contrastRules()) {
            if (!rule.passes) {
                out += "${rule.name}: ${"%.2f".format(rule.actual)} < ${rule.minimum}"
            }
        }
        for (dark in listOf(true, false)) {
            val roles = SableSystemTokens.roles(dark)
            if (roles.privacy == roles.danger) out += "privacy equals danger (${if (dark) "dark" else "light"})"
        }
        val g = SableGeometryTokens
        if (g.spaces.any { it % g.SPACE_GRID_DP != 0 }) out += "space token off the 4dp grid"
        if (g.ROW_MIN_HEIGHT_DP < 48) out += "row minimum below 48dp"
        if (g.FOCUS_STROKE_DP < 2) out += "focus stroke below 2dp"
        return out
    }
}

internal fun rgbHex(argb: Long): String = "%06X".format(argb and 0xFFFFFF)
