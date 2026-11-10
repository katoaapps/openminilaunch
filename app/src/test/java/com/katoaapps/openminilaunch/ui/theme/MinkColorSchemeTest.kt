package com.katoaapps.openminilaunch.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MinkColorSchemeTest {
    @Test
    fun lightSchemeContentPairsMeetTextContrast() {
        assertContentContrast(MinkLightColorScheme)
    }

    @Test
    fun darkSchemeContentPairsMeetTextContrast() {
        assertContentContrast(MinkDarkColorScheme)
    }

    @Test
    fun customBackgroundRecalculatesReadableSurfaceContent() {
        listOf(Color(0xFFF7E7CE), Color(0xFF120B35)).forEach { background ->
            val scheme = MinkLightColorScheme.withAppBackground(background.toArgb())

            assertTrue(contrastRatio(scheme.background, scheme.onBackground) >= 4.5f)
            assertTrue(contrastRatio(scheme.surface, scheme.onSurface) >= 4.5f)
        }
    }

    @Test
    fun customBackgroundKeepsInteractiveAccentsVisibleWithoutChangingSemanticError() {
        listOf(
            MinkLightColorScheme to Color(0xFF101512),
            MinkDarkColorScheme to Color(0xFFF4F1E8),
        ).forEach { (base, background) ->
            val customized = base.withAppBackground(background.toArgb())

            assertTrue(contrastRatio(background, customized.primary) >= 3f)
            assertTrue(contrastRatio(background, customized.secondary) >= 3f)
            assertTrue(contrastRatio(background, customized.tertiary) >= 3f)
            assertTrue(contrastRatio(customized.primary, customized.onPrimary) >= 4.5f)
            assertEquals(base.error, customized.error)
        }
    }

    private fun assertContentContrast(scheme: ColorScheme) {
        val pairs = listOf(
            scheme.primary to scheme.onPrimary,
            scheme.primaryContainer to scheme.onPrimaryContainer,
            scheme.secondary to scheme.onSecondary,
            scheme.secondaryContainer to scheme.onSecondaryContainer,
            scheme.tertiary to scheme.onTertiary,
            scheme.tertiaryContainer to scheme.onTertiaryContainer,
            scheme.error to scheme.onError,
            scheme.errorContainer to scheme.onErrorContainer,
            scheme.background to scheme.onBackground,
            scheme.surface to scheme.onSurface,
            scheme.surfaceVariant to scheme.onSurfaceVariant,
            scheme.inverseSurface to scheme.inverseOnSurface,
        )
        pairs.forEach { (background, content) ->
            assertTrue(
                "Expected ${background.value} and ${content.value} to meet text contrast",
                contrastRatio(background, content) >= 4.5f,
            )
        }
    }
}
