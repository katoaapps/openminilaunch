package com.katoaapps.openminilaunch.ui.components

import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.iconpacks.IconPackRepository
import com.katoaapps.openminilaunch.model.IconSource
import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.ui.theme.MinkBlack
import com.katoaapps.openminilaunch.ui.theme.MinkWhite

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    val appearance = LauncherStore.get(context).iconAppearance
    val iconPacks = remember(context) { IconPackRepository.get(context) }
    val iconPacksRevision by iconPacks.revision.collectAsState()
    val launcherAppsRevision = actions?.launcherAppsRevision?.collectAsState()?.value ?: 0L
    val originalIcon = {
        actions?.appIcon(packageName)
            ?: runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
    }
    RenderedAppIcon(
        iconKey = "$packageName:$launcherAppsRevision:$iconPacksRevision:$appearance",
        drawable = {
            appearance.iconPackPackage
                ?.takeIf { appearance.source == IconSource.ICON_PACK }
                ?.let { iconPacks.iconForPackage(packageName, it) }
                ?: originalIcon()
        },
        size = size,
        themedTint = appearance.minkAppIconTint(themedTint),
        contentDescription = contentDescription,
    )
}

/** Uses LauncherActivityInfo's badged icon so Android supplies the correct work-profile badge. */
@Composable
internal fun LauncherAppIcon(
    target: LauncherAppTarget,
    actions: DeviceActions,
    size: Dp,
    themedTint: Color? = null,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val appearance = LauncherStore.get(context).iconAppearance
    val iconPacks = remember(context) { IconPackRepository.get(context) }
    val iconPacksRevision by iconPacks.revision.collectAsState()
    val launcherAppsRevision by actions.launcherAppsRevision.collectAsState()
    RenderedAppIcon(
        iconKey = "${target.selectionKey}:$launcherAppsRevision:$iconPacksRevision:$appearance",
        drawable = {
            appearance.iconPackPackage
                ?.takeIf { appearance.source == IconSource.ICON_PACK }
                ?.let { iconPacks.iconFor(target, it) }
                ?: actions.launcherAppIcon(target)
        },
        size = size,
        themedTint = appearance.minkAppIconTint(themedTint),
        contentDescription = contentDescription,
    )
}

/** Renders either a launcher activity icon or an icon published for an app shortcut. */
@Composable
internal fun LauncherTargetIcon(
    target: LauncherTarget,
    actions: DeviceActions,
    size: Dp,
    themedTint: Color? = null,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val appearance = LauncherStore.get(context).iconAppearance
    val iconPacks = remember(context) { IconPackRepository.get(context) }
    val iconPacksRevision by iconPacks.revision.collectAsState()
    val appsRevision by actions.launcherAppsRevision.collectAsState()
    val shortcutsRevision by actions.launcherShortcutsRevision.collectAsState()
    RenderedAppIcon(
        iconKey = "${target.selectionKey}:$appsRevision:$shortcutsRevision:$iconPacksRevision:$appearance",
        drawable = {
            appearance.iconPackPackage
                ?.takeIf { appearance.source == IconSource.ICON_PACK }
                ?.let { iconPacks.iconFor(target, it) }
                ?: actions.launcherTargetIcon(target)
        },
        size = size,
        themedTint = appearance.minkAppIconTint(themedTint),
        contentDescription = contentDescription,
    )
}

@Composable
internal fun DrawableIcon(
    drawable: android.graphics.drawable.Drawable?,
    iconKey: String,
    size: Dp,
    contentDescription: String? = null,
) {
    RenderedAppIcon(
        iconKey = iconKey,
        drawable = { drawable },
        size = size,
        themedTint = null,
        contentDescription = contentDescription,
    )
}

@Composable
private fun RenderedAppIcon(
    iconKey: String,
    drawable: () -> android.graphics.drawable.Drawable?,
    size: Dp,
    themedTint: Color?,
    contentDescription: String?,
) {
    val density = LocalDensity.current
    val targetBitmapSize = with(density) {
        (size * MONOCHROME_ICON_SCALE).roundToPx()
    }.coerceAtLeast(MIN_ICON_BITMAP_SIZE_PX)
    val rendered = remember(iconKey, themedTint != null, targetBitmapSize) {
        val drawable = drawable()
        val monochrome = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (drawable as? AdaptiveIconDrawable)?.monochrome
        } else {
            null
        }
        val source = if (themedTint != null) monochrome ?: drawable else drawable
        source?.toBitmap(width = targetBitmapSize, height = targetBitmapSize)?.asImageBitmap()?.let {
            RenderedIconBitmap(it, monochrome != null && source === monochrome)
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

private data class RenderedIconBitmap(val bitmap: ImageBitmap, val isMonochrome: Boolean)
