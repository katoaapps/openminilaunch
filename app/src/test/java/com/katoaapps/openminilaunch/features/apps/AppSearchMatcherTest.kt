package com.katoaapps.openminilaunch.features.apps

import android.content.ComponentName
import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.model.LauncherTarget
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchMatcherTest {
    private val publisherLabels = mutableMapOf<String, String>()
    private val matcher = AppSearchMatcher(
        packageLabel = { packageName -> publisherLabels[packageName].orEmpty() },
        locale = Locale.US,
    )

    @Test fun normalizesSpacingPunctuationAccentsAndTurkishI() {
        assertEquals("cafe music", normalizeAppSearchText("  Café---Music  ").value)
        assertEquals("istanbul", normalizeAppSearchText("İSTANBUL").value)
        assertEquals("istanbul", normalizeAppSearchText("ıstanbul").value)
    }

    @Test fun preservesExactAndPrefixPriority() {
        val exact = app("You")
        val prefix = app("YouTube")

        val results = matcher.rank(listOf(prefix, exact), "you")

        assertEquals(listOf(exact, prefix), results.map(AppSearchMatch::target))
        assertEquals(listOf(AppSearchTier.EXACT, AppSearchTier.PREFIX), results.map(AppSearchMatch::tier))
    }

    @Test fun matchesWordsAndOrderedTokenPrefixes() {
        assertEquals(AppSearchTier.WORD_PREFIX, matcher.match(app("Google Maps"), "map")?.tier)
        assertEquals(AppSearchTier.TOKEN_PREFIX, matcher.match(app("Android Studio"), "and stu")?.tier)
    }

    @Test fun matchesCamelCaseInitialsAndConservativeSubsequences() {
        assertEquals(AppSearchTier.INITIALISM, matcher.match(app("YouTube Music"), "ytm")?.tier)
        assertEquals(AppSearchTier.INITIALISM, matcher.match(app("Google Maps"), "gm")?.tier)
        assertEquals(AppSearchTier.SUBSEQUENCE, matcher.match(app("Google Maps"), "gmap")?.tier)
    }

    @Test fun matchesSmallTyposButDoesNotFuzzVeryShortQueries() {
        assertEquals(AppSearchTier.TYPO, matcher.match(app("Spotify"), "spotfy")?.tier)
        assertEquals(AppSearchTier.TYPO, matcher.match(app("Spotify Music"), "spotfy")?.tier)
        assertNull(matcher.match(app("Set"), "st"))
        assertNull(matcher.match(app("Calendar"), "cld"))
    }

    @Test fun matchesAccentedLabelsWithoutChangingTheirDisplayText() {
        val result = matcher.match(app("Café"), "cafe")

        assertEquals(AppSearchTier.EXACT, result?.tier)
        assertEquals("Café", result?.matchedText)
    }

    @Test fun shortcutsCanMatchTheirOwnLabelOrPublisher() {
        publisherLabels["com.maps"] = "Google Maps"
        val shortcut = shortcut("Search", packageName = "com.maps")

        val ownLabel = matcher.match(shortcut, "sea")
        val publisher = matcher.match(shortcut, "google")

        assertEquals(AppSearchTier.PREFIX, ownLabel?.tier)
        assertEquals("Search", ownLabel?.matchedText)
        assertEquals(AppSearchTier.PREFIX, publisher?.tier)
        assertEquals("Google Maps", publisher?.matchedText)
    }

    @Test fun rankingIsStableWhenVisibleLabelsHaveEqualQuality() {
        val work = app("Maps", key = "work", isWorkProfile = true)
        val personal = app("Maps", key = "personal")
        val laterKey = app("Maps", key = "z-key")

        val results = matcher.rank(listOf(work, laterKey, personal), "maps")

        assertEquals(listOf(personal, laterKey, work), results.map(AppSearchMatch::target))
    }

    @Test fun mixedScriptsDoNotBecomeBroadMatches() {
        val result = matcher.match(app("東京 Maps"), "東京")

        assertEquals(AppSearchTier.PREFIX, result?.tier)
        assertTrue(matcher.rank(listOf(app("Maps"), app("Music")), "東京").isEmpty())
    }

    private fun app(
        label: String,
        key: String = label,
        isWorkProfile: Boolean = false,
    ): LauncherTarget = LauncherAppTarget(
        label = label,
        packageName = "com.example.${key.lowercase(Locale.ROOT)}",
        componentName = ComponentName("com.example", "Activity"),
        userSerial = if (isWorkProfile) 10 else 0,
        isWorkProfile = isWorkProfile,
        selectionKey = key,
    )

    private fun shortcut(
        label: String,
        packageName: String,
    ): LauncherTarget = LauncherShortcutTarget(
        label = label,
        packageName = packageName,
        shortcutId = label,
        userSerial = 0,
        isWorkProfile = false,
        selectionKey = "shortcut:$packageName:$label",
    )
}
