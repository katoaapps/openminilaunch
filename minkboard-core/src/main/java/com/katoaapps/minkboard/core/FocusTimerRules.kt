package com.katoaapps.minkboard.core

object FocusTimerRules {
    const val DEFAULT_DURATION_MILLIS: Long = 5L * 60L * 1_000L
    const val MIN_DURATION_MILLIS: Long = 1_000L
    const val MAX_DURATION_MILLIS: Long = ((24L * 60L * 60L) - 1L) * 1_000L

    fun durationMillis(value: Long): Long {
        require(value in MIN_DURATION_MILLIS..MAX_DURATION_MILLIS) {
            "Focus duration must be between one second and 23:59:59"
        }
        return value
    }

    fun durationMillis(hours: Int, minutes: Int, seconds: Int): Long {
        require(hours in 0..23) { "Hours must be between 0 and 23" }
        require(minutes in 0..59) { "Minutes must be between 0 and 59" }
        require(seconds in 0..59) { "Seconds must be between 0 and 59" }
        return durationMillis(
            ((hours * 60L * 60L) + (minutes * 60L) + seconds) * 1_000L,
        )
    }
}
