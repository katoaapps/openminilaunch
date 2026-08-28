package com.katoaapps.openminilaunch.features.wellbeing

import com.katoaapps.openminilaunch.model.MinkAppPauseMode

internal data class MinkAppAccessState(
    val mode: MinkAppPauseMode,
    val trackedPackages: Set<String>,
    val collectiveUsageMillis: Long,
    val dailyLimitMinutes: Int,
    val usageAccessGranted: Boolean,
    val isResolved: Boolean,
) {
    fun isPaused(packageName: String): Boolean = shouldPauseMinkApp(
        packageName = packageName,
        trackedPackages = trackedPackages,
        mode = mode,
        collectiveUsageMillis = collectiveUsageMillis,
        dailyLimitMinutes = dailyLimitMinutes,
        usageAccessGranted = usageAccessGranted,
    )
}

internal fun shouldPauseMinkApp(
    packageName: String,
    trackedPackages: Set<String>,
    mode: MinkAppPauseMode,
    collectiveUsageMillis: Long,
    dailyLimitMinutes: Int,
    usageAccessGranted: Boolean,
): Boolean {
    if (packageName !in trackedPackages) return false
    return when (mode) {
        MinkAppPauseMode.ALWAYS -> true
        MinkAppPauseMode.AFTER_DAILY_LIMIT -> usageAccessGranted &&
            collectiveUsageMillis >= dailyLimitMinutes.coerceAtLeast(0) * 60_000L
        MinkAppPauseMode.NEVER -> false
    }
}
