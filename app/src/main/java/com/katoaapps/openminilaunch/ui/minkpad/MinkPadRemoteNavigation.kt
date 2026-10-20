package com.katoaapps.openminilaunch.ui.minkpad

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadAction
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadConsumerAction

@Composable
internal fun RemoteNavigationCluster(
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
                label = stringResource(R.string.minkpad_volume_short),
                upDescription = stringResource(R.string.minkpad_volume_up),
                downDescription = stringResource(R.string.minkpad_volume_down),
                enabled = mediaEnabled,
                width = rockerWidth,
                height = rockerHeight,
                onUp = { onConsumerAction(MinkPadConsumerAction.VOLUME_UP) },
                onDown = { onConsumerAction(MinkPadConsumerAction.VOLUME_DOWN) },
            )
            RemoteDirectionalPad(keyboardEnabled, dialSize, onDirection)
            RemoteRocker(
                label = stringResource(R.string.minkpad_channel_short),
                upDescription = stringResource(R.string.minkpad_channel_up),
                downDescription = stringResource(R.string.minkpad_channel_down),
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
                Icons.Default.KeyboardArrowUp,
                stringResource(R.string.minkpad_up),
                enabled,
                Modifier.align(Alignment.TopCenter).size(directionButtonSize),
            ) { onAction(MinkPadAction.UP) }
            RemoteDirectionButton(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                stringResource(R.string.minkpad_left),
                enabled,
                Modifier.align(Alignment.CenterStart).size(directionButtonSize),
            ) { onAction(MinkPadAction.LEFT) }
            FilledTonalButton(
                onClick = { onAction(MinkPadAction.ENTER) },
                enabled = enabled,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.align(Alignment.Center).size(okButtonSize),
            ) {
                Text(stringResource(R.string.minkpad_ok), fontWeight = FontWeight.Bold)
            }
            RemoteDirectionButton(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                stringResource(R.string.minkpad_right),
                enabled,
                Modifier.align(Alignment.CenterEnd).size(directionButtonSize),
            ) { onAction(MinkPadAction.RIGHT) }
            RemoteDirectionButton(
                Icons.Default.KeyboardArrowDown,
                stringResource(R.string.minkpad_down),
                enabled,
                Modifier.align(Alignment.BottomCenter).size(directionButtonSize),
            ) { onAction(MinkPadAction.DOWN) }
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
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .semantics { contentDescription = upDescription },
                contentPadding = PaddingValues(0.dp),
            ) { Text("+", style = MaterialTheme.typography.headlineSmall) }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onDown,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .semantics { contentDescription = downDescription },
                contentPadding = PaddingValues(0.dp),
            ) { Text("−", style = MaterialTheme.typography.headlineSmall) }
        }
    }
}
