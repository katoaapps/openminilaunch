package org.sableos.design

/*
 * Sable app corner style (pure Kotlin: no android.* or Compose imports, so the
 * codec and the shape table are unit-tested on the JVM). SableTheme turns the
 * table into Material Shapes; SableGlobalAppearance reads the style from
 * content://org.sableos.appearance/appearance (column corner_style).
 */

/**
 * Corner style for Sable apps, chosen by the user in App display compatibility >
 * Sable app style and published through org.sableos.appearance (column corner_style).
 * Compact is the default, so nothing changes until the user picks Rounded.
 */
enum class SableCornerStyle(
    val stableValue: String,
    val displayName: String,
) {
    Compact("compact", "Compact"),
    Rounded("rounded", "Rounded"),
    ;

    companion object {
        val DEFAULT: SableCornerStyle = Compact

        fun fromStableValue(value: String?): SableCornerStyle = entries.firstOrNull { it.stableValue == value } ?: DEFAULT
    }
}

/** Corner radii in dp for the five Material shape roles. */
data class SableShapeScale(
    val extraSmallDp: Int,
    val smallDp: Int,
    val mediumDp: Int,
    val largeDp: Int,
    val extraLargeDp: Int,
) {
    val radiiDp: List<Int>
        get() = listOf(extraSmallDp, smallDp, mediumDp, largeDp, extraLargeDp)

    val maxDp: Int
        get() = radiiDp.max()
}

/**
 * The shape table per corner style. Pure, so it is unit-tested on the JVM.
 *
 * Compact is the original Sable app geometry (2-6dp). Rounded is the DESIGN-KF-B
 * geometry shared with SystemUI: 8dp controls (extraSmall, small) and 12dp cards,
 * sheets and dialogs (medium, large, extraLarge); these equal
 * SableGeometryTokens.SMALL_CONTROL_RADIUS_DP and CARD_RADIUS_DP, which
 * SableSystemTokensTest and tests/check-sable-design.py cross-check.
 */
object SableShapeTable {
    val Compact = SableShapeScale(extraSmallDp = 2, smallDp = 3, mediumDp = 4, largeDp = 6, extraLargeDp = 6)
    val Rounded = SableShapeScale(extraSmallDp = 8, smallDp = 8, mediumDp = 12, largeDp = 12, extraLargeDp = 12)

    fun forStyle(style: SableCornerStyle): SableShapeScale =
        when (style) {
            SableCornerStyle.Compact -> Compact
            SableCornerStyle.Rounded -> Rounded
        }
}
