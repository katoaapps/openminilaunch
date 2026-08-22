package com.katoaapps.openminilaunch

import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap

private const val MONOCHROME_ICON_SCALE = 1.3f
private const val MIN_ICON_BITMAP_SIZE_PX = 96

/** Shared app-icon rendering used by search, setup, the drawer, and Settings. */
@Composable
internal fun AppIcon(
    packageName: String,
    actions: DeviceActions?,
    size: Dp,
    themedTint: Color? = null,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val targetBitmapSize = with(density) {
        (size * MONOCHROME_ICON_SCALE).roundToPx()
    }.coerceAtLeast(MIN_ICON_BITMAP_SIZE_PX)
    val rendered = remember(packageName, actions, themedTint != null, targetBitmapSize) {
        val drawable = actions?.appIcon(packageName)
            ?: runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
        val monochrome = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (drawable as? AdaptiveIconDrawable)?.monochrome
        } else {
            null
        }
        val source = if (themedTint != null) monochrome ?: drawable else drawable
        source?.toBitmap(width = targetBitmapSize, height = targetBitmapSize)?.asImageBitmap()?.let {
            RenderedAppIcon(it, monochrome != null && source === monochrome)
        }
    }
    if (rendered != null) {
        val renderedSize = if (rendered.isMonochrome) {
            size * MONOCHROME_ICON_SCALE
        } else {
            size
        }
        Image(
            rendered.bitmap,
            contentDescription,
            Modifier.size(renderedSize),
            colorFilter = themedTint?.takeIf { rendered.isMonochrome }?.let { ColorFilter.tint(it) },
        )
    } else {
        val fallbackColor = themedTint ?: MaterialTheme.colorScheme.onSurface
        Box(
            Modifier.size(size).clip(CircleShape).background(fallbackColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Apps,
                contentDescription,
                tint = if (themedTint != null) {
                    if (fallbackColor.luminance() > .5f) MinkBlack else MinkWhite
                } else {
                    MaterialTheme.colorScheme.background
                },
                modifier = Modifier.size(size * .55f),
            )
        }
    }
}

private data class RenderedAppIcon(val bitmap: ImageBitmap, val isMonochrome: Boolean)
