package com.katoaapps.openminilaunch.ui.settings

import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.appearance.WallpaperImageProcessor
import com.katoaapps.openminilaunch.model.HomeWallpaperAppearance
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val WALLPAPER_APPLYING_FEEDBACK_MILLIS = 1_000L

@Composable
internal fun WallpaperCropDialog(
    imageUri: Uri,
    applyWallpaper: (Bitmap) -> Result<HomeWallpaperAppearance>,
    onApplied: (HomeWallpaperAppearance) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val targetSize = remember(context) { WallpaperImageProcessor.deviceDisplaySize(context) }
    var sourceBitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember(imageUri) { mutableStateOf(false) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var applying by remember { mutableStateOf(false) }
    var applyFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(imageUri, targetSize) {
        sourceBitmap = withContext(Dispatchers.IO) {
            runCatching {
                WallpaperImageProcessor.decodeForDisplay(context, imageUri, targetSize)
            }.getOrNull()
        }
        loadFailed = sourceBitmap == null
    }
    DisposableEffect(sourceBitmap) {
        val bitmap = sourceBitmap
        onDispose { bitmap?.takeUnless(Bitmap::isRecycled)?.recycle() }
    }

    Dialog(
        onDismissRequest = { if (!applying) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AnimatedContent(
                targetState = applying,
                modifier = Modifier.fillMaxSize(),
                label = "wallpaper applying feedback",
            ) { applyingFeedback ->
                if (applyingFeedback) {
                    AppearanceApplyingScreen(
                        title = stringResource(R.string.applying_wallpaper),
                        selectionLabel = stringResource(R.string.home_wallpaper),
                        description = stringResource(R.string.applying_wallpaper_description),
                    )
                } else {
                    Column(
                        Modifier.fillMaxSize().statusBarsPadding()
                            .padding(start = Dimens.dp16, top = Dimens.dp16, end = Dimens.dp16),
                        verticalArrangement = Arrangement.spacedBy(Dimens.dp12),
                    ) {
                        Text(
                            stringResource(R.string.crop_wallpaper),
                            fontSize = Dimens.sp22,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            stringResource(
                                R.string.crop_wallpaper_description,
                                targetSize.width,
                                targetSize.height,
                            ),
                            color = Muted,
                            fontSize = Dimens.sp12,
                        )
                        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                            val targetRatio = targetSize.width.toFloat() / targetSize.height
                            val availableRatio = maxWidth.value / maxHeight.value
                            val previewModifier = if (availableRatio > targetRatio) {
                                Modifier.height(maxHeight).aspectRatio(targetRatio)
                            } else {
                                Modifier.width(maxWidth).aspectRatio(targetRatio)
                            }
                            Surface(
                                modifier = Modifier.align(Alignment.Center).then(previewModifier),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = MaterialTheme.shapes.large,
                                border = BorderStroke(
                                    width = Dimens.dp4,
                                    color = MaterialTheme.colorScheme.primary,
                                ),
                            ) {
                                val bitmap = sourceBitmap
                                when {
                                    bitmap != null -> WallpaperCropCanvas(
                                        bitmap = bitmap,
                                        zoom = zoom,
                                        offset = offset,
                                        onViewportChanged = { size ->
                                            viewportSize = size
                                            offset = WallpaperImageProcessor.constrainOffset(
                                                IntSize(bitmap.width, bitmap.height),
                                                size,
                                                zoom,
                                                offset,
                                            )
                                        },
                                        onTransform = { gestureZoom, pan ->
                                            val nextZoom = (zoom * gestureZoom).coerceIn(1f, 6f)
                                            zoom = nextZoom
                                            offset = WallpaperImageProcessor.constrainOffset(
                                                IntSize(bitmap.width, bitmap.height),
                                                viewportSize,
                                                nextZoom,
                                                offset + pan,
                                            )
                                        },
                                    )
                                    loadFailed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            stringResource(R.string.wallpaper_image_load_failed),
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                        if (applyFailed) {
                            Text(stringResource(R.string.wallpaper_apply_failed), color = MaterialTheme.colorScheme.error)
                        }
                        Row(
                            Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = Dimens.dp48),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.dp8, Alignment.End),
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(stringResource(R.string.cancel))
                            }
                            OutlinedButton(
                                onClick = {
                                    zoom = 1f
                                    offset = Offset.Zero
                                },
                                enabled = sourceBitmap != null,
                            ) {
                                Text(stringResource(R.string.reset_crop))
                            }
                            Button(
                                onClick = {
                                    val bitmap = sourceBitmap ?: return@Button
                                    applying = true
                                    applyFailed = false
                                    scope.launch {
                                        val feedbackStartedAt = SystemClock.elapsedRealtime()
                                        val result = withContext(Dispatchers.IO) {
                                            runCatching {
                                                WallpaperImageProcessor.useRenderedCrop(
                                                    source = bitmap,
                                                    viewport = viewportSize,
                                                    target = targetSize,
                                                    zoom = zoom,
                                                    offset = offset,
                                                ) { crop -> applyWallpaper(crop).getOrThrow() }
                                            }
                                        }
                                        val remainingFeedbackTime = WALLPAPER_APPLYING_FEEDBACK_MILLIS -
                                            (SystemClock.elapsedRealtime() - feedbackStartedAt)
                                        if (remainingFeedbackTime > 0) delay(remainingFeedbackTime)
                                        applying = false
                                        result.fold(onSuccess = onApplied) { applyFailed = true }
                                    }
                                },
                                enabled = sourceBitmap != null && viewportSize != IntSize.Zero,
                            ) {
                                Text(stringResource(R.string.apply))
                            }
                        }
                    }
                }
            }
        }
    }
}
