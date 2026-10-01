package com.katoaapps.openminilaunch.ui.minkspace.media

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkspace.media.MediaItem
import com.katoaapps.openminilaunch.features.minkspace.media.MediaPreviewLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun MediaPreview(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var drawable by remember(item.uri) { mutableStateOf<Drawable?>(null) }
    var loading by remember(item.uri) { mutableStateOf(true) }

    LaunchedEffect(item.uri) {
        loading = true
        drawable = withContext(Dispatchers.IO) {
            MediaPreviewLoader.load(context.applicationContext, item)
        }
        loading = false
    }
    DisposableEffect(drawable) {
        (drawable as? Animatable)?.start()
        onDispose { (drawable as? Animatable)?.stop() }
    }

    Box(modifier.clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { imageContext ->
                ImageView(imageContext).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
            },
            update = { imageView -> imageView.setImageDrawable(drawable) },
            modifier = Modifier.fillMaxSize(),
        )
        if (loading) CircularProgressIndicator()
        if (!loading && drawable == null) {
            Icon(Icons.Default.AddPhotoAlternate, stringResource(R.string.media_preview_unavailable))
        }
        if (item.isVideo) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = stringResource(R.string.open_video),
                modifier = Modifier.fillMaxSize(.22f),
                tint = Color.White.copy(alpha = .92f),
            )
        }
    }
}
