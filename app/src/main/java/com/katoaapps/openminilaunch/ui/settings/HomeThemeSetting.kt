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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

internal const val CUSTOM_THEME_SELECTION_ID = "custom-theme"

@Composable
internal fun HomeThemeSetting(
    selectedThemeId: String?,
    customThemeSelected: Boolean,
    savedPhotoAvailable: Boolean,
    imageRevision: Int,
    applyingSelectionId: String?,
    customBackgroundArgb: Int,
    customPanelArgb: Int,
    panelTransparency: Float,
    onChoosePhoto: () -> Unit,
    onUseSavedPhoto: () -> Unit,
    onSelectTheme: (BundledHomeTheme) -> Unit,
    onCustomBackgroundSelected: (Int) -> Unit,
    onCustomPanelSelected: (String, Int) -> Unit,
    onTransparencyChanged: (Float) -> Unit,
) {
    val entries = remember {
        listOf(HomeThemeEntry.Custom) +
            BundledHomeThemeCatalog.themes.mapIndexed { index, theme ->
                HomeThemeEntry.Bundled(theme, index + 1)
            }
    }
    var editingColor by remember { mutableStateOf<CustomThemeColor?>(null) }
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
                Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.onSurface)
                Column(Modifier.padding(start = Dimens.dp12)) {
                    Text(stringResource(R.string.home_themes), fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.home_themes_description),
                        color = Muted,
                        fontSize = Dimens.sp12,
                    )
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
                                    selected = when (entry) {
                                        HomeThemeEntry.Custom -> customThemeSelected
                                        is HomeThemeEntry.Bundled -> entry.theme.id == selectedThemeId
                                    },
                                    savedPhotoAvailable = savedPhotoAvailable,
                                    imageRevision = imageRevision,
                                    customBackgroundArgb = customBackgroundArgb,
                                    customPanelArgb = customPanelArgb,
                                    applying = applyingSelectionId == entry.selectionId,
                                    enabled = applyingSelectionId == null,
                                    onClick = {
                                        when (entry) {
                                            HomeThemeEntry.Custom -> {
                                                if (savedPhotoAvailable) onUseSavedPhoto() else onChoosePhoto()
                                            }
                                            is HomeThemeEntry.Bundled -> onSelectTheme(entry.theme)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
            if (customThemeSelected) {
                HorizontalDivider()
                CustomThemeEditor(
                    savedPhotoAvailable = savedPhotoAvailable,
                    customBackgroundArgb = customBackgroundArgb,
                    customPanelArgb = customPanelArgb,
                    onChoosePhoto = onChoosePhoto,
                    onEditBackground = { editingColor = CustomThemeColor.Background },
                    onEditPanel = { editingColor = CustomThemeColor.Panel },
                )
            }
            HorizontalDivider()
            PillTransparencyControl(
                transparency = panelTransparency,
                onTransparencyChanged = onTransparencyChanged,
            )
        }
    }

    editingColor?.let { color ->
        val editingPanel = color == CustomThemeColor.Panel
        AppearanceColorDialog(
            initialArgb = if (editingPanel) customPanelArgb else customBackgroundArgb,
            titleRes = if (editingPanel) R.string.custom_panel_color else R.string.custom_background_color,
            descriptionRes = if (editingPanel) {
                R.string.custom_panel_color_description
            } else {
                R.string.custom_background_color_description
            },
            colorHotphrases = if (editingPanel) {
                mapOf(
                    com.katoaapps.openminilaunch.features.appearance.WATERMELON_HOTPHRASE to
                        com.katoaapps.openminilaunch.features.appearance.WATERMELON_PILL_COLOR_ARGB,
                )
            } else {
                emptyMap()
            },
            onConfirm = { input, argb ->
                if (editingPanel) onCustomPanelSelected(input, argb)
                else onCustomBackgroundSelected(argb)
                editingColor = null
            },
            onDismiss = { editingColor = null },
        )
    }
}

@Composable
private fun HomeThemeTile(
    entry: HomeThemeEntry,
    width: Dp,
    selected: Boolean,
    savedPhotoAvailable: Boolean,
    imageRevision: Int,
    customBackgroundArgb: Int,
    customPanelArgb: Int,
    applying: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val label = when (entry) {
        HomeThemeEntry.Custom -> stringResource(R.string.custom_theme)
        is HomeThemeEntry.Bundled -> stringResource(R.string.theme_number, entry.number)
    }
    val backgroundArgb = when (entry) {
        HomeThemeEntry.Custom -> customBackgroundArgb
        is HomeThemeEntry.Bundled -> entry.theme.backgroundColorArgb
    }
    val panelArgb = when (entry) {
        HomeThemeEntry.Custom -> customPanelArgb
        is HomeThemeEntry.Bundled -> entry.theme.panelColorArgb
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
                    HomeThemeEntry.Custom -> CustomPhotoThumbnail(
                        available = savedPhotoAvailable,
                        imageRevision = imageRevision,
                        fallbackArgb = customBackgroundArgb,
                    )
                    is HomeThemeEntry.Bundled -> BundledWallpaperThumbnail(entry.theme.wallpaper)
                }
                ThemeColorPreview(
                    backgroundArgb = backgroundArgb,
                    panelArgb = panelArgb,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
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
private fun CustomThemeEditor(
    savedPhotoAvailable: Boolean,
    customBackgroundArgb: Int,
    customPanelArgb: Int,
    onChoosePhoto: () -> Unit,
    onEditBackground: () -> Unit,
    onEditPanel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp10)) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp2)) {
            Text(stringResource(R.string.custom_theme), fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.custom_theme_description),
                color = Muted,
                fontSize = Dimens.sp12,
            )
        }
        OutlinedButton(onClick = onChoosePhoto, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PhotoLibrary, null, Modifier.size(Dimens.dp18))
            Text(
                stringResource(if (savedPhotoAvailable) R.string.change_photo else R.string.choose_photo),
                Modifier.padding(start = Dimens.dp8),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
            CustomThemeColorButton(
                label = stringResource(R.string.app_background_color),
                argb = customBackgroundArgb,
                onClick = onEditBackground,
                modifier = Modifier.weight(1f),
            )
            CustomThemeColorButton(
                label = stringResource(R.string.home_panel_color),
                argb = customPanelArgb,
                onClick = onEditPanel,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CustomThemeColorButton(
    label: String,
    argb: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Surface(
            modifier = Modifier.size(Dimens.dp18),
            shape = CircleShape,
            color = Color(argb),
        ) {}
        Text(
            label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = Dimens.dp6).weight(1f),
        )
        Icon(Icons.Default.Edit, null, Modifier.size(Dimens.dp16))
    }
}

@Composable
private fun CustomPhotoThumbnail(
    available: Boolean,
    imageRevision: Int,
    fallbackArgb: Int,
) {
    val context = LocalContext.current
    val repository = remember(context) { HomeWallpaperRepository(context) }
    val systemFallbackArgb = repository.systemWallpaperAppearance?.averageColorArgb ?: fallbackArgb
    val bitmap by produceState<Bitmap?>(
        null,
        available,
        imageRevision,
        repository.wallpaperFile.lastModified(),
        systemFallbackArgb,
    ) {
        value = withContext(Dispatchers.IO) {
            if (available) repository.loadThumbnail(maximumSide = 256)
            else repository.loadSystemWallpaperThumbnail(maximumSide = 256)
        }
    }
    RecycledBitmapImage(bitmap)
    if (bitmap == null) {
        Box(Modifier.fillMaxSize().background(Color(systemFallbackArgb)))
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

private sealed interface HomeThemeEntry {
    val selectionId: String

    data object Custom : HomeThemeEntry {
        override val selectionId = CUSTOM_THEME_SELECTION_ID
    }

    data class Bundled(val theme: BundledHomeTheme, val number: Int) : HomeThemeEntry {
        override val selectionId = theme.id
    }
}

private enum class CustomThemeColor { Background, Panel }
