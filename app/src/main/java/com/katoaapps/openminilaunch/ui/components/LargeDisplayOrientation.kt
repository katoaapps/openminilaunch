package com.katoaapps.openminilaunch.ui.components

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration

private const val LARGE_DISPLAY_MIN_SMALLEST_WIDTH_DP = 600

/** Lets opted-in large screens follow the user's rotation setting; keeps phone Home portrait. */
internal fun applyLargeDisplayOrientation(activity: Activity, enabled: Boolean) {
    val orientation = preferredActivityOrientation(activity.resources.configuration, enabled)
    if (activity.requestedOrientation != orientation) activity.requestedOrientation = orientation
}

internal fun preferredActivityOrientation(configuration: Configuration, enabled: Boolean): Int =
    preferredActivityOrientation(configuration.smallestScreenWidthDp, enabled)

internal fun preferredActivityOrientation(smallestWidthDp: Int, enabled: Boolean): Int =
    if (enabled && smallestWidthDp >= LARGE_DISPLAY_MIN_SMALLEST_WIDTH_DP) {
        ActivityInfo.SCREEN_ORIENTATION_USER
    } else {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
