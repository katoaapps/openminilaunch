package com.katoaapps.openminilaunch.ui.settings

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.appearance.HomeWallpaperRepository
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MinkWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val THEME_BACKGROUND_SELECTION_ID = "theme-background"
internal const val DEVICE_WALLPAPER_SELECTION_ID = "device-wallpaper"

@Composable
internal fun WallpaperSourceChoices(
    themeSelected: Boolean,
    deviceSelected: Boolean,
    savedDeviceWallpaperAvailable: Boolean,
    savedDeviceWallpaperRevision: Int,
    applyingSourceId: String?,
    enabled: Boolean,
    onUseTheme: () -> Unit,
    onUseDevice: () -> Unit,
    onChoosePhoto: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { HomeWallpaperRepository(context) }
    val savedDeviceWallpaperPreview by produceState<Bitmap?>(
        initialValue = null,
        savedDeviceWallpaperAvailable,
        savedDeviceWallpaperRevision,
    ) {
        value = if (savedDeviceWallpaperAvailable) {
            withContext(Dispatchers.IO) { repository.loadThumbnail(maximumSide = 256) }
        } else {
            null
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
    ) {
        WallpaperSourceChoice(
            label = stringResource(R.string.theme_background),
            icon = Icons.Default.Palette,
            selected = themeSelected,
            applying = applyingSourceId == THEME_BACKGROUND_SELECTION_ID,
            enabled = enabled,
            onClick = onUseTheme,
            modifier = Modifier.weight(1f),
        )
        WallpaperSourceChoice(
            label = stringResource(R.string.device_wallpaper),
            icon = Icons.Default.Wallpaper,
            selected = deviceSelected,
            applying = applyingSourceId == DEVICE_WALLPAPER_SELECTION_ID,
            enabled = enabled,
            onClick = onUseDevice,
            previewBitmap = savedDeviceWallpaperPreview,
            modifier = Modifier.weight(1f),
        )
        WallpaperSourceChoice(
            label = stringResource(R.string.choose_photo),
            icon = Icons.Default.PhotoLibrary,
            selected = false,
            applying = false,
            enabled = enabled,
            onClick = onChoosePhoto,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WallpaperSourceChoice(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    applying: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    previewBitmap: Bitmap? = null,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Dimens.dp12)
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(Dimens.dp72)
            .then(
                if (selected) Modifier.border(Dimens.dp2, MaterialTheme.colorScheme.primary, shape)
                else Modifier,
            )
            .semantics {
                contentDescription = label
                this.selected = selected
            },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Box(Modifier.fillMaxSize()) {
            previewBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .38f)))
            }
            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(horizontal = Dimens.dp6, vertical = Dimens.dp8),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (applying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimens.dp20),
                        strokeWidth = Dimens.dp2,
                    )
                } else {
                    Icon(
                        imageVector = if (selected) Icons.Default.Check else icon,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.dp20),
                        tint = if (previewBitmap != null) MinkWhite else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = label,
                    modifier = Modifier.padding(top = Dimens.dp4),
                    color = if (previewBitmap != null) MinkWhite else MaterialTheme.colorScheme.onSurface,
                    fontSize = Dimens.sp10,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
