package com.katoaapps.openminilaunch.features.appearance

import android.graphics.Bitmap
import android.graphics.Color
import com.katoaapps.openminilaunch.model.HomeWallpaperAppearance

internal object HomeWallpaperAppearanceAnalyzer {
    fun analyze(bitmap: Bitmap): HomeWallpaperAppearance = HomeWallpaperAppearance(
        averageColorArgb = bitmap.averageColorBetween(0f, 1f),
        headerColorArgb = bitmap.averageColorBetween(0f, HEADER_SAMPLE_END_FRACTION),
    )

    private fun Bitmap.averageColorBetween(startYFraction: Float, endYFraction: Float): Int {
        val startY = (height * startYFraction).toInt().coerceIn(0, height - 1)
        val endY = (height * endYFraction).toInt().coerceIn(startY + 1, height)
        val horizontalSamples = minOf(width, COLOR_SAMPLE_COLUMNS)
        val verticalSamples = minOf(endY - startY, COLOR_SAMPLE_ROWS)
        var red = 0L
        var green = 0L
        var blue = 0L
        var count = 0L

        repeat(verticalSamples) { row ->
            val y = startY + ((row + .5f) * (endY - startY) / verticalSamples).toInt()
                .coerceAtMost(endY - startY - 1)
            repeat(horizontalSamples) { column ->
                val x = ((column + .5f) * width / horizontalSamples).toInt()
                    .coerceAtMost(width - 1)
                val pixel = getPixel(x, y)
                red += Color.red(pixel)
                green += Color.green(pixel)
                blue += Color.blue(pixel)
                count++
            }
        }

        return Color.rgb(
            (red / count).toInt(),
            (green / count).toInt(),
            (blue / count).toInt(),
        )
    }

    private const val HEADER_SAMPLE_END_FRACTION = .18f
    private const val COLOR_SAMPLE_COLUMNS = 48
    private const val COLOR_SAMPLE_ROWS = 32
}
