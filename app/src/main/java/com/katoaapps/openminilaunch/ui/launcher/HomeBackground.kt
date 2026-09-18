package com.katoaapps.openminilaunch.ui.launcher

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.ui.theme.readableContentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun HomeBackground(store: LauncherStore, modifier: Modifier = Modifier) {
    val imageEnabled = store.appBackgroundImageEnabled
    val imageRevision = store.appBackgroundImageRevision
    val wallpaperFile = store.appBackgroundImageFile
    val wallpaperBitmap by produceState<Bitmap?>(
        initialValue = null,
        imageEnabled,
        imageRevision,
        wallpaperFile,
    ) {
        value = if (imageEnabled) {
            withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(wallpaperFile.absolutePath)
            }
        } else {
            null
        }
    }
    DisposableEffect(wallpaperBitmap) {
        val bitmap = wallpaperBitmap
        onDispose { bitmap?.takeUnless(Bitmap::isRecycled)?.recycle() }
    }

    if (wallpaperBitmap != null) {
        val wallpaper = remember(wallpaperBitmap) { checkNotNull(wallpaperBitmap).asImageBitmap() }
        Image(
            bitmap = wallpaper,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize(),
        )
    } else {
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
    }
}

@Composable
internal fun launcherBackgroundColor(store: LauncherStore, header: Boolean = false): Color {
    val sampledArgb = if (header) store.visibleAppHeaderColorArgb else store.visibleAppBackgroundColorArgb
    return sampledArgb?.let(::Color) ?: MaterialTheme.colorScheme.background
}

@Composable
internal fun launcherBackgroundContentColor(store: LauncherStore, header: Boolean = false): Color =
    readableContentColor(launcherBackgroundColor(store, header))
