package com.katoaapps.openminilaunch.ui.minkspace.music

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkspace.music.MusicPlaybackAvailability
import com.katoaapps.openminilaunch.features.minkspace.music.MusicPlaybackState
import com.katoaapps.openminilaunch.ui.theme.Dimens

@Composable
internal fun MusicHeading(
    state: MusicPlaybackState,
    expanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (state.availability) {
        MusicPlaybackAvailability.ACCESS_REQUIRED -> stringResource(R.string.music_access_required)
        MusicPlaybackAvailability.NOTHING_PLAYING -> stringResource(R.string.music_nothing_playing)
        MusicPlaybackAvailability.READY -> state.title.ifBlank { state.appName }
    }
    val subtitle = when (state.availability) {
        MusicPlaybackAvailability.ACCESS_REQUIRED -> stringResource(R.string.music_access_description)
        MusicPlaybackAvailability.NOTHING_PLAYING -> stringResource(R.string.music_start_playback_hint)
        MusicPlaybackAvailability.READY -> state.subtitle
    }
    val headingEnabled = expanded ||
        state.hasSession ||
        state.availability == MusicPlaybackAvailability.NOTHING_PLAYING

    Column(
        modifier = modifier.clickable(
            enabled = headingEnabled,
            onClickLabel = stringResource(
                when {
                    expanded -> R.string.music_collapse_player
                    state.hasSession -> R.string.music_expand_player
                    else -> R.string.music_open_music_app
                },
            ),
            onClick = {
                when {
                    expanded -> onExpandedChanged(false)
                    state.hasSession -> onExpandedChanged(true)
                    state.availability == MusicPlaybackAvailability.NOTHING_PLAYING -> onOpenPlayer()
                }
            },
        ),
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = if (expanded) Dimens.sp24 else Dimens.sp18,
            fontWeight = FontWeight.Bold,
            maxLines = if (expanded) 2 else 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle.ifBlank { " " },
            color = Color.White.copy(alpha = 0.78f),
            fontSize = if (expanded) Dimens.sp15 else Dimens.sp12,
            maxLines = if (expanded) 2 else 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (expanded || state.hasSession) {
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.size(Dimens.dp20),
            )
        } else {
            Spacer(Modifier.size(Dimens.dp20))
        }
    }
}

@Composable
internal fun PlayerAppIcon(
    state: MusicPlaybackState,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canOpen = state.hasSession || state.availability == MusicPlaybackAvailability.NOTHING_PLAYING
    val openDescription = stringResource(
        if (state.hasSession) R.string.music_open_player else R.string.music_open_music_app,
    )
    val sizeModifier = modifier
        .size(Dimens.dp44)
        .clip(CircleShape)
        .clickable(
            enabled = canOpen,
            onClickLabel = openDescription,
            onClick = onOpenPlayer,
        )
    val appIcon = state.appIcon

    if (appIcon != null) {
        Image(
            bitmap = appIcon.asImageBitmap(),
            contentDescription = openDescription,
            contentScale = ContentScale.Crop,
            modifier = sizeModifier,
        )
    } else {
        Box(sizeModifier, contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = openDescription,
                tint = Color.White,
                modifier = Modifier.size(Dimens.dp28),
            )
        }
    }
}
