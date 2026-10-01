package com.katoaapps.openminilaunch.ui.minkspace.music

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.katoaapps.openminilaunch.features.minkspace.music.MusicPlaybackState

/** Album art first, then player artwork, with a stable dark canvas when neither is available. */
@Composable
internal fun MusicArtworkBackground(
    state: MusicPlaybackState,
    modifier: Modifier = Modifier,
) {
    val backgroundBitmap = state.artwork ?: state.appIcon
    Box(
        modifier = modifier.background(Color(0xFF111315)),
        contentAlignment = Alignment.Center,
    ) {
        if (backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxSize(0.42f),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.68f),
                        0.46f to Color.Black.copy(alpha = 0.28f),
                        1f to Color.Black.copy(alpha = 0.78f),
                    ),
                ),
        )
    }
}
