package com.katoaapps.openminilaunch.ui.minkpad

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.features.minkpad.MinkPadRepository
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadAction
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadConsumerAction

/** Native HID keyboard navigation and consumer-page media controls. */
@Composable
internal fun MinkPadRemoteControls(repository: MinkPadRepository, keyboardEnabled: Boolean, mediaEnabled: Boolean) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("TV remote", style = MaterialTheme.typography.titleLarge)
        Text("D-pad uses keyboard arrows and Enter. Back uses the standard TV/browser Back command.", style = MaterialTheme.typography.bodySmall)

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

        if (!mediaEnabled) Text("Media controls become available after the native HID connection is ready.", style = MaterialTheme.typography.bodySmall)
        Text("Keyboard entry remains available below. A television must accept Bluetooth HID accessories; this is not the Google TV network remote.", style = MaterialTheme.typography.bodySmall)
    }
}
@Composable
private fun RemoteNavigationCluster(
    keyboardEnabled: Boolean,
    mediaEnabled: Boolean,
    onDirection: (MinkPadAction) -> Unit,
    onConsumerAction: (MinkPadConsumerAction) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val rockerWidth = if (maxWidth < 340.dp) 56.dp else 64.dp
        val spacing = 8.dp
        val dialSize = (maxWidth - rockerWidth * 2 - spacing * 2).coerceAtMost(200.dp)
        val rockerHeight = dialSize * 0.82f

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteRocker(
                label = "VOL",
                upDescription = "Volume up",
                downDescription = "Volume down",
                enabled = mediaEnabled,
                width = rockerWidth,
                height = rockerHeight,
                onUp = { onConsumerAction(MinkPadConsumerAction.VOLUME_UP) },
                onDown = { onConsumerAction(MinkPadConsumerAction.VOLUME_DOWN) },
            )
            RemoteDirectionalPad(
                enabled = keyboardEnabled,
                size = dialSize,
                onAction = onDirection,
            )
            RemoteRocker(
                label = "CH",
                upDescription = "Channel up",
                downDescription = "Channel down",
                enabled = mediaEnabled,
                width = rockerWidth,
                height = rockerHeight,
                onUp = { onConsumerAction(MinkPadConsumerAction.CHANNEL_UP) },
                onDown = { onConsumerAction(MinkPadConsumerAction.CHANNEL_DOWN) },
            )
        }
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
                description = "Back",
                enabled = backEnabled,
                size = buttonSize,
                onClick = onBack,
            )
            RemoteActionButton(
                icon = Icons.AutoMirrored.Filled.VolumeOff,
                description = "Mute",
                enabled = mediaEnabled,
                size = buttonSize,
                onClick = { onConsumerAction(MinkPadConsumerAction.MUTE) },
            )
            RemoteActionButton(
                icon = Icons.Default.SkipPrevious,
                description = "Previous",
                enabled = mediaEnabled,
                size = buttonSize,
                onClick = { onConsumerAction(MinkPadConsumerAction.PREVIOUS) },
            )
            RemoteActionButton(
                icon = Icons.Default.PlayArrow,
                description = "Play or pause",
                enabled = mediaEnabled,
                size = primaryButtonSize,
                primary = true,
                onClick = { onConsumerAction(MinkPadConsumerAction.PLAY_PAUSE) },
            )
            RemoteActionButton(
                icon = Icons.Default.SkipNext,
                description = "Next",
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

@Composable
private fun RemoteDirectionalPad(
    enabled: Boolean,
    size: Dp,
    onAction: (MinkPadAction) -> Unit,
) {
    val compact = size < 176.dp
    val directionButtonSize = if (compact) 42.dp else 48.dp
    val okButtonSize = if (compact) 52.dp else 62.dp

    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        tonalElevation = 3.dp,
        shadowElevation = 5.dp,
    ) {
        Box(Modifier.fillMaxSize().padding(8.dp)) {
            RemoteDirectionButton(
                icon = Icons.Default.KeyboardArrowUp,
                description = "Up",
                enabled = enabled,
                modifier = Modifier.align(Alignment.TopCenter).size(directionButtonSize),
                onClick = { onAction(MinkPadAction.UP) },
            )
            RemoteDirectionButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                description = "Left",
                enabled = enabled,
                modifier = Modifier.align(Alignment.CenterStart).size(directionButtonSize),
                onClick = { onAction(MinkPadAction.LEFT) },
            )
            FilledTonalButton(
                onClick = { onAction(MinkPadAction.ENTER) },
                enabled = enabled,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.align(Alignment.Center).size(okButtonSize),
            ) {
                Text("OK", fontWeight = FontWeight.Bold)
            }
            RemoteDirectionButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                description = "Right",
                enabled = enabled,
                modifier = Modifier.align(Alignment.CenterEnd).size(directionButtonSize),
                onClick = { onAction(MinkPadAction.RIGHT) },
            )
            RemoteDirectionButton(
                icon = Icons.Default.KeyboardArrowDown,
                description = "Down",
                enabled = enabled,
                modifier = Modifier.align(Alignment.BottomCenter).size(directionButtonSize),
                onClick = { onAction(MinkPadAction.DOWN) },
            )
        }
    }
}

@Composable
private fun RemoteDirectionButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(icon, contentDescription = description)
    }
}

@Composable
private fun RemoteRocker(
    label: String,
    upDescription: String,
    downDescription: String,
    enabled: Boolean,
    width: Dp,
    height: Dp,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    Surface(
        modifier = Modifier.width(width).height(height),
        shape = RoundedCornerShape(width / 2),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        tonalElevation = 2.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TextButton(
                onClick = onUp,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .semantics { contentDescription = upDescription },
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onDown,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .semantics { contentDescription = downDescription },
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("−", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}
