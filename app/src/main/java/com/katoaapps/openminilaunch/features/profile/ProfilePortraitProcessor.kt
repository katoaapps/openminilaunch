package com.katoaapps.openminilaunch.features.profile

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.katoaapps.openminilaunch.features.appearance.WallpaperImageProcessor

internal object ProfilePortraitProcessor {
    private val target = IntSize(512, 512)

    fun decode(context: Context, uri: Uri): Bitmap {
        val source = WallpaperImageProcessor.decodeForDisplay(context, uri, target)
        return try {
            WallpaperImageProcessor.renderCrop(
                source = source,
                viewport = target,
                target = target,
                zoom = 1f,
                offset = Offset.Zero,
            )
        } finally {
            source.recycle()
        }
    }
}
