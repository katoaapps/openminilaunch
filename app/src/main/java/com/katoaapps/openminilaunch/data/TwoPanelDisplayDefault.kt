package com.katoaapps.openminilaunch.data

import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.view.Surface
import android.view.WindowManager

/** Opt in a fresh large display if it is landscape now or landscape in its natural orientation. */
@Suppress("DEPRECATION") // Display size and rotation are paired here to identify natural orientation, not size UI.
internal fun defaultTwoPanelModeForDisplay(context: Context): Boolean {
    val smallestWidthDp = context.resources.configuration.smallestScreenWidthDp
    if (smallestWidthDp < 600) return false

    val display = context.getSystemService(WindowManager::class.java)?.defaultDisplay ?: return false
    val size = Point()
    display.getRealSize(size)
    return shouldDefaultTwoPanelMode(
        smallestWidthDp = smallestWidthDp,
        currentOrientation = context.resources.configuration.orientation,
        landscapeNaturalOrientation = hasLandscapeNaturalOrientation(
            smallestWidthDp, size.x, size.y, display.rotation,
        ),
    )
}

internal fun shouldDefaultTwoPanelMode(
    smallestWidthDp: Int,
    currentOrientation: Int,
    landscapeNaturalOrientation: Boolean,
): Boolean = smallestWidthDp >= 600 &&
    (landscapeNaturalOrientation || currentOrientation == Configuration.ORIENTATION_LANDSCAPE)

/** Display dimensions follow the current rotation; undo it to determine natural orientation. */
internal fun hasLandscapeNaturalOrientation(
    smallestWidthDp: Int,
    displayWidthPx: Int,
    displayHeightPx: Int,
    rotation: Int,
): Boolean {
    if (smallestWidthDp < 600 || displayWidthPx <= 0 || displayHeightPx <= 0) return false
    return when (rotation) {
        Surface.ROTATION_0, Surface.ROTATION_180 -> displayWidthPx > displayHeightPx
        Surface.ROTATION_90, Surface.ROTATION_270 -> displayHeightPx > displayWidthPx
        else -> false
    }
}
