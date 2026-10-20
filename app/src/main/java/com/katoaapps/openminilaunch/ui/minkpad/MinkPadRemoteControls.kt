package com.katoaapps.openminilaunch.ui.minkpad

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkpad.MinkPadRepository
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadAction
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadConsumerAction

/** Native HID keyboard navigation and consumer-page media controls. */
@Composable
internal fun MinkPadRemoteControls(repository: MinkPadRepository, keyboardEnabled: Boolean, mediaEnabled: Boolean) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.minkpad_tv_remote), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.minkpad_remote_description), style = MaterialTheme.typography.bodySmall)

        RemoteNavigationCluster(
            keyboardEnabled = keyboardEnabled,
            mediaEnabled = mediaEnabled,
            onDirection = repository::perform,
            onConsumerAction = { repository.consumer(it.usage) },
        )

        RemoteActionControls(
            backEnabled = mediaEnabled || keyboardEnabled,
            mediaEnabled = mediaEnabled,
            onBack = {
                if (mediaEnabled) repository.consumer(MinkPadConsumerAction.BACK.usage)
                else repository.perform(MinkPadAction.ESCAPE)
            },
            onConsumerAction = { repository.consumer(it.usage) },
        )

        if (!mediaEnabled) Text(
            stringResource(R.string.minkpad_media_unavailable),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(stringResource(R.string.minkpad_remote_keyboard_note), style = MaterialTheme.typography.bodySmall)
    }
}
@Composable
private fun RemoteActionControls(
    backEnabled: Boolean,
    mediaEnabled: Boolean,
    onBack: () -> Unit,
    onConsumerAction: (MinkPadConsumerAction) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 340.dp
        val buttonSize = if (compact) 44.dp else 50.dp
        val primaryButtonSize = if (compact) 54.dp else 62.dp
        val spacing = if (compact) 5.dp else 8.dp

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteActionButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                description = stringResource(R.string.back),
                enabled = backEnabled,
                size = buttonSize,
                onClick = onBack,
            )
            RemoteActionButton(
                icon = Icons.AutoMirrored.Filled.VolumeOff,
                description = stringResource(R.string.minkpad_mute),
                enabled = mediaEnabled,
                size = buttonSize,
                onClick = { onConsumerAction(MinkPadConsumerAction.MUTE) },
            )
            RemoteActionButton(
                icon = Icons.Default.SkipPrevious,
                description = stringResource(R.string.minkpad_previous),
                enabled = mediaEnabled,
                size = buttonSize,
                onClick = { onConsumerAction(MinkPadConsumerAction.PREVIOUS) },
            )
            RemoteActionButton(
                icon = Icons.Default.PlayArrow,
                description = stringResource(R.string.minkpad_play_pause),
                enabled = mediaEnabled,
                size = primaryButtonSize,
                primary = true,
                onClick = { onConsumerAction(MinkPadConsumerAction.PLAY_PAUSE) },
            )
            RemoteActionButton(
                icon = Icons.Default.SkipNext,
                description = stringResource(R.string.minkpad_next),
                enabled = mediaEnabled,
                size = buttonSize,
                onClick = { onConsumerAction(MinkPadConsumerAction.NEXT) },
            )
        }
    }
}
@Composable
private fun RemoteActionButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    size: Dp,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    if (primary) {
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(size),
        ) {
            Icon(icon, contentDescription = description)
        }
    } else {
        FilledTonalIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(size),
        ) {
            Icon(icon, contentDescription = description)
        }
    }
}
