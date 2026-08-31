package com.katoaapps.openminilaunch.model

import androidx.annotation.StringRes
import com.katoaapps.openminilaunch.R

enum class MinkAppPauseMode(
    @param:StringRes val labelRes: Int,
    @param:StringRes val descriptionRes: Int,
) {
    ALWAYS(R.string.pause_mode_always, R.string.pause_mode_always_description),
    AFTER_DAILY_LIMIT(R.string.pause_mode_after_limit, R.string.pause_mode_after_limit_description),
    NEVER(R.string.pause_mode_never, R.string.pause_mode_never_description),
}

internal const val MIN_SOCIAL_GOAL_HOURS = 0
internal const val MAX_SOCIAL_GOAL_HOURS = 23
