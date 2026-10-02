@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.katoaapps.openminilaunch.ui.minkspace.music

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkspace.music.MusicPlaybackState
import com.katoaapps.openminilaunch.ui.theme.Dimens

@Composable
internal fun MusicControls(
    state: MusicPlaybackState,
    expanded: Boolean,
    onSeekBack: () -> Unit,
    onRestart: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlayback: () -> Unit,
    onOpenPlayer: () -> Unit,
    onHideForeground: () -> Unit,
    onSeekToFraction: (Float) -> Unit,
    onSeekForward: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (expanded) {
            HorizontalMediaProgress(
                progress = state.progress,
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                enabled = state.canSeek,
                onSeekToFraction = onSeekToFraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.dp10),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(if (expanded) Dimens.dp28 else Dimens.dp18),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MultiTapControl(
                icon = Icons.Default.Replay10,
                description = stringResource(R.string.music_back_control_description),
                enabled = state.hasSession && (state.canSeek || state.canSkipPrevious),
                onSingleTap = onSeekBack,
                onDoubleTap = onRestart,
                onTripleTap = onPrevious,
            )
            PlaybackControl(
                state = state,
                showCircularProgress = !expanded,
                onTogglePlayback = onTogglePlayback,
                onOpenPlayer = onOpenPlayer,
                onHideForeground = onHideForeground,
            )
            MultiTapControl(
                icon = Icons.Default.Forward10,
                description = stringResource(R.string.music_forward_control_description),
                enabled = state.hasSession && (state.canSeek || state.canSkipNext || state.canShuffle),
                onSingleTap = onSeekForward,
                onDoubleTap = onNext,
                onTripleTap = onShuffle,
            )
        }
    }
}

@Composable
private fun MultiTapControl(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onSingleTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onTripleTap: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(Dimens.dp48)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.42f))
            .multiTapClickable(
                enabled = enabled,
                accessibilityClickLabel = description,
                onSingleTap = onSingleTap,
                onDoubleTap = onDoubleTap,
                onTripleTap = onTripleTap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(Dimens.dp26),
        )
    }
}

@Composable
private fun PlaybackControl(
    state: MusicPlaybackState,
    showCircularProgress: Boolean,
    onTogglePlayback: () -> Unit,
    onOpenPlayer: () -> Unit,
    onHideForeground: () -> Unit,
) {
    val canTogglePlayback = state.hasSession && state.canTogglePlayback
    val description = stringResource(
        when {
            !canTogglePlayback -> R.string.music_open_player
            state.isPlaying -> R.string.music_pause
            else -> R.string.music_play
        },
    )
    Box(
        modifier = Modifier
            .size(if (showCircularProgress) Dimens.dp64 else Dimens.dp58)
            .alpha(if (canTogglePlayback) 1f else 0.5f)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.96f))
            .combinedClickable(
                enabled = state.hasSession,
                onClickLabel = description,
                onLongClickLabel = stringResource(R.string.music_hide_controls),
                onClick = if (canTogglePlayback) onTogglePlayback else onOpenPlayer,
                onLongClick = onHideForeground,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (showCircularProgress) {
            CircularMediaProgress(
                progress = state.progress,
                modifier = Modifier.matchParentSize(),
            )
        }
        Icon(
            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(Dimens.dp32),
        )
    }
}
