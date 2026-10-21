package com.katoaapps.openminilaunch.ui.settings

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.appearance.BundledWallpaper
import com.katoaapps.openminilaunch.features.appearance.BundledWallpaperCatalog
import com.katoaapps.openminilaunch.features.appearance.HomeWallpaperRepository
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MinkWhite
import com.katoaapps.openminilaunch.ui.theme.Muted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val CUSTOM_PHOTO_SELECTION_ID = "custom-photo"

@Composable
internal fun WallpaperLibrarySetting(
    selectedPresetId: String?,
    customPhotoSelected: Boolean,
    savedPhotoAvailable: Boolean,
    imageRevision: Int,
    applyingSelectionId: String?,
    onChoosePhoto: () -> Unit,
    onUseSavedPhoto: () -> Unit,
    onSelectPreset: (BundledWallpaper) -> Unit,
) {
    val entries = remember {
        listOf(WallpaperLibraryEntry.CustomPhoto) +
            BundledWallpaperCatalog.wallpapers.mapIndexed { index, wallpaper ->
                WallpaperLibraryEntry.Preset(wallpaper, index + 1)
            }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.dp16),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.padding(Dimens.dp14),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp10),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Collections, null, tint = MaterialTheme.colorScheme.onSurface)
                Column(Modifier.padding(start = Dimens.dp12)) {
                    Text(stringResource(R.string.wallpaper_library), fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.wallpaper_library_description),
                        color = Muted,
                        fontSize = Dimens.sp12,
                    )
                }
            }
            if (savedPhotoAvailable) {
                OutlinedButton(
                    onClick = onChoosePhoto,
                    enabled = applyingSelectionId == null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, Modifier.size(Dimens.dp18))
                    Text(stringResource(R.string.change_photo), Modifier.padding(start = Dimens.dp8))
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = when {
                    maxWidth >= Dimens.dp560 -> 5
                    maxWidth >= Dimens.dp420 -> 4
                    else -> 3
                }
                val gap = Dimens.dp8
                val tileWidth = (maxWidth - gap * (columns - 1)) / columns
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp10)) {
                    entries.chunked(columns).forEach { rowEntries ->
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            rowEntries.forEach { entry ->
                                WallpaperLibraryTile(
                                    entry = entry,
                                    width = tileWidth,
                                    selected = when (entry) {
                                        WallpaperLibraryEntry.CustomPhoto -> customPhotoSelected
                                        is WallpaperLibraryEntry.Preset -> entry.wallpaper.id == selectedPresetId
                                    },
                                    savedPhotoAvailable = savedPhotoAvailable,
                                    imageRevision = imageRevision,
                                    applying = applyingSelectionId == entry.selectionId,
                                    enabled = applyingSelectionId == null,
                                    onClick = {
                                        when (entry) {
                                            WallpaperLibraryEntry.CustomPhoto -> {
                                                if (savedPhotoAvailable) onUseSavedPhoto() else onChoosePhoto()
                                            }
                                            is WallpaperLibraryEntry.Preset -> onSelectPreset(entry.wallpaper)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperLibraryTile(
    entry: WallpaperLibraryEntry,
    width: Dp,
    selected: Boolean,
    savedPhotoAvailable: Boolean,
    imageRevision: Int,
    applying: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val label = when (entry) {
        WallpaperLibraryEntry.CustomPhoto -> stringResource(
            if (savedPhotoAvailable) R.string.your_photo else R.string.choose_photo,
        )
        is WallpaperLibraryEntry.Preset -> stringResource(R.string.wallpaper_number, entry.number)
    }
    Column(
        modifier = Modifier.width(width),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val shape = RoundedCornerShape(Dimens.dp14)
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
                .aspectRatio(1f)
                .then(
                    if (selected) Modifier.border(Dimens.dp3, MaterialTheme.colorScheme.primary, shape)
                    else Modifier,
                )
                .semantics {
                    contentDescription = label
                    this.selected = selected
                },
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (entry) {
                    WallpaperLibraryEntry.CustomPhoto -> CustomPhotoThumbnail(
                        available = savedPhotoAvailable,
                        imageRevision = imageRevision,
                    )
                    is WallpaperLibraryEntry.Preset -> BundledWallpaperThumbnail(entry.wallpaper)
                }
                if (selected || applying) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(Dimens.dp6).size(Dimens.dp26),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = .62f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (applying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(Dimens.dp16),
                                    strokeWidth = Dimens.dp2,
                                    color = MinkWhite,
                                )
                            } else {
                                Icon(
                                    Icons.Default.Check,
                                    stringResource(R.string.selected_label),
                                    tint = MinkWhite,
                                    modifier = Modifier.size(Dimens.dp17),
                                )
                            }
                        }
                    }
                }
            }
        }
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.onSurface else Muted,
            fontSize = Dimens.sp10,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp4),
        )
    }
}

@Composable
private fun CustomPhotoThumbnail(available: Boolean, imageRevision: Int) {
    val context = LocalContext.current
    val repository = remember(context) { HomeWallpaperRepository(context) }
    val bitmap by produceState<Bitmap?>(null, available, imageRevision, repository.wallpaperFile.lastModified()) {
        value = if (available) {
            withContext(Dispatchers.IO) { repository.loadThumbnail(maximumSide = 256) }
        } else null
    }
    RecycledBitmapImage(bitmap)
    if (bitmap == null) {
        Icon(
            Icons.Default.PhotoLibrary,
            stringResource(R.string.choose_photo),
            tint = Muted,
            modifier = Modifier.size(Dimens.dp30),
        )
    }
}

@Composable
private fun BundledWallpaperThumbnail(wallpaper: BundledWallpaper) {
    val context = LocalContext.current
    val repository = remember(context) { HomeWallpaperRepository(context) }
    val bitmap by produceState<Bitmap?>(null, wallpaper.drawableRes) {
        value = withContext(Dispatchers.IO) {
            repository.loadBundledThumbnail(wallpaper.drawableRes, maximumSide = 256)
        }
    }
    RecycledBitmapImage(bitmap)
}

@Composable
private fun RecycledBitmapImage(bitmap: Bitmap?) {
    DisposableEffect(bitmap) {
        onDispose { bitmap?.takeUnless(Bitmap::isRecycled)?.recycle() }
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(Dimens.dp14)),
        )
    }
}

private sealed interface WallpaperLibraryEntry {
    val selectionId: String

    data object CustomPhoto : WallpaperLibraryEntry {
        override val selectionId = CUSTOM_PHOTO_SELECTION_ID
    }

    data class Preset(val wallpaper: BundledWallpaper, val number: Int) : WallpaperLibraryEntry {
        override val selectionId = wallpaper.id
    }
}
