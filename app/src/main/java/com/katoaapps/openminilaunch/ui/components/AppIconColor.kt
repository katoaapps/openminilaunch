package com.katoaapps.openminilaunch.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap

/** Picks a representative saturated color from an app or shortcut icon. */
internal fun dominantAppColor(drawable: Drawable?): Color? {
    val bitmap = runCatching { drawable?.toBitmap(width = 40, height = 40) }.getOrNull() ?: return null
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val hueWeights = FloatArray(24)
    val redTotals = FloatArray(24)
    val greenTotals = FloatArray(24)
    val blueTotals = FloatArray(24)
    var fallbackRed = 0f
    var fallbackGreen = 0f
    var fallbackBlue = 0f
    var fallbackWeight = 0f
    val hsv = FloatArray(3)

    pixels.forEach { pixel ->
        val alpha = AndroidColor.alpha(pixel)
        if (alpha < 96) return@forEach
        val red = AndroidColor.red(pixel).toFloat()
        val green = AndroidColor.green(pixel).toFloat()
        val blue = AndroidColor.blue(pixel).toFloat()
        val alphaWeight = alpha / 255f
        fallbackRed += red * alphaWeight
        fallbackGreen += green * alphaWeight
        fallbackBlue += blue * alphaWeight
        fallbackWeight += alphaWeight

        AndroidColor.colorToHSV(pixel, hsv)
        val saturation = hsv[1]
        val value = hsv[2]
        if (saturation < .16f || value < .12f || value > .98f) return@forEach
        val bin = ((hsv[0] / 360f) * hueWeights.size).toInt().coerceIn(hueWeights.indices)
        val weight = alphaWeight * saturation * (.45f + value)
        hueWeights[bin] += weight
        redTotals[bin] += red * weight
        greenTotals[bin] += green * weight
        blueTotals[bin] += blue * weight
    }

    val strongestBin = hueWeights.indices.maxByOrNull(hueWeights::get)
    val strongestWeight = strongestBin?.let(hueWeights::get) ?: 0f
    return if (strongestBin != null && strongestWeight > 0f) {
        Color(
            red = redTotals[strongestBin] / strongestWeight / 255f,
            green = greenTotals[strongestBin] / strongestWeight / 255f,
            blue = blueTotals[strongestBin] / strongestWeight / 255f,
        )
    } else if (fallbackWeight > 0f) {
        Color(
            red = fallbackRed / fallbackWeight / 255f,
            green = fallbackGreen / fallbackWeight / 255f,
            blue = fallbackBlue / fallbackWeight / 255f,
        )
    } else {
        null
    }
}
