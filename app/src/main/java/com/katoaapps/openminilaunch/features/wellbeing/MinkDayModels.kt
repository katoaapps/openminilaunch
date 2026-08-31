package com.katoaapps.openminilaunch.features.wellbeing

import android.content.Context
import com.katoaapps.openminilaunch.R
import java.time.Instant
import java.time.ZoneId

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
                headline = context.getString(
                    if (hour >= 22 || hour < 5) R.string.mink_made_it_home else R.string.mink_checking_trail,
                ),
                detail = context.getString(R.string.mink_loading_detail),
                isLoading = true,
            )
        }

        fun unavailable(
            context: Context,
            accessGranted: Boolean,
            nowMillis: Long = System.currentTimeMillis(),
        ): MinkDaySummary {
            val hour = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).hour
            return MinkDaySummary(
                accessGranted = accessGranted,
                state = if (hour >= 22 || hour < 5) MinkState.SLEEPING else MinkState.WALKING,
                headline = context.getString(R.string.mink_lost_trail),
                detail = context.getString(R.string.mink_unavailable_detail),
                errorMessage = context.getString(
                    R.string.mink_unavailable_error,
                    context.getString(R.string.app_name),
                ),
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
