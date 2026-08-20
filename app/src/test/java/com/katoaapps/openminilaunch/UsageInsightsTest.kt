package com.katoaapps.openminilaunch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageInsightsTest {
    @Test
    fun automaticAndCustomSocialSelectionsRemainDistinct() {
        val defaults = setOf("social.chat", "social.photos")

        assertEquals(defaults, effectiveTrackedPackages(emptySet(), defaults, usesAutomaticSocialApps = true))
        assertEquals(
            setOf("social.custom"),
            effectiveTrackedPackages(setOf("social.custom"), defaults, usesAutomaticSocialApps = false),
        )
        assertEquals(emptySet<String>(), effectiveTrackedPackages(emptySet(), defaults, usesAutomaticSocialApps = false))
    }

    @Test
    fun trackedSessionCrossingMidnightIsClampedToToday() {
        val analysis = analyzeUsageTimeline(
            dayStart = 1_000L,
            now = 5_000L,
            events = listOf(
                event(500L, "social.reader", UsageEventKind.FOREGROUND),
                event(2_000L, "social.reader", UsageEventKind.BACKGROUND),
                event(2_100L, "social.chat", UsageEventKind.FOREGROUND),
                event(3_100L, "social.chat", UsageEventKind.BACKGROUND),
            ),
            trackedPackages = setOf("social.reader", "social.chat"),
            ignoredPackages = emptySet(),
        )
        assertEquals(1_000L, analysis.packageDurations["social.reader"])
        assertEquals(1_000L, analysis.packageDurations["social.chat"])
        assertEquals(1, analysis.opensToday)
    }

    @Test
    fun longestSessionIsNotCumulativeUsage() {
        val analysis = analyzeUsageTimeline(
            dayStart = 0L,
            now = 10_000L,
            events = listOf(
                event(1_000L, "social.chat", UsageEventKind.FOREGROUND),
                event(3_000L, "social.chat", UsageEventKind.BACKGROUND),
                event(5_000L, "social.chat", UsageEventKind.FOREGROUND),
                event(8_000L, "social.chat", UsageEventKind.BACKGROUND),
            ),
            trackedPackages = setOf("social.chat"),
            ignoredPackages = emptySet(),
        )
        assertEquals(5_000L, analysis.packageDurations["social.chat"])
        assertEquals(3_000L, analysis.longestSessions["social.chat"])
        assertEquals(2, analysis.opensToday)
    }

    @Test
    fun duplicateForegroundEventsDoNotResetOrCreateFakeOpens() {
        val analysis = analyzeUsageTimeline(
            dayStart = 0L,
            now = 5_000L,
            events = listOf(
                event(1_000L, "social.chat", UsageEventKind.FOREGROUND),
                event(1_100L, "social.chat", UsageEventKind.FOREGROUND),
                event(4_000L, "social.chat", UsageEventKind.BACKGROUND),
            ),
            trackedPackages = setOf("social.chat"),
            ignoredPackages = emptySet(),
        )
        assertEquals(3_000L, analysis.packageDurations["social.chat"])
        assertEquals(1, analysis.opensToday)
    }

    @Test
    fun nonTrackedForegroundEndsTrackedSessionAndNeverAppears() {
        val analysis = analyzeUsageTimeline(
            dayStart = 0L,
            now = 20_000L,
            events = listOf(
                event(1_000L, "social.chat", UsageEventKind.FOREGROUND),
                // Android does not always provide the matching PAUSED event.
                event(4_000L, "maps", UsageEventKind.FOREGROUND),
            ),
            trackedPackages = setOf("social.chat"),
            ignoredPackages = emptySet(),
        )
        assertEquals(3_000L, analysis.packageDurations["social.chat"])
        assertNull(analysis.packageDurations["maps"])
        assertEquals(1, analysis.opensToday)
    }

    @Test
    fun ignoredSystemSurfaceDoesNotInterruptTrackedSession() {
        val analysis = analyzeUsageTimeline(
            dayStart = 0L,
            now = 5_000L,
            events = listOf(
                event(1_000L, "social.chat", UsageEventKind.FOREGROUND),
                event(2_000L, "systemui", UsageEventKind.FOREGROUND),
                event(2_500L, "social.chat", UsageEventKind.FOREGROUND),
                event(4_000L, "social.chat", UsageEventKind.BACKGROUND),
            ),
            trackedPackages = setOf("social.chat"),
            ignoredPackages = setOf("systemui"),
        )
        assertEquals(3_000L, analysis.packageDurations["social.chat"])
        assertEquals(1, analysis.opensToday)
    }

    @Test
    fun screenOffEndsTrackedSession() {
        val analysis = analyzeUsageTimeline(
            dayStart = 0L,
            now = 10_000L,
            events = listOf(
                event(1_000L, "social.chat", UsageEventKind.FOREGROUND),
                event(4_000L, null, UsageEventKind.SCREEN_OFF),
            ),
            trackedPackages = setOf("social.chat"),
            ignoredPackages = emptySet(),
        )
        assertEquals(3_000L, analysis.packageDurations["social.chat"])
    }

    @Test
    fun onlyRecentTrackedOpensCountTowardLastHour() {
        val hour = 60 * MINUTE
        val analysis = analyzeUsageTimeline(
            dayStart = 0L,
            now = 3 * hour,
            events = listOf(
                event(30 * MINUTE, "social.chat", UsageEventKind.FOREGROUND),
                event(31 * MINUTE, "social.chat", UsageEventKind.BACKGROUND),
                event(150 * MINUTE, "social.chat", UsageEventKind.FOREGROUND),
                event(151 * MINUTE, "social.chat", UsageEventKind.BACKGROUND),
            ),
            trackedPackages = setOf("social.chat"),
            ignoredPackages = emptySet(),
        )
        assertEquals(2, analysis.opensToday)
        assertEquals(1, analysis.opensLastHour)
    }

    @Test
    fun stateIsDrivenOnlyByTrackedSocialBehavior() {
        assertEquals(MinkState.PURPOSEFUL, chooseMinkState(14, 0, 60, 0, 0))
        assertEquals(MinkState.PHONE, chooseMinkState(14, 60 * MINUTE, 60, 0, 0))
        assertEquals(MinkState.DISTRACTED, chooseMinkState(14, 5 * MINUTE, 60, 8, 0))
        assertEquals(MinkState.RESTING, chooseMinkState(14, 20 * MINUTE, 60, 2, 30 * MINUTE))
        assertEquals(MinkState.WALKING, chooseMinkState(14, 5 * MINUTE, 60, 2, 5 * MINUTE))
    }

    @Test
    fun attentionIsReservedForActionableOrFailedInsights() {
        assertTrue(summary(MinkState.PHONE).needsAttention())
        assertTrue(summary(MinkState.DISTRACTED).needsAttention())
        assertTrue(summary(MinkState.RESTING).needsAttention())
        assertFalse(summary(MinkState.PURPOSEFUL).needsAttention())
        assertFalse(summary(MinkState.WALKING, accessGranted = false).needsAttention())
        assertTrue(summary(MinkState.WALKING, errorMessage = "Unavailable").needsAttention())
    }

    private fun summary(
        state: MinkState,
        accessGranted: Boolean = true,
        errorMessage: String? = null,
    ) = MinkDaySummary(
        accessGranted = accessGranted,
        state = state,
        headline = "Headline",
        detail = "Detail",
        errorMessage = errorMessage,
    )

    private fun event(timestamp: Long, packageName: String?, kind: UsageEventKind) =
        UsageTimelineEvent(timestamp, packageName, kind)

    private companion object {
        const val MINUTE = 60_000L
    }
}
