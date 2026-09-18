package com.katoaapps.openminilaunch.ui.settings

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.roundToInt

/** Interactive rendering surface for the exact device-ratio wallpaper crop. */
@Composable
internal fun WallpaperCropCanvas(
    bitmap: Bitmap,
    zoom: Float,
    offset: Offset,
    onViewportChanged: (IntSize) -> Unit,
    onTransform: (zoom: Float, pan: Offset) -> Unit,
) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    Canvas(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .onSizeChanged(onViewportChanged)
            .pointerInput(bitmap) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    onTransform(gestureZoom, pan)
                }
            },
    ) {
        val baseScale = max(size.width / bitmap.width, size.height / bitmap.height)
        val scale = baseScale * zoom
        val drawnWidth = bitmap.width * scale
        val drawnHeight = bitmap.height * scale
        drawImage(
            image = image,
            dstOffset = IntOffset(
                ((size.width - drawnWidth) / 2f + offset.x).roundToInt(),
                ((size.height - drawnHeight) / 2f + offset.y).roundToInt(),
            ),
            dstSize = IntSize(drawnWidth.roundToInt(), drawnHeight.roundToInt()),
            filterQuality = FilterQuality.High,
        )
    }
}
