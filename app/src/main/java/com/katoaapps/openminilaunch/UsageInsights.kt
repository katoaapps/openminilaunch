package com.katoaapps.openminilaunch

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

internal enum class MinkState { WALKING, PURPOSEFUL, PHONE, DISTRACTED, RESTING, SLEEPING }

internal enum class UsageEventKind { FOREGROUND, BACKGROUND, SCREEN_ON, SCREEN_OFF }

internal data class UsageTimelineEvent(
    val timestamp: Long,
    val packageName: String?,
    val kind: UsageEventKind,
)

internal data class UsageTimelineAnalysis(
    val packageDurations: Map<String, Long>,
    val longestSessions: Map<String, Long>,
    val opensToday: Int,
    val opensLastHour: Int,
)

internal data class MinkAppUsage(
    val packageName: String,
    val label: String,
    val foregroundMillis: Long,
)

internal data class MinkDaySummary(
    val accessGranted: Boolean,
    val state: MinkState,
    val socialMillis: Long = 0,
    val socialOpensToday: Int = 0,
    val socialOpensLastHour: Int = 0,
    val topApps: List<MinkAppUsage> = emptyList(),
    val headline: String,
    val detail: String,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    companion object {
        fun loading(context: Context, nowMillis: Long = System.currentTimeMillis()): MinkDaySummary {
            val hour = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).hour
            return MinkDaySummary(
                accessGranted = false,
                state = if (hour >= 22 || hour < 5) MinkState.SLEEPING else MinkState.WALKING,
                headline = context.getString(if (hour >= 22 || hour < 5) R.string.mink_made_it_home else R.string.mink_checking_trail),
                detail = context.getString(R.string.mink_loading_detail),
                isLoading = true,
            )
        }

        fun unavailable(context: Context, accessGranted: Boolean, nowMillis: Long = System.currentTimeMillis()): MinkDaySummary {
            val hour = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).hour
            return MinkDaySummary(
                accessGranted = accessGranted,
                state = if (hour >= 22 || hour < 5) MinkState.SLEEPING else MinkState.WALKING,
                headline = context.getString(R.string.mink_lost_trail),
                detail = context.getString(R.string.mink_unavailable_detail),
                errorMessage = context.getString(R.string.mink_unavailable_error, context.getString(R.string.app_name)),
            )
        }
    }
}

internal fun effectiveTrackedPackages(
    selectedPackages: Set<String>,
    androidSocialPackages: Set<String>,
    usesAutomaticSocialApps: Boolean,
): Set<String> = if (usesAutomaticSocialApps) androidSocialPackages else selectedPackages

internal fun MinkDaySummary.needsAttention(): Boolean = errorMessage != null || accessGranted && when (state) {
    MinkState.PHONE, MinkState.DISTRACTED, MinkState.RESTING -> true
    MinkState.WALKING, MinkState.PURPOSEFUL, MinkState.SLEEPING -> false
}

internal fun analyzeUsageTimeline(
    dayStart: Long,
    now: Long,
    events: List<UsageTimelineEvent>,
    trackedPackages: Set<String>,
    ignoredPackages: Set<String>,
): UsageTimelineAnalysis {
    if (now <= dayStart) return UsageTimelineAnalysis(emptyMap(), emptyMap(), 0, 0)
    val sorted = events.asSequence().filter { it.timestamp <= now }.sortedBy(UsageTimelineEvent::timestamp).toList()
    val durations = mutableMapOf<String, Long>()
    val longest = mutableMapOf<String, Long>()
    val trackedOpens = mutableListOf<Long>()
    var activePackage: String? = null
    var activeSince: Long? = null

    fun closeActive(at: Long) {
        val packageName = activePackage
        val began = activeSince
        if (packageName != null && began != null) addSession(packageName, began, at, durations, longest)
        activePackage = null
        activeSince = null
    }

    sorted.asSequence().filter { it.timestamp < dayStart }.forEach { event ->
        when (event.kind) {
            UsageEventKind.SCREEN_OFF -> closeActive(dayStart)
            UsageEventKind.SCREEN_ON -> Unit
            UsageEventKind.FOREGROUND -> event.packageName?.takeUnless { it in ignoredPackages }?.let { packageName ->
                activePackage = packageName.takeIf { it in trackedPackages }
                activeSince = activePackage?.let { dayStart }
            }
            UsageEventKind.BACKGROUND -> if (event.packageName == activePackage) closeActive(dayStart)
        }
    }

    sorted.asSequence().filter { it.timestamp >= dayStart }.forEach { event ->
        val eventTime = event.timestamp.coerceAtMost(now)
        when (event.kind) {
            UsageEventKind.SCREEN_ON -> Unit
            UsageEventKind.SCREEN_OFF -> closeActive(eventTime)
            UsageEventKind.FOREGROUND -> {
                val packageName = event.packageName ?: return@forEach
                if (packageName in ignoredPackages) return@forEach
                if (packageName != activePackage) {
                    closeActive(eventTime)
                    if (packageName in trackedPackages) {
                        activePackage = packageName
                        activeSince = eventTime
                        trackedOpens += eventTime
                    }
                }
            }
            UsageEventKind.BACKGROUND -> {
                val packageName = event.packageName ?: return@forEach
                if (packageName == activePackage) closeActive(eventTime)
            }
        }
    }
    closeActive(now)
    val lastHourStart = now - 3_600_000L
    return UsageTimelineAnalysis(
        packageDurations = durations,
        longestSessions = longest,
        opensToday = trackedOpens.size,
        opensLastHour = trackedOpens.count { it >= lastHourStart },
    )
}

private fun addSession(
    packageName: String,
    began: Long,
    ended: Long,
    durations: MutableMap<String, Long>,
    longest: MutableMap<String, Long>,
) {
    if (ended <= began) return
    val duration = ended - began
    durations[packageName] = (durations[packageName] ?: 0L) + duration
    longest[packageName] = maxOf(longest[packageName] ?: 0L, duration)
}

internal fun chooseMinkState(
    hour: Int,
    socialMillis: Long,
    socialGoalMinutes: Int,
    socialOpensLastHour: Int,
    longestSocialSession: Long,
): MinkState = when {
    hour >= 22 || hour < 5 -> MinkState.SLEEPING
    socialMillis >= socialGoalMinutes * 60_000L -> MinkState.PHONE
    socialOpensLastHour >= 8 -> MinkState.DISTRACTED
    longestSocialSession >= 30 * 60_000L -> MinkState.RESTING
    socialMillis == 0L -> MinkState.PURPOSEFUL
    else -> MinkState.WALKING
}

internal class UsageInsightsRepository(private val context: Context) {
    private val usageStats = context.getSystemService(UsageStatsManager::class.java)
    private val packageManager = context.packageManager
    private val labelCache = ConcurrentHashMap<String, String>()
    private val socialCategoryCache = ConcurrentHashMap<String, Boolean>()
    private val homePackages: Set<String> by lazy {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        packageManager.queryIntentActivities(homeIntent, 0).mapTo(mutableSetOf()) { it.activityInfo.packageName }
    }

    fun hasAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun accessSettingsIntent(): Intent {
        val usageIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        return if (usageIntent.resolveActivity(packageManager) != null) usageIntent else Intent(Settings.ACTION_SETTINGS)
    }

    fun summary(
        socialPackages: Set<String>,
        usesAutomaticSocialApps: Boolean,
        socialGoalMinutes: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): MinkDaySummary {
        val zone = ZoneId.systemDefault()
        val hour = Instant.ofEpochMilli(nowMillis).atZone(zone).hour
        if (!hasAccess()) return noAccessSummary(hour)
        val dayStart = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val events = readTimelineEvents(dayStart - EVENT_LOOKBACK_MILLIS, nowMillis)
        val observedPackages = events.mapNotNull(UsageTimelineEvent::packageName).toSet()
        val automaticPackages = observedPackages.filterTo(mutableSetOf()) { isSocial(it, emptySet()) }
        val trackedPackages = effectiveTrackedPackages(socialPackages, automaticPackages, usesAutomaticSocialApps)
        val ignored = observedPackages.filterTo(mutableSetOf(), ::isIgnoredPackage)
        val analysis = analyzeUsageTimeline(
            dayStart = dayStart,
            now = nowMillis,
            events = events,
            trackedPackages = trackedPackages,
            ignoredPackages = ignored,
        )
        val allApps = analysis.packageDurations.map { (packageName, duration) ->
            MinkAppUsage(packageName, appLabel(packageName), duration)
        }.sortedByDescending(MinkAppUsage::foregroundMillis)
        val socialMillis = allApps.sumOf(MinkAppUsage::foregroundMillis)
        val longestSocial = allApps.maxByOrNull { analysis.longestSessions[it.packageName] ?: 0L }
        val longestSocialMillis = longestSocial?.let { analysis.longestSessions[it.packageName] } ?: 0L
        val state = chooseMinkState(
            hour = hour,
            socialMillis = socialMillis,
            socialGoalMinutes = socialGoalMinutes,
            socialOpensLastHour = analysis.opensLastHour,
            longestSocialSession = longestSocialMillis,
        )
        val (headline, detail) = stateCopy(
            state = state,
            socialMillis = socialMillis,
            socialGoalMinutes = socialGoalMinutes,
            socialOpensLastHour = analysis.opensLastHour,
            longestSocial = longestSocial,
            longestSocialMillis = longestSocialMillis,
            topSocial = allApps.firstOrNull { it.foregroundMillis >= DISPLAY_THRESHOLD_MILLIS },
        )
        return MinkDaySummary(
            accessGranted = true,
            state = state,
            socialMillis = socialMillis,
            socialOpensToday = analysis.opensToday,
            socialOpensLastHour = analysis.opensLastHour,
            topApps = allApps.filter { it.foregroundMillis >= DISPLAY_THRESHOLD_MILLIS }.take(5),
            headline = headline,
            detail = detail,
        )
    }

    fun launchableApps(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, 0)
            .map { LaunchableApp(it.loadLabel(packageManager).toString(), it.activityInfo.packageName) }
            .distinctBy(LaunchableApp::packageName)
            .sortedBy { it.label.lowercase() }
    }

    fun automaticSocialPackages(apps: List<LaunchableApp>): Set<String> = apps.asSequence()
        .map(LaunchableApp::packageName)
        .filter { isSocial(it, emptySet()) }
        .toSet()

    private fun readTimelineEvents(begin: Long, end: Long): List<UsageTimelineEvent> {
        val result = mutableListOf<UsageTimelineEvent>()
        val event = UsageEvents.Event()
        usageStats.queryEvents(begin, end).let { events ->
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                // RESUMED/PAUSED retain the same event values used by the pre-29
                // MOVE_TO_FOREGROUND/BACKGROUND names, so one branch covers minSdk 26+.
                val kind = when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> UsageEventKind.FOREGROUND
                    UsageEvents.Event.ACTIVITY_PAUSED -> UsageEventKind.BACKGROUND
                    UsageEvents.Event.SCREEN_INTERACTIVE -> UsageEventKind.SCREEN_ON
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageEventKind.SCREEN_OFF
                    else -> null
                }
                if (kind != null) result += UsageTimelineEvent(event.timeStamp, event.packageName, kind)
            }
        }
        return result
    }

    private fun noAccessSummary(hour: Int) = MinkDaySummary(
        accessGranted = false,
        state = if (hour >= 22 || hour < 5) MinkState.SLEEPING else MinkState.WALKING,
        headline = context.getString(if (hour >= 22 || hour < 5) R.string.mink_made_it_home else R.string.mink_ready_for_day),
        detail = context.getString(R.string.mink_enable_usage_detail),
    )

    private fun stateCopy(
        state: MinkState,
        socialMillis: Long,
        socialGoalMinutes: Int,
        socialOpensLastHour: Int,
        longestSocial: MinkAppUsage?,
        longestSocialMillis: Long,
        topSocial: MinkAppUsage?,
    ): Pair<String, String> = when (state) {
        MinkState.SLEEPING -> context.getString(R.string.mink_made_it_home) to context.getString(R.string.mink_sleeping_detail)
        MinkState.PHONE -> context.getString(R.string.mink_stopped_to_scroll) to if (topSocial != null) {
            context.getString(R.string.mink_top_app_goal_detail, topSocial.label, formatDuration(context, socialMillis), socialGoalMinutes)
        } else context.getString(R.string.mink_goal_passed_detail, socialGoalMinutes)
        MinkState.DISTRACTED -> context.getString(R.string.mink_checking_headline) to
            context.resources.getQuantityString(
                R.plurals.mink_opens_detail,
                socialOpensLastHour,
                socialOpensLastHour,
            )
        MinkState.RESTING -> context.getString(R.string.mink_pause_headline) to if (longestSocial != null) {
            context.getString(R.string.mink_longest_visit_detail, formatDuration(context, longestSocialMillis), longestSocial.label)
        } else context.getString(R.string.mink_break_detail)
        MinkState.PURPOSEFUL -> context.getString(R.string.mink_quiet_trail_headline) to context.getString(R.string.mink_no_social_detail)
        MinkState.WALKING -> context.getString(R.string.mink_moving_headline) to if (topSocial == null) {
            context.getString(R.string.mink_not_enough_detail)
        } else context.getString(R.string.mink_leading_detail, topSocial.label, formatDuration(context, topSocial.foregroundMillis))
    }

    private fun isSocial(packageName: String, selected: Set<String>): Boolean {
        if (packageName in selected) return true
        if (selected.isNotEmpty()) return false
        return socialCategoryCache.getOrPut(packageName) {
            val info = runCatching { packageManager.getApplicationInfo(packageName, 0) }.getOrNull()
            info?.category == ApplicationInfo.CATEGORY_SOCIAL
        }
    }

    private fun appLabel(packageName: String): String = labelCache.getOrPut(packageName) {
        runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName.substringAfterLast('.'))
    }

    private fun isIgnoredPackage(packageName: String): Boolean = packageName == context.packageName ||
        packageName == "com.android.systemui" ||
        packageName.contains("permissioncontroller", ignoreCase = true) ||
        packageName in homePackages ||
        packageName.contains("launcher", ignoreCase = true)

    private companion object {
        const val EVENT_LOOKBACK_MILLIS = 24 * 60 * 60_000L
        const val DISPLAY_THRESHOLD_MILLIS = 60_000L
    }
}

internal fun formatDuration(context: Context, millis: Long): String {
    val minutes = (millis / 60_000L).coerceAtLeast(0)
    val hours = minutes / 60
    val remaining = minutes % 60
    return when {
        hours > 0 && remaining > 0 -> context.getString(R.string.duration_hours_minutes, hours, remaining)
        hours > 0 -> context.getString(R.string.hours_short, hours)
        else -> context.getString(R.string.minutes_short, minutes)
    }
}
