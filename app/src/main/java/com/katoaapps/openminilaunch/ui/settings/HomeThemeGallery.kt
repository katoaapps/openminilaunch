package com.katoaapps.openminilaunch.ui.settings

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import com.katoaapps.openminilaunch.features.appearance.BundledHomeTheme
import com.katoaapps.openminilaunch.features.appearance.BundledHomeThemeCatalog
import com.katoaapps.openminilaunch.features.appearance.BundledWallpaper
import com.katoaapps.openminilaunch.features.appearance.HomeWallpaperRepository
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MinkWhite
import com.katoaapps.openminilaunch.ui.theme.Muted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Shared complete-theme gallery used by both first-run setup and the appearance editor. */
@Composable
internal fun HomeThemeGallery(
    selectedThemeId: String?,
    applyingThemeId: String?,
    onSelectTheme: (BundledHomeTheme) -> Unit,
) {
    val entries = remember {
        BundledHomeThemeCatalog.themes.mapIndexed { index, theme ->
            HomeThemeEntry(theme, number = index.takeIf { it > 0 })
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
                        HomeThemeTile(
                            entry = entry,
                            width = tileWidth,
                            selected = entry.theme.id == selectedThemeId,
                            applying = entry.theme.id == applyingThemeId,
                            enabled = applyingThemeId == null,
                            onClick = { onSelectTheme(entry.theme) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeThemeTile(
    entry: HomeThemeEntry,
    width: Dp,
    selected: Boolean,
    applying: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val label = entry.number?.let { stringResource(R.string.theme_number, it) }
        ?: stringResource(R.string.mink_theme)
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
                entry.theme.wallpaper?.let { wallpaper ->
                    BundledWallpaperThumbnail(wallpaper)
                } ?: Box(
                    Modifier.fillMaxSize().background(Color(entry.theme.backgroundColorArgb)),
                )
                ThemeColorPreview(
                    backgroundArgb = entry.theme.backgroundColorArgb,
                    panelArgb = entry.theme.panelColorArgb,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
                if (selected || applying) {
                    ThemeSelectionBadge(
                        applying = applying,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
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
private fun ThemeSelectionBadge(applying: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(Dimens.dp6).size(Dimens.dp26),
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

@Composable
private fun ThemeColorPreview(
    backgroundArgb: Int,
    panelArgb: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(Dimens.dp25),
        color = Color(backgroundArgb),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.fillMaxWidth(.58f).height(Dimens.dp11),
                shape = RoundedCornerShape(Dimens.dp10),
                color = Color(panelArgb),
                shadowElevation = Dimens.dp2,
            ) {}
        }
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
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(Dimens.dp14)),
        )
    }
}

private data class HomeThemeEntry(
    val theme: BundledHomeTheme,
    /** Null identifies the named Mink Theme; numbered themes begin at one. */
    val number: Int?,
)
