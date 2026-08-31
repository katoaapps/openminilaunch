package com.katoaapps.openminilaunch.features.wellbeing

/** Reduces Android usage events to app durations and open counts for one day. */
internal fun analyzeUsageTimeline(
    dayStart: Long,
    now: Long,
    events: List<UsageTimelineEvent>,
    trackedPackages: Set<String>,
    ignoredPackages: Set<String>,
): UsageTimelineAnalysis {
    if (now <= dayStart) return UsageTimelineAnalysis(emptyMap(), emptyMap(), 0, 0)
    val sorted = events.asSequence()
        .filter { it.timestamp <= now }
        .sortedBy(UsageTimelineEvent::timestamp)
        .toList()
    val durations = mutableMapOf<String, Long>()
    val longest = mutableMapOf<String, Long>()
    val trackedOpens = mutableListOf<Long>()
    var activePackage: String? = null
    var activeSince: Long? = null

    fun closeActive(at: Long) {
        val packageName = activePackage
        val began = activeSince
        if (packageName != null && began != null) {
            addSession(packageName, began, at, durations, longest)
        }
        activePackage = null
        activeSince = null
    }

    sorted.asSequence().filter { it.timestamp < dayStart }.forEach { event ->
        when (event.kind) {
            UsageEventKind.SCREEN_OFF -> closeActive(dayStart)
            UsageEventKind.SCREEN_ON -> Unit
            UsageEventKind.FOREGROUND -> event.packageName
                ?.takeUnless { it in ignoredPackages }
                ?.let { packageName ->
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
