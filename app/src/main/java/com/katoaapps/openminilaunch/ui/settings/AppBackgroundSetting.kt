package com.katoaapps.openminilaunch.ui.settings

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Wallpaper
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.appearance.HomeWallpaperRepository
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MinkWhite
import com.katoaapps.openminilaunch.ui.theme.Muted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun AppBackgroundSetting(
    selectedArgb: Int?,
    imageSelected: Boolean,
    imageRevision: Int,
    onColorSelected: (Int) -> Unit,
    onChooseImage: () -> Unit,
    onUseThemeDefault: () -> Unit,
) {
    val context = LocalContext.current
    val presets = listOf(
        HomePanelColorPreset(R.string.color_paper, androidx.core.content.ContextCompat.getColor(context, R.color.app_background_paper)),
        HomePanelColorPreset(R.string.color_midnight, androidx.core.content.ContextCompat.getColor(context, R.color.app_background_midnight)),
        HomePanelColorPreset(R.string.color_cream, androidx.core.content.ContextCompat.getColor(context, R.color.app_background_cream)),
        HomePanelColorPreset(R.string.color_slate, androidx.core.content.ContextCompat.getColor(context, R.color.app_background_slate)),
    )
    AppearanceColorSetting(
        selectedArgb = selectedArgb.takeUnless { imageSelected },
        pickerArgb = selectedArgb ?: MaterialTheme.colorScheme.background.toArgb(),
        titleRes = R.string.app_background_color,
        descriptionRes = R.string.app_background_color_description,
        customTitleRes = R.string.custom_background_color,
        customDescriptionRes = R.string.custom_background_color_description,
        presets = presets,
        onColorSelected = onColorSelected,
        selectedValueLabel = stringResource(R.string.background_image).takeIf { imageSelected },
        presetTrailingContent = {
            BackgroundImageChoice(
                selected = imageSelected,
                imageRevision = imageRevision,
                onClick = onChooseImage,
            )
        },
        onUseThemeDefault = onUseThemeDefault,
    )
}

@Composable
private fun BackgroundImageChoice(
    selected: Boolean,
    imageRevision: Int,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { HomeWallpaperRepository(context) }
    val thumbnail by produceState<Bitmap?>(null, imageRevision, repository.wallpaperFile.lastModified()) {
        value = withContext(Dispatchers.IO) { repository.loadThumbnail(maximumSide = 160) }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(Dimens.dp42).then(
                if (selected) Modifier.border(Dimens.dp3, MaterialTheme.colorScheme.onSurface, CircleShape)
                else Modifier,
            ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = if (selected) Dimens.dp4 else Dimens.dp0,
        ) {
            Box(contentAlignment = Alignment.Center) {
                thumbnail?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } ?: Icon(
                    Icons.Default.Wallpaper,
                    stringResource(R.string.background_image),
                    modifier = Modifier.size(Dimens.dp22),
                )
                if (selected) {
                    Icon(
                        Icons.Default.Check,
                        stringResource(R.string.selected),
                        tint = MinkWhite,
                        modifier = Modifier.size(Dimens.dp20),
                    )
                }
            }
        }
        Text(
            stringResource(R.string.background_image),
            color = Muted,
            fontSize = Dimens.sp9,
            modifier = Modifier.padding(top = Dimens.dp4),
        )
    }
}
